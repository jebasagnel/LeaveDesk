# LeaveDesk: Complete UI/UX Source Code & Architecture Reference

This document locks in and details the complete UI/UX source code, state management, components, and functions for **LeaveDesk**.

---

## 📁 Project File Map

```
app/src/main/java/com/example/
├── MainActivity.kt                <- Shell, Edge-to-Edge, Top Bar, Role-Aware Navigation, Profile/Sign-Out
├── model/
│   └── Models.kt                  <- Data models, Enums, Quotas, Leave Statuses, Steps
├── data/
│   ├── LeaveDeskRepository.kt     <- Core Reactive State, Business Rules, Quotas, Audit Trail
│   └── api/
│       └── LeaveDeskBackendApi.kt <- Retrofit definitions for the 3 Microservices
├── ui/
│   ├── theme/
│   │   ├── Color.kt               <- Role-based palette (Teal, Indigo, Purple, Status colors)
│   │   ├── Theme.kt               <- M3 Theme wrapper & LocalRoleTheme provider
│   │   └── Type.kt                <- Inter & Roboto typography system
│   ├── components/
│   │   ├── CommonComponents.kt    <- Status chips, Leave type badges, KPI Stat cards
│   │   └── RoleSwitcherModal.kt   <- Auxiliary user selection modal
│   └── screens/
│       ├── LoginScreen.kt         <- Corporate Single Sign-On (Org, ID/Email, Password)
│       ├── EmployeeScreens.kt     <- Home, Balance Cards, Apply Form, My Requests
│       ├── TeamLeadScreens.kt     <- Team Inbox, Presence Threshold Gauge, Approvals
│       ├── CeoScreens.kt          <- CEO Executive Dashboard, Calendar Locks, Approvals, Audit
│       └── SharedScreens.kt       <- Company Calendar, Team Chat, Notification Center
```

---

## 1. Corporate Authentication (`LoginScreen.kt`)
- **UI/UX Pattern**: Production-grade Single Sign-On (SSO).
- **Features**:
  - Organization domain/code validation (`leavedesk.corp` or `CORP01`).
  - Employee ID or Corporate Email dual-resolution (`EMP101` or `alex.chen@leavedesk.corp`).
  - Secure password field with visibility toggle.
  - No internal demo credentials shown on screen.
  - Zero-flicker animated validation feedback.

---

## 2. Navigation & App Scaffolding (`MainActivity.kt`)
- **UI/UX Pattern**: Role-driven dynamic navigation bar.
  - **Employee**: `Home` | `Apply` | `Calendar` | `Requests` | `Chat`
  - **Team Lead**: `Home` | `Team Inbox` | `Calendar` | `My Leaves` | `Chat`
  - **CEO**: `Dashboard` | `Approvals` | `Calendar` | `Audit` | `Chat`
- **User Avatar & Profile Menu**:
  - Displays user initials, Full Name, Employee ID, Role Badge, Work Email, and Department.
  - Secure **Sign Out** button that clears session and returns to login.

---

## 3. Employee Workspace (`EmployeeScreens.kt`)
- **Home Screen**:
  - Live greeting banner with designation and group tags.
  - Multi-quota carousel (Casual `CL`, Sick `SL`, Earned `EL`, Half-Day `HD`, WFH, Short).
  - Quick action launcher ("Apply Leave", "Company Calendar", "My Requests").
  - Upcoming holidays banner and recent submission tracking.
- **Leave Application Form (`EmployeeApplyScreen`)**:
  - Leave type selector with remaining balance badges.
  - Interactive Date Range Pickers (Start Date, End Date).
  - Half-Day (0.5 day) toggle switch.
  - Automated Working Days computation (automatically subtracts weekends and gazetted public holidays).
  - Peer Backup Assignment dropdown (selects teammate from same group).
  - Document/proof attachment simulator (for Medical or Earned leave > 3 days).
  - Department presence validation (warns user if department headcount would drop below minimum threshold).
- **My Requests Screen (`EmployeeRequestsScreen`)**:
  - Status badges (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`).
  - Multi-step approval stepper (TL step, CEO step).
  - 48-Hour Escalation tag for overdue reviews.
  - Emergency self-cancellation button with balance restoration.

---

## 4. Team Lead Workspace (`TeamLeadScreens.kt`)
- **Team Lead Home**:
  - Live Team Presence Gauge (`x of y members active today`).
  - Department Minimum Presence Threshold indicator (e.g. minimum 3 engineers).
  - Pending action badges.
- **Team Inbox (`TeamLeadRequestsScreen`)**:
  - List of requests submitted by group subordinates.
  - Approval Rules:
    - 1–2 days non-ML leave: Team Lead approval is final (`APPROVED`).
    - 3+ days or Maternity Leave: Team Lead approval forwards request to CEO (`Escalated to CEO`).
  - Query dialog: allows TL to submit a question/feedback to the applicant before deciding.
  - Rejection dialog with mandatory reason text.

---

## 5. CEO Executive Workspace (`CeoScreens.kt`)
- **Executive Dashboard (`CeoDashboardScreen`)**:
  - Real-time KPI Metric Cards:
    - Total Headcount & Active Attendance Rate.
    - Escalated Requests Pending Final CEO Review (>48h flag).
    - Monthly Leave Utilization %.
  - 1-Tap Calendar Date Lock banner.
- **CEO Approvals Screen (`CeoApprovalsScreen`)**:
  - Review queue for escalated leaves, executive leaves, and department threshold overrides.
  - 1-Tap CEO Approval with automatic balance deduction and audit trail generation.
  - Rejection with mandatory justification.
- **Company Calendar Editor (`CeoCompanyCalendarEditor`)**:
  - 1-Tap Date Locking: CEO can lock any date to prevent employee leave applications (e.g., quarterly reviews, product releases).
  - Add custom public holidays and company closure days.
- **Audit Log Viewer (`CeoAuditScreen`)**:
  - Searchable compliance feed detailing actor, role, action, target, timestamp, and details.

---

## 6. Shared Team Utilities (`SharedScreens.kt`)
- **Company Interactive Calendar (`CompanyCalendarScreen`)**:
  - Month/Day view displaying locked CEO dates, public holidays, and approved team leaves.
- **Real-Time Team Chat (`QuickChatScreen`)**:
  - Threaded discussions for leave requests and general announcements.
- **Notification Center (`NotificationsScreen`)**:
  - 24-hour review reminders, 48-hour executive escalations, and calendar change broadcasts.

---

## 7. Business Logic Engine (`LeaveDeskRepository.kt`)
- **Reactive State Flow**:
  - `currentUser`: Current authenticated `User`
  - `isLoggedIn`: Boolean session state
  - `userBalances`: Map of `userId` -> `List<LeaveQuota>`
  - `leaveRequests`: Observable list of `LeaveRequest`
  - `companyHolidays`: Observable list of `CompanyHoliday`
  - `auditLogs`: Immutable history of `AuditLogEntry`
- **Core Functions**:
  - `login(identifier, password, organization)`: Validates credentials and returns boolean.
  - `logout()`: Clears active session.
  - `calculateWorkingDays(groupId, startDate, endDate, isHalfDay)`: Skips Saturdays, Sundays, and gazetted holidays.
  - `validatePresenceThreshold(groupId, startDate, endDate)`: Enforces departmental headcount minimums.
  - `submitLeaveRequest(...)`: Creates relational request, deducts balance, logs audit.
  - `tlApproveRequest(...)`: Approves or escalates to CEO.
  - `ceoApproveRequest(...)`: Final approval and balance settlement.
  - `emergencyCancelLeave(...)`: Restores used quota and updates audit log.
  - `ceoToggleDateLock(...)`: Locks/unlocks calendar date.
