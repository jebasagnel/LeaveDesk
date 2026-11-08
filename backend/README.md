# LeaveDesk Enterprise Backend (MySQL + 3 Microservices Cluster)

LeaveDesk provides a complete enterprise backend architected into 3 distinct microservices connected to **MySQL** (with automated zero-config in-memory fallback).

---

## 🔑 Default Corporate Logins (Pre-Seeded in MySQL)

| Role | Employee ID | Corporate Email | Password | Scope & Responsibilities |
| :--- | :--- | :--- | :--- | :--- |
| **CEO** | `CEO001` | `robert.sterling@leavedesk.corp` | `password123` | Executive final approvals, 1-tap date locking, emergency cancel override |
| **Team Lead** | `TL201` | `marcus.v@leavedesk.corp` | `password123` | Engineering dept approvals, group presence threshold manager |
| **Team Lead** | `TL202` | `elena.r@leavedesk.corp` | `password123` | Design & Operations lead |
| **Employee** | `EMP101` | `alex.chen@leavedesk.corp` | `password123` | Senior Engineer, leave applications, peer backup picker |
| **Employee** | `EMP102` | `sarah.j@leavedesk.corp` | `password123` | Backend Architect (EMP101 backup) |

---

## 🚀 Quick Start (2 Easy Ways)

### Option 1: One-Click Docker (MySQL + phpMyAdmin + 3 Microservices)
If you have Docker or Docker Desktop installed:
```bash
docker-compose up -d
```
- **Unified Gateway**: http://localhost:8084
- **phpMyAdmin GUI**: http://localhost:8085 (User: `root`, Password: `rootpassword`)
- **MySQL Database**: `localhost:3306` (Database: `leavedesk_db`, User: `leavedesk`, Pass: `leavedesk_pass`)

---

### Option 2: Native Node.js & Local MySQL
1. Ensure MySQL is running on your machine and run the schema script:
```bash
mysql -u root -p < schema.sql
```
2. Install dependencies & verify tables:
```bash
npm install
npm run db:verify
```
3. Start the entire cluster:
```bash
npm start
```

*(Note: If MySQL is not running yet, the backend automatically runs in resilient in-memory mode, so you can test all endpoints immediately!)*

---

## 🏗️ Architecture & Dedicated Ports

| Port | Service | Role | Key Endpoints |
| :--- | :--- | :--- | :--- |
| **:8081** | Identity & Authentication | Validates logins, issues JWT tokens | `POST /api/v1/auth/login`<br>`GET /api/v1/auth/users`<br>`GET /api/v1/auth/teams/:groupId/members` |
| **:8082** | Leave Workflow Engine | Quotas, working days, approvals, audit logs | `POST /api/v1/leaves/apply`<br>`POST /api/v1/leaves/:id/decision`<br>`GET /api/v1/leaves/balances/:userId`<br>`POST /api/v1/leaves/:id/emergency-cancel`<br>`GET /api/v1/audit/logs` |
| **:8083** | Real-Time Calendar Hub | WebSockets, CEO 1-tap date locking | `GET /api/v1/calendar/holidays`<br>`POST /api/v1/calendar/date-lock`<br>`GET/POST /api/v1/chat/messages`<br>`ws://localhost:8083` |
| **:8084** | Unified Reverse Proxy Gateway | Reverse proxy routing requests to all 3 services | `http://localhost:8084/...` |

---

## 🧪 Verification cURL Commands

```bash
# 1. Health & Database check
curl http://localhost:8084/health

# 2. Login as Employee Alex Chen (EMP101)
curl -X POST http://localhost:8084/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"EMP101","password":"password123"}'

# 3. Check Leave Quotas
curl http://localhost:8084/api/v1/leaves/balances/u_emp1

# 4. View Company Holidays & CEO Locks
curl http://localhost:8084/api/v1/calendar/holidays
```
