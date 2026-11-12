/**
 * LeaveDesk Production MySQL Database Adapter
 * Powered by mysql2/promise with connection pooling and prepared statements.
 * Features automated schema provisioning for:
 * 1. users
 * 2. user_quotas
 * 3. leave_requests
 * 4. holidays
 * 5. activity_logs
 * 6. audit_logs
 * 7. chat_messages
 * 8. teams
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
      user: process.env.MYSQL_USER || 'leavedesk_app',
      password: process.env.MYSQL_PASSWORD || '4235',
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
      // 1. Initialize connection pool directly with configured credentials
      this.pool = mysql.createPool(this.config);
      await this.pool.query('SELECT 1');
      this.connected = true;
      console.log(`🐬 [MySQL] Successfully connected to database '${this.config.database}' at ${this.config.host}:${this.config.port} (User: ${this.config.user})`);

      // 2. Ensure all 8 production tables exist
      await this.migrateTables();

      // 3. Ensure seed data exists
      await this.seedInitialData();

      return true;
    } catch (err) {
      this.connected = false;
      console.error(`❌ [MySQL] Fatal Connection Error: ${err.message}`);
      console.log(`ℹ️ [MySQL] Please verify that MySQL is running on ${this.config.host}:${this.config.port} and user '${this.config.user}' exists with database '${this.config.database}'.`);
      return false;
    }
  }

  async migrateTables() {
    if (!this.pool) throw new Error("Database pool not initialized");

    // 1. TEAMS TABLE
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

    // 2. USERS TABLE
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
        INDEX idx_users_email (email),
        INDEX idx_users_group (group_id)
      ) ENGINE=InnoDB;
    `);

    // 3. USER LEAVE QUOTAS TABLE
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

    // 4. HOLIDAYS & CEO LOCKS TABLE
    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS holidays (
        id INT AUTO_INCREMENT PRIMARY KEY,
        date DATE NOT NULL UNIQUE,
        title VARCHAR(128) NOT NULL,
        is_locked_by_ceo BOOLEAN NOT NULL DEFAULT FALSE,
        lock_reason VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_holiday_date (date)
      ) ENGINE=InnoDB;
    `);

    // 5. LEAVE REQUESTS TABLE
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
        INDEX idx_req_status (status),
        INDEX idx_req_dates (start_date, end_date)
      ) ENGINE=InnoDB;
    `);

    // 6. REQUEST ACTIVITY LOGS TABLE (Automated middleware writes here)
    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS activity_logs (
        id CHAR(36) PRIMARY KEY,
        request_id CHAR(36) UNIQUE,
        service VARCHAR(64) NOT NULL,
        actor_user_id VARCHAR(64),
        actor_role VARCHAR(32),
        action VARCHAR(64) NOT NULL,
        resource_type VARCHAR(64) NOT NULL,
        outcome ENUM('SUCCESS', 'FAILURE') NOT NULL,
        http_method VARCHAR(10) NOT NULL,
        route VARCHAR(255) NOT NULL,
        status_code SMALLINT NOT NULL,
        duration_ms INT NOT NULL,
        details JSON,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_act_created (created_at),
        INDEX idx_act_actor (actor_user_id),
        INDEX idx_act_action (action)
      ) ENGINE=InnoDB;
    `);

    // 7. BUSINESS AUDIT LOGS TABLE (Logins, approvals, locks, cancellations)
    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS audit_logs (
        id VARCHAR(64) PRIMARY KEY,
        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        actor_name VARCHAR(128) NOT NULL,
        actor_role VARCHAR(32) NOT NULL,
        action VARCHAR(64) NOT NULL,
        target VARCHAR(128) NOT NULL,
        reason TEXT,
        details TEXT,
        INDEX idx_audit_timestamp (timestamp),
        INDEX idx_audit_action (action)
      ) ENGINE=InnoDB;
    `);

    // 8. CHAT MESSAGES TABLE
    await this.pool.query(`
      CREATE TABLE IF NOT EXISTS chat_messages (
        id VARCHAR(64) PRIMARY KEY,
        sender_id VARCHAR(64) NOT NULL,
        sender_name VARCHAR(128) NOT NULL,
        sender_role VARCHAR(32) NOT NULL,
        message TEXT NOT NULL,
        thread_id VARCHAR(64) NOT NULL DEFAULT 'general',
        related_request_id VARCHAR(64) NULL,
        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_chat_thread (thread_id)
      ) ENGINE=InnoDB;
    `);

    console.log('✅ [MySQL] All 8 tables migrated and verified.');
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
      ('2027-01-01', 'New Year\\'s Day', TRUE, 'New Year Celebration');
    `);

    // Initial Audit Log
    await this.pool.query(`
      INSERT INTO audit_logs (id, actor_name, actor_role, action, target, reason, details) VALUES
      ('AUD-1001', 'Robert Sterling', 'CEO', 'SYSTEM_INIT', 'MySQL Cluster', 'Production MySQL Engine Initialized', 'Schema migrated and 3 primary roles (CEO001, TL201, EMP101) seeded.');
    `);

    console.log('✅ [MySQL] Initial seed completed successfully.');
  }

  // --- TRANSACTION SUPPORT ---
  async executeTransaction(actionFn) {
    if (!this.pool) throw new Error("Database pool is not active");
    const connection = await this.pool.getConnection();
    try {
      await connection.beginTransaction();
      const result = await actionFn(connection);
      await connection.commit();
      return result;
    } catch (sqlErr) {
      await connection.rollback();
      console.error("❌ [MySQL Transaction Rollback Error]:", sqlErr.sqlMessage || sqlErr.message, sqlErr);
      throw sqlErr;
    } finally {
      connection.release();
    }
  }

  // --- QUERY & WRITE OPERATIONS (STRICT PARAMETERIZED SQL) ---

  async findUserByIdentifier(identifier) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      const clean = identifier.trim().toLowerCase();
      const [rows] = await this.pool.query(
        `SELECT id, employee_id as employeeId, name, email, password_hash as password, role,
                group_id as groupId, group_name as groupName, designation, avatar_initials as avatarInitials
         FROM users WHERE LOWER(employee_id) = ? OR LOWER(email) = ? LIMIT 1`,
        [clean, clean]
      );
      return rows[0] || null;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in findUserByIdentifier]:", sqlErr.sqlMessage || sqlErr.message, "Identifier:", identifier);
      throw sqlErr;
    }
  }

  async getUser(id) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      const [rows] = await this.pool.query(
        `SELECT id, employee_id as employeeId, name, email, password_hash as password, role,
                group_id as groupId, group_name as groupName, designation, avatar_initials as avatarInitials
         FROM users WHERE id = ? LIMIT 1`,
        [id]
      );
      return rows[0] || null;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getUser]:", sqlErr.sqlMessage || sqlErr.message, "ID:", id);
      throw sqlErr;
    }
  }

  async getAllUsers() {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      const [rows] = await this.pool.query(
        `SELECT id, employee_id as employeeId, name, email, role,
                group_id as groupId, group_name as groupName, designation, avatar_initials as avatarInitials
         FROM users ORDER BY name ASC`
      );
      return rows;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getAllUsers]:", sqlErr.sqlMessage || sqlErr.message);
      throw sqlErr;
    }
  }

  async getTeamMembers(groupId) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      const [rows] = await this.pool.query(
        `SELECT id, employee_id as employeeId, name, email, role,
                group_id as groupId, group_name as groupName, designation, avatar_initials as avatarInitials
         FROM users WHERE group_id = ? ORDER BY name ASC`,
        [groupId]
      );
      return rows;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getTeamMembers]:", sqlErr.sqlMessage || sqlErr.message, "GroupId:", groupId);
      throw sqlErr;
    }
  }

  async getGroup(id) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
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
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getGroup]:", sqlErr.sqlMessage || sqlErr.message, "ID:", id);
      throw sqlErr;
    }
  }

  async getQuotas(userId) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      const [rows] = await this.pool.query(
        `SELECT leave_type as leaveType, total_quota as total, used_quota as used, is_no_cap as isNoCap
         FROM user_quotas WHERE user_id = ?`,
        [userId]
      );
      return rows;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getQuotas]:", sqlErr.sqlMessage || sqlErr.message, "UserId:", userId);
      throw sqlErr;
    }
  }

  async deductQuota(userId, leaveType, days) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      await this.pool.query(
        `UPDATE user_quotas SET used_quota = used_quota + ? WHERE user_id = ? AND leave_type = ? AND is_no_cap = FALSE`,
        [days, userId, leaveType]
      );
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in deductQuota]:", sqlErr.sqlMessage || sqlErr.message, "UserId:", userId, "Type:", leaveType);
      throw sqlErr;
    }
  }

  async restoreQuota(userId, leaveType, days) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      await this.pool.query(
        `UPDATE user_quotas SET used_quota = GREATEST(0, used_quota - ?) WHERE user_id = ? AND leave_type = ? AND is_no_cap = FALSE`,
        [days, userId, leaveType]
      );
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in restoreQuota]:", sqlErr.sqlMessage || sqlErr.message, "UserId:", userId, "Type:", leaveType);
      throw sqlErr;
    }
  }

  async getLeaveRequests(filter = {}) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
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
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getLeaveRequests]:", sqlErr.sqlMessage || sqlErr.message, "Filter:", filter);
      throw sqlErr;
    }
  }

  async createLeaveRequest(data) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      await this.pool.query(
        `INSERT INTO leave_requests (
          id, user_id, user_name, user_role, group_id, group_name,
          leave_type, start_date, end_date, working_days, is_half_day,
          reason, backup_user_id, backup_user_name, backup_note, proof_file_name, status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          user_id = VALUES(user_id), user_name = VALUES(user_name), leave_type = VALUES(leave_type),
          start_date = VALUES(start_date), end_date = VALUES(end_date), working_days = VALUES(working_days),
          status = VALUES(status)`,
        [
          String(data.id || '').trim(),
          data.userId,
          data.userName || 'Employee',
          data.userRole || 'EMPLOYEE',
          data.groupId || 'g_eng',
          data.groupName || 'Engineering',
          data.leaveType,
          data.startDate,
          data.endDate,
          data.workingDays,
          data.isHalfDay ? 1 : 0,
          data.reason || 'General Leave',
          data.backupUserId || null,
          data.backupUserName || null,
          data.backupNote || null,
          data.proofFileName || null,
          data.status || 'PENDING'
        ]
      );
      return data;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in createLeaveRequest]:", sqlErr.sqlMessage || sqlErr.message, "Data payload:", data);
      throw sqlErr;
    }
  }

  async updateLeaveDecision(id, updateData) {
    if (!this.pool) throw new Error("Database pool is not active");
    const fields = [];
    const values = [];

    if (updateData.status) { fields.push('status = ?'); values.push(updateData.status); }
    if (updateData.tlDecision) { fields.push('tl_decision = ?'); values.push(updateData.tlDecision); }
    if (updateData.tlComment) { fields.push('tl_comment = ?'); values.push(updateData.tlComment); }
    if (updateData.ceoDecision) { fields.push('ceo_decision = ?'); values.push(updateData.ceoDecision); }
    if (updateData.ceoComment) { fields.push('ceo_comment = ?'); values.push(updateData.ceoComment); }
    if (updateData.cancellationReason) { fields.push('cancellation_reason = ?'); values.push(updateData.cancellationReason); }

    const cleanId = String(id || '').trim();
    const altId = cleanId.toLowerCase().startsWith('lr-') ? cleanId.substring(3) : ('LR-' + cleanId);
    values.push(cleanId, altId);

    try {
      if (fields.length > 0) {
        await this.pool.query(`UPDATE leave_requests SET ${fields.join(', ')} WHERE id = ? OR id = ?`, values);
      }
      const [rows] = await this.pool.query(`SELECT * FROM leave_requests WHERE id = ? OR id = ? LIMIT 1`, [cleanId, altId]);
      return rows[0] || null;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in updateLeaveDecision]:", sqlErr.sqlMessage || sqlErr.message, "Fields:", fields, "Values:", values);
      throw sqlErr;
    }
  }

  async getHolidays() {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      const [rows] = await this.pool.query(`
        SELECT id, DATE_FORMAT(date, '%Y-%m-%d') as date, title,
               is_locked_by_ceo as isLockedByCeo, lock_reason as lockReason
        FROM holidays ORDER BY date ASC
      `);
      return rows;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getHolidays]:", sqlErr.message);
      throw sqlErr;
    }
  }

  async setHolidayLock(date, reason, isLocked) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      await this.pool.query(`
        INSERT INTO holidays (date, title, is_locked_by_ceo, lock_reason)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE is_locked_by_ceo = VALUES(is_locked_by_ceo), lock_reason = VALUES(lock_reason)
      `, [date, reason || 'Executive Lock', isLocked ? 1 : 0, isLocked ? (reason || 'Locked by CEO') : null]);

      const [rows] = await this.pool.query(`SELECT * FROM holidays WHERE date = ?`, [date]);
      return rows[0] || null;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in setHolidayLock]:", sqlErr.message, "Date:", date);
      throw sqlErr;
    }
  }

  async getChatMessages(threadId = 'general') {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      const [rows] = await this.pool.query(`
        SELECT id, sender_id as senderId, sender_name as senderName, sender_role as senderRole,
               message, thread_id as threadId, related_request_id as relatedRequestId, timestamp
        FROM chat_messages WHERE thread_id = ? ORDER BY timestamp ASC
      `, [threadId]);
      return rows;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getChatMessages]:", sqlErr.message, "ThreadId:", threadId);
      throw sqlErr;
    }
  }

  async addChatMessage(msg) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      await this.pool.query(`
        INSERT INTO chat_messages (id, sender_id, sender_name, sender_role, message, thread_id, related_request_id)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `, [
        msg.id,
        msg.senderId,
        msg.senderName || 'Corporate User',
        msg.senderRole || 'EMPLOYEE',
        msg.message || msg.text || 'Message',
        msg.threadId || 'general',
        msg.relatedRequestId || null
      ]);
      return msg;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in addChatMessage]:", sqlErr.message, "Payload:", msg);
      throw sqlErr;
    }
  }

  // --- ACTIVITY LOGS (MIDDLEWARE WRITES) ---
  async addActivityLog(data) {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      await this.pool.query(`
        INSERT INTO activity_logs (
          id, request_id, service, actor_user_id, actor_role, action,
          resource_type, outcome, http_method, route, status_code, duration_ms, details
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `, [
        data.id,
        data.requestId,
        data.service,
        data.actorUserId || null,
        data.actorRole || null,
        data.action,
        data.resourceType,
        data.outcome,
        data.httpMethod,
        data.route,
        data.statusCode,
        data.durationMs,
        typeof data.details === 'object' ? JSON.stringify(data.details) : data.details
      ]);
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in addActivityLog]:", sqlErr.message);
    }
  }

  async getActivityLogs() {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      const [rows] = await this.pool.query(
        `SELECT * FROM activity_logs ORDER BY created_at DESC LIMIT 100`
      );
      return rows;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getActivityLogs]:", sqlErr.message);
      throw sqlErr;
    }
  }

  // --- AUDIT LOGS (BUSINESS OPERATIONS & LOGINS) ---
  async getAuditLogs() {
    if (!this.pool) throw new Error("Database pool is not active");
    try {
      const [rows] = await this.pool.query(`
        SELECT id, timestamp, actor_name as actorName, actor_role as actorRole,
               action, target, reason, details
        FROM audit_logs ORDER BY timestamp DESC LIMIT 100
      `);
      return rows;
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in getAuditLogs]:", sqlErr.message);
      throw sqlErr;
    }
  }

  async addAudit(actorName, actorRole, action, target, reason, details) {
    if (!this.pool) throw new Error("Database pool is not active");
    const id = "AUD-" + Math.random().toString(36).substring(2, 8).toUpperCase();
    try {
      await this.pool.query(`
        INSERT INTO audit_logs (id, actor_name, actor_role, action, target, reason, details)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `, [
        id,
        actorName || 'System',
        actorRole || 'EMPLOYEE',
        action || 'ACTION',
        target || 'SYSTEM',
        reason || 'Standard operation',
        details || ''
      ]);
      return { id, timestamp: new Date().toISOString(), actorName, actorRole, action, target, reason, details };
    } catch (sqlErr) {
      console.error("❌ [MySQL Error in addAudit]:", sqlErr.message, "Actor:", actorName, "Action:", action);
      throw sqlErr;
    }
  }
}

module.exports = new MySQLDatabase();
