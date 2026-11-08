# LeaveDesk: Enterprise Architecture, MySQL Database & Microservices Specification

---

## 1. Executive Summary & Deliverables

LeaveDesk has been updated with a production-grade **MySQL relational database layer**, container orchestration via **Docker Compose**, and full support for 3 authenticated corporate roles:

- **CEO (`CEO001`)**: Executive dashboards, company-wide leave approvals, 1-tap CEO calendar date locks, and emergency cancellations.
- **Team Lead (`TL201`)**: Departmental presence monitoring, team leave approvals (1–2 days auto-granted, 3+ days escalated to CEO), minimum presence threshold enforcement.
- **Employee (`EMP101`)**: Leave balances, working-day auto calculation, multi-day/half-day submissions, peer backup selection.

All components run seamlessly on your desktop via VS Code and Docker.

---

## 2. Default Login Credentials (Pre-Seeded in MySQL & Android)

| Role | Employee ID | Email | Password | Designation | Group |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **CEO** | `CEO001` | `robert.sterling@leavedesk.corp` | `password123` | Chief Executive Officer | Executive Office |
| **Team Lead** | `TL201` | `marcus.v@leavedesk.corp` | `password123` | Engineering Lead | Engineering & Product |
| **Team Lead** | `TL202` | `elena.r@leavedesk.corp` | `password123` | Operations Lead | Design & Operations |
| **Employee** | `EMP101` | `alex.chen@leavedesk.corp` | `password123` | Senior Android Engineer | Engineering & Product |
| **Employee** | `EMP102` | `sarah.j@leavedesk.corp` | `password123` | Backend Architect | Engineering & Product |
| **Employee** | `EMP103` | `david.kim@leavedesk.corp` | `password123` | QA Specialist | Engineering & Product |
| **Employee** | `EMP104` | `priya.patel@leavedesk.corp` | `password123` | Fullstack Developer | Engineering & Product |

---

## 3. MySQL Database Architecture (`/backend/schema.sql` & `mysql-db.js`)

The database runs on standard MySQL 8.0 with InnoDB engine and UTF8mb4 encoding:

1. **`users` Table**: Primary user profiles, unique employee IDs, email index, hashed passwords, group memberships, and role ENUM (`CEO`, `TEAM_LEAD`, `EMPLOYEE`).
2. **`teams` Table**: Departments, lead mapping, and configurable minimum present thresholds (e.g. `g_eng` threshold = 3).
3. **`user_quotas` Table**: Leave balances (Casual `CL`, Sick `SL`, Earned `EL`, Maternity `ML`, Bereavement `BL`, Loss of Pay `LOP`, Half-Day `HD`, Work From Home `WFH`, Short Leave `SHORT`).
4. **`holidays` Table**: Corporate calendar with dates, titles, and `is_locked_by_ceo` boolean flags.
5. **`leave_requests` Table**: Full relational state machine tracking applicant, backup peer, date range, working days, status (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`), TL decisions/comments, and CEO decisions/comments.
6. **`audit_logs` Table**: Immutable compliance trail capturing every system boot, leave application, approval, rejection, and cancellation.
7. **`chat_messages` Table**: Group and request-specific discussion threads.

*Resilience Feature*: When MySQL is started, `mysql-db.js` automatically creates missing tables and seeds initial data. If MySQL is temporarily offline, the backend seamlessly switches to an in-memory replica without throwing 500 errors.

---

## 4. Microservices Topology & Ports

```
[ Android App / Web / Desktop UI ]
               │
               ▼ HTTP (:8084)
 ┌─────────────────────────────────────────────────────────┐
 │               UNIFIED API GATEWAY (:8084)               │
 └───────┬─────────────────────────┬─────────────────┬─────┘
         │ /api/v1/auth            │ /api/v1/leaves  │ /api/v1/calendar
         ▼                         ▼                 ▼
 ┌────────────────┐       ┌────────────────┐ ┌────────────────┐
 │ AUTH SERVICE   │       │ WORKFLOW       │ │ CALENDAR & WSS │
 │ Port: 8081     │       │ Port: 8082     │ │ Port: 8083     │
 └───────┬────────┘       └───────┬────────┘ └───────┬────────┘
         │                        │                  │
         └────────────────────────┼──────────────────┘
                                  ▼
                   ┌──────────────────────────────┐
                   │  MySQL 8.0 Cluster (:3306)   │
                   │  Database: leavedesk_db      │
                   └──────────────────────────────┘
```

---

## 5. Ready-to-Paste Prompt for Your VS Code AI

Copy and paste the prompt below directly into GitHub Copilot, Cursor, Continue, or Claude in your VS Code editor:

```markdown
I have downloaded the LeaveDesk project repository. Please perform the following steps to verify, run, and test the entire stack with MySQL:

1. Check the project structure:
   - Verify `/backend/package.json`, `/backend/schema.sql`, `/backend/server.js`, and `docker-compose.yml`.
   - Verify the 3 microservices in `/backend/services/` (auth-service :8081, workflow-service :8082, calendar-service :8083).

2. Start the backend with MySQL:
   - Run `docker-compose up -d` in the project root to spin up MySQL 8 (`localhost:3306`), phpMyAdmin (`http://localhost:8085`), and the 3-microservice backend cluster (`http://localhost:8084`).
   - If using local MySQL instead of Docker, run `mysql -u root -p < backend/schema.sql`, then inside `/backend` run `npm install && npm start`.

3. Verify Database and Credential Health:
   - In `/backend`, run `npm run db:verify` to check that tables are seeded and the 3 roles exist:
     * CEO: `CEO001` / `password123`
     * Team Lead: `TL201` / `password123`
     * Employee: `EMP101` / `password123`

4. Test API Endpoints via cURL or VS Code REST Client:
   - Health check: `curl http://localhost:8084/health`
   - Test login: `curl -X POST http://localhost:8084/api/v1/auth/login -H "Content-Type: application/json" -d '{"identifier":"EMP101","password":"password123"}'`
   - Test quota check: `curl http://localhost:8084/api/v1/leaves/balances/u_emp1`

Please guide me through each step and confirm when the backend and database are verified and operational!
```
