-- =====================================================================
-- LEAVEDESK ENTERPRISE MYSQL DATABASE INITIALIZATION SCRIPT
-- Database: leavedesk_db
-- User:     leavedesk_app
-- Password: 4235
-- Host:     localhost / 127.0.0.1
-- =====================================================================

-- 1. Create the Database if it doesn't already exist
CREATE DATABASE IF NOT EXISTS leavedesk_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE leavedesk_db;

-- 2. Create or Update the Application User and Grant Privileges
-- Works on MySQL 8.0+, MariaDB 10.3+, and MySQL 5.7
CREATE USER IF NOT EXISTS 'leavedesk_app'@'localhost' IDENTIFIED BY '4235';
ALTER USER 'leavedesk_app'@'localhost' IDENTIFIED BY '4235';
GRANT ALL PRIVILEGES ON leavedesk_db.* TO 'leavedesk_app'@'localhost';

-- Also grant for remote / container host if needed (e.g. Docker '%')
CREATE USER IF NOT EXISTS 'leavedesk_app'@'%' IDENTIFIED BY '4235';
ALTER USER 'leavedesk_app'@'%' IDENTIFIED BY '4235';
GRANT ALL PRIVILEGES ON leavedesk_db.* TO 'leavedesk_app'@'%';

FLUSH PRIVILEGES;

-- =====================================================================
-- 3. SCHEMA CREATION (InnoDB Engine with utf8mb4)
-- =====================================================================

-- Table 1: TEAMS / GROUPS
CREATE TABLE IF NOT EXISTS teams (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  tl_user_id VARCHAR(64),
  tl_name VARCHAR(128),
  min_present_threshold INT NOT NULL DEFAULT 3,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Table 2: USERS
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

-- Table 3: USER LEAVE QUOTAS
CREATE TABLE IF NOT EXISTS user_quotas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  leave_type VARCHAR(16) NOT NULL,
  total_quota DECIMAL(5,1) NOT NULL,
  used_quota DECIMAL(5,1) NOT NULL DEFAULT 0.0,
  is_no_cap BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_user_leave (user_id, leave_type),
  INDEX idx_quotas_user (user_id)
) ENGINE=InnoDB;

-- Table 4: COMPANY HOLIDAYS & CEO CALENDAR LOCKS
CREATE TABLE IF NOT EXISTS holidays (
  id INT AUTO_INCREMENT PRIMARY KEY,
  date DATE NOT NULL UNIQUE,
  title VARCHAR(128) NOT NULL,
  is_locked_by_ceo BOOLEAN NOT NULL DEFAULT FALSE,
  lock_reason VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Table 5: LEAVE REQUESTS
CREATE TABLE IF NOT EXISTS leave_requests (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  user_name VARCHAR(128) NOT NULL,
  user_role ENUM('CEO', 'TEAM_LEAD', 'EMPLOYEE') NOT NULL,
  group_id VARCHAR(64) NOT NULL,
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
  status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'QUERY') NOT NULL DEFAULT 'PENDING',
  tl_decision ENUM('APPROVED', 'REJECTED', 'QUERY', 'NONE') DEFAULT 'NONE',
  tl_comment TEXT,
  ceo_decision ENUM('APPROVED', 'REJECTED', 'QUERY', 'NONE') DEFAULT 'NONE',
  ceo_comment TEXT,
  cancellation_reason TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_lr_user (user_id),
  INDEX idx_lr_group (group_id),
  INDEX idx_lr_status (status)
) ENGINE=InnoDB;

-- Table 6: ACTIVITY LOGS (AUTOMATIC REQUEST MIDDLEWARE)
CREATE TABLE IF NOT EXISTS activity_logs (
  id CHAR(36) PRIMARY KEY,
  request_id CHAR(36) NOT NULL UNIQUE,
  service VARCHAR(64) NOT NULL,
  actor_user_id VARCHAR(64),
  actor_role VARCHAR(32),
  action VARCHAR(128) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  outcome ENUM('SUCCESS', 'FAILURE') NOT NULL,
  http_method VARCHAR(10) NOT NULL,
  route VARCHAR(255) NOT NULL,
  status_code SMALLINT NOT NULL,
  duration_ms INT NOT NULL,
  details JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_activity_created (created_at),
  INDEX idx_activity_actor (actor_user_id)
) ENGINE=InnoDB;

-- Table 7: AUDIT LOGS (BUSINESS OPERATIONS & USER LOGINS)
CREATE TABLE IF NOT EXISTS audit_logs (
  id VARCHAR(64) PRIMARY KEY,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  actor_name VARCHAR(128) NOT NULL,
  actor_role VARCHAR(32) NOT NULL,
  action VARCHAR(64) NOT NULL,
  target VARCHAR(128) NOT NULL,
  reason VARCHAR(255),
  details TEXT,
  INDEX idx_audit_time (timestamp),
  INDEX idx_audit_action (action)
) ENGINE=InnoDB;

-- Table 8: CORPORATE CHAT & QUERY PROOF MESSAGES
CREATE TABLE IF NOT EXISTS chat_messages (
  id VARCHAR(64) PRIMARY KEY,
  sender_id VARCHAR(64) NOT NULL,
  sender_name VARCHAR(128) NOT NULL,
  sender_role VARCHAR(32) NOT NULL,
  message TEXT NOT NULL,
  thread_id VARCHAR(128) NOT NULL,
  related_request_id VARCHAR(64),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_chat_thread (thread_id)
) ENGINE=InnoDB;

-- =====================================================================
-- 4. INITIAL SEED DATA
-- =====================================================================

INSERT IGNORE INTO teams (id, name, tl_user_id, tl_name, min_present_threshold) VALUES
('g_exec', 'Executive Office', 'u_ceo', 'Robert Sterling', 1),
('g_eng', 'Engineering & Product', 'u_tl1', 'Marcus Vance', 3),
('g_ops', 'Design & Operations', 'u_tl2', 'Elena Rostova', 3);

INSERT IGNORE INTO users (id, employee_id, name, email, password_hash, role, group_id, group_name, designation, avatar_initials) VALUES
('u_ceo', 'CEO001', 'Robert Sterling', 'robert.sterling@leavedesk.corp', 'password123', 'CEO', 'g_exec', 'Executive Office', 'Chief Executive Officer', 'RS'),
('u_tl1', 'TL201', 'Marcus Vance', 'marcus.v@leavedesk.corp', 'password123', 'TEAM_LEAD', 'g_eng', 'Engineering & Product', 'Engineering Lead', 'MV'),
('u_tl2', 'TL202', 'Elena Rostova', 'elena.r@leavedesk.corp', 'password123', 'TEAM_LEAD', 'g_ops', 'Design & Operations', 'Operations Lead', 'ER'),
('u_emp1', 'EMP101', 'Alex Chen', 'alex.chen@leavedesk.corp', 'password123', 'EMPLOYEE', 'g_eng', 'Engineering & Product', 'Senior Android Engineer', 'AC'),
('u_emp2', 'EMP102', 'Sarah Jenkins', 'sarah.j@leavedesk.corp', 'password123', 'EMPLOYEE', 'g_eng', 'Engineering & Product', 'Staff Backend Engineer', 'SJ'),
('u_emp3', 'EMP103', 'David Kim', 'david.k@leavedesk.corp', 'password123', 'EMPLOYEE', 'g_eng', 'Engineering & Product', 'QA Automation Lead', 'DK'),
('u_emp4', 'EMP104', 'Priya Patel', 'priya.p@leavedesk.corp', 'password123', 'EMPLOYEE', 'g_ops', 'Design & Operations', 'Lead Product Designer', 'PP'),
('u_emp5', 'EMP105', 'Liam O\'Connor', 'liam.o@leavedesk.corp', 'password123', 'EMPLOYEE', 'g_ops', 'Design & Operations', 'Operations Specialist', 'LO'),
('u_emp6', 'EMP106', 'Maya Lin', 'maya.l@leavedesk.corp', 'password123', 'EMPLOYEE', 'g_ops', 'Design & Operations', 'UX Researcher', 'ML');

-- Seed Standard Leave Quotas for u_emp1 (Alex Chen)
INSERT IGNORE INTO user_quotas (user_id, leave_type, total_quota, used_quota, is_no_cap) VALUES
('u_emp1', 'CL', 12.0, 2.0, FALSE),
('u_emp1', 'SL', 10.0, 1.0, FALSE),
('u_emp1', 'PL', 15.0, 5.0, FALSE),
('u_emp1', 'ML', 0.0, 0.0, TRUE),
('u_emp1', 'LOP', 0.0, 0.0, TRUE);

-- Initial Audit Log Entry Confirming MySQL Schema Online
INSERT IGNORE INTO audit_logs (id, actor_name, actor_role, action, target, reason, details) VALUES
('AUD-INIT-01', 'System Administrator', 'SYSTEM', 'SCHEMA_INITIALIZED', 'leavedesk_db', 'Production MySQL database bootstrapped successfully', 'User leavedesk_app active on port 3306');
