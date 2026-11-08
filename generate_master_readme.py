import os
import zipfile

def read_file(path):
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            return f.read()
    return f"// File {path} not found"

doc_header = """# LeaveDesk Enterprise: Complete Architecture, System Design & Source Code Repository
**Format:** Single All-in-One Comprehensive README (Ultra-Low MB File)
**Application ID:** `com.aistudio.leavedesk.kxmpzq`
**Target Platform:** Android (Jetpack Compose, Kotlin, Material 3) + Node/SQL Cloud Backend

---

## Table of Contents
1. [Executive Summary & What LeaveDesk Is](#1-executive-summary--what-leavedesk-is)
2. [What, When, and How: System Architecture & Workflows](#2-what-when-and-how-system-architecture--workflows)
   - [2.1 Employee Role Workflow](#21-employee-role-workflow)
   - [2.2 Team Lead (TL) Role Workflow](#22-team-lead-tl-role-workflow)
   - [2.3 Chief Executive Officer (CEO) Role Workflow](#23-chief-executive-officer-ceo-role-workflow)
   - [2.4 Interactive Chat Box & Mobile Gallery Attachment Engine](#24-interactive-chat-box--mobile-gallery-attachment-engine)
   - [2.5 Leave Calculation, Quorum, & Policy Engine](#25-leave-calculation-quorum--policy-engine)
   - [2.6 Backend & Database Architecture](#26-backend--database-architecture)
3. [Test Accounts & Credentials](#3-test-accounts--credentials)
4. [Complete Production Source Code](#4-complete-production-source-code)
   - [File 1: app/build.gradle.kts](#file-1-appbuildgradlekts)
   - [File 2: app/src/main/AndroidManifest.xml](#file-2-appsrcmainandroidmanifestxml)
   - [File 3: app/src/main/java/com/example/model/Models.kt](#file-3-modelskt)
   - [File 4: app/src/main/java/com/example/data/LeaveDeskRepository.kt](#file-4-leavedeskrepositorykt)
   - [File 5: app/src/main/java/com/example/ui/theme/Color.kt](#file-5-colorkt)
   - [File 6: app/src/main/java/com/example/ui/theme/Theme.kt](#file-6-themett)
   - [File 7: app/src/main/java/com/example/ui/components/CommonComponents.kt](#file-7-commoncomponentskt)
   - [File 8: app/src/main/java/com/example/ui/screens/LoginScreen.kt](#file-8-loginscreenkt)
   - [File 9: app/src/main/java/com/example/ui/screens/SharedScreens.kt](#file-9-sharedscreenskt)
   - [File 10: app/src/main/java/com/example/ui/screens/EmployeeScreens.kt](#file-10-employeescreenskt)
   - [File 11: app/src/main/java/com/example/ui/screens/TeamLeadScreens.kt](#file-11-teamleadscreenskt)
   - [File 12: app/src/main/java/com/example/ui/screens/CeoScreens.kt](#file-12-ceoscreenskt)
   - [File 13: app/src/main/java/com/example/MainActivity.kt](#file-13-mainactivitykt)
   - [File 14: backend/schema.sql](#file-14-backendschemasql)
   - [File 15: backend/server.js](#file-15-backendserverjs)

---

## 1. Executive Summary & What LeaveDesk Is

### What It Is
LeaveDesk is a production-grade Enterprise Leave Management & Collaboration Platform built natively for Android using Jetpack Compose, Material 3, and Kotlin Coroutines, supported by a PostgreSQL/MySQL backend schema.

### Core Problems Solved
1. **Friction in Leave Governance**: Eliminates lost emails and confusing status checks by organizing leave workflows into strict multi-tier escalation channels.
2. **Context-Driven Clarifications (No Deadlock)**: Approvers (CEO / TL) do not have to just binary "Accept" or "Reject". They can **Query** an application, opening an interactive chat thread with attachment sharing where the employee can clarify and upload proof from their mobile gallery.
3. **Role Gating & Privacy Boundaries**:
   - Employees cannot cold-message the CEO; communication is strictly unlocked when the CEO initiates a query on a leave request.
   - Team Leads cannot message peer Team Leads, but have team-wide broadcast and direct communication channels with all members in their department.
4. **Group Quorum & Continuity Protection**: Prevents department understaffing by enforcing real-time minimum present thresholds (e.g. at least 3 active engineers present).
5. **Zero-Permission Gallery Integration**: Direct access to phone camera rolls and visual media through Android's zero-permission Photo Picker (`PickVisualMedia`) without intrusive legacy storage permissions.

---

## 2. What, When, and How: System Architecture & Workflows

### 2.1 Employee Role Workflow
- **WHAT**: Employees can view quota balances, inspect group calendars, apply for 6 leave types (CL, SL, PL, ML, PL_PAT, LOP), assign a mandatory backup colleague, attach medical/travel proof from the mobile gallery, track approval progress, request cancellations, and reply to queries.
- **WHEN**:
  - *Casual / Privilege Leave*: Applied at least 48 hours in advance.
  - *Sick Leave*: Can be applied on the day of illness. Requires hospital/medical proof if duration is $\ge 3$ days.
  - *Query Response*: Triggered immediately when a TL or CEO puts a request in `QUERY` status.
- **HOW**:
  - Validated reactively against `LeaveDeskRepository.validateLeaveApplication()`.
  - Attaches photos via `ActivityResultContracts.PickVisualMedia()` or documents via `ActivityResultContracts.GetContent()`.
  - Submissions notify the Team Lead first; if duration $> 5$ days or if the applicant is a Team Lead, it automatically escalates to the CEO.

### 2.2 Team Lead (TL) Role Workflow
- **WHAT**: Supervise department leave pipelines, verify backup coverage, monitor the live minimum present threshold, approve or reject applications, query with suggested alternate dates, attach sprint timelines, and trigger emergency cancellations.
- **WHEN**:
  - As soon as a team member submits an application.
  - When team bandwidth falls below the required threshold (e.g. $< 3$ present).
- **HOW**:
  - Approving instantly decrements user quota and registers the leave in the team calendar.
  - Querying updates the request to `QUERY` status and automatically posts the question + attachments to the private chat thread between the TL and the employee.

### 2.3 Chief Executive Officer (CEO) Role Workflow
- **WHAT**: Executive authority on company-wide operations, approvals for leaves $> 5$ days or submitted by Team Leads, executive overrides, querying employees directly with attachments, company holiday calendar locks/overrides, and complete compliance audit logging.
- **WHEN**:
  - Escalated automatically via multi-tier routing rules.
  - When national or emergency holidays need company-wide locking.
- **HOW**:
  - Executive dashboard displays real-time company stats.
  - Querying an employee delivers the inquiry directly to the employee's chat box with proof request attachments.
  - The employee can then reply back in the chat with proofs selected from their phone's gallery.

### 2.4 Interactive Chat Box & Mobile Gallery Attachment Engine
- **WHAT**: Real-time contextual chat that flows seamlessly with leave queries, supporting inline photo rendering, document pills, and zero-permission mobile gallery selection.
- **WHEN**:
  - Available at all times for TL $\leftrightarrow$ Team Member conversations.
  - Unlocked for Employee $\leftrightarrow$ CEO conversations as soon as the CEO raises a query.
- **HOW**:
  1. User taps paperclip `📎` in `QuickChatScreen`.
  2. Dialog prompts: **"Choose from Mobile Gallery"** or **"Browse Device Files (PDF / Docs)"**.
  3. Uses `rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())` for gallery images.
  4. Resolves `displayName`, `size`, and MIME type using Android `ContentResolver` (`OpenableColumns`).
  5. Renders a 28dp thumbnail chip in the composer.
  6. Upon sending, Coil's `AsyncImage` renders the photo inside the chat bubble.
  7. Tapping the bubble opens a full-screen viewer rendering directly from the local device URI.

### 2.5 Leave Calculation, Quorum, & Policy Engine
- **Quotas**: Casual (12/yr), Sick (10/yr), Privilege (15/yr), Maternity (180 days), Paternity (15 days), Loss of Pay (uncapped).
- **Half-Day Sessions**: Supports "First Half" and "Second Half" deductions calculated as $0.5$ days.
- **Sandwich Rule**: If leave spans across weekends or holidays with leave on both sides, intervening holidays are flagged and counted.
- **Group Quorum**: If approvals would bring active group headcount below `minPresentThreshold`, a warning blocks submission unless emergency flag is set.
- **Auto-Delegation**: Requests pending without review for $> 72$ hours escalate to the next management level.
- **Audit Logging**: Every transition (APPLY, APPROVE, REJECT, QUERY, QUERY_REPLY, CANCEL) records an immutable entry with timestamp, actor ID, actor role, and reason.

### 2.6 Backend & Database Architecture
- **Database Engine**: Relational schema (PostgreSQL / MySQL) with foreign keys, indexes, and ACID transaction safety.
- **Storage**: Object storage (AWS S3 / GCP Cloud Storage / MinIO) for media proofs with pre-signed URLs.
- **Real-Time Layer**: WebSocket or Socket.io gateway for live chat broadcasts and push notifications.

---

## 3. Test Accounts & Credentials
Use these verified enterprise credentials to test each role in the app:

| Role | Employee ID | Email | Password | Group / Department |
| :--- | :--- | :--- | :--- | :--- |
| **CEO** | `CEO001` | `sarah.jenkins@leavedesk.corp` | `password123` | Executive Leadership |
| **Team Lead** | `TL201` | `marcus.vance@leavedesk.corp` | `password123` | Core Platform Engineering |
| **Employee** | `EMP101` | `alex.chen@leavedesk.corp` | `password123` | Core Platform Engineering |
| **Employee** | `EMP102` | `priya.sharma@leavedesk.corp` | `password123` | Core Platform Engineering |
| **Org Domain** | - | `leavedesk.corp` | - | Enterprise Default |

---

## 4. Complete Production Source Code
Below is the complete, pristine source code for all project files.
"""

files_to_embed = [
    ("File 1: app/build.gradle.kts", "kotlin", "app/build.gradle.kts"),
    ("File 2: app/src/main/AndroidManifest.xml", "xml", "app/src/main/AndroidManifest.xml"),
    ("File 3: app/src/main/java/com/example/model/Models.kt", "kotlin", "app/src/main/java/com/example/model/Models.kt"),
    ("File 4: app/src/main/java/com/example/data/LeaveDeskRepository.kt", "kotlin", "app/src/main/java/com/example/data/LeaveDeskRepository.kt"),
    ("File 5: app/src/main/java/com/example/ui/theme/Color.kt", "kotlin", "app/src/main/java/com/example/ui/theme/Color.kt"),
    ("File 6: app/src/main/java/com/example/ui/theme/Theme.kt", "kotlin", "app/src/main/java/com/example/ui/theme/Theme.kt"),
    ("File 7: app/src/main/java/com/example/ui/components/CommonComponents.kt", "kotlin", "app/src/main/java/com/example/ui/components/CommonComponents.kt"),
    ("File 8: app/src/main/java/com/example/ui/screens/LoginScreen.kt", "kotlin", "app/src/main/java/com/example/ui/screens/LoginScreen.kt"),
    ("File 9: app/src/main/java/com/example/ui/screens/SharedScreens.kt", "kotlin", "app/src/main/java/com/example/ui/screens/SharedScreens.kt"),
    ("File 10: app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "kotlin", "app/src/main/java/com/example/ui/screens/EmployeeScreens.kt"),
    ("File 11: app/src/main/java/com/example/ui/screens/TeamLeadScreens.kt", "kotlin", "app/src/main/java/com/example/ui/screens/TeamLeadScreens.kt"),
    ("File 12: app/src/main/java/com/example/ui/screens/CeoScreens.kt", "kotlin", "app/src/main/java/com/example/ui/screens/CeoScreens.kt"),
    ("File 13: app/src/main/java/com/example/MainActivity.kt", "kotlin", "app/src/main/java/com/example/MainActivity.kt"),
    ("File 14: backend/schema.sql", "sql", "backend/schema.sql"),
    ("File 15: backend/server.js", "javascript", "backend/server.js"),
]

readme_content = doc_header

for title, lang, path in files_to_embed:
    content = read_file(path)
    readme_content += f"\n\n### {title}\n"
    readme_content += f"**File Path:** `{path}`\n\n"
    readme_content += f"```{lang}\n{content}\n```\n"

# 1. Write to workspace README.md
with open("README.md", "w", encoding="utf-8") as f:
    f.write(readme_content)
print("Wrote README.md (size:", len(readme_content), "bytes)")

# 2. Write to artifact directory
artifact_dir = "/.aistudio/artifacts/brain/3e47b693-83df-42bd-ab08-614d7bda8915"
artifact_path = os.path.join(artifact_dir, "LeaveDesk_Complete_Source_And_Architecture.md")
with open(artifact_path, "w", encoding="utf-8") as f:
    f.write(readme_content)
print("Wrote artifact:", artifact_path)

# 3. Create a tiny zip file containing README.md
zip_name = "LeaveDesk_Readme_And_Source.zip"
with zipfile.ZipFile(zip_name, "w", zipfile.ZIP_DEFLATED) as zipf:
    zipf.write("README.md", arcname="README.md")
    if os.path.exists("backend/schema.sql"):
        zipf.write("backend/schema.sql", arcname="schema.sql")
    if os.path.exists("LeaveDesk_FRD.md"):
        zipf.write("LeaveDesk_FRD.md", arcname="LeaveDesk_FRD.md")
print("Created zip archive:", zip_name, "size:", os.path.getsize(zip_name), "bytes")
