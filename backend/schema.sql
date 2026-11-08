-- ====================================================================
-- LeaveDesk Enterprise MySQL Database Schema & Initial Data
-- Version: 1.0.0
-- Compatible with MySQL 8.0+ and MariaDB 10.5+
-- ====================================================================

CREATE DATABASE IF NOT EXISTS leavedesk_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE leavedesk_db;

-- 1. TEAMS / DEPARTMENTS TABLE
CREATE TABLE IF NOT EXISTS teams (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  tl_user_id VARCHAR(64),
  tl_name VARCHAR(128),
  min_present_threshold INT NOT NULL DEFAULT 3,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. USERS & CREDENTIALS TABLE (3 Core Roles: CEO, TEAM_LEAD, EMPLOYEE)
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

-- 3. USER LEAVE QUOTAS TABLE
CREATE TABLE IF NOT EXISTS user_quotas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  leave_type VARCHAR(16) NOT NULL,
  total_quota DECIMAL(5,1) NOT NULL DEFAULT 0.0,
  used_quota DECIMAL(5,1) NOT NULL DEFAULT 0.0,
  is_no_cap BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_leave (user_id, leave_type),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. COMPANY HOLIDAYS & CEO LOCKS TABLE
CREATE TABLE IF NOT EXISTS holidays (
  id INT AUTO_INCREMENT PRIMARY KEY,
  date DATE NOT NULL UNIQUE,
  title VARCHAR(128) NOT NULL,
  is_locked_by_ceo BOOLEAN NOT NULL DEFAULT FALSE,
  lock_reason VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_holiday_date (date)
) ENGINE=InnoDB;

-- 5. LEAVE REQUESTS TABLE (State Machine: PENDING, APPROVED, REJECTED, CANCELLED)
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
  INDEX idx_req_dates (start_date, end_date),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. IMMUTABLE AUDIT LOGS TABLE
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

-- 7. REAL-TIME CHAT & NOTIFICATIONS
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

-- ====================================================================
-- SEED INITIAL DATA: TEAMS, USERS, QUOTAS, HOLIDAYS
-- ====================================================================

-- Insert Teams
INSERT INTO teams (id, name, tl_user_id, tl_name, min_present_threshold) VALUES
('g_eng', 'Engineering & Product', 'u_tl1', 'Marcus Vance', 3),
('g_ops', 'Design & Operations', 'u_tl2', 'Elena Rostova', 2)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert 3 Core Roles: CEO (CEO001), Team Lead (TL201), Employee (EMP101)
INSERT INTO users (id, employee_id, name, email, password_hash, role, group_id, group_name, designation, avatar_initials) VALUES
('u_ceo',  'CEO001', 'Robert Sterling', 'robert.sterling@leavedesk.corp', 'password123', 'CEO',       'g_exec', 'Executive Office',    'Chief Executive Officer',  'RS'),
('u_tl1',  'TL201',  'Marcus Vance',    'marcus.v@leavedesk.corp',       'password123', 'TEAM_LEAD', 'g_eng',  'Engineering & Product', 'Engineering Lead',         'MV'),
('u_tl2',  'TL202',  'Elena Rostova',   'elena.r@leavedesk.corp',        'password123', 'TEAM_LEAD', 'g_ops',  'Design & Operations',   'Operations Lead',          'ER'),
('u_emp1', 'EMP101', 'Alex Chen',       'alex.chen@leavedesk.corp',      'password123', 'EMPLOYEE',  'g_eng',  'Engineering & Product', 'Senior Android Engineer',   'AC'),
('u_emp2', 'EMP102', 'Sarah Jenkins',   'sarah.j@leavedesk.corp',        'password123', 'EMPLOYEE',  'g_eng',  'Engineering & Product', 'Backend Architect',        'SJ'),
('u_emp3', 'EMP103', 'David Kim',       'david.kim@leavedesk.corp',      'password123', 'EMPLOYEE',  'g_eng',  'Engineering & Product', 'QA Specialist',           'DK'),
('u_emp4', 'EMP104', 'Priya Patel',     'priya.p@leavedesk.corp',        'password123', 'EMPLOYEE',  'g_eng',  'Engineering & Product', 'Fullstack Developer',      'PP')
ON DUPLICATE KEY UPDATE name=VALUES(name), password_hash=VALUES(password_hash);

-- Insert Leave Quotas for EMP101
INSERT INTO user_quotas (user_id, leave_type, total_quota, used_quota, is_no_cap) VALUES
('u_emp1', 'CL', 12, 2, FALSE),
('u_emp1', 'SL', 10, 1, FALSE),
('u_emp1', 'EL', 15, 4, FALSE),
('u_emp1', 'ML', 90, 0, FALSE),
('u_emp1', 'BL', 5, 0, FALSE),
('u_emp1', 'LOP', 0, 0, TRUE),
('u_emp1', 'HD', 8, 1, FALSE),
('u_emp1', 'WFH', 0, 3, TRUE),
('u_emp1', 'SHORT', 12, 2, FALSE)
ON DUPLICATE KEY UPDATE total_quota=VALUES(total_quota), used_quota=VALUES(used_quota);

-- Insert Leave Quotas for TL201 (Marcus Vance)
INSERT INTO user_quotas (user_id, leave_type, total_quota, used_quota, is_no_cap) VALUES
('u_tl1', 'CL', 12, 3, FALSE),
('u_tl1', 'SL', 10, 0, FALSE),
('u_tl1', 'EL', 15, 5, FALSE),
('u_tl1', 'ML', 90, 0, FALSE),
('u_tl1', 'BL', 5, 0, FALSE),
('u_tl1', 'LOP', 0, 0, TRUE),
('u_tl1', 'HD', 8, 0, FALSE),
('u_tl1', 'WFH', 0, 2, TRUE),
('u_tl1', 'SHORT', 12, 1, FALSE)
ON DUPLICATE KEY UPDATE total_quota=VALUES(total_quota), used_quota=VALUES(used_quota);

-- Insert Leave Quotas for CEO001 (Robert Sterling)
INSERT INTO user_quotas (user_id, leave_type, total_quota, used_quota, is_no_cap) VALUES
('u_ceo', 'CL', 15, 0, FALSE),
('u_ceo', 'SL', 15, 0, FALSE),
('u_ceo', 'EL', 20, 2, FALSE),
('u_ceo', 'ML', 90, 0, FALSE),
('u_ceo', 'BL', 5, 0, FALSE),
('u_ceo', 'LOP', 0, 0, TRUE),
('u_ceo', 'HD', 10, 0, FALSE),
('u_ceo', 'WFH', 0, 0, TRUE),
('u_ceo', 'SHORT', 20, 0, FALSE)
ON DUPLICATE KEY UPDATE total_quota=VALUES(total_quota), used_quota=VALUES(used_quota);

-- Insert Initial Company Holidays
INSERT INTO holidays (date, title, is_locked_by_ceo, lock_reason) VALUES
('2026-11-26', 'Thanksgiving Day', TRUE, 'Company-Wide Thanksgiving Holiday'),
('2026-11-27', 'Day After Thanksgiving', TRUE, 'Extended Holiday'),
('2026-12-25', 'Christmas Day', TRUE, 'Christmas Holiday'),
('2027-01-01', 'New Year\'s Day', TRUE, 'New Year Celebration')
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- Insert Demo Pending Leave Request
INSERT INTO leave_requests (
  id, user_id, user_name, user_role, group_id, group_name,
  leave_type, start_date, end_date, working_days, is_half_day,
  reason, backup_user_id, backup_user_name, status
) VALUES (
  'LR-7892', 'u_emp1', 'Alex Chen', 'EMPLOYEE', 'g_eng', 'Engineering & Product',
  'CL', '2026-11-16', '2026-11-17', 2, FALSE,
  'Personal family commitment and travel out of town.', 'u_emp2', 'Sarah Jenkins', 'PENDING'
) ON DUPLICATE KEY UPDATE status=VALUES(status);

-- Insert System Initial Audit Log
INSERT INTO audit_logs (id, actor_name, actor_role, action, target, reason, details) VALUES
('AUD-1001', 'Robert Sterling', 'CEO', 'SYSTEM_INIT', 'MySQL Cluster', 'Production MySQL Engine Initialized', 'Schema migrated and 3 primary roles (CEO001, TL201, EMP101) verified.')
ON DUPLICATE KEY UPDATE action=VALUES(action);
