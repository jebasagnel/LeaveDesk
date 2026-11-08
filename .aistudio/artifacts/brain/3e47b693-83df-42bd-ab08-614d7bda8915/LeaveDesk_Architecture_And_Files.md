# LeaveDesk - Final Codebase & Project Files

This document provides a complete directory of all files, source code components, and instructions for running and exporting the final **LeaveDesk** Android project.

---

## 📂 Project Directory Structure

```text
LeaveDesk/
├── app/
│   ├── build.gradle.kts                # App-level dependencies, plugins & compile configs
│   ├── proguard-rules.pro              # Proguard optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     # Application manifest & intent filters
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt     # App entry, bottom nav bar, role state shell
│       │   │   ├── model/
│       │   │   │   └── Models.kt       # Data classes (LeaveRequest, LeaveType, User, Holiday, etc.)
│       │   │   ├── data/
│       │   │   │   └── LeaveDeskRepository.kt # Reactive StateFlow, policies, validations & audit
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Color.kt    # Role palettes (Teal, Indigo, Purple) & status colors
│       │   │   │   │   ├── Theme.kt    # Role-adaptive Material 3 theme provider
│       │   │   │   │   └── Type.kt     # Typography specs
│       │   │   │   ├── components/
│       │   │   │   │   ├── CommonComponents.kt   # Status badges, StatCards, Timeline, CalendarDatePickerDialog
│       │   │   │   │   └── RoleSwitcherModal.kt  # Fast 1-tap persona & role switcher modal
│       │   │   │   └── screens/
│       │   │   │       ├── EmployeeScreens.kt   # Employee Home, Apply Leave step form, My Requests
│       │   │   │       ├── TeamLeadScreens.kt   # Team Lead Inbox, Coverage checks, Group overrides
│       │   │   │       ├── CeoScreens.kt        # Executive Dashboard, Approval Queue, Calendar & Lock Manager, Audit
│       │   │   │       ├── SharedScreens.kt     # Company Calendar, Quick Chat Support, Notifications
│       │   │   │       └── LoginScreen.kt       # Role & persona picker demo screen
│       │   └── res/
│       │       ├── drawable/
│       │       │   ├── ic_launcher_background.xml # Adaptive icon background (#1F8A8A)
│       │       │   ├── ic_launcher_foreground.xml # Adaptive icon foreground layer
│       │       │   └── leavedesk_icon_*.jpg       # High-resolution generated app icon
│       │       ├── mipmap-*/                      # Custom raster launcher icons (PNGs)
│       │       └── values/
│       │           ├── strings.xml     # App name ("LeaveDesk")
│       │           ├── colors.xml      # Color resources
│       │           └── themes.xml      # Splash theme
│       └── test/java/com/example/
│           └── ExampleUnitTest.kt      # Unit tests (working days, 2-step routing, locks, SLAs)
├── gradle/
│   ├── libs.versions.toml              # Version catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts                    # Root build configuration
├── settings.gradle.kts                 # Root project name ("LeaveDesk") & modules
├── metadata.json                       # AI Studio project platform identity
├── .env.example                        # Environment variable documentation
└── LeaveDesk_Project.zip               # Complete pre-packaged project ZIP
```

---

## 🛠 Key Code File Overview

| File Path | Description |
|---|---|
| `app/src/main/java/com/example/MainActivity.kt` | Host activity featuring the 390px mobile-first container, role-switching bottom navigation bar, notification bell badge, profile trigger, and backstack handling. |
| `app/src/main/java/com/example/model/Models.kt` | Core types: `User`, `TeamGroup`, `LeaveType` (CL, SL, EL, ML, BL, LOP, HD, WFH, Short), `LeaveStatus`, `CompanyHoliday`, `AuditLogEntry`, `AppNotification`, and `ChatMessage`. |
| `app/src/main/java/com/example/data/LeaveDeskRepository.kt` | Comprehensive business logic repository containing live working-day calculation, team attendance threshold validation, 2-step approval routing, instant date locking, auto-recalculation of affected leaves upon holiday publishing, notifications, and immutable audit trails. |
| `app/src/main/java/com/example/ui/screens/CeoScreens.kt` | Executive Suite containing CEO Dashboard KPIs, Approval Queue with Exception overrides, **Interactive Monthly Calendar with 1-Tap Date Lock/Unlock**, Policy Publisher, and Searchable Audit Log. |
| `app/src/main/java/com/example/ui/screens/EmployeeScreens.kt` | Employee experience: Balance cards with remaining quotas, single-screen step form with visual `CalendarDatePickerDialog` for start/end dates, team slot availability strips, proof upload chip, backup colleague assignment, and request tracking with 24h reminders and 48h escalation flags. |
| `app/src/main/java/com/example/ui/screens/TeamLeadScreens.kt` | Team Lead portal: Urgent team review inbox displaying "Who else is off those dates", coverage threshold warnings, endorsements, queries, and emergency cancellations. |
| `app/src/main/java/com/example/ui/screens/SharedScreens.kt` | Company-wide Calendar with color legend and tap details, threaded Quick Chat Support linked to leave requests, and bell notifications. |
| `app/src/main/java/com/example/ui/components/CommonComponents.kt` | Reusable UI atoms: `CalendarDatePickerDialog`, `LeaveStatusBadge`, `LeaveTypeBadge`, `StatCard`, `RequestTimeline`, and `EmergencyMailDialog`. |

---

## 📥 How to Download & Export the Project

You can obtain the entire project using any of these methods:

1. **AI Studio Settings (Export as ZIP)**:
   - Click the **Settings** or **Project Menu** icon in the AI Studio header.
   - Choose **Export as ZIP** to download the complete codebase directly to your computer.
   - You can also select **Push to GitHub** to publish the project to a GitHub repository.

2. **Pre-Built Archive in Workspace**:
   - The file `LeaveDesk_Project.zip` has been created directly in the workspace root.
   - It contains all Kotlin sources, resources, Gradle configurations, and unit tests ready to be opened in **Android Studio**.
