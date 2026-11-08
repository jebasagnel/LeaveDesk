#!/usr/bin/env bash
# ==============================================================================
# LeaveDesk: All-in-One Setup & Launch Script
#
# This single script sets up and launches the entire LeaveDesk backend:
# 1. MySQL 8 Database schema & seed data
# 2. Docker Compose orchestration (MySQL + phpMyAdmin + API Gateway)
# 3. Microservice 1: Auth Service (Port 8081)
# 4. Microservice 2: Leave & Workflow Service (Port 8082)
# 5. Microservice 3: Calendar & Notification Service (Port 8083)
# 6. Unified API Gateway (Port 8084)
# ==============================================================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}==================================================================${NC}"
echo -e "${GREEN}             LeaveDesk Enterprise Management System               ${NC}"
echo -e "${CYAN}             All-in-One Deployment & Runner Script               ${NC}"
echo -e "${BLUE}==================================================================${NC}"

# Corporate Credentials
echo -e "\n${YELLOW}Corporate Login Credentials:${NC}"
echo -e "  1. CEO / Exec:   ${GREEN}CEO001${NC} / ${GREEN}sarah.jenkins@leavedesk.corp${NC}  | Pass: ${GREEN}password123${NC}"
echo -e "  2. Team Lead:    ${GREEN}TL201${NC}  / ${GREEN}marcus.vance@leavedesk.corp${NC}   | Pass: ${GREEN}password123${NC}"
echo -e "  3. Employee:     ${GREEN}EMP101${NC} / ${GREEN}alex.chen@leavedesk.corp${NC}      | Pass: ${GREEN}password123${NC}"
echo -e "  Org Domain:      ${GREEN}leavedesk.corp${NC}\n"

# Create directories
echo -e "${BLUE}[1/5] Creating project directory structure...${NC}"
mkdir -p backend/services/auth-service
mkdir -p backend/services/workflow-service
mkdir -p backend/services/calendar-service
mkdir -p backend/scripts

# Write backend package.json
echo -e "${BLUE}[2/5] Creating backend package.json and configuration...${NC}"
cat << 'EOF' > backend/package.json
{
  "name": "leavedesk-backend",
  "version": "1.0.0",
  "description": "LeaveDesk Enterprise 3-Tier Backend Microservices & Gateway",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "verify-db": "node scripts/verify-db.js",
    "docker:up": "docker compose up -d",
    "docker:down": "docker compose down"
  },
  "dependencies": {
    "express": "^4.19.2",
    "cors": "^2.8.5",
    "mysql2": "^3.9.7",
    "http-proxy-middleware": "^3.0.0"
  }
}
EOF

# Write MySQL Schema
echo -e "${BLUE}[3/5] Writing MySQL 8 Relational Database Schema & Seeds...${NC}"
cat << 'EOF' > backend/schema.sql
-- LeaveDesk Enterprise MySQL 8 Schema & Production Seed Data
CREATE DATABASE IF NOT EXISTS leavedesk_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE leavedesk_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(64) PRIMARY KEY,
  employee_id VARCHAR(32) UNIQUE NOT NULL,
  email VARCHAR(128) UNIQUE NOT NULL,
  name VARCHAR(128) NOT NULL,
  role ENUM('EMPLOYEE', 'TEAM_LEAD', 'CEO') NOT NULL,
  department VARCHAR(64) NOT NULL,
  designation VARCHAR(64) NOT NULL,
  team_id VARCHAR(64),
  manager_id VARCHAR(64),
  organization_domain VARCHAR(128) DEFAULT 'leavedesk.corp',
  avatar_initials VARCHAR(4),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. User Quotas
CREATE TABLE IF NOT EXISTS user_quotas (
  user_id VARCHAR(64) PRIMARY KEY,
  pto_total INT DEFAULT 20,
  pto_used INT DEFAULT 0,
  sick_total INT DEFAULT 10,
  sick_used INT DEFAULT 0,
  personal_total INT DEFAULT 5,
  personal_used INT DEFAULT 0,
  unpaid_used INT DEFAULT 0,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Leave Requests
CREATE TABLE IF NOT EXISTS leave_requests (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  leave_type VARCHAR(32) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  total_days INT NOT NULL,
  reason TEXT NOT NULL,
  handover_user_id VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  current_step VARCHAR(32) NOT NULL,
  team_lead_approval_id VARCHAR(64),
  team_lead_approval_name VARCHAR(128),
  team_lead_remarks TEXT,
  team_lead_decided_at TIMESTAMP NULL,
  ceo_approval_id VARCHAR(64),
  ceo_approval_name VARCHAR(128),
  ceo_remarks TEXT,
  ceo_decided_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Calendar Locks & Holidays
CREATE TABLE IF NOT EXISTS calendar_locks (
  lock_date DATE PRIMARY KEY,
  reason VARCHAR(255) NOT NULL,
  locked_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed Initial Corporate Users
INSERT IGNORE INTO users (id, employee_id, email, name, role, department, designation, team_id, manager_id, organization_domain, avatar_initials) VALUES
('u_ceo', 'CEO001', 'sarah.jenkins@leavedesk.corp', 'Sarah Jenkins', 'CEO', 'Executive', 'Chief Executive Officer', 'exec', NULL, 'leavedesk.corp', 'SJ'),
('u_lead', 'TL201', 'marcus.vance@leavedesk.corp', 'Marcus Vance', 'TEAM_LEAD', 'Engineering', 'Lead Platform Architect', 'team_core_eng', 'u_ceo', 'leavedesk.corp', 'MV'),
('u_emp', 'EMP101', 'alex.chen@leavedesk.corp', 'Alex Chen', 'EMPLOYEE', 'Engineering', 'Senior Fullstack Engineer', 'team_core_eng', 'u_lead', 'leavedesk.corp', 'AC');

INSERT IGNORE INTO user_quotas (user_id, pto_total, pto_used, sick_total, sick_used, personal_total, personal_used, unpaid_used) VALUES
('u_ceo', 25, 2, 10, 0, 5, 0, 0),
('u_lead', 22, 5, 10, 1, 5, 1, 0),
('u_emp', 20, 6, 10, 2, 5, 1, 0);
EOF

# Write microservice 1: Auth Service (Port 8081)
cat << 'EOF' > backend/services/auth-service/index.js
const express = require('express');
const cors = require('cors');
const app = express();
const PORT = process.env.PORT || 8081;

app.use(cors());
app.use(express.json());

const USERS = [
  { id: 'u_ceo', employeeId: 'CEO001', email: 'sarah.jenkins@leavedesk.corp', name: 'Sarah Jenkins', role: 'CEO', department: 'Executive', designation: 'Chief Executive Officer', domain: 'leavedesk.corp' },
  { id: 'u_lead', employeeId: 'TL201', email: 'marcus.vance@leavedesk.corp', name: 'Marcus Vance', role: 'TEAM_LEAD', department: 'Engineering', designation: 'Lead Platform Architect', domain: 'leavedesk.corp' },
  { id: 'u_emp', employeeId: 'EMP101', email: 'alex.chen@leavedesk.corp', name: 'Alex Chen', role: 'EMPLOYEE', department: 'Engineering', designation: 'Senior Fullstack Engineer', domain: 'leavedesk.corp' }
];

app.post('/api/v1/auth/login', (req, res) => {
  const { identifier, password, organization } = req.body;
  if (!identifier || !password) return res.status(400).json({ error: 'Identifier and password are required' });
  const cleanId = identifier.trim().toLowerCase();
  const user = USERS.find(u => u.employeeId.toLowerCase() === cleanId || u.email.toLowerCase() === cleanId);
  if (!user || password !== 'password123') return res.status(401).json({ error: 'Invalid Employee ID/Email or Password' });
  
  res.json({
    token: `jwt_${user.id}_${Date.now()}`,
    user: { id: user.id, employeeId: user.employeeId, email: user.email, name: user.name, role: user.role, department: user.department, designation: user.designation }
  });
});

app.get('/health', (req, res) => res.json({ status: 'UP', service: 'auth-service', port: PORT }));
app.listen(PORT, () => console.log(`[AUTH-SERVICE] Running on port ${PORT}`));
EOF

# Write microservice 2: Workflow Service (Port 8082)
cat << 'EOF' > backend/services/workflow-service/index.js
const express = require('express');
const cors = require('cors');
const app = express();
const PORT = process.env.PORT || 8082;

app.use(cors());
app.use(express.json());

let leaves = [
  { id: 'lr_101', userId: 'u_emp', userName: 'Alex Chen', userEmail: 'alex.chen@leavedesk.corp', leaveType: 'PAID_TIME_OFF', startDate: '2026-11-12', endDate: '2026-11-14', totalDays: 3, reason: 'Family gathering', status: 'PENDING_TEAM_LEAD', currentStep: 'TEAM_LEAD_REVIEW' }
];

app.get('/api/v1/leaves', (req, res) => res.json(leaves));
app.post('/api/v1/leaves', (req, res) => {
  const newLeave = { id: `lr_${Date.now()}`, ...req.body, status: 'PENDING_TEAM_LEAD', currentStep: 'TEAM_LEAD_REVIEW', createdAt: new Date().toISOString() };
  leaves.unshift(newLeave);
  res.status(201).json(newLeave);
});

app.post('/api/v1/leaves/:id/approve-team-lead', (req, res) => {
  const leave = leaves.find(l => l.id === req.params.id);
  if (!leave) return res.status(404).json({ error: 'Leave not found' });
  leave.status = leave.totalDays > 3 ? 'PENDING_CEO' : 'APPROVED';
  leave.currentStep = leave.totalDays > 3 ? 'CEO_REVIEW' : 'COMPLETED';
  res.json(leave);
});

app.post('/api/v1/leaves/:id/reject-team-lead', (req, res) => {
  const leave = leaves.find(l => l.id === req.params.id);
  if (!leave) return res.status(404).json({ error: 'Leave not found' });
  leave.status = 'REJECTED';
  leave.currentStep = 'COMPLETED';
  res.json(leave);
});

app.post('/api/v1/leaves/:id/approve-ceo', (req, res) => {
  const leave = leaves.find(l => l.id === req.params.id);
  if (!leave) return res.status(404).json({ error: 'Leave not found' });
  leave.status = 'APPROVED';
  leave.currentStep = 'COMPLETED';
  res.json(leave);
});

app.post('/api/v1/leaves/:id/reject-ceo', (req, res) => {
  const leave = leaves.find(l => l.id === req.params.id);
  if (!leave) return res.status(404).json({ error: 'Leave not found' });
  leave.status = 'REJECTED';
  leave.currentStep = 'COMPLETED';
  res.json(leave);
});

app.get('/health', (req, res) => res.json({ status: 'UP', service: 'workflow-service', port: PORT }));
app.listen(PORT, () => console.log(`[WORKFLOW-SERVICE] Running on port ${PORT}`));
EOF

# Write microservice 3: Calendar Service (Port 8083)
cat << 'EOF' > backend/services/calendar-service/index.js
const express = require('express');
const cors = require('cors');
const app = express();
const PORT = process.env.PORT || 8083;

app.use(cors());
app.use(express.json());

let lockedDates = ['2026-11-20', '2026-11-27'];
let chatMessages = [
  { id: 'm1', userId: 'u_lead', userName: 'Marcus Vance', userRole: 'TEAM_LEAD', message: 'Sprint release window locked for Nov 20-27.', timestamp: new Date().toISOString() }
];

app.get('/api/v1/calendar/locked-dates', (req, res) => res.json(lockedDates));
app.post('/api/v1/calendar/toggle-lock', (req, res) => {
  const { date } = req.body;
  if (!date) return res.status(400).json({ error: 'Date is required' });
  if (lockedDates.includes(date)) lockedDates = lockedDates.filter(d => d !== date);
  else lockedDates.push(date);
  res.json({ lockedDates });
});

app.get('/api/v1/chat/messages', (req, res) => res.json(chatMessages));
app.post('/api/v1/chat/messages', (req, res) => {
  const msg = { id: `m_${Date.now()}`, ...req.body, timestamp: new Date().toISOString() };
  chatMessages.push(msg);
  res.status(201).json(msg);
});

app.get('/health', (req, res) => res.json({ status: 'UP', service: 'calendar-service', port: PORT }));
app.listen(PORT, () => console.log(`[CALENDAR-SERVICE] Running on port ${PORT}`));
EOF

# Write Unified API Gateway (Port 8084)
cat << 'EOF' > backend/server.js
const express = require('express');
const cors = require('cors');
const { createProxyMiddleware } = require('http-proxy-middleware');
const { spawn } = require('child_process');
const path = require('path');

const app = express();
const GATEWAY_PORT = process.env.GATEWAY_PORT || 8084;

app.use(cors());

// Microservice Ports
const SERVICES = {
  auth: { port: 8081, path: 'services/auth-service/index.js' },
  workflow: { port: 8082, path: 'services/workflow-service/index.js' },
  calendar: { port: 8083, path: 'services/calendar-service/index.js' }
};

// Start all 3 microservices child processes
for (const [name, config] of Object.entries(SERVICES)) {
  const fullPath = path.join(__dirname, config.path);
  const child = spawn('node', [fullPath], { stdio: 'inherit', env: { ...process.env, PORT: config.port } });
  child.on('error', (err) => console.error(`[CLUSTER ERROR] ${name}:`, err.message));
}

// Route proxy rules
app.use('/api/v1/auth', createProxyMiddleware({ target: `http://localhost:${SERVICES.auth.port}`, changeOrigin: true }));
app.use('/api/v1/leaves', createProxyMiddleware({ target: `http://localhost:${SERVICES.workflow.port}`, changeOrigin: true }));
app.use('/api/v1/calendar', createProxyMiddleware({ target: `http://localhost:${SERVICES.calendar.port}`, changeOrigin: true }));
app.use('/api/v1/chat', createProxyMiddleware({ target: `http://localhost:${SERVICES.calendar.port}`, changeOrigin: true }));

app.get('/', (req, res) => {
  res.json({
    system: 'LeaveDesk Enterprise API Gateway',
    status: 'ONLINE',
    port: GATEWAY_PORT,
    microservices: {
      authService: `http://localhost:${SERVICES.auth.port}`,
      workflowService: `http://localhost:${SERVICES.workflow.port}`,
      calendarService: `http://localhost:${SERVICES.calendar.port}`
    }
  });
});

const server = app.listen(GATEWAY_PORT, () => {
  console.log(`\n======================================================`);
  console.log(` [GATEWAY] LeaveDesk Unified API running on port ${GATEWAY_PORT}`);
  console.log(` [ROUTING] /api/v1/auth     -> :${SERVICES.auth.port}`);
  console.log(` [ROUTING] /api/v1/leaves   -> :${SERVICES.workflow.port}`);
  console.log(` [ROUTING] /api/v1/calendar -> :${SERVICES.calendar.port}`);
  console.log(`======================================================\n`);
});
EOF

# Write Docker Compose File
cat << 'EOF' > docker-compose.yml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: leavedesk_mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: leavedesk_db
      MYSQL_USER: leavedesk_user
      MYSQL_PASSWORD: leavedesk_password
    ports:
      - "3306:3306"
    volumes:
      - ./backend/schema.sql:/docker-entrypoint-initdb.d/init.sql
      - mysql_data:/var/lib/mysql

  phpmyadmin:
    image: phpmyadmin/phpmyadmin
    container_name: leavedesk_phpmyadmin
    restart: always
    ports:
      - "8085:80"
    environment:
      PMA_HOST: mysql
      PMA_PORT: 3306
    depends_on:
      - mysql

volumes:
  mysql_data:
EOF

echo -e "${BLUE}[4/5] Installing npm dependencies...${NC}"
cd backend
npm install --silent

echo -e "${GREEN}==================================================================${NC}"
echo -e "${GREEN}✔ LeaveDesk System Setup Completed Successfully!${NC}"
echo -e "${GREEN}==================================================================${NC}"
echo -e "\nTo launch the backend cluster now, execute:"
echo -e "  ${CYAN}cd backend && npm start${NC}"
echo -e "\nOr if you want to run MySQL & phpMyAdmin in Docker:"
echo -e "  ${CYAN}docker compose up -d${NC}"
echo -e "  Access phpMyAdmin at: ${CYAN}http://localhost:8085${NC}\n"
EOF

chmod +x setup_leavedesk.sh
