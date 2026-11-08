# Functional Requirements Document (FRD)
## LeaveDesk: Enterprise Absence & Workforce Governance System

**Document Version:** 1.0.0  
**Status:** Approved for Engineering & Stakeholder Review  
**Product Owner:** Executive Leadership & Human Resources  
**Lead Architect:** Principal Systems Engineer  
**Target Audience:** C-Suite Stakeholders, HR Operations, Engineering Leads, Android & Backend Developers, QA Teams  

---

## 1. Executive Summary & Business Objectives

### 1.1 Problem Statement
Modern corporate organizations suffer from fragmented, asynchronous, and compliance-risky leave tracking:
* Approvals frequently occur in unmonitored chat channels (Slack/WhatsApp) without verifiable audit trails.
* Multi-day leaves (≥ 3 days) often breach critical team quorum and sprint delivery capacity.
* Executive leadership lacks visibility into concurrent absences and calendar freeze dates.
* Evidence/proofs (medical slips, handover documents) are scattered across email threads without binding links to leave requests.

### 1.2 Proposed Solution: LeaveDesk
**LeaveDesk** is an enterprise-grade workforce absence and governance platform consisting of:
1. **Native Android Client:** Jetpack Compose with Material Design 3, dynamic role theming (Teal, Indigo, Purple), and zero-leak corporate SSO.
2. **Backend Microservices Cluster:** 3 decoupled Node.js services (Auth, Workflow, Calendar/Chat) orchestrated behind a unified API Gateway.
3. **Enterprise Data Layer:** MySQL 8 ACID-compliant relational schema paired with Cloud Object Storage (S3/MinIO) for immutable evidence tracking.
4. **Hierarchical Governance:** Multi-tier approval state machine, integrated direct clarification chat with proof attachments, and CEO calendar lock authority.

---

## 2. User Personas & Role-Based Access Matrix (RBAC)

| Persona | Role Code | Responsibilities & UI View | Permitted Actions |
| :--- | :--- | :--- | :--- |
| **Employee** | `EMPLOYEE` | Individual contributor view (`EmployeeScreens.kt`). Balance cards, leave submission wizard, requests timeline, team calendar. | • Apply for leave (CL, SL, EL, ML, BL, HD, WFH)<br>• Designate peer backup person<br>• Attach proof files (PDF, JPG, PNG)<br>• Message Team Lead directly<br>• Reply with proofs to CEO queries |
| **Team Lead** | `TEAM_LEAD` | Team management view (`TeamLeadScreens.kt`). Presence gauge, quorum alerts, team requests inbox. | • Approve short leaves (1–2 days)<br>• Endorse long leaves (≥ 3 days) to CEO<br>• Reject leaves with logged remarks<br>• Query employees & suggest alternate dates<br>• Direct chat with supervised team members |
| **CEO / Executive** | `CEO` | Executive oversight view (`CeoScreens.kt`). Company KPIs, executive approval queue, calendar editor, audit trail. | • Approve/Reject all escalated leaves<br>• Query employees on pending leaves with directive attachments<br>• Lock/Unlock corporate calendar dates<br>• Inspect organization-wide immutable audit trail |

---

## 3. Detailed Functional Requirements

### FR-1: Authentication & Corporate Single Sign-On (SSO)
* **FR-1.1:** The system shall provide a clean corporate SSO portal without exposed demo credentials or shortcuts.
* **FR-1.2:** Authentication must accept **Organization Domain** (e.g., `leavedesk.corp`), and either **Employee ID** (`EMP101`, `TL201`, `CEO001`) or **Work Email** (`alex.chen@leavedesk.corp`).
* **FR-1.3:** Secure password verification with masked input and visibility toggle.
* **FR-1.4:** Session tokens must be issued and stored securely. Logging out must wipe the in-memory session and return to the SSO root.

### FR-2: Leave Quota & Entitlement Engine
* **FR-2.1:** Each employee profile maintains independent balances across predefined leave types:
  * Casual Leave (CL): 12 days/year (max 3 consecutive)
  * Sick Leave (SL): 10 days/year (never blocked by team thresholds)
  * Earned Leave (EL): 15 days/year (requires approval)
  * Maternity Leave (ML): 90 days/year (proof mandatory)
  * Bereavement (BL): 5 days
  * Half Day (HD): 8 days (First Half or Second Half session)
  * Work From Home (WFH): Uncapped/flexible
* **FR-2.2:** System must compute fractional remaining quotas (`remaining = total - used`).
* **FR-2.3:** Submissions exceeding available quota must be rejected at input validation.
* **FR-2.4:** Rejections or approved cancellations must automatically credit working days back to the employee's quota.

### FR-3: Leave Application & Availability Engine
* **FR-3.1:** Auto-calculation of net working days, strictly omitting weekends (Saturday/Sunday) and declared company holidays.
* **FR-3.2:** Mandatory proof attachment trigger:
  * Required for all Maternity Leaves (ML).
  * Required for any leave span ≥ 3 consecutive working days.
* **FR-3.3:** Real-time quorum threshold check: Evaluates if approving a request drops team presence below the configured minimum threshold (e.g., minimum 3 engineers present).
* **FR-3.4:** Peer Backup Designation: Employee selects a colleague within the same team to cover duties, accompanied by handover notes.

### FR-4: Multi-Tier Approval Workflow Engine
The approval routing must adhere to the following deterministic state machine:

```
[Draft Request] 
      │
      ▼
[Pending Review]
      │
      ├───────────────────────┬────────────────────────┐
      │ (1–2 days CL/SL/HD)   │ (≥ 3 days, ML, or TL)  │ (Emergency Sick)
      ▼                       ▼                        ▼
[TL Decision]           [TL Endorsement]         [Auto-Fastrack]
      │                       │
 ┌────┴────┐                  ▼
 │         │            [CEO Queue]
Approved Rejected             │
                         ┌────┴──────────────┐
                         │         │         │
                     Approved    Reject    Query (Direct Chat)
```

* **FR-4.1 (Short Leaves 1–2 days):** Team Lead holds final approval authority.
* **FR-4.2 (Long Leaves ≥ 3 days & Maternity):** Requires Team Lead Level-1 endorsement followed by CEO Level-2 final sign-off.
* **FR-4.3 (Team Lead Leaves):** Any leave requested by a Team Lead routes directly to the CEO.
* **FR-4.4 (Automated Reminders & Escalations):** Requests pending > 24 hours trigger automated approver reminder notices; requests pending > 48 hours trigger high-priority executive escalation tags.

### FR-5: CEO Governance & Action Suite
* **FR-5.1:** The CEO approval interface provides 3 actions on every pending request:
  1. **Approve:** Executes final sign-off, deducts quota, and updates the public schedule.
  2. **Query:** Opens a clarification prompt allowing the CEO to write questions and attach directive documentation.
  3. **Reject:** Requires an official justification logged permanently into the audit trail.
* **FR-5.2 (Calendar Blackout Lock):** CEO has exclusive authority to lock corporate dates (e.g., major release freezes, quarterly board reviews). Locked dates prohibit team-level overrides and flag warnings on employee leave applications.
* **FR-5.3 (Executive KPI Dashboard):** Real-time aggregation of Total Headcount, Staff at Work, Staff on Leave, Average Leave Utilization (%), and Department Breakdown.

### FR-6: Integrated Real-Time Query & Direct Chat System
* **FR-6.1 (Strict Communication Hierarchy):**
  * **Employee & CEO:** An employee **cannot initiate unsolicited contact** with the CEO. The direct chat channel between an Employee and the CEO remains **strictly locked** until the CEO initiates a Query on an approval. Once queried, the channel unlocks, allowing the employee to reply, converse, and attach evidence.
  * **Employee & Team Lead:** Employees can initiate direct messages to their Team Lead at any time.
  * **Team Lead & Peers:** Team Leads **cannot direct message peer Team Leads** (blocked per corporate hierarchy).
  * **Team Lead & Team:** Team Leads can message all supervised team members.
* **FR-6.2 (Automated Event Delivery):**
  * CEO queries are delivered directly into the Employee-CEO chat box with the subject leave request linked.
  * Rejection justifications automatically post to the relevant direct chat thread.
* **FR-6.3 (Mutual In-Chat Attachments):** Both employee and approver (TL/CEO) can attach documents (`.pdf`, `.png`, `.jpg`, `.docx`) directly in the chat composer using the attachment picker.
* **FR-6.4 (Document Viewer):** Tapping any attachment opens an in-app verified corporate document viewer modal.

### FR-7: Public Calendar & Blackout Engine
* **FR-7.1:** Unified organization calendar categorizing:
  * Public / Gazetted Holidays (Red)
  * Optional Holidays (Blue)
  * Company Closures (Purple)
  * Special Working Days (Amber)
  * Approved Team Absences (Green)
* **FR-7.2 (Retroactive Balance Recalculation):** If the CEO declares a new public holiday or company closure that overlaps with an already-approved employee leave, the system automatically recalculates the net working days and restores the difference to the employee's quota balance.

### FR-8: Immutable Audit Trail & Compliance
* **FR-8.1:** Every governance-relevant action generates an append-only audit log record:
  * `ID`, `Timestamp`, `Actor Name`, `Actor Role`, `Action Code`, `Target Request/Date`, `Reason/Remarks`, `Technical Details`.
* **FR-8.2:** Audit logs cannot be deleted or modified by any user role (including CEO).

---

## 4. Technical Architecture & Database Design

### 4.1 System Topology
* **Frontend:** Kotlin, Jetpack Compose, Material 3, StateFlow, Coroutines, Navigation Compose.
* **API Gateway (`server.js` - Port 8084):** Reverse proxy, CORS termination, request validation.
* **Microservices:**
  * `auth-service` (Port 8081): Token issuance, password verification, SSO identity mapping.
  * `workflow-service` (Port 8082): Leave state machine, quota deductions, approval engine.
  * `calendar-service` (Port 8083): Holidays, date locking, chat messaging, notification center.
* **Database:** MySQL 8.0 with InnoDB engine, connection pooling, and prepared statements.
* **Media Cloud:** S3-compatible object storage (AWS S3 / Cloudflare R2 / MinIO) for proofs and documents.

### 4.2 Core Relational Schema (DDL)
```sql
-- 1. Corporate Directory
CREATE TABLE users (
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
  avatar_initials VARCHAR(4)
);

-- 2. Leave Quotas
CREATE TABLE user_quotas (
  user_id VARCHAR(64) PRIMARY KEY,
  pto_total INT DEFAULT 20, pto_used INT DEFAULT 0,
  sick_total INT DEFAULT 10, sick_used INT DEFAULT 0,
  personal_total INT DEFAULT 5, personal_used INT DEFAULT 0,
  unpaid_used INT DEFAULT 0,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Leave Requests
CREATE TABLE leave_requests (
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
  query_question TEXT,
  team_lead_remarks TEXT,
  ceo_remarks TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Chat Messages & Attachments
CREATE TABLE chat_messages (
  id VARCHAR(64) PRIMARY KEY,
  thread_id VARCHAR(128) NOT NULL,
  sender_id VARCHAR(64) NOT NULL,
  sender_role VARCHAR(32) NOT NULL,
  text TEXT NOT NULL,
  attachment_file_name VARCHAR(255),
  attachment_file_type VARCHAR(32),
  attachment_file_size VARCHAR(32),
  is_official_query BOOLEAN DEFAULT FALSE,
  is_official_decision BOOLEAN DEFAULT FALSE,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Immutable Audit Trail
CREATE TABLE audit_logs (
  id VARCHAR(64) PRIMARY KEY,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  actor_name VARCHAR(128) NOT NULL,
  actor_role VARCHAR(32) NOT NULL,
  action VARCHAR(64) NOT NULL,
  target VARCHAR(128) NOT NULL,
  reason TEXT,
  details TEXT
);
```

---

## 5. Non-Functional Requirements (NFRs)

* **NFR-1 (Performance):** UI transitions and state renders must maintain 60 fps on standard mobile hardware. Backend API endpoints must respond in < 150ms at P95.
* **NFR-2 (Security):** Passwords salted and hashed with bcrypt. API communications encrypted over TLS 1.3. No sensitive tokens or secrets stored in plaintext.
* **NFR-3 (Data Integrity & ACID):** Quota deductions and request status updates execute inside serializable database transactions to prevent double-spending or race conditions.
* **NFR-4 (Accessibility):** Adheres to WCAG 2.1 AA standards; all interactive touch targets meet the minimum 48dp dimension requirement.

---

## 6. Acceptance Criteria for Stakeholder Sign-Off

| Test ID | Scenario | Expected Outcome | Sign-off Status |
| :--- | :--- | :--- | :--- |
| **AC-01** | Employee logs in with `EMP101` and applies for 4-day Earned Leave. | System mandates proof upload, calculates 4 working days, flags CEO approval required. | **PASSED** |
| **AC-02** | Team Lead (`TL201`) reviews 4-day request. | TL can Endorse to CEO, Query, or Reject. Endorsing routes request to CEO queue. | **PASSED** |
| **AC-03** | CEO (`CEO001`) raises Query on 4-day request. | Request enters `QUERY` state. CEO message and attached guide auto-delivered to Employee chat box. | **PASSED** |
| **AC-04** | Employee (`EMP101`) inspects CEO chat thread. | CEO direct channel unfreezes. Employee types reply, attaches signed handover proof, and submits. | **PASSED** |
| **AC-05** | Employee attempts to initiate unprompted chat to CEO. | System blocks action with explanation: *"Direct CEO chat unlocks when CEO queries on an approval."* | **PASSED** |
| **AC-06** | Team Lead attempts to message peer Team Lead. | Blocked per corporate hierarchy policy; only team subordinates and CEO accessible. | **PASSED** |
| **AC-07** | CEO rejects leave request. | Mandatory reason logged in audit trail and posted into Employee-CEO chat box. Quota restored. | **PASSED** |
| **AC-08** | CEO declares new company holiday on an approved leave date. | System recalculates leave working days and auto-refunds the difference back to employee quota. | **PASSED** |

---

## 7. Sign-off & Approvals

| Stakeholder Role | Name | Title | Date | Signature Status |
| :--- | :--- | :--- | :--- | :--- |
| **Executive Sponsor** | Sarah Jenkins | Chief Executive Officer | Nov 2026 | *Approved* |
| **VP of Engineering** | Marcus Vance | Lead Architect / Tech Lead | Nov 2026 | *Approved* |
| **Head of People / HR** | Elena Rostova | VP of HR & Operations | Nov 2026 | *Approved* |
| **Lead QA Engineer** | David Kim | Senior Quality Architect | Nov 2026 | *Approved* |
