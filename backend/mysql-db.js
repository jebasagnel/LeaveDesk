/**
 * LeaveDesk MySQL Database Adapter
 * Powered by mysql2/promise for connection pooling and prepared statements.
 * Features automated schema provisioning and initial role seeding (CEO001, TL201, EMP101).
 */

require('dotenv').config();
const mysql = require('mysql2/promise');

class MySQLDatabase {
  constructor() {
    this.pool = null;
    this.connected = false;
    this.config = {
      host: process.env.MYSQL_HOST || 'localhost',
      port: parseInt(process.env.MYSQL_PORT || '3306', 10),
      user: process.env.MYSQL_USER || 'root',
      password: process.env.MYSQL_PASSWORD || '',
      database: process.env.MYSQL_DATABASE || 'leavedesk_db',
      waitForConnections: true,
      connectionLimit: 10,
      queueLimit: 0
    };
  }

  isAvailable() {
    return this.connected;
  }

  async initMySQL() {
    try {
      // 1. Test preliminary connection to server to ensure database exists
      const testConn = await mysql.createConnection({
        host: this.config.host,
        port: this.config.port,
        user: this.config.user,
        password: this.config.password
      });

      await testConn.query(`CREATE DATABASE IF NOT EXISTS \`${this.config.database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
      await testConn.end();

      // 2. Initialize connection pool targeting leavedesk_db
      this.pool = mysql.createPool(this.config);
      await this.pool.query('SELECT 1');
      this.connected = true;
      console.log(`🐬 [MySQL] Successfully connected to database '${this.config.database}' at ${this.config.host}:${this.config.port}`);

      // 3. Ensure tables exist
      await this.migrateTables();

      // 4. Ensure seed data exists
      await this.seedInitialData();

      return true;
    } catch (err) {
      this.connected = false;
      console.log(`⚠️  [MySQL] Connection check: ${err.message}`);
      console.log(`ℹ️  [MySQL] Operating in resilient fallback mode (in-memory state active). When MySQL is available, services auto-sync.`);
      return false;
    }
  }

  async migrateTables() {
    if (!this.pool) return;

    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS teams (
        id VARCHAR(64) PRIMARY KEY,
        name VARCHAR(128) NOT NULL,
        tl_user_id VARCHAR(64),
        tl_name VARCHAR(128),
        min_present_threshold INT NOT NULL DEFAULT 3,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      ) ENGINE=InnoDB;
    `);

    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS users (
        id VARCHAR(64) PRIMARY KEY,
        employee_id VARCHAR(32) NOT NULL UNIQUE,
        name VARCHAR(128) NOT NULL,
        email VARCHAR(128) NOT NULL UNIQUE,
        password_hash VARCHAR(255) NOT NULL,
        role ENUM('CEO', 'TEAM_LEAD', 'EMPLOYEE') NOT NULL,
        group_id VARCHAR(64),
        group_name VARCHAR(128),
        designation VARCHAR(128),
        avatar_initials VARCHAR(4),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_users_empid (employee_id),
        INDEX idx_users_email (email)
      ) ENGINE=InnoDB;
    `);

    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS user_quotas (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id VARCHAR(64) NOT NULL,
        leave_type VARCHAR(16) NOT NULL,
        total_quota DECIMAL(5,1) NOT NULL DEFAULT 0.0,
        used_quota DECIMAL(5,1) NOT NULL DEFAULT 0.0,
        is_no_cap BOOLEAN NOT NULL DEFAULT FALSE,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uk_user_leave (user_id, leave_type)
      ) ENGINE=InnoDB;
    `);

    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS holidays (
        id INT AUTO_INCREMENT PRIMARY KEY,
        date DATE NOT NULL UNIQUE,
        title VARCHAR(128) NOT NULL,
        is_locked_by_ceo BOOLEAN NOT NULL DEFAULT FALSE,
        lock_reason VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      ) ENGINE=InnoDB;
    `);

    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS leave_requests (
        id VARCHAR(64) PRIMARY KEY,
        user_id VARCHAR(64) NOT NULL,
        user_name VARCHAR(128) NOT NULL,
        user_role VARCHAR(32) NOT NULL,
        group_id VARCHAR(64),
        group_name VARCHAR(128),
        leave_type VARCHAR(16) NOT NULL,
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        working_days DECIMAL(5,1) NOT NULL,
        is_half_day BOOLEAN NOT NULL DEFAULT FALSE,
        reason TEXT NOT NULL,
        backup_user_id VARCHAR(64),
        backup_user_name VARCHAR(128),
        backup_note TEXT,
        proof_file_name VARCHAR(255),
        status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
        tl_decision ENUM('APPROVED', 'REJECTED', 'QUERY') NULL,
        tl_comment TEXT NULL,
        ceo_decision ENUM('APPROVED', 'REJECTED') NULL,
        ceo_comment TEXT NULL,
        cancellation_reason TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_req_user (user_id),
        INDEX idx_req_group (group_id),
        INDEX idx_req_status (status)
      ) ENGINE=InnoDB;
    `);

    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS audit_logs (
        id VARCHAR(64) PRIMARY KEY,
        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        actor_name VARCHAR(128) NOT NULL,
        actor_role VARCHAR(32) NOT NULL,
        action VARCHAR(64) NOT NULL,
        target VARCHAR(128) NOT NULL,
        reason TEXT,
        details TEXT
      ) ENGINE=InnoDB;
    `);

    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS chat_messages (
        id VARCHAR(64) PRIMARY KEY,
        sender_id VARCHAR(64) NOT NULL,
        sender_name VARCHAR(128) NOT NULL,
        sender_role VARCHAR(32) NOT NULL,
        message TEXT NOT NULL,
        thread_id VARCHAR(64) NOT NULL DEFAULT 'general',
        related_request_id VARCHAR(64) NULL,
        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      ) ENGINE=InnoDB;
    `);
  }

  async seedInitialData() {
    if (!this.pool) return;
    const [rows] = await this.pool.query('SELECT COUNT(*) AS total FROM users');
    if (rows[0].total > 0) return;

    console.log('🌱 [MySQL] Seeding initial tables with 3 Core Roles (CEO001, TL201, EMP101)...');

    // Teams
    await this.pool.query(`
      INSERT INTO teams (id, name, tl_user_id, tl_name, min_present_threshold) VALUES
      ('g_eng', 'Engineering & Product', 'u_tl1', 'Marcus Vance', 3),
      ('g_ops', 'Design & Operations', 'u_tl2', 'Elena Rostova', 2)
      ON DUPLICATE KEY UPDATE name=VALUES(name);
    `);

    // Users
    await this.pool.query(`
      INSERT INTO users (id, employee_id, name, email, password_hash, role, group_id, group_name, designation, avatar_initials) VALUES
      ('u_ceo',  'CEO001', 'Robert Sterling', 'robert.sterling@leavedesk.corp', 'password123', 'CEO',       'g_exec', 'Executive Office',    'Chief Executive Officer',  'RS'),
      ('u_tl1',  'TL201',  'Marcus Vance',    'marcus.v@leavedesk.corp',       'password123', 'TEAM_LEAD', 'g_eng',  'Engineering & Product', 'Engineering Lead',         'MV'),
      ('u_tl2',  'TL202',  'Elena Rostova',   'elena.r@leavedesk.corp',        'password123', 'TEAM_LEAD', 'g_ops',  'Design & Operations',   'Operations Lead',          'ER'),
      ('u_emp1', 'EMP101', 'Alex Chen',       'alex.chen@leavedesk.corp',      'password123', 'EMPLOYEE',  'g_eng',  'Engineering & Product', 'Senior Android Engineer',   'AC'),
      ('u_emp2', 'EMP102', 'Sarah Jenkins',   'sarah.j@leavedesk.corp',        'password123', 'EMPLOYEE',  'g_eng',  'Engineering & Product', 'Backend Architect',        'SJ'),
      ('u_emp3', 'EMP103', 'David Kim',       'david.kim@leavedesk.corp',      'password123', 'EMPLOYEE',  'g_eng',  'Engineering & Product', 'QA Specialist',           'DK'),
      ('u_emp4', 'EMP104', 'Priya Patel',     'priya.p@leavedesk.corp',        'password123', 'EMPLOYEE',  'g_eng',  'Engineering & Product', 'Fullstack Developer',      'PP')
      ON DUPLICATE KEY UPDATE name=VALUES(name);
    `);

    // Leave Quotas
    await this.pool.query(`
      INSERT INTO user_quotas (user_id, leave_type, total_quota, used_quota, is_no_cap) VALUES
      ('u_emp1', 'CL', 12, 2, FALSE), ('u_emp1', 'SL', 10, 1, FALSE), ('u_emp1', 'EL', 15, 4, FALSE),
      ('u_emp1', 'ML', 90, 0, FALSE), ('u_emp1', 'BL', 5, 0, FALSE), ('u_emp1', 'LOP', 0, 0, TRUE),
      ('u_emp1', 'HD', 8, 1, FALSE), ('u_emp1', 'WFH', 0, 3, TRUE), ('u_emp1', 'SHORT', 12, 2, FALSE),
      ('u_tl1', 'CL', 12, 3, FALSE), ('u_tl1', 'SL', 10, 0, FALSE), ('u_tl1', 'EL', 15, 5, FALSE),
      ('u_tl1', 'ML', 90, 0, FALSE), ('u_tl1', 'BL', 5, 0, FALSE), ('u_tl1', 'LOP', 0, 0, TRUE),
      ('u_tl1', 'HD', 8, 0, FALSE), ('u_tl1', 'WFH', 0, 2, TRUE), ('u_tl1', 'SHORT', 12, 1, FALSE),
      ('u_ceo', 'CL', 15, 0, FALSE), ('u_ceo', 'SL', 15, 0, FALSE), ('u_ceo', 'EL', 20, 2, FALSE),
      ('u_ceo', 'ML', 90, 0, FALSE), ('u_ceo', 'BL', 5, 0, FALSE), ('u_ceo', 'LOP', 0, 0, TRUE),
      ('u_ceo', 'HD', 10, 0, FALSE), ('u_ceo', 'WFH', 0, 0, TRUE), ('u_ceo', 'SHORT', 20, 0, FALSE);
    `);

    // Holidays
    await this.pool.query(`
      INSERT INTO holidays (date, title, is_locked_by_ceo, lock_reason) VALUES
      ('2026-11-26', 'Thanksgiving Day', TRUE, 'Company-Wide Thanksgiving Holiday'),
      ('2026-11-27', 'Day After Thanksgiving', TRUE, 'Extended Holiday'),
      ('2026-12-25', 'Christmas Day', TRUE, 'Christmas Holiday'),
      ('2027-01-01', 'New Year\'s Day', TRUE, 'New Year Celebration');
    `);

    // Initial Leave Request
    await this.pool.query(`
      INSERT INTO leave_requests (
        id, user_id, user_name, user_role, group_id, group_name,
        leave_type, start_date, end_date, working_days, is_half_day,
        reason, backup_user_id, backup_user_name, status
      ) VALUES (
        'LR-7892', 'u_emp1', 'Alex Chen', 'EMPLOYEE', 'g_eng', 'Engineering & Product',
        'CL', '2026-11-16', '2026-11-17', 2, FALSE,
        'Personal family commitment and travel out of town.', 'u_emp2', 'Sarah Jenkins', 'PENDING'
      );
    `);

    // Initial Audit Log
    await this.pool.query(`
      INSERT INTO audit_logs (id, actor_name, actor_role, action, target, reason, details) VALUES
      ('AUD-1001', 'Robert Sterling', 'CEO', 'SYSTEM_INIT', 'MySQL Cluster', 'Production MySQL Engine Initialized', 'Schema migrated and 3 primary roles (CEO001, TL201, EMP101) seeded.');
    `);

    console.log('✅ [MySQL] Initial seed completed successfully.');
  }

  // --- QUERY APIS ---

  async findUserByIdentifier(identifier) {
    if (!this.connected) return null;
    const clean = identifier.trim().toLowerCase();
    const [rows] = await this.pool.query(
      `SELECT id, employee_id as employeeId, name, email, password_hash as password, role,
              group_id as groupId, group_name as groupName, designation, avatar_initials as avatarInitials
       FROM users WHERE LOWER(employee_id) = ? OR LOWER(email) = ? LIMIT 1`,
      [clean, clean]
    );
    return rows[0] || null;
  }

  async getUser(id) {
    if (!this.connected) return null;
    const [rows] = await this.pool.query(
      `SELECT id, employee_id as employeeId, name, email, password_hash as password, role,
              group_id as groupId, group_name as groupName, designation, avatar_initials as avatarInitials
       FROM users WHERE id = ? LIMIT 1`,
      [id]
    );
    return rows[0] || null;
  }

  async getAllUsers() {
    if (!this.connected) return [];
    const [rows] = await this.pool.query(
      `SELECT id, employee_id as employeeId, name, email, role,
              group_id as groupId, group_name as groupName, designation, avatar_initials as avatarInitials
       FROM users ORDER BY name ASC`
    );
    return rows;
  }

  async getTeamMembers(groupId) {
    if (!this.connected) return [];
    const [rows] = await this.pool.query(
      `SELECT id, employee_id as employeeId, name, email, role,
              group_id as groupId, group_name as groupName, designation, avatar_initials as avatarInitials
       FROM users WHERE group_id = ? ORDER BY name ASC`,
      [groupId]
    );
    return rows;
  }

  async getGroup(id) {
    if (!this.connected) return null;
    const [teams] = await this.pool.query(`SELECT * FROM teams WHERE id = ? LIMIT 1`, [id]);
    if (!teams[0]) return null;
    const [members] = await this.pool.query(`SELECT id FROM users WHERE group_id = ?`, [id]);
    return {
      id: teams[0].id,
      name: teams[0].name,
      tlUserId: teams[0].tl_user_id,
      tlName: teams[0].tl_name,
      minPresentThreshold: teams[0].min_present_threshold,
      memberIds: members.map(m => m.id)
    };
  }

  async getQuotas(userId) {
    if (!this.connected) return [];
    const [rows] = await this.pool.query(
      `SELECT leave_type as leaveType, total_quota as total, used_quota as used, is_no_cap as isNoCap
       FROM user_quotas WHERE user_id = ?`,
      [userId]
    );
    return rows;
  }

  async deductQuota(userId, leaveType, days) {
    if (!this.connected) return;
    await this.pool.query(
      `UPDATE user_quotas SET used_quota = used_quota + ? WHERE user_id = ? AND leave_type = ? AND is_no_cap = FALSE`,
      [days, userId, leaveType]
    );
  }

  async restoreQuota(userId, leaveType, days) {
    if (!this.connected) return;
    await this.pool.query(
      `UPDATE user_quotas SET used_quota = GREATEST(0, used_quota - ?) WHERE user_id = ? AND leave_type = ? AND is_no_cap = FALSE`,
      [days, userId, leaveType]
    );
  }

  async getLeaveRequests(filter = {}) {
    if (!this.connected) return [];
    let query = `
      SELECT id, user_id as userId, user_name as userName, user_role as userRole,
             group_id as groupId, group_name as groupName, leave_type as leaveType,
             DATE_FORMAT(start_date, '%Y-%m-%d') as startDate,
             DATE_FORMAT(end_date, '%Y-%m-%d') as endDate,
             working_days as workingDays, is_half_day as isHalfDay,
             reason, backup_user_id as backupUserId, backup_user_name as backupUserName,
             backup_note as backupNote, proof_file_name as proofFileName,
             status, tl_decision as tlDecision, tl_comment as tlComment,
             ceo_decision as ceoDecision, ceo_comment as ceoComment,
             cancellation_reason as cancellationReason, created_at as createdAt
      FROM leave_requests
    `;
    const params = [];
    const conditions = [];

    if (filter.userId) {
      conditions.push(`user_id = ?`);
      params.push(filter.userId);
    } else if (filter.groupId) {
      conditions.push(`group_id = ?`);
      params.push(filter.groupId);
    }

    if (conditions.length > 0) {
      query += ` WHERE ` + conditions.join(' AND ');
    }
    query += ` ORDER BY created_at DESC`;

    const [rows] = await this.pool.query(query, params);
    return rows;
  }

  async createLeaveRequest(data) {
    if (!this.connected) return null;
    await this.pool.query(
      `INSERT INTO leave_requests (
        id, user_id, user_name, user_role, group_id, group_name,
        leave_type, start_date, end_date, working_days, is_half_day,
        reason, backup_user_id, backup_user_name, backup_note, proof_file_name, status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')`,
      [
        data.id, data.userId, data.userName, data.userRole, data.groupId, data.groupName,
        data.leaveType, data.startDate, data.endDate, data.workingDays, data.isHalfDay ? 1 : 0,
        data.reason, data.backupUserId || null, data.backupUserName || null,
        data.backupNote || null, data.proofFileName || null
      ]
    );
    return data;
  }

  async updateLeaveDecision(id, updateData) {
    if (!this.connected) return null;
    const fields = [];
    const values = [];

    if (updateData.status) { fields.push('status = ?'); values.push(updateData.status); }
    if (updateData.tlDecision) { fields.push('tl_decision = ?'); values.push(updateData.tlDecision); }
    if (updateData.tlComment) { fields.push('tl_comment = ?'); values.push(updateData.tlComment); }
    if (updateData.ceoDecision) { fields.push('ceo_decision = ?'); values.push(updateData.ceoDecision); }
    if (updateData.ceoComment) { fields.push('ceo_comment = ?'); values.push(updateData.ceoComment); }
    if (updateData.cancellationReason) { fields.push('cancellation_reason = ?'); values.push(updateData.cancellationReason); }

    values.push(id);
    await this.pool.query(`UPDATE leave_requests SET ${fields.join(', ')} WHERE id = ?`, values);
    const [rows] = await this.pool.query(`SELECT * FROM leave_requests WHERE id = ?`, [id]);
    return rows[0] || null;
  }

  async getHolidays() {
    if (!this.connected) return [];
    const [rows] = await this.pool.query(`
      SELECT id, DATE_FORMAT(date, '%Y-%m-%d') as date, title,
             is_locked_by_ceo as isLockedByCeo, lock_reason as lockReason
      FROM holidays ORDER BY date ASC
    `);
    return rows;
  }

  async setHolidayLock(date, reason, isLocked) {
    if (!this.connected) return null;
    await this.pool.query(`
      INSERT INTO holidays (date, title, is_locked_by_ceo, lock_reason)
      VALUES (?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE is_locked_by_ceo = VALUES(is_locked_by_ceo), lock_reason = VALUES(lock_reason)
    `, [date, reason || 'Executive Lock', isLocked ? 1 : 0, isLocked ? (reason || 'Locked by CEO') : null]);

    const [rows] = await this.pool.query(`SELECT * FROM holidays WHERE date = ?`, [date]);
    return rows[0] || null;
  }

  async getChatMessages(threadId = 'general') {
    if (!this.connected) return [];
    const [rows] = await this.pool.query(`
      SELECT id, sender_id as senderId, sender_name as senderName, sender_role as senderRole,
             message, thread_id as threadId, related_request_id as relatedRequestId, timestamp
      FROM chat_messages WHERE thread_id = ? ORDER BY timestamp ASC
    `, [threadId]);
    return rows;
  }

  async addChatMessage(msg) {
    if (!this.connected) return null;
    await this.pool.query(`
      INSERT INTO chat_messages (id, sender_id, sender_name, sender_role, message, thread_id, related_request_id)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `, [msg.id, msg.senderId, msg.senderName, msg.senderRole, msg.message, msg.threadId || 'general', msg.relatedRequestId || null]);
    return msg;
  }

  async getAuditLogs() {
    if (!this.connected) return [];
    const [rows] = await this.pool.query(`
      SELECT id, timestamp, actor_name as actorName, actor_role as actorRole,
             action, target, reason, details
      FROM audit_logs ORDER BY timestamp DESC LIMIT 100
    `);
    return rows;
  }

  async addAudit(actorName, actorRole, action, target, reason, details) {
    const id = "AUD-" + Math.random().toString(36).substring(2, 8).toUpperCase();
    if (!this.connected) return { id, actorName, actorRole, action, target, reason, details };
    await this.pool.query(`
      INSERT INTO audit_logs (id, actor_name, actor_role, action, target, reason, details)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `, [id, actorName, actorRole, action, target, reason, details || '']);
    return { id, timestamp: new Date().toISOString(), actorName, actorRole, action, target, reason, details };
  }
}

module.exports = new MySQLDatabase();
