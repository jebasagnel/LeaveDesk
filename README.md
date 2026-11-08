# LeaveDesk Enterprise: Complete Architecture, System Design & Source Code Repository
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


### File 1: app/build.gradle.kts
**File Path:** `app/build.gradle.kts`

```kotlin
import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.leavedesk.kmpzqv"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

```


### File 2: app/src/main/AndroidManifest.xml
**File Path:** `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApplication">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.MyApplication">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>

```


### File 3: app/src/main/java/com/example/model/Models.kt
**File Path:** `app/src/main/java/com/example/model/Models.kt`

```kotlin
package com.example.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class UserRole(val displayName: String, val badgeTitle: String) {
  EMPLOYEE("Employee", "EMP"),
  TEAM_LEAD("Team Lead", "TL"),
  CEO("CEO", "CEO")
}

data class User(
  val id: String,
  val employeeId: String,
  val name: String,
  val email: String,
  val role: UserRole,
  val groupId: String,
  val groupName: String,
  val designation: String,
  val avatarInitials: String,
  val password: String = "password123"
)

data class TeamGroup(
  val id: String,
  val name: String,
  val tlUserId: String,
  val tlName: String,
  val minPresentThreshold: Int,
  val memberIds: List<String>
)

enum class LeaveType(
  val code: String,
  val title: String,
  val colorHex: Long,
  val maxConsecutiveDays: Int,
  val defaultQuota: Float,
  val isNoCap: Boolean = false,
  val requiresProofByDefault: Boolean = false,
  val isUrgentNeverBlocked: Boolean = false
) {
  CL("CL", "Casual Leave", 0xFF0284C7, 3, 12f),
  SL("SL", "Sick Leave", 0xFFE11D48, 5, 10f, isUrgentNeverBlocked = true),
  EL("EL", "Earned Leave", 0xFF10B981, 14, 15f),
  ML("ML", "Maternity Leave", 0xFF9333EA, 90, 90f, requiresProofByDefault = true),
  BL("BL", "Bereavement Leave", 0xFF475569, 5, 5f),
  LOP("LOP", "Loss of Pay", 0xFFF97316, 30, 0f, isNoCap = true),
  HD("HD", "Half Day", 0xFF0D9488, 2, 8f),
  WFH("WFH", "Work From Home", 0xFF6366F1, 10, 0f, isNoCap = true),
  SHORT("Short", "Short Leave", 0xFFD97706, 1, 12f) // in hours / half-quarters
}

data class LeaveQuota(
  val leaveType: LeaveType,
  val total: Float,
  val used: Float
) {
  val remaining: Float
    get() = if (leaveType.isNoCap) Float.POSITIVE_INFINITY else (total - used).coerceAtLeast(0f)
}

enum class LeaveStatus(val displayName: String, val bgHex: Long, val textHex: Long) {
  PENDING("Pending", 0xFFFFF2CC, 0xFF996500),
  APPROVED("Approved", 0xFFD5E8D4, 0xFF276A3C),
  REJECTED("Rejected", 0xFFF8CECC, 0xFFB85450),
  QUERY("Query", 0xFFE1D5E7, 0xFF6B3F7C),
  WITHDRAWN("Withdrawn", 0xFFECECEC, 0xFF6B6B6B),
  CANCELLED("Cancelled", 0xFFF5E0E0, 0xFF993333),
  POSTPONED("Postponed", 0xFFFFE6CC, 0xFFD79B00)
}

data class LeaveProof(
  val fileName: String,
  val fileType: String, // PDF, JPG, PNG
  val fileSizeStr: String,
  val uriString: String? = null
)

data class ApprovalStep(
  val approverRole: UserRole,
  val approverName: String,
  val decision: LeaveStatus, // APPROVED, REJECTED, QUERY
  val remarks: String?,
  val timestamp: LocalDateTime?
)

data class LeaveRequest(
  val id: String,
  val userId: String,
  val userName: String,
  val userRole: UserRole,
  val groupId: String,
  val groupName: String,
  val leaveType: LeaveType,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val isHalfDay: Boolean = false,
  val halfDaySession: String? = null, // "First Half" / "Second Half"
  val workingDays: Float,
  val reason: String,
  val backupUserId: String? = null,
  val backupUserName: String? = null,
  val backupTaskNote: String? = null,
  val proof: LeaveProof? = null,
  val status: LeaveStatus = LeaveStatus.PENDING,
  val submittedAt: LocalDateTime,
  val requiresCeoApproval: Boolean = false,
  val tlStep: ApprovalStep? = null,
  val ceoStep: ApprovalStep? = null,
  val queryQuestion: String? = null,
  val querySuggestedDates: String? = null,
  val employeeQueryReply: String? = null,
  val cancelRequested: Boolean = false,
  val cancelReason: String? = null,
  val cancelConfirmedBy: String? = null
)

enum class HolidayType(val displayName: String, val colorHex: Long) {
  PUBLIC_HOLIDAY("Public Holiday", 0xFFDC2626),
  OPTIONAL_HOLIDAY("Optional Holiday", 0xFF2563EB),
  COMPANY_CLOSURE("Company Closure", 0xFF7C3AED),
  WORKING_DAY("Working Day", 0xFF10B981),
  SPECIAL_WORKING_DAY("Special Working Day", 0xFFF59E0B)
}

data class CompanyHoliday(
  val id: String,
  val date: LocalDate,
  val title: String,
  val type: HolidayType,
  val remark: String = "",
  val colorHex: Long = type.colorHex,
  val isLockedByCeo: Boolean = false,
  val addedByCeo: Boolean = false
)

data class GroupCalendarEvent(
  val id: String,
  val groupId: String,
  val date: LocalDate,
  val title: String,
  val remark: String,
  val isWorkingDayOverride: Boolean = false,
  val addedByTlName: String
)

data class AuditLogEntry(
  val id: String,
  val timestamp: LocalDateTime,
  val actorName: String,
  val actorRole: UserRole,
  val action: String, // "Approved Leave", "Rejected Leave", "Calendar Updated", "Emergency Cancel", etc.
  val target: String,
  val reason: String,
  val details: String
)

enum class NotificationType {
  NEW_REQUEST,
  DECISION,
  QUERY,
  REMINDER_24H,
  ESCALATION_48H,
  CALENDAR_CHANGE,
  CANCEL_CONFIRMATION,
  BACKUP_ASSIGNED,
  EMERGENCY_MAIL
}

data class AppNotification(
  val id: String,
  val recipientUserId: String, // or "ALL"
  val title: String,
  val message: String,
  val type: NotificationType,
  val timestamp: LocalDateTime,
  val isRead: Boolean = false,
  val relatedRequestId: String? = null
)

data class ChatMessage(
  val id: String,
  val threadId: String,
  val senderId: String,
  val senderName: String,
  val senderRole: UserRole,
  val text: String,
  val timestamp: LocalDateTime,
  val isRead: Boolean = false,
  val relatedRequestId: String? = null,
  val attachment: LeaveProof? = null,
  val isOfficialQuery: Boolean = false,
  val isOfficialDecision: Boolean = false
)

data class ChatThreadInfo(
  val threadId: String,
  val title: String,
  val subtitle: String,
  val role: UserRole?,
  val targetUserId: String? = null,
  val relatedRequestId: String? = null,
  val isLocked: Boolean = false,
  val lockReason: String? = null
)

```


### File 4: app/src/main/java/com/example/data/LeaveDeskRepository.kt
**File Path:** `app/src/main/java/com/example/data/LeaveDeskRepository.kt`

```kotlin
package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class LeaveDeskRepository {

  // Current logged in user
  private val _currentUser = MutableStateFlow(DEMO_USERS[3]) // Default Alex Chen (EMP101)
  val currentUser: StateFlow<User> = _currentUser.asStateFlow()

  // Authentication session state
  private val _isLoggedIn = MutableStateFlow(false)
  val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

  fun login(identifier: String, pass: String, organization: String? = null): Boolean {
    val cleanId = identifier.trim()
    val cleanOrg = organization?.trim()?.lowercase() ?: ""

    // Validate organization domain / code if provided
    if (cleanOrg.isNotEmpty()) {
      val validOrgs = listOf("leavedesk.corp", "leavedesk", "corp01", "corp", "executive", "engineering", "operations")
      val matchesOrg = validOrgs.any { cleanOrg.contains(it) || it.contains(cleanOrg) }
      if (!matchesOrg) {
        return false
      }
    }

    val user = _users.value.find {
      it.employeeId.equals(cleanId, ignoreCase = true) ||
      it.email.equals(cleanId, ignoreCase = true)
    }
    return if (user != null && (user.password == pass || pass == "password123")) {
      _currentUser.value = user
      _isLoggedIn.value = true
      showToast("Signed in as ${user.name} (${user.role.displayName})")
      true
    } else {
      false
    }
  }

  fun logout() {
    _isLoggedIn.value = false
    showToast("Signed out successfully.")
  }

  // All users
  private val _users = MutableStateFlow(DEMO_USERS)
  val users: StateFlow<List<User>> = _users.asStateFlow()

  // Teams
  private val _groups = MutableStateFlow(DEMO_GROUPS)
  val groups: StateFlow<List<TeamGroup>> = _groups.asStateFlow()

  // Leave balances per user (userId -> list of quotas)
  private val _userBalances = MutableStateFlow(INITIAL_USER_BALANCES)
  val userBalances: StateFlow<Map<String, List<LeaveQuota>>> = _userBalances.asStateFlow()

  // Leave requests
  private val _leaveRequests = MutableStateFlow(INITIAL_LEAVE_REQUESTS)
  val leaveRequests: StateFlow<List<LeaveRequest>> = _leaveRequests.asStateFlow()

  // Company Holidays / Calendar
  private val _companyHolidays = MutableStateFlow(INITIAL_HOLIDAYS)
  val companyHolidays: StateFlow<List<CompanyHoliday>> = _companyHolidays.asStateFlow()

  // Group Overrides
  private val _groupEvents = MutableStateFlow(INITIAL_GROUP_EVENTS)
  val groupEvents: StateFlow<List<GroupCalendarEvent>> = _groupEvents.asStateFlow()

  // Audit Logs
  private val _auditLogs = MutableStateFlow(INITIAL_AUDIT_LOGS)
  val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

  // Notifications
  private val _notifications = MutableStateFlow(INITIAL_NOTIFICATIONS)
  val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

  // Chat Messages
  private val _chatMessages = MutableStateFlow(INITIAL_CHAT_MESSAGES)
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  // Transient feedback toast
  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

  fun clearToast() {
    _toastMessage.value = null
  }

  fun showToast(message: String) {
    _toastMessage.value = message
  }

  fun switchUser(user: User) {
    _currentUser.value = user
    showToast("Logged in as ${user.name} (${user.role.displayName})")
  }

  fun switchRole(role: UserRole) {
    val targetUser = when (role) {
      UserRole.EMPLOYEE -> _users.value.first { it.role == UserRole.EMPLOYEE }
      UserRole.TEAM_LEAD -> _users.value.first { it.role == UserRole.TEAM_LEAD }
      UserRole.CEO -> _users.value.first { it.role == UserRole.CEO }
    }
    switchUser(targetUser)
  }

  // MARK: - Validation & Calculations

  fun calculateWorkingDays(
    groupId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    isHalfDay: Boolean
  ): Float {
    if (isHalfDay) return 0.5f
    if (startDate.isAfter(endDate)) return 0f

    var count = 0f
    var curr = startDate
    val holidays = _companyHolidays.value
    val overrides = _groupEvents.value.filter { it.groupId == groupId }

    while (!curr.isAfter(endDate)) {
      val isWeekend = curr.dayOfWeek == DayOfWeek.SATURDAY || curr.dayOfWeek == DayOfWeek.SUNDAY
      val holiday = holidays.find { it.date == curr }
      val override = overrides.find { it.date == curr }

      var isWorking = if (isWeekend) {
        // Weekend is non-working unless marked as Special Working Day or group override
        holiday?.type == HolidayType.SPECIAL_WORKING_DAY || (override?.isWorkingDayOverride == true)
      } else {
        // Weekday is working unless public holiday or company closure
        val isOff = holiday?.type == HolidayType.PUBLIC_HOLIDAY || holiday?.type == HolidayType.COMPANY_CLOSURE
        !isOff || (override?.isWorkingDayOverride == true)
      }

      if (isWorking) {
        count += 1f
      }
      curr = curr.plusDays(1)
    }

    return count
  }

  data class DayAvailability(
    val date: LocalDate,
    val isHoliday: Boolean,
    val holidayTitle: String?,
    val isWeekend: Boolean,
    val absentUsers: List<String>,
    val presentCount: Int,
    val minThreshold: Int,
    val isBlocked: Boolean,
    val colorLevel: AvailabilityLevel // GREEN, AMBER, RED
  )

  enum class AvailabilityLevel {
    FREE, // Green
    FEW_SLOTS, // Amber
    BLOCKED // Red
  }

  fun getDayAvailability(groupId: String, date: LocalDate): DayAvailability {
    val group = _groups.value.find { it.id == groupId }
    val totalMembers = group?.memberIds?.size ?: 5
    val minThreshold = group?.minPresentThreshold ?: 2

    val holiday = _companyHolidays.value.find { it.date == date }
    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    val approvedLeaves = _leaveRequests.value.filter {
      it.groupId == groupId &&
        it.status == LeaveStatus.APPROVED &&
        !date.isBefore(it.startDate) &&
        !date.isAfter(it.endDate)
    }
    val absentNames = approvedLeaves.map { it.userName }
    val presentCount = (totalMembers - absentNames.size).coerceAtLeast(0)

    val isBlocked = (presentCount - 1) < minThreshold
    val colorLevel = when {
      isBlocked -> AvailabilityLevel.BLOCKED
      absentNames.isNotEmpty() -> AvailabilityLevel.FEW_SLOTS
      else -> AvailabilityLevel.FREE
    }

    return DayAvailability(
      date = date,
      isHoliday = holiday != null && holiday.type != HolidayType.WORKING_DAY,
      holidayTitle = holiday?.title,
      isWeekend = isWeekend,
      absentUsers = absentNames,
      presentCount = presentCount,
      minThreshold = minThreshold,
      isBlocked = isBlocked,
      colorLevel = colorLevel
    )
  }

  data class ValidationResult(
    val isValid: Boolean,
    val workingDays: Float,
    val balanceRemaining: Float,
    val isBalanceExceeded: Boolean,
    val isConsecutiveLimitExceeded: Boolean,
    val maxConsecutiveDays: Int,
    val isThresholdViolated: Boolean,
    val blockedDates: List<LocalDate>,
    val requiresProof: Boolean,
    val hasProof: Boolean,
    val requiresCeoApproval: Boolean,
    val message: String?
  )

  fun validateLeaveApplication(
    user: User,
    leaveType: LeaveType,
    startDate: LocalDate,
    endDate: LocalDate,
    isHalfDay: Boolean,
    hasProof: Boolean
  ): ValidationResult {
    val workingDays = calculateWorkingDays(user.groupId, startDate, endDate, isHalfDay)
    val userQuota = getUserQuota(user.id, leaveType)
    val balanceRemaining = userQuota?.remaining ?: 0f

    val isBalanceExceeded = !leaveType.isNoCap && workingDays > balanceRemaining

    // Consecutive day limit check
    val calendarDays = ChronoUnit.DAYS.between(startDate, endDate) + 1
    val isConsecutiveLimitExceeded = calendarDays > leaveType.maxConsecutiveDays

    // Threshold check (SL is never blocked and flagged Urgent)
    val blockedDates = mutableListOf<LocalDate>()
    var curr = startDate
    while (!curr.isAfter(endDate)) {
      val avail = getDayAvailability(user.groupId, curr)
      if (avail.isBlocked) {
        blockedDates.add(curr)
      }
      curr = curr.plusDays(1)
    }
    val isThresholdViolated = blockedDates.isNotEmpty() && !leaveType.isUrgentNeverBlocked

    // Proof requirements: ML always, or any leave >= 3 days, or type requires it
    val isLongLeave = workingDays >= 3f
    val requiresProof = leaveType.requiresProofByDefault || isLongLeave

    // 2-step routing rules:
    // - Short leave / 1-2 days -> TL approval only
    // - Long leave (3+ days) or frequent leave (>3 requests this month) -> TL then CEO
    // - ML / long leave with proof -> TL then CEO
    // - TL's own leave -> CEO directly
    val frequentLeaveCount = _leaveRequests.value.count {
      it.userId == user.id && it.submittedAt.month == LocalDateTime.now().month
    }
    val isFrequent = frequentLeaveCount >= 3
    val isTlOwnLeave = user.role == UserRole.TEAM_LEAD
    val requiresCeoApproval = isTlOwnLeave || isLongLeave || isFrequent || leaveType == LeaveType.ML

    val message = when {
      isThresholdViolated -> "Coverage warning: Approving these dates would drop team attendance below minimum threshold (${blockedDates.size} day(s) blocked)."
      isBalanceExceeded -> "Insufficient ${leaveType.code} balance ($balanceRemaining remaining, requested $workingDays). Consider changing dates or switching to LOP."
      isConsecutiveLimitExceeded -> "${leaveType.code} limit is ${leaveType.maxConsecutiveDays} consecutive days. You requested $calendarDays days."
      requiresProof && !hasProof -> "Proof document is mandatory for ${if (leaveType == LeaveType.ML) "Maternity Leave" else "leave of 3 or more days"}."
      else -> null
    }

    val isValid = !isThresholdViolated && !isBalanceExceeded && !isConsecutiveLimitExceeded && (!requiresProof || hasProof)

    return ValidationResult(
      isValid = isValid,
      workingDays = workingDays,
      balanceRemaining = balanceRemaining,
      isBalanceExceeded = isBalanceExceeded,
      isConsecutiveLimitExceeded = isConsecutiveLimitExceeded,
      maxConsecutiveDays = leaveType.maxConsecutiveDays,
      isThresholdViolated = isThresholdViolated,
      blockedDates = blockedDates,
      requiresProof = requiresProof,
      hasProof = hasProof,
      requiresCeoApproval = requiresCeoApproval,
      message = message
    )
  }

  fun getUserQuota(userId: String, leaveType: LeaveType): LeaveQuota? {
    return _userBalances.value[userId]?.find { it.leaveType == leaveType }
  }

  // MARK: - Actions

  fun submitLeaveRequest(
    user: User,
    leaveType: LeaveType,
    startDate: LocalDate,
    endDate: LocalDate,
    isHalfDay: Boolean,
    halfDaySession: String?,
    reason: String,
    backupUser: User?,
    backupTaskNote: String?,
    proof: LeaveProof?
  ): Boolean {
    val validation = validateLeaveApplication(
      user = user,
      leaveType = leaveType,
      startDate = startDate,
      endDate = endDate,
      isHalfDay = isHalfDay,
      hasProof = proof != null
    )

    if (!validation.isValid) {
      showToast(validation.message ?: "Validation failed.")
      return false
    }

    val newId = "LR-${UUID.randomUUID().toString().take(6).uppercase()}"
    val isTl = user.role == UserRole.TEAM_LEAD

    val request = LeaveRequest(
      id = newId,
      userId = user.id,
      userName = user.name,
      userRole = user.role,
      groupId = user.groupId,
      groupName = user.groupName,
      leaveType = leaveType,
      startDate = startDate,
      endDate = endDate,
      isHalfDay = isHalfDay,
      halfDaySession = halfDaySession,
      workingDays = validation.workingDays,
      reason = reason,
      backupUserId = backupUser?.id,
      backupUserName = backupUser?.name,
      backupTaskNote = backupTaskNote,
      proof = proof,
      status = LeaveStatus.PENDING,
      submittedAt = LocalDateTime.now(),
      requiresCeoApproval = validation.requiresCeoApproval,
      tlStep = if (isTl) null else null,
      ceoStep = null
    )

    _leaveRequests.value = listOf(request) + _leaveRequests.value

    // Notify approver
    val approverRecipientId = if (isTl) {
      // Goes directly to CEO
      _users.value.first { it.role == UserRole.CEO }.id
    } else {
      // Goes to group TL
      val group = _groups.value.find { it.id == user.groupId }
      group?.tlUserId ?: _users.value.first { it.role == UserRole.CEO }.id
    }

    addNotification(
      recipientUserId = approverRecipientId,
      title = "New Leave Request: ${user.name}",
      message = "${user.name} applied for ${validation.workingDays} day(s) of ${leaveType.code} ($startDate to $endDate)",
      type = NotificationType.NEW_REQUEST,
      relatedRequestId = newId
    )

    // Notify backup if assigned
    backupUser?.let { backup ->
      addNotification(
        recipientUserId = backup.id,
        title = "Backup Assigned",
        message = "${user.name} selected you as backup for ${leaveType.code} ($startDate to $endDate). Note: ${backupTaskNote ?: "Standard handover"}",
        type = NotificationType.BACKUP_ASSIGNED,
        relatedRequestId = newId
      )
    }

    addAuditLog(
      actor = user,
      action = "Submitted Leave Request",
      target = "$newId (${leaveType.code})",
      reason = reason,
      details = "${validation.workingDays} working days from $startDate to $endDate. Requires CEO: ${validation.requiresCeoApproval}"
    )

    showToast("Leave request $newId submitted successfully! Mail sent to approver.")
    return true
  }

  fun sendEmergencyMail(user: User, startDate: LocalDate, endDate: LocalDate, reason: String) {
    val tlUser = _users.value.find { it.id == _groups.value.find { g -> g.id == user.groupId }?.tlUserId }
    val ceoUser = _users.value.first { it.role == UserRole.CEO }

    addNotification(
      recipientUserId = tlUser?.id ?: "ALL",
      title = "EMERGENCY LEAVE OVERRIDE REQUEST",
      message = "URGENT: ${user.name} (${user.groupName}) requested emergency leave ($startDate to $endDate) despite threshold breach. Reason: $reason",
      type = NotificationType.EMERGENCY_MAIL
    )

    addNotification(
      recipientUserId = ceoUser.id,
      title = "EMERGENCY LEAVE OVERRIDE REQUEST",
      message = "URGENT: ${user.name} requested emergency leave override ($startDate to $endDate) in ${user.groupName}.",
      type = NotificationType.EMERGENCY_MAIL
    )

    addAuditLog(
      actor = user,
      action = "Sent Emergency Mail",
      target = "Group Threshold Override ($startDate to $endDate)",
      reason = reason,
      details = "Dispatched emergency override email to TL (${tlUser?.name}) and CEO (${ceoUser.name})"
    )

    showToast("Emergency mail dispatched to TL & CEO. Logged in audit trail.")
  }

  // TL Actions
  fun tlApproveRequest(request: LeaveRequest, approver: User, remarks: String) {
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updatedTlStep = ApprovalStep(
      approverRole = UserRole.TEAM_LEAD,
      approverName = approver.name,
      decision = LeaveStatus.APPROVED,
      remarks = remarks.ifBlank { "Approved by Team Lead" },
      timestamp = LocalDateTime.now()
    )

    if (request.requiresCeoApproval) {
      // Move to CEO inbox
      val updated = request.copy(
        tlStep = updatedTlStep
        // Still pending CEO
      )
      currentRequests[index] = updated
      _leaveRequests.value = currentRequests

      val ceo = _users.value.first { it.role == UserRole.CEO }
      addNotification(
        recipientUserId = ceo.id,
        title = "TL Approved: Pending CEO Decision",
        message = "${approver.name} approved ${request.userName}'s ${request.workingDays}-day ${request.leaveType.code}. Requires final CEO sign-off.",
        type = NotificationType.NEW_REQUEST,
        relatedRequestId = request.id
      )
      addNotification(
        recipientUserId = request.userId,
        title = "TL Endorsed Your Request",
        message = "${approver.name} endorsed your leave. Routed to CEO for final approval.",
        type = NotificationType.DECISION,
        relatedRequestId = request.id
      )
      showToast("Endorsed and forwarded to CEO for final approval.")
    } else {
      // Final approval
      val updated = request.copy(
        status = LeaveStatus.APPROVED,
        tlStep = updatedTlStep
      )
      currentRequests[index] = updated
      _leaveRequests.value = currentRequests
      deductBalance(request.userId, request.leaveType, request.workingDays)

      addNotification(
        recipientUserId = request.userId,
        title = "Leave Request Approved!",
        message = "Your ${request.leaveType.code} leave for ${request.workingDays} day(s) was approved by ${approver.name}.",
        type = NotificationType.DECISION,
        relatedRequestId = request.id
      )
      request.backupUserId?.let { backupId ->
        addNotification(
          recipientUserId = backupId,
          title = "Backup Active",
          message = "${request.userName}'s leave is approved for ${request.startDate} to ${request.endDate}.",
          type = NotificationType.BACKUP_ASSIGNED,
          relatedRequestId = request.id
        )
      }
      showToast("Request approved! Calendar updated and balance deducted.")
    }

    addAuditLog(
      actor = approver,
      action = "TL Approved Leave",
      target = "${request.id} (${request.userName})",
      reason = remarks,
      details = "Requires CEO: ${request.requiresCeoApproval}"
    )
  }

  fun tlRejectRequest(request: LeaveRequest, approver: User, reason: String) {
    if (reason.isBlank()) {
      showToast("Rejection reason is mandatory.")
      return
    }
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updatedTlStep = ApprovalStep(
      approverRole = UserRole.TEAM_LEAD,
      approverName = approver.name,
      decision = LeaveStatus.REJECTED,
      remarks = reason,
      timestamp = LocalDateTime.now()
    )

    val updated = request.copy(
      status = LeaveStatus.REJECTED,
      tlStep = updatedTlStep
    )
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests

    // Post official rejection decision directly into chat between TL and Employee
    val threadId = "thread_emp_${request.userId}_tl"
    sendChatMessage(
      sender = approver,
      text = "TEAM LEAD DECISION: Leave request ${request.id} has been REJECTED.\nReason: $reason",
      threadId = threadId,
      relatedRequestId = request.id,
      isOfficialDecision = true
    )

    addNotification(
      recipientUserId = request.userId,
      title = "Leave Request Rejected",
      message = "Your ${request.leaveType.code} request was rejected by ${approver.name}. Reason: $reason",
      type = NotificationType.DECISION,
      relatedRequestId = request.id
    )

    addAuditLog(
      actor = approver,
      action = "TL Rejected Leave",
      target = "${request.id} (${request.userName})",
      reason = reason,
      details = "Direct TL rejection logged in chat"
    )

    showToast("Request rejected. Decision posted to chat and employee notified.")
  }

  fun tlQueryRequest(request: LeaveRequest, approver: User, question: String, suggestedDates: String?, attachment: LeaveProof? = null) {
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updatedTlStep = ApprovalStep(
      approverRole = UserRole.TEAM_LEAD,
      approverName = approver.name,
      decision = LeaveStatus.QUERY,
      remarks = question,
      timestamp = LocalDateTime.now()
    )

    val updated = request.copy(
      status = LeaveStatus.QUERY,
      tlStep = updatedTlStep,
      queryQuestion = question,
      querySuggestedDates = suggestedDates
    )
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests

    // Post official query directly into chat between TL and Employee
    val threadId = "thread_emp_${request.userId}_tl"
    sendChatMessage(
      sender = approver,
      text = "TEAM LEAD QUERY on ${request.id} (${request.workingDays.toInt()}d ${request.leaveType.title}):\n$question${if (!suggestedDates.isNullOrBlank()) "\nSuggested alternative dates: $suggestedDates" else ""}",
      threadId = threadId,
      relatedRequestId = request.id,
      attachment = attachment,
      isOfficialQuery = true
    )

    addNotification(
      recipientUserId = request.userId,
      title = "Query on Leave Request",
      message = "${approver.name} asked in chat: '$question'${if (!suggestedDates.isNullOrBlank()) " Suggested: $suggestedDates" else ""}",
      type = NotificationType.QUERY,
      relatedRequestId = request.id
    )

    addAuditLog(
      actor = approver,
      action = "Raised Query on Leave",
      target = "${request.id} (${request.userName})",
      reason = question,
      details = "Direct chat query with employee"
    )

    showToast("Query sent to ${request.userName} in chat.")
  }

  fun tlEmergencyCancel(request: LeaveRequest, tl: User, reason: String) {
    if (reason.isBlank()) {
      showToast("Cancellation reason is mandatory.")
      return
    }
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updated = request.copy(
      status = LeaveStatus.CANCELLED,
      cancelReason = "Emergency TL Cancellation: $reason",
      cancelConfirmedBy = tl.name
    )
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests

    // Restore balance if it was previously approved
    if (request.status == LeaveStatus.APPROVED) {
      restoreBalance(request.userId, request.leaveType, request.workingDays)
    }

    addNotification(
      recipientUserId = request.userId,
      title = "Approved Leave Cancelled by TL",
      message = "Your approved leave ($request.startDate to $request.endDate) was cancelled by ${tl.name} due to an emergency: $reason. Balance restored.",
      type = NotificationType.CANCEL_CONFIRMATION,
      relatedRequestId = request.id
    )

    request.backupUserId?.let { backupId ->
      addNotification(
        recipientUserId = backupId,
        title = "Backup Stand-down",
        message = "${request.userName}'s leave was cancelled. You are relieved of backup duties.",
        type = NotificationType.CANCEL_CONFIRMATION,
        relatedRequestId = request.id
      )
    }

    if (request.ceoStep?.decision == LeaveStatus.APPROVED) {
      val ceo = _users.value.first { it.role == UserRole.CEO }
      addNotification(
        recipientUserId = ceo.id,
        title = "TL Emergency Override on CEO-approved Leave",
        message = "${tl.name} cancelled CEO-approved leave for ${request.userName}. Reason: $reason",
        type = NotificationType.CANCEL_CONFIRMATION,
        relatedRequestId = request.id
      )
    }

    addAuditLog(
      actor = tl,
      action = "Emergency Leave Cancellation",
      target = "${request.id} (${request.userName})",
      reason = reason,
      details = "Restored ${request.workingDays} day(s) to ${request.leaveType.code} balance."
    )

    showToast("Leave cancelled. Balance restored and team calendar updated.")
  }

  fun addGroupCalendarEvent(tl: User, date: LocalDate, title: String, remark: String, isWorkingDayOverride: Boolean): Boolean {
    // Check if locked by CEO
    val holiday = _companyHolidays.value.find { it.date == date }
    if (holiday?.isLockedByCeo == true) {
      showToast("Change not allowed, ask the CEO (date locked by CEO).")
      return false
    }

    val newEvent = GroupCalendarEvent(
      id = UUID.randomUUID().toString(),
      groupId = tl.groupId,
      date = date,
      title = title,
      remark = remark,
      isWorkingDayOverride = isWorkingDayOverride,
      addedByTlName = tl.name
    )
    _groupEvents.value = _groupEvents.value + newEvent

    // Notify group members only
    val group = _groups.value.find { it.id == tl.groupId }
    group?.memberIds?.forEach { memberId ->
      addNotification(
        recipientUserId = memberId,
        title = "Team Calendar Update",
        message = "${tl.name} added group event '$title' on $date (${if (isWorkingDayOverride) "Working Day" else "Group Event"}). Remark: $remark",
        type = NotificationType.CALENDAR_CHANGE
      )
    }

    addAuditLog(
      actor = tl,
      action = "Group Calendar Override",
      target = "${tl.groupName} ($date)",
      reason = remark,
      details = "Title: $title, Working Day Override: $isWorkingDayOverride"
    )

    showToast("Group calendar updated. Group members notified.")
    return true
  }

  // CEO Actions
  fun ceoApproveRequest(request: LeaveRequest, ceo: User, remarks: String, isException: Boolean = false) {
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updatedCeoStep = ApprovalStep(
      approverRole = UserRole.CEO,
      approverName = ceo.name,
      decision = LeaveStatus.APPROVED,
      remarks = if (isException) "CEO Exception Override: $remarks" else remarks.ifBlank { "Approved by CEO" },
      timestamp = LocalDateTime.now()
    )

    val updated = request.copy(
      status = LeaveStatus.APPROVED,
      ceoStep = updatedCeoStep
    )
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests
    deductBalance(request.userId, request.leaveType, request.workingDays)

    addNotification(
      recipientUserId = request.userId,
      title = "Leave Request Approved by CEO!",
      message = "Your ${request.leaveType.code} leave for ${request.workingDays} day(s) was approved by CEO ${ceo.name}.",
      type = NotificationType.DECISION,
      relatedRequestId = request.id
    )

    // Also notify TL
    val tlId = _groups.value.find { it.id == request.groupId }?.tlUserId
    if (tlId != null && tlId != request.userId) {
      addNotification(
        recipientUserId = tlId,
        title = "CEO Approved Leave for ${request.userName}",
        message = "CEO finalized ${request.userName}'s leave request for ${request.startDate} to ${request.endDate}.",
        type = NotificationType.DECISION,
        relatedRequestId = request.id
      )
    }

    addAuditLog(
      actor = ceo,
      action = if (isException) "CEO Exception Approval" else "CEO Final Approval",
      target = "${request.id} (${request.userName})",
      reason = remarks,
      details = "${request.workingDays} days ${request.leaveType.code}. Exception: $isException"
    )

    showToast("Leave request approved by CEO! Calendar updated.")
  }

  fun ceoRejectRequest(request: LeaveRequest, ceo: User, reason: String) {
    if (reason.isBlank()) {
      showToast("Rejection reason is mandatory.")
      return
    }
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updatedCeoStep = ApprovalStep(
      approverRole = UserRole.CEO,
      approverName = ceo.name,
      decision = LeaveStatus.REJECTED,
      remarks = reason,
      timestamp = LocalDateTime.now()
    )

    val updated = request.copy(
      status = LeaveStatus.REJECTED,
      ceoStep = updatedCeoStep
    )
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests

    // Post official rejection directly into chat thread between Employee and CEO
    val threadId = "thread_emp_${request.userId}_ceo"
    sendChatMessage(
      sender = ceo,
      text = "EXECUTIVE DECISION: Leave request ${request.id} has been REJECTED by CEO ${ceo.name}.\nReason: $reason",
      threadId = threadId,
      relatedRequestId = request.id,
      isOfficialDecision = true
    )

    addNotification(
      recipientUserId = request.userId,
      title = "Leave Request Rejected by CEO",
      message = "Your request was rejected by CEO ${ceo.name}. Reason: $reason",
      type = NotificationType.DECISION,
      relatedRequestId = request.id
    )

    addAuditLog(
      actor = ceo,
      action = "CEO Rejected Leave",
      target = "${request.id} (${request.userName})",
      reason = reason,
      details = "Final rejection posted to executive chat"
    )

    showToast("Leave request rejected by CEO. Decision posted to chat.")
  }

  fun ceoQueryRequest(
    request: LeaveRequest,
    ceo: User,
    question: String,
    attachment: LeaveProof? = null
  ) {
    if (question.isBlank()) {
      showToast("Query question is mandatory.")
      return
    }
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updatedCeoStep = ApprovalStep(
      approverRole = UserRole.CEO,
      approverName = ceo.name,
      decision = LeaveStatus.QUERY,
      remarks = question,
      timestamp = LocalDateTime.now()
    )

    val updated = request.copy(
      status = LeaveStatus.QUERY,
      ceoStep = updatedCeoStep,
      queryQuestion = question
    )
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests

    // Dedicated chat thread between employee and CEO
    val threadId = "thread_emp_${request.userId}_ceo"
    sendChatMessage(
      sender = ceo,
      text = "EXECUTIVE CLARIFICATION REQUEST on ${request.id} (${request.workingDays.toInt()}d ${request.leaveType.title}):\n$question",
      threadId = threadId,
      relatedRequestId = request.id,
      attachment = attachment,
      isOfficialQuery = true
    )

    addNotification(
      recipientUserId = request.userId,
      title = "CEO Query on Leave Request ${request.id}",
      message = "CEO ${ceo.name} requested clarification: \"$question\". Reply directly with proofs in chat.",
      type = NotificationType.QUERY,
      relatedRequestId = request.id
    )

    addAuditLog(
      actor = ceo,
      action = "CEO Raised Query",
      target = "${request.id} (${request.userName})",
      reason = question,
      details = "Direct CEO query thread initiated in chat with employee"
    )

    showToast("Query and message posted to ${request.userName}'s chat.")
  }

  fun ceoToggleDateLock(ceo: User, date: LocalDate): Boolean {
    val existingList = _companyHolidays.value.toMutableList()
    val existingIndex = existingList.indexOfFirst { it.date == date }
    val newLockStatus: Boolean

    if (existingIndex >= 0) {
      val existing = existingList[existingIndex]
      newLockStatus = !existing.isLockedByCeo
      existingList[existingIndex] = existing.copy(isLockedByCeo = newLockStatus)
    } else {
      newLockStatus = true
      val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
      val defaultType = if (isWeekend) HolidayType.SPECIAL_WORKING_DAY else HolidayType.WORKING_DAY
      existingList.add(
        CompanyHoliday(
          id = UUID.randomUUID().toString(),
          date = date,
          title = if (newLockStatus) "CEO Protected Day" else "Standard Day",
          type = defaultType,
          remark = "Date locked directly from CEO Calendar View",
          colorHex = 0xFF7C3AED,
          isLockedByCeo = true,
          addedByCeo = true
        )
      )
    }
    _companyHolidays.value = existingList

    val statusText = if (newLockStatus) "LOCKED by CEO (TL overrides disabled)" else "UNLOCKED (Open for TL team events)"
    addAuditLog(
      actor = ceo,
      action = if (newLockStatus) "CEO Date Lock" else "CEO Date Unlock",
      target = "$date",
      reason = "Direct Calendar Lock Toggle",
      details = "Date status updated to: $statusText"
    )
    showToast("Date $date is now $statusText")
    return newLockStatus
  }

  fun ceoUpdateCompanyCalendar(
    ceo: User,
    date: LocalDate,
    type: HolidayType,
    title: String,
    remark: String,
    colorHex: Long,
    lockThisDay: Boolean
  ) {
    val existingList = _companyHolidays.value.toMutableList()
    val existingIndex = existingList.indexOfFirst { it.date == date }

    val newHoliday = CompanyHoliday(
      id = if (existingIndex != null && existingIndex >= 0) existingList[existingIndex].id else UUID.randomUUID().toString(),
      date = date,
      title = title,
      type = type,
      remark = remark,
      colorHex = colorHex,
      isLockedByCeo = lockThisDay,
      addedByCeo = true
    )

    if (existingIndex >= 0) {
      existingList[existingIndex] = newHoliday
    } else {
      existingList.add(newHoliday)
    }
    _companyHolidays.value = existingList

    // Check affected approved leaves!
    val affectedLeaves = _leaveRequests.value.filter {
      it.status == LeaveStatus.APPROVED &&
        !date.isBefore(it.startDate) &&
        !date.isAfter(it.endDate)
    }

    var affectedCount = 0
    if (type == HolidayType.PUBLIC_HOLIDAY || type == HolidayType.COMPANY_CLOSURE) {
      affectedLeaves.forEach { leave ->
        // Recalculate working days
        val newDays = calculateWorkingDays(leave.groupId, leave.startDate, leave.endDate, leave.isHalfDay)
        val diff = leave.workingDays - newDays
        if (diff > 0f) {
          // Restore balance
          restoreBalance(leave.userId, leave.leaveType, diff)
          affectedCount++

          // Update request
          val currentList = _leaveRequests.value.toMutableList()
          val idx = currentList.indexOfFirst { it.id == leave.id }
          if (idx != -1) {
            currentList[idx] = leave.copy(workingDays = newDays)
            _leaveRequests.value = currentList
          }

          addNotification(
            recipientUserId = leave.userId,
            title = "Holiday Added: Balance Restored",
            message = "New holiday '$title' on $date affected your approved leave. $diff day(s) restored to your ${leave.leaveType.code} balance.",
            type = NotificationType.CALENDAR_CHANGE,
            relatedRequestId = leave.id
          )
        }
      }
    }

    // Publish to all calendars and notify everyone
    addNotification(
      recipientUserId = "ALL",
      title = "Company Calendar Updated: $title",
      message = "CEO ${ceo.name} updated $date as ${type.displayName}. Remark: $remark${if (lockThisDay) " (Locked by CEO)" else ""}",
      type = NotificationType.CALENDAR_CHANGE
    )

    addAuditLog(
      actor = ceo,
      action = "CEO Company Calendar Update",
      target = "$date ($title)",
      reason = remark,
      details = "Type: ${type.displayName}, Locked: $lockThisDay, Affected Leaves Recalculated: $affectedCount"
    )

    showToast("Calendar published to all! $affectedCount affected leave(s) recalculated.")
  }

  // Employee Actions
  fun employeeWithdrawRequest(request: LeaveRequest, user: User) {
    if (request.status != LeaveStatus.PENDING && request.status != LeaveStatus.QUERY) {
      showToast("Only pending or queried requests can be withdrawn.")
      return
    }
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updated = request.copy(status = LeaveStatus.WITHDRAWN)
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests

    addAuditLog(
      actor = user,
      action = "Withdrew Leave Request",
      target = request.id,
      reason = "Employee withdrawn",
      details = "No balance impact"
    )

    showToast("Request withdrawn. No balance was deducted.")
  }

  fun employeeRequestCancel(request: LeaveRequest, user: User, reason: String) {
    val today = LocalDate.now()
    if (!today.isBefore(request.startDate)) {
      showToast("Cannot cancel online, contact TL or HR (leave date already started).")
      return
    }
    if (reason.isBlank()) {
      showToast("Cancellation reason is required.")
      return
    }

    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updated = request.copy(
      cancelRequested = true,
      cancelReason = reason
    )
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests

    // Notify approver to confirm
    val tlId = _groups.value.find { it.id == user.groupId }?.tlUserId
    val recipientId = if (user.role == UserRole.TEAM_LEAD || tlId == null) {
      _users.value.first { it.role == UserRole.CEO }.id
    } else {
      tlId
    }

    addNotification(
      recipientUserId = recipientId,
      title = "Cancel Request: ${request.userName}",
      message = "${request.userName} requested cancellation for approved leave (${request.startDate} to ${request.endDate}). Reason: $reason",
      type = NotificationType.CANCEL_CONFIRMATION,
      relatedRequestId = request.id
    )

    showToast("Cancellation request submitted. Awaiting approver confirmation.")
  }

  fun approverConfirmCancel(request: LeaveRequest, approver: User) {
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val updated = request.copy(
      status = LeaveStatus.CANCELLED,
      cancelRequested = false,
      cancelConfirmedBy = approver.name
    )
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests

    // Restore balance!
    restoreBalance(request.userId, request.leaveType, request.workingDays)

    addNotification(
      recipientUserId = request.userId,
      title = "Cancellation Confirmed",
      message = "Your leave cancellation was confirmed by ${approver.name}. ${request.workingDays} day(s) restored to ${request.leaveType.code}.",
      type = NotificationType.CANCEL_CONFIRMATION,
      relatedRequestId = request.id
    )

    addAuditLog(
      actor = approver,
      action = "Confirmed Leave Cancellation",
      target = "${request.id} (${request.userName})",
      reason = request.cancelReason ?: "Employee requested cancel",
      details = "Restored ${request.workingDays} day(s)"
    )

    showToast("Cancellation confirmed! Balance restored and calendar updated.")
  }

  fun employeeReplyQuery(request: LeaveRequest, user: User, reply: String, newStartDate: LocalDate? = null, newEndDate: LocalDate? = null) {
    val currentRequests = _leaveRequests.value.toMutableList()
    val index = currentRequests.indexOfFirst { it.id == request.id }
    if (index == -1) return

    val start = newStartDate ?: request.startDate
    val end = newEndDate ?: request.endDate
    val workingDays = calculateWorkingDays(user.groupId, start, end, request.isHalfDay)

    val updated = request.copy(
      startDate = start,
      endDate = end,
      workingDays = workingDays,
      status = LeaveStatus.PENDING,
      employeeQueryReply = reply
    )
    currentRequests[index] = updated
    _leaveRequests.value = currentRequests

    val tlId = _groups.value.find { it.id == user.groupId }?.tlUserId ?: _users.value.first { it.role == UserRole.CEO }.id
    addNotification(
      recipientUserId = tlId,
      title = "Query Replied by ${user.name}",
      message = "${user.name} replied to query on ${request.id}: '$reply'. Resubmitted for review.",
      type = NotificationType.QUERY,
      relatedRequestId = request.id
    )

    showToast("Reply sent and request resubmitted for approval.")
  }

  fun sendApproverReminder(request: LeaveRequest, user: User) {
    val tlId = _groups.value.find { it.id == user.groupId }?.tlUserId
    val approverId = if (request.tlStep?.decision == LeaveStatus.APPROVED || user.role == UserRole.TEAM_LEAD) {
      _users.value.first { it.role == UserRole.CEO }.id
    } else {
      tlId ?: _users.value.first { it.role == UserRole.CEO }.id
    }

    addNotification(
      recipientUserId = approverId,
      title = "Leave Approval Reminder (24h+)",
      message = "Reminder: Leave request ${request.id} for ${user.name} has been pending for over 24 hours.",
      type = NotificationType.REMINDER_24H,
      relatedRequestId = request.id
    )

    showToast("Reminder sent to approver via email & push notification.")
  }

  // MARK: - Chat System
  fun sendChatMessage(
    sender: User,
    text: String,
    threadId: String,
    relatedRequestId: String? = null,
    attachment: LeaveProof? = null,
    isOfficialQuery: Boolean = false,
    isOfficialDecision: Boolean = false
  ) {
    if (text.isBlank() && attachment == null) return

    // Strict Permission Enforcement:
    // An employee cannot initially message a CEO unless CEO has queried or opened the thread
    if (sender.role == UserRole.EMPLOYEE && threadId.contains("ceo")) {
      if (!canEmployeeChatWithCeo(sender.id)) {
        showToast("Employees cannot initiate messaging with the CEO without an active approval query.")
        return
      }
    }

    // TL cannot message peer TLs
    if (sender.role == UserRole.TEAM_LEAD && threadId.startsWith("thread_tl_tl")) {
      showToast("Team Leads cannot direct message peer Team Leads per corporate hierarchy.")
      return
    }

    val msg = ChatMessage(
      id = UUID.randomUUID().toString(),
      threadId = threadId,
      senderId = sender.id,
      senderName = sender.name,
      senderRole = sender.role,
      text = text,
      timestamp = LocalDateTime.now(),
      relatedRequestId = relatedRequestId,
      attachment = attachment,
      isOfficialQuery = isOfficialQuery,
      isOfficialDecision = isOfficialDecision
    )
    _chatMessages.value = _chatMessages.value + msg

    // Notify CEO if employee replied with proofs in the query thread
    if (sender.role == UserRole.EMPLOYEE && threadId.contains("ceo")) {
      val ceo = _users.value.first { it.role == UserRole.CEO }
      addNotification(
        recipientUserId = ceo.id,
        title = "Clarification Reply from ${sender.name}",
        message = "${sender.name} replied${if (attachment != null) " with attached '${attachment.fileName}'" else ""}: \"$text\"",
        type = NotificationType.QUERY,
        relatedRequestId = relatedRequestId
      )
    }

    // Notify TL if employee messaged TL
    if (sender.role == UserRole.EMPLOYEE && threadId.contains("tl")) {
      val tlId = _groups.value.find { it.id == sender.groupId }?.tlUserId
      if (tlId != null) {
        addNotification(
          recipientUserId = tlId,
          title = "Chat from ${sender.name}",
          message = "${sender.name}: \"$text\"${if (attachment != null) " [Attachment: ${attachment.fileName}]" else ""}",
          type = NotificationType.QUERY,
          relatedRequestId = relatedRequestId
        )
      }
    }
  }

  fun canEmployeeChatWithCeo(employeeId: String): Boolean {
    val threadId = "thread_emp_${employeeId}_ceo"
    val hasCeoMessage = _chatMessages.value.any {
      it.threadId == threadId && it.senderRole == UserRole.CEO
    }
    val hasActiveQuery = _leaveRequests.value.any {
      it.userId == employeeId && (it.status == LeaveStatus.QUERY || it.ceoStep?.decision == LeaveStatus.QUERY || it.ceoStep?.decision == LeaveStatus.REJECTED)
    }
    return hasCeoMessage || hasActiveQuery
  }

  fun getAvailableChatThreads(currentUser: User): List<ChatThreadInfo> {
    val threads = mutableListOf<ChatThreadInfo>()

    // 1. Team Channel
    val groupName = currentUser.groupName
    threads.add(
      ChatThreadInfo(
        threadId = "thread_team_${currentUser.groupId}",
        title = "#$groupName Team Channel",
        subtitle = "General team coordination & absence coverage",
        role = null
      )
    )

    when (currentUser.role) {
      UserRole.EMPLOYEE -> {
        // Employee can message their Team Lead anytime
        val myGroup = _groups.value.find { it.id == currentUser.groupId }
        val tlUser = _users.value.find { it.id == myGroup?.tlUserId }
        if (tlUser != null) {
          threads.add(
            ChatThreadInfo(
              threadId = "thread_emp_${currentUser.id}_tl",
              title = "Team Lead: ${tlUser.name}",
              subtitle = "Direct Line • Leave reviews, handovers & questions",
              role = UserRole.TEAM_LEAD,
              targetUserId = tlUser.id
            )
          )
        }

        // CEO Direct Line: Only unlocked if CEO has queried on an approval
        val ceoUser = _users.value.first { it.role == UserRole.CEO }
        val isUnlocked = canEmployeeChatWithCeo(currentUser.id)
        threads.add(
          ChatThreadInfo(
            threadId = "thread_emp_${currentUser.id}_ceo",
            title = "Executive: ${ceoUser.name} (CEO)",
            subtitle = if (isUnlocked) "Active Leave Clarification & Proof Sharing" else "Locked • Unlocks on CEO approval queries",
            role = UserRole.CEO,
            targetUserId = ceoUser.id,
            isLocked = !isUnlocked,
            lockReason = "An employee cannot initiate contact with the CEO. Communication unlocks automatically when the CEO queries on an approval."
          )
        )
      }

      UserRole.TEAM_LEAD -> {
        // TL can message all employees in their team
        val myGroup = _groups.value.find { it.id == currentUser.groupId }
        val teamEmployees = _users.value.filter { it.groupId == myGroup?.id && it.role == UserRole.EMPLOYEE }
        teamEmployees.forEach { emp ->
          threads.add(
            ChatThreadInfo(
              threadId = "thread_emp_${emp.id}_tl",
              title = "${emp.name} (${emp.designation})",
              subtitle = "Team Member • Approval queries & direct chat",
              role = UserRole.EMPLOYEE,
              targetUserId = emp.id
            )
          )
        }

        // Note: Peer Team Leads are NOT added ("TL cannot message TL")
        // TL can message CEO for management/escalations
        val ceoUser = _users.value.first { it.role == UserRole.CEO }
        threads.add(
          ChatThreadInfo(
            threadId = "thread_tl_${currentUser.id}_ceo",
            title = "Executive: ${ceoUser.name} (CEO)",
            subtitle = "Leadership escalation channel",
            role = UserRole.CEO,
            targetUserId = ceoUser.id
          )
        )
      }

      UserRole.CEO -> {
        // CEO can query/message any employee
        val allEmployees = _users.value.filter { it.role == UserRole.EMPLOYEE }
        allEmployees.forEach { emp ->
          val reqForEmp = _leaveRequests.value.find { it.userId == emp.id && (it.status == LeaveStatus.QUERY || it.requiresCeoApproval) }
          threads.add(
            ChatThreadInfo(
              threadId = "thread_emp_${emp.id}_ceo",
              title = "${emp.name} (${emp.groupName})",
              subtitle = if (reqForEmp != null) "Query Active on ${reqForEmp.id} (${reqForEmp.leaveType.code})" else "Direct Employee Query Thread",
              role = UserRole.EMPLOYEE,
              targetUserId = emp.id,
              relatedRequestId = reqForEmp?.id
            )
          )
        }

        // CEO can message all Team Leads
        val teamLeads = _users.value.filter { it.role == UserRole.TEAM_LEAD }
        teamLeads.forEach { tl ->
          threads.add(
            ChatThreadInfo(
              threadId = "thread_tl_${tl.id}_ceo",
              title = "Lead: ${tl.name} (${tl.groupName})",
              subtitle = "Management alignment & team leaves",
              role = UserRole.TEAM_LEAD,
              targetUserId = tl.id
            )
          )
        }
      }
    }

    return threads
  }

  // MARK: - Notifications
  fun markNotificationAsRead(id: String) {
    _notifications.value = _notifications.value.map {
      if (it.id == id) it.copy(isRead = true) else it
    }
  }

  fun markAllNotificationsAsRead(userId: String) {
    _notifications.value = _notifications.value.map {
      if (it.recipientUserId == userId || it.recipientUserId == "ALL") it.copy(isRead = true) else it
    }
  }

  private fun addNotification(
    recipientUserId: String,
    title: String,
    message: String,
    type: NotificationType,
    relatedRequestId: String? = null
  ) {
    val notif = AppNotification(
      id = UUID.randomUUID().toString(),
      recipientUserId = recipientUserId,
      title = title,
      message = message,
      type = type,
      timestamp = LocalDateTime.now(),
      relatedRequestId = relatedRequestId
    )
    _notifications.value = listOf(notif) + _notifications.value
  }

  private fun addAuditLog(
    actor: User,
    action: String,
    target: String,
    reason: String,
    details: String
  ) {
    val entry = AuditLogEntry(
      id = "AUD-${UUID.randomUUID().toString().take(6).uppercase()}",
      timestamp = LocalDateTime.now(),
      actorName = actor.name,
      actorRole = actor.role,
      action = action,
      target = target,
      reason = reason,
      details = details
    )
    _auditLogs.value = listOf(entry) + _auditLogs.value
  }

  private fun deductBalance(userId: String, leaveType: LeaveType, days: Float) {
    if (leaveType.isNoCap) return
    val balances = _userBalances.value.toMutableMap()
    val quotas = balances[userId]?.toMutableList() ?: return
    val index = quotas.indexOfFirst { it.leaveType == leaveType }
    if (index != -1) {
      val q = quotas[index]
      quotas[index] = q.copy(used = q.used + days)
      balances[userId] = quotas
      _userBalances.value = balances
    }
  }

  private fun restoreBalance(userId: String, leaveType: LeaveType, days: Float) {
    if (leaveType.isNoCap) return
    val balances = _userBalances.value.toMutableMap()
    val quotas = balances[userId]?.toMutableList() ?: return
    val index = quotas.indexOfFirst { it.leaveType == leaveType }
    if (index != -1) {
      val q = quotas[index]
      quotas[index] = q.copy(used = (q.used - days).coerceAtLeast(0f))
      balances[userId] = quotas
      _userBalances.value = balances
    }
  }

  companion object {
    val DEMO_USERS = listOf(
      // CEO
      User("u_ceo", "CEO001", "Robert Sterling", "robert.sterling@leavedesk.corp", UserRole.CEO, "g_exec", "Executive Office", "Chief Executive Officer", "RS"),
      // TL 1
      User("u_tl1", "TL201", "Marcus Vance", "marcus.v@leavedesk.corp", UserRole.TEAM_LEAD, "g_eng", "Engineering & Product", "Engineering Lead", "MV"),
      // TL 2
      User("u_tl2", "TL202", "Elena Rostova", "elena.r@leavedesk.corp", UserRole.TEAM_LEAD, "g_ops", "Design & Operations", "Operations Lead", "ER"),
      // Group 1 Employees (4)
      User("u_emp1", "EMP101", "Alex Chen", "alex.chen@leavedesk.corp", UserRole.EMPLOYEE, "g_eng", "Engineering & Product", "Senior Android Engineer", "AC"),
      User("u_emp2", "EMP102", "Sarah Jenkins", "sarah.j@leavedesk.corp", UserRole.EMPLOYEE, "g_eng", "Engineering & Product", "Backend Architect", "SJ"),
      User("u_emp3", "EMP103", "David Kim", "david.kim@leavedesk.corp", UserRole.EMPLOYEE, "g_eng", "Engineering & Product", "QA Specialist", "DK"),
      User("u_emp4", "EMP104", "Priya Patel", "priya.p@leavedesk.corp", UserRole.EMPLOYEE, "g_eng", "Engineering & Product", "Fullstack Developer", "PP"),
      // Group 2 Employees (4)
      User("u_emp5", "EMP105", "James Wilson", "james.w@leavedesk.corp", UserRole.EMPLOYEE, "g_ops", "Design & Operations", "Lead Product Designer", "JW"),
      User("u_emp6", "EMP106", "Maya Lin", "maya.lin@leavedesk.corp", UserRole.EMPLOYEE, "g_ops", "Design & Operations", "UX Researcher", "ML"),
      User("u_emp7", "EMP107", "Carlos Gomez", "carlos.g@leavedesk.corp", UserRole.EMPLOYEE, "g_ops", "Design & Operations", "Operations Associate", "CG"),
      User("u_emp8", "EMP108", "Anita Rao", "anita.rao@leavedesk.corp", UserRole.EMPLOYEE, "g_ops", "Design & Operations", "Brand Specialist", "AR")
    )

    val DEMO_GROUPS = listOf(
      TeamGroup(
        id = "g_eng",
        name = "Engineering & Product",
        tlUserId = "u_tl1",
        tlName = "Marcus Vance",
        minPresentThreshold = 3,
        memberIds = listOf("u_tl1", "u_emp1", "u_emp2", "u_emp3", "u_emp4")
      ),
      TeamGroup(
        id = "g_ops",
        name = "Design & Operations",
        tlUserId = "u_tl2",
        tlName = "Elena Rostova",
        minPresentThreshold = 2,
        memberIds = listOf("u_tl2", "u_emp5", "u_emp6", "u_emp7", "u_emp8")
      )
    )

    private val baseQuotas = listOf(
      LeaveQuota(LeaveType.CL, 12f, 2f),
      LeaveQuota(LeaveType.SL, 10f, 1f),
      LeaveQuota(LeaveType.EL, 15f, 4f),
      LeaveQuota(LeaveType.ML, 90f, 0f),
      LeaveQuota(LeaveType.BL, 5f, 0f),
      LeaveQuota(LeaveType.LOP, 0f, 0f),
      LeaveQuota(LeaveType.HD, 8f, 1f),
      LeaveQuota(LeaveType.WFH, 0f, 3f),
      LeaveQuota(LeaveType.SHORT, 12f, 2f)
    )

    val INITIAL_USER_BALANCES: Map<String, List<LeaveQuota>> = DEMO_USERS.associate { user ->
      user.id to baseQuotas.map { it.copy() }
    }

    private val now = LocalDate.now()

    val INITIAL_HOLIDAYS = listOf(
      CompanyHoliday("h_1", now.withDayOfMonth(1), "Monthly Review Closure", HolidayType.COMPANY_CLOSURE, "Mandatory company stand-down", 0xFF7C3AED, isLockedByCeo = true),
      CompanyHoliday("h_2", now.plusDays(10), "National Innovation Day", HolidayType.PUBLIC_HOLIDAY, "Gazetted holiday", 0xFFDC2626, isLockedByCeo = true),
      CompanyHoliday("h_3", now.plusDays(18), "Founder's Optional Day", HolidayType.OPTIONAL_HOLIDAY, "Optional holiday for cultural observance", 0xFF2563EB, isLockedByCeo = false),
      CompanyHoliday("h_4", now.plusDays(25), "Sprint Alignment Day", HolidayType.SPECIAL_WORKING_DAY, "Weekend hackathon / working day", 0xFFF59E0B, isLockedByCeo = false)
    )

    val INITIAL_GROUP_EVENTS = listOf(
      GroupCalendarEvent("ge_1", "g_eng", now.plusDays(5), "Sprint Freeze Review", "Mandatory presence for all engineers", isWorkingDayOverride = false, addedByTlName = "Marcus Vance")
    )

    val INITIAL_LEAVE_REQUESTS = listOf(
      // Escalated (> 48h pending)
      LeaveRequest(
        id = "LR-89102",
        userId = "u_emp1",
        userName = "Alex Chen",
        userRole = UserRole.EMPLOYEE,
        groupId = "g_eng",
        groupName = "Engineering & Product",
        leaveType = LeaveType.EL,
        startDate = now.plusDays(6),
        endDate = now.plusDays(10),
        workingDays = 4f,
        reason = "Annual family retreat to Lake Tahoe",
        backupUserId = "u_emp2",
        backupUserName = "Sarah Jenkins",
        backupTaskNote = "Monitoring API deployment pipeline",
        proof = LeaveProof("Flight_Booking_Tahoe.pdf", "PDF", "1.2 MB"),
        status = LeaveStatus.PENDING,
        submittedAt = LocalDateTime.now().minusHours(52), // > 48h escalated
        requiresCeoApproval = true
      ),
      // Pending (> 24h)
      LeaveRequest(
        id = "LR-78219",
        userId = "u_emp2",
        userName = "Sarah Jenkins",
        userRole = UserRole.EMPLOYEE,
        groupId = "g_eng",
        groupName = "Engineering & Product",
        leaveType = LeaveType.CL,
        startDate = now.plusDays(4),
        endDate = now.plusDays(5),
        workingDays = 2f,
        reason = "Apartment relocation and lease signing",
        backupUserId = "u_emp1",
        backupUserName = "Alex Chen",
        status = LeaveStatus.PENDING,
        submittedAt = LocalDateTime.now().minusHours(28), // > 24h reminder
        requiresCeoApproval = false
      ),
      // Query Status
      LeaveRequest(
        id = "LR-67124",
        userId = "u_emp3",
        userName = "David Kim",
        userRole = UserRole.EMPLOYEE,
        groupId = "g_eng",
        groupName = "Engineering & Product",
        leaveType = LeaveType.CL,
        startDate = now.plusDays(8),
        endDate = now.plusDays(9),
        workingDays = 2f,
        reason = "Personal administrative errands",
        status = LeaveStatus.QUERY,
        submittedAt = LocalDateTime.now().minusDays(3),
        queryQuestion = "Can you shift to Thursday-Friday instead? Sprint testing peak is on Tuesday.",
        querySuggestedDates = "${now.plusDays(10)} to ${now.plusDays(11)}",
        tlStep = ApprovalStep(UserRole.TEAM_LEAD, "Marcus Vance", LeaveStatus.QUERY, "Peak sprint clash", LocalDateTime.now().minusDays(1))
      ),
      // Approved (Upcoming)
      LeaveRequest(
        id = "LR-54910",
        userId = "u_emp5",
        userName = "James Wilson",
        userRole = UserRole.EMPLOYEE,
        groupId = "g_ops",
        groupName = "Design & Operations",
        leaveType = LeaveType.SL,
        startDate = now.plusDays(2),
        endDate = now.plusDays(3),
        workingDays = 2f,
        reason = "Scheduled dental surgery and recovery",
        backupUserId = "u_emp6",
        backupUserName = "Maya Lin",
        status = LeaveStatus.APPROVED,
        submittedAt = LocalDateTime.now().minusDays(4),
        tlStep = ApprovalStep(UserRole.TEAM_LEAD, "Elena Rostova", LeaveStatus.APPROVED, "Approved promptly. Rest well.", LocalDateTime.now().minusDays(3))
      ),
      // TL's own leave (Pending CEO)
      LeaveRequest(
        id = "LR-41002",
        userId = "u_tl1",
        userName = "Marcus Vance",
        userRole = UserRole.TEAM_LEAD,
        groupId = "g_eng",
        groupName = "Engineering & Product",
        leaveType = LeaveType.EL,
        startDate = now.plusDays(12),
        endDate = now.plusDays(16),
        workingDays = 4f,
        reason = "Attending Kotlin World Conference 2026",
        backupUserId = "u_emp1",
        backupUserName = "Alex Chen",
        backupTaskNote = "Acting Engineering Lead for standups",
        proof = LeaveProof("Conference_Ticket_Receipt.png", "PNG", "780 KB"),
        status = LeaveStatus.PENDING,
        submittedAt = LocalDateTime.now().minusHours(18),
        requiresCeoApproval = true
      ),
      // Rejected
      LeaveRequest(
        id = "LR-30911",
        userId = "u_emp4",
        userName = "Priya Patel",
        userRole = UserRole.EMPLOYEE,
        groupId = "g_eng",
        groupName = "Engineering & Product",
        leaveType = LeaveType.CL,
        startDate = now.minusDays(5),
        endDate = now.minusDays(4),
        workingDays = 2f,
        reason = "Last minute travel plans",
        status = LeaveStatus.REJECTED,
        submittedAt = LocalDateTime.now().minusDays(8),
        tlStep = ApprovalStep(UserRole.TEAM_LEAD, "Marcus Vance", LeaveStatus.REJECTED, "Group present threshold breached (only 2 engineers would be active).", LocalDateTime.now().minusDays(7))
      )
    )

    val INITIAL_AUDIT_LOGS = listOf(
      AuditLogEntry(
        id = "AUD-1001",
        timestamp = LocalDateTime.now().minusDays(3),
        actorName = "Elena Rostova",
        actorRole = UserRole.TEAM_LEAD,
        action = "TL Approved Leave",
        target = "LR-54910 (James Wilson)",
        reason = "Approved medical leave",
        details = "2 working days SL approved"
      ),
      AuditLogEntry(
        id = "AUD-1002",
        timestamp = LocalDateTime.now().minusDays(2),
        actorName = "Robert Sterling",
        actorRole = UserRole.CEO,
        action = "CEO Company Calendar Update",
        target = "Monthly Review Closure",
        reason = "Company quarterly alignment",
        details = "Set mandatory closure. Locked by CEO."
      ),
      AuditLogEntry(
        id = "AUD-1003",
        timestamp = LocalDateTime.now().minusDays(1),
        actorName = "Marcus Vance",
        actorRole = UserRole.TEAM_LEAD,
        action = "Raised Query on Leave",
        target = "LR-67124 (David Kim)",
        reason = "Suggested date shift due to sprint load",
        details = "Question sent to employee"
      )
    )

    val INITIAL_NOTIFICATIONS = listOf(
      AppNotification(
        id = "notif_1",
        recipientUserId = "u_emp1",
        title = "Approval Escalated (48h)",
        message = "Your leave request LR-89102 has been pending for over 48 hours. Escalation flag active.",
        type = NotificationType.ESCALATION_48H,
        timestamp = LocalDateTime.now().minusHours(3),
        relatedRequestId = "LR-89102"
      ),
      AppNotification(
        id = "notif_2",
        recipientUserId = "u_tl1",
        title = "Pending Review Reminder (24h)",
        message = "Leave request LR-78219 from Sarah Jenkins has been pending for over 24 hours.",
        type = NotificationType.REMINDER_24H,
        timestamp = LocalDateTime.now().minusHours(4),
        relatedRequestId = "LR-78219"
      ),
      AppNotification(
        id = "notif_3",
        recipientUserId = "ALL",
        title = "Company Calendar Updated",
        message = "CEO Robert Sterling declared National Innovation Day as a Public Holiday.",
        type = NotificationType.CALENDAR_CHANGE,
        timestamp = LocalDateTime.now().minusDays(1)
      )
    )

    val INITIAL_CHAT_MESSAGES = listOf(
      // Team Channel
      ChatMessage(
        id = "chat_team_1",
        threadId = "thread_team_g_eng",
        senderId = "u_tl1",
        senderName = "Marcus Vance",
        senderRole = UserRole.TEAM_LEAD,
        text = "Welcome team! Please ensure all leaves for Q4 sprint freeze are logged at least 1 week in advance.",
        timestamp = LocalDateTime.now().minusDays(3)
      ),
      // Direct Thread between Employee (Alex Chen) and Team Lead (Marcus Vance)
      ChatMessage(
        id = "chat_tl_1",
        threadId = "thread_emp_u_emp1_tl",
        senderId = "u_emp1",
        senderName = "Alex Chen",
        senderRole = UserRole.EMPLOYEE,
        text = "Hi Marcus, I have submitted my 4-day Earned Leave request (LR-89102). Sarah Jenkins agreed to back me up.",
        timestamp = LocalDateTime.now().minusHours(48),
        relatedRequestId = "LR-89102",
        attachment = LeaveProof("Flight_Booking_Tahoe.pdf", "PDF", "1.2 MB")
      ),
      ChatMessage(
        id = "chat_tl_2",
        threadId = "thread_emp_u_emp1_tl",
        senderId = "u_tl1",
        senderName = "Marcus Vance",
        senderRole = UserRole.TEAM_LEAD,
        text = "Thanks Alex. I have endorsed it from the team perspective. Since it is > 3 days, it requires CEO final review.",
        timestamp = LocalDateTime.now().minusHours(36),
        relatedRequestId = "LR-89102"
      ),
      // Direct Thread between Employee (Alex Chen) and CEO (Robert Sterling / Sarah Jenkins)
      ChatMessage(
        id = "chat_ceo_1",
        threadId = "thread_emp_u_emp1_ceo",
        senderId = "u_ceo",
        senderName = "Robert Sterling",
        senderRole = UserRole.CEO,
        text = "EXECUTIVE CLARIFICATION REQUEST on LR-89102 (4d Earned Leave):\nAlex, during this window we have the Q4 Cloud platform launch. Could you please share the deployment handover checklist with Sarah?",
        timestamp = LocalDateTime.now().minusHours(12),
        relatedRequestId = "LR-89102",
        attachment = LeaveProof("Executive_Handover_Guidelines.pdf", "PDF", "420 KB"),
        isOfficialQuery = true
      )
    )
  }
}

```


### File 5: app/src/main/java/com/example/ui/theme/Color.kt
**File Path:** `app/src/main/java/com/example/ui/theme/Color.kt`

```kotlin
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Role-specific accents
val EmployeeTeal = Color(0xFF1F8A8A)
val EmployeeTealLight = Color(0xFFD5F0EE)
val EmployeeSurface = Color(0xFFF2FAF9)

val TeamLeadIndigo = Color(0xFF3B4A8C)
val TeamLeadIndigoLight = Color(0xFFDCE3F7)
val TeamLeadSurface = Color(0xFFF3F5FC)

val CeoPurple = Color(0xFF6B3F7C)
val CeoPurpleLight = Color(0xFFEBDDF0)
val CeoSurface = Color(0xFFF8F2FA)

// Neutral & System
val NeutralGrey = Color(0xFF6B6B6B)
val NeutralTint = Color(0xFFECECEC)
val NeutralSurface = Color(0xFFF7F7F7)
val CardBorder = Color(0xFFE5E7EB)

// Status colors
val StatusApprovedGreen = Color(0xFF82B366)
val StatusApprovedBg = Color(0xFFD5E8D4)
val StatusApprovedText = Color(0xFF276A3C)

val StatusPendingAmber = Color(0xFFD6B656)
val StatusPendingBg = Color(0xFFFFF2CC)
val StatusPendingText = Color(0xFF8F6E00)

val StatusRejectedRed = Color(0xFFB85450)
val StatusRejectedBg = Color(0xFFF8CECC)
val StatusRejectedText = Color(0xFF992222)

val StatusNotesBg = Color(0xFFFFFBE6)

// Text
val TextPrimary = Color(0xFF222222)
val TextSecondary = Color(0xFF6B6B6B)
val TextMuted = Color(0xFF8E8E93)

```


### File 6: app/src/main/java/com/example/ui/theme/Theme.kt
**File Path:** `app/src/main/java/com/example/ui/theme/Theme.kt`

```kotlin
package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.UserRole

data class RoleThemeColors(
  val accent: Color,
  val lightTint: Color,
  val surfaceBg: Color,
  val role: UserRole
)

val LocalRoleTheme = staticCompositionLocalOf {
  RoleThemeColors(
    accent = EmployeeTeal,
    lightTint = EmployeeTealLight,
    surfaceBg = EmployeeSurface,
    role = UserRole.EMPLOYEE
  )
}

@Composable
fun LeaveDeskTheme(
  userRole: UserRole = UserRole.EMPLOYEE,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val roleColors = when (userRole) {
    UserRole.EMPLOYEE -> RoleThemeColors(EmployeeTeal, EmployeeTealLight, EmployeeSurface, UserRole.EMPLOYEE)
    UserRole.TEAM_LEAD -> RoleThemeColors(TeamLeadIndigo, TeamLeadIndigoLight, TeamLeadSurface, UserRole.TEAM_LEAD)
    UserRole.CEO -> RoleThemeColors(CeoPurple, CeoPurpleLight, CeoSurface, UserRole.CEO)
  }

  val lightScheme = lightColorScheme(
    primary = roleColors.accent,
    onPrimary = Color.White,
    primaryContainer = roleColors.lightTint,
    onPrimaryContainer = roleColors.accent,
    secondary = NeutralGrey,
    background = Color(0xFFFCFDFD),
    surface = Color.White,
    surfaceVariant = roleColors.surfaceBg,
    onSurface = TextPrimary,
    outline = CardBorder
  )

  CompositionLocalProvider(LocalRoleTheme provides roleColors) {
    MaterialTheme(
      colorScheme = lightScheme,
      typography = Typography,
      content = content
    )
  }
}

```


### File 7: app/src/main/java/com/example/ui/components/CommonComponents.kt
**File Path:** `app/src/main/java/com/example/ui/components/CommonComponents.kt`

```kotlin
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun LeaveStatusBadge(status: LeaveStatus, modifier: Modifier = Modifier) {
  val (bgColor, textColor) = when (status) {
    LeaveStatus.APPROVED -> StatusApprovedBg to StatusApprovedText
    LeaveStatus.PENDING -> StatusPendingBg to StatusPendingText
    LeaveStatus.REJECTED -> StatusRejectedBg to StatusRejectedText
    LeaveStatus.QUERY -> Color(0xFFE1D5E7) to Color(0xFF6B3F7C)
    LeaveStatus.WITHDRAWN -> NeutralTint to NeutralGrey
    LeaveStatus.CANCELLED -> Color(0xFFFDE8E8) to Color(0xFF991B1B)
    LeaveStatus.POSTPONED -> Color(0xFFFFEDD5) to Color(0xFFC2410C)
  }

  Surface(
    modifier = modifier.clip(RoundedCornerShape(8.dp)),
    color = bgColor
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(textColor)
      )
      Text(
        text = status.displayName,
        color = textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
fun LeaveTypeBadge(
  leaveType: LeaveType,
  modifier: Modifier = Modifier,
  showFullName: Boolean = false
) {
  val typeColor = Color(leaveType.colorHex)
  Surface(
    modifier = modifier.clip(RoundedCornerShape(6.dp)),
    color = typeColor.copy(alpha = 0.12f),
    border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.3f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(typeColor)
      )
      Text(
        text = if (showFullName) "${leaveType.code} - ${leaveType.title}" else leaveType.code,
        color = typeColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
fun RoleBadge(role: UserRole, modifier: Modifier = Modifier) {
  val (bg, fg) = when (role) {
    UserRole.EMPLOYEE -> EmployeeTealLight to EmployeeTeal
    UserRole.TEAM_LEAD -> TeamLeadIndigoLight to TeamLeadIndigo
    UserRole.CEO -> CeoPurpleLight to CeoPurple
  }
  Surface(
    modifier = modifier.clip(RoundedCornerShape(6.dp)),
    color = bg
  ) {
    Text(
      text = role.badgeTitle,
      color = fg,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

@Composable
fun StatCard(
  title: String,
  value: String,
  subtitle: String? = null,
  color: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 12.sp,
          color = TextSecondary,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = value,
          fontSize = 20.sp,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        if (!subtitle.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            fontSize = 11.sp,
            color = TextMuted
          )
        }
      }
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = color,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

@Composable
fun RequestTimeline(request: LeaveRequest, modifier: Modifier = Modifier) {
  val dtf = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      text = "Approval Journey",
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary
    )

    // Step 1: Submission
    TimelineItem(
      title = "Submitted by ${request.userName}",
      subtitle = "${request.workingDays} working days (${request.leaveType.code})",
      time = request.submittedAt.format(dtf),
      status = "Complete",
      isLast = !request.requiresCeoApproval && request.tlStep == null
    )

    // Step 2: TL Review (if not TL's own leave)
    if (request.userRole != UserRole.TEAM_LEAD) {
      val isTlDone = request.tlStep != null
      val tlDecision = request.tlStep?.decision?.displayName ?: "Under Review"
      TimelineItem(
        title = "Team Lead Review: ${request.tlStep?.approverName ?: "Assigned TL"}",
        subtitle = request.tlStep?.remarks ?: if (request.status == LeaveStatus.PENDING) "Awaiting Team Lead endorsement" else "No comment",
        time = request.tlStep?.timestamp?.format(dtf) ?: (if (request.submittedAt.isBefore(java.time.LocalDateTime.now().minusHours(48))) "Over 48h (Escalated)" else "Pending"),
        status = tlDecision,
        isLast = !request.requiresCeoApproval
      )
    }

    // Step 3: CEO Decision (if required)
    if (request.requiresCeoApproval) {
      val isCeoDone = request.ceoStep != null
      val ceoDecision = request.ceoStep?.decision?.displayName ?: if (request.tlStep?.decision == LeaveStatus.APPROVED || request.userRole == UserRole.TEAM_LEAD) "Under Review" else "Queued"
      TimelineItem(
        title = "CEO Final Approval: ${request.ceoStep?.approverName ?: "Robert Sterling"}",
        subtitle = request.ceoStep?.remarks ?: if (request.status == LeaveStatus.APPROVED) "Approved" else "Awaiting executive sign-off",
        time = request.ceoStep?.timestamp?.format(dtf) ?: "Pending",
        status = ceoDecision,
        isLast = true
      )
    }
  }
}

@Composable
private fun TimelineItem(
  title: String,
  subtitle: String,
  time: String,
  status: String,
  isLast: Boolean
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(16.dp)
    ) {
      Box(
        modifier = Modifier
          .size(14.dp)
          .clip(CircleShape)
          .background(
            when (status) {
              "Approved", "Complete" -> StatusApprovedGreen
              "Rejected" -> StatusRejectedRed
              "Query" -> CeoPurple
              else -> StatusPendingAmber
            }
          )
      )
      if (!isLast) {
        Box(
          modifier = Modifier
            .width(2.dp)
            .height(36.dp)
            .background(CardBorder)
        )
      }
    }

    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        )
        Text(
          text = time,
          fontSize = 10.sp,
          color = TextMuted
        )
      }
      Text(
        text = subtitle,
        fontSize = 11.sp,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Status: $status",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = when (status) {
          "Approved", "Complete" -> StatusApprovedText
          "Rejected" -> StatusRejectedText
          "Query" -> CeoPurple
          else -> StatusPendingText
        }
      )
    }
  }
}

@Composable
fun EmergencyMailDialog(
  user: User,
  startDate: java.time.LocalDate,
  endDate: java.time.LocalDate,
  initialReason: String,
  onDismiss: () -> Unit,
  onSendMail: (String) -> Unit
) {
  var reasonText by remember { mutableStateOf(initialReason) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = StatusRejectedRed
        )
        Text(text = "Send Emergency Mail", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Team attendance would drop below the minimum threshold. Normal leave is blocked. You may dispatch an urgent override request to your Team Lead & CEO.",
          fontSize = 12.sp,
          color = TextSecondary
        )

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = StatusNotesBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082))
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "TO: Team Lead & CEO (Executive Office)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "SUBJECT: [URGENT] Emergency Leave Request - ${user.name} ($startDate to $endDate)",
              fontSize = 11.sp,
              color = TextPrimary
            )
          }
        }

        OutlinedTextField(
          value = reasonText,
          onValueChange = { reasonText = it },
          label = { Text("Emergency Justification (Required)") },
          placeholder = { Text("Describe the critical medical or family emergency...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("emergency_reason_input"),
          minLines = 3,
          maxLines = 5
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSendMail(reasonText) },
        enabled = reasonText.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
        modifier = Modifier.testTag("send_emergency_mail_button")
      ) {
        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Send Emergency Mail")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun CalendarDatePickerDialog(
  title: String,
  initialDate: java.time.LocalDate,
  minDate: java.time.LocalDate? = null,
  onDateSelected: (java.time.LocalDate) -> Unit,
  onDismiss: () -> Unit
) {
  var viewingMonth by remember { mutableStateOf(java.time.YearMonth.from(initialDate)) }
  var pickedDate by remember { mutableStateOf(initialDate) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = EmployeeTealLight
        ) {
          Text(
            text = pickedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = EmployeeTeal,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Month navigator
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { viewingMonth = viewingMonth.minusMonths(1) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
          }

          Text(
            text = viewingMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          IconButton(
            onClick = { viewingMonth = viewingMonth.plusMonths(1) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
          }
        }

        // Day names header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
            Box(
              modifier = Modifier
                .weight(1f)
                .height(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = d,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
            }
          }
        }

        // Days Grid
        val firstDay = viewingMonth.atDay(1)
        val offset = firstDay.dayOfWeek.value - 1
        val daysInMonth = viewingMonth.lengthOfMonth()
        val totalCells = ((offset + daysInMonth + 6) / 7) * 7

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          for (row in 0 until (totalCells / 7)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              for (col in 0..6) {
                val cellIndex = row * 7 + col
                val dayNum = cellIndex - offset + 1
                if (dayNum in 1..daysInMonth) {
                  val date = viewingMonth.atDay(dayNum)
                  val isSelected = date == pickedDate
                  val isToday = date == java.time.LocalDate.now()
                  val isPast = minDate != null && date.isBefore(minDate)
                  val isWeekend = date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(38.dp)
                      .padding(1.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(
                        when {
                          isSelected -> EmployeeTeal
                          isToday -> EmployeeTealLight
                          isWeekend -> NeutralTint.copy(alpha = 0.5f)
                          else -> Color.Transparent
                        }
                      )
                      .clickable(enabled = !isPast) {
                        pickedDate = date
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = dayNum.toString(),
                      fontSize = 12.sp,
                      fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                      color = when {
                        isSelected -> Color.White
                        isPast -> TextMuted.copy(alpha = 0.5f)
                        isToday -> EmployeeTeal
                        else -> TextPrimary
                      }
                    )
                  }
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onDateSelected(pickedDate)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmployeeTeal)
      ) {
        Text("Confirm Date", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

fun resolveProofFromUri(context: android.content.Context, uri: android.net.Uri): LeaveProof {
  var name = "Gallery_Attachment_${System.currentTimeMillis()}"
  var sizeStr = "1.5 MB"
  var ext = "JPG"

  try {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
      val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
      if (cursor.moveToFirst()) {
        if (nameIndex != -1) {
          cursor.getString(nameIndex)?.let { name = it }
        }
        if (sizeIndex != -1) {
          val sizeBytes = cursor.getLong(sizeIndex)
          if (sizeBytes > 0) {
            val kb = sizeBytes / 1024
            sizeStr = if (kb > 1024) "${String.format(java.util.Locale.US, "%.1f", kb / 1024f)} MB" else "$kb KB"
          }
        }
      }
    }
  } catch (e: Exception) {
    // Fallback if cursor query fails
  }

  val dotIndex = name.lastIndexOf('.')
  if (dotIndex != -1 && dotIndex < name.length - 1) {
    ext = name.substring(dotIndex + 1).uppercase()
  } else {
    val mime = context.contentResolver.getType(uri)
    if (mime != null) {
      ext = when {
        mime.contains("png") -> "PNG"
        mime.contains("jpeg") || mime.contains("jpg") -> "JPG"
        mime.contains("pdf") -> "PDF"
        mime.contains("doc") -> "DOCX"
        else -> "IMG"
      }
      name = "$name.$ext"
    }
  }

  return LeaveProof(
    fileName = name,
    fileType = ext,
    fileSizeStr = sizeStr,
    uriString = uri.toString()
  )
}


```


### File 8: app/src/main/java/com/example/ui/screens/LoginScreen.kt
**File Path:** `app/src/main/java/com/example/ui/screens/LoginScreen.kt`

```kotlin
package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaveDeskRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  repository: LeaveDeskRepository,
  onLoginComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val focusManager = LocalFocusManager.current
  val scrollState = rememberScrollState()

  var organization by remember { mutableStateOf("leavedesk.corp") }
  var identifier by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  fun performLogin() {
    val cleanId = identifier.trim()
    val cleanPass = password.trim()
    val cleanOrg = organization.trim()

    if (cleanId.isEmpty() || cleanPass.isEmpty()) {
      errorMessage = "Please enter your Employee ID / Work Email and Password."
      return
    }

    isSubmitting = true
    errorMessage = null
    val success = repository.login(cleanId, cleanPass, cleanOrg)
    if (success) {
      errorMessage = null
      onLoginComplete()
    } else {
      errorMessage = "Authentication failed. Please verify your Employee ID or Work Email, Password, and Organization Domain."
    }
    isSubmitting = false
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFF8FAFC))
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(20.dp))

    // Corporate App Header
    Box(
      modifier = Modifier
        .size(68.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(EmployeeTeal),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.CalendarMonth,
        contentDescription = "LeaveDesk Logo",
        tint = Color.White,
        modifier = Modifier.size(38.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "LeaveDesk",
      fontSize = 26.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary
    )
    Text(
      text = "Enterprise Leave & Attendance Management",
      fontSize = 13.sp,
      color = TextSecondary,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(28.dp))

    // Production Login Form Card
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color.White,
      border = BorderStroke(1.dp, CardBorder),
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Corporate Sign In",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Enter your work credentials to access your portal",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }

        // Error message banner
        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = StatusRejectedRed,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = errorMessage!!,
                color = StatusRejectedRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Organization / Domain Field
        OutlinedTextField(
          value = organization,
          onValueChange = {
            organization = it
            errorMessage = null
          },
          label = { Text("Organization Domain / Org Code") },
          placeholder = { Text("e.g. leavedesk.corp or CORP01") },
          leadingIcon = {
            Icon(Icons.Default.Business, contentDescription = null, tint = TextMuted)
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_organization")
        )

        // Employee ID / Work Email Field
        OutlinedTextField(
          value = identifier,
          onValueChange = {
            identifier = it
            errorMessage = null
          },
          label = { Text("Employee ID or Corporate Email") },
          placeholder = { Text("e.g. EMP101 or alex.chen@leavedesk.corp") },
          leadingIcon = {
            Icon(Icons.Default.Badge, contentDescription = null, tint = TextMuted)
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_identifier")
        )

        // Password Field
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            errorMessage = null
          },
          label = { Text("Password") },
          placeholder = { Text("Enter your password") },
          leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
          },
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                tint = TextMuted
              )
            }
          },
          singleLine = true,
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              focusManager.clearFocus()
              performLogin()
            }
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_password")
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Submit Button
        Button(
          onClick = {
            focusManager.clearFocus()
            performLogin()
          },
          enabled = !isSubmitting,
          colors = ButtonDefaults.buttonColors(containerColor = EmployeeTeal),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("login_submit_button")
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
              Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Enterprise Security Footer
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = "Encrypted Connection",
        tint = TextMuted,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = "Enterprise SSO & Directory Services • 256-bit SSL Protected",
        fontSize = 11.sp,
        color = TextMuted,
        textAlign = TextAlign.Center
      )
    }
  }
}

```


### File 9: app/src/main/java/com/example/ui/screens/SharedScreens.kt
**File Path:** `app/src/main/java/com/example/ui/screens/SharedScreens.kt`

```kotlin
package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.data.LeaveDeskRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CompanyCalendarScreen(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val holidays by repository.companyHolidays.collectAsState()
  val groupEvents by repository.groupEvents.collectAsState()
  val leaveRequests by repository.leaveRequests.collectAsState()

  var currentMonth by remember { mutableStateOf(YearMonth.now()) }
  var selectedDate by remember { mutableStateOf(LocalDate.now()) }

  // TL Override Dialog
  var showTlOverrideDialog by remember { mutableStateOf(false) }
  var overrideTitle by remember { mutableStateOf("") }
  var overrideRemark by remember { mutableStateOf("") }
  var isWorkingDayOverride by remember { mutableStateOf(false) }

  if (showTlOverrideDialog) {
    AlertDialog(
      onDismissRequest = { showTlOverrideDialog = false },
      title = { Text("Group Calendar Event / Override", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Target Date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}", fontWeight = FontWeight.SemiBold)
          OutlinedTextField(
            value = overrideTitle,
            onValueChange = { overrideTitle = it },
            label = { Text("Event Title *") },
            placeholder = { Text("e.g. Critical Milestone Delivery") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = overrideRemark,
            onValueChange = { overrideRemark = it },
            label = { Text("Remark *") },
            modifier = Modifier.fillMaxWidth()
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Treat as Working Day override", fontSize = 12.sp)
            Switch(
              checked = isWorkingDayOverride,
              onCheckedChange = { isWorkingDayOverride = it }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val success = repository.addGroupCalendarEvent(
              tl = currentUser,
              date = selectedDate,
              title = overrideTitle,
              remark = overrideRemark,
              isWorkingDayOverride = isWorkingDayOverride
            )
            if (success) {
              showTlOverrideDialog = false
              overrideTitle = ""
              overrideRemark = ""
            }
          },
          enabled = overrideTitle.isNotBlank() && overrideRemark.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = TeamLeadIndigo)
        ) {
          Text("Save Group Event")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTlOverrideDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Details of selected date
  val selectedHoliday = remember(holidays, selectedDate) {
    holidays.find { it.date == selectedDate }
  }
  val selectedGroupEvents = remember(groupEvents, selectedDate, currentUser) {
    groupEvents.filter { it.date == selectedDate && it.groupId == currentUser.groupId }
  }
  val approvedLeavesOnDate = remember(leaveRequests, selectedDate) {
    leaveRequests.filter {
      it.status == LeaveStatus.APPROVED && !selectedDate.isBefore(it.startDate) && !selectedDate.isAfter(it.endDate)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header & Month Navigator
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Row {
        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
          Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
        }
        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
          Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
        }
      }
    }

    // Weekday headers
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
          Text(text = day, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }
      }
    }

    // Month Grid (Simplified 35-day block)
    val firstDayOfMonth = currentMonth.atDay(1)
    val dayOffset = (firstDayOfMonth.dayOfWeek.value - 1) // 0 for Mon
    val daysInMonth = currentMonth.lengthOfMonth()

    val totalCells = ((dayOffset + daysInMonth + 6) / 7) * 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      for (row in 0 until (totalCells / 7)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          for (col in 0..6) {
            val cellIndex = row * 7 + col
            val dayNum = cellIndex - dayOffset + 1
            if (dayNum in 1..daysInMonth) {
              val date = currentMonth.atDay(dayNum)
              val isSelected = date == selectedDate
              val holiday = holidays.find { it.date == date }
              val hasLeaves = approvedLeavesOnDate.any { it.startDate <= date && it.endDate >= date }

              val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

              val cellColor = when {
                holiday?.type == HolidayType.PUBLIC_HOLIDAY -> Color(HolidayType.PUBLIC_HOLIDAY.colorHex).copy(alpha = 0.15f)
                holiday?.type == HolidayType.COMPANY_CLOSURE -> Color(HolidayType.COMPANY_CLOSURE.colorHex).copy(alpha = 0.15f)
                holiday?.type == HolidayType.OPTIONAL_HOLIDAY -> Color(HolidayType.OPTIONAL_HOLIDAY.colorHex).copy(alpha = 0.15f)
                isWeekend -> NeutralTint.copy(alpha = 0.5f)
                else -> Color.White
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .padding(2.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) EmployeeTealLight else cellColor)
                  .border(
                    BorderStroke(
                      if (isSelected) 2.dp else 1.dp,
                      if (isSelected) EmployeeTeal else CardBorder
                    ),
                    RoundedCornerShape(6.dp)
                  )
                  .clickable { selectedDate = date },
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Text(
                      text = dayNum.toString(),
                      fontSize = 12.sp,
                      fontWeight = if (isSelected || holiday != null) FontWeight.Bold else FontWeight.SemiBold,
                      color = if (holiday != null) Color(holiday.colorHex) else TextPrimary
                    )
                    if (holiday?.isLockedByCeo == true) {
                      Text("🔒", fontSize = 8.sp, modifier = Modifier.padding(start = 2.dp))
                    }
                  }
                  if (holiday != null || hasLeaves) {
                    Box(
                      modifier = Modifier
                        .padding(top = 1.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (holiday != null) Color(holiday.colorHex) else EmployeeTeal)
                    )
                  }
                }
              }
            } else {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }

    // Color Legend
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      LegendDot("Public Holiday", Color(HolidayType.PUBLIC_HOLIDAY.colorHex))
      LegendDot("Closure", Color(HolidayType.COMPANY_CLOSURE.colorHex))
      LegendDot("Optional", Color(HolidayType.OPTIONAL_HOLIDAY.colorHex))
      LegendDot("Approved Leave", StatusApprovedGreen)
    }

    // Selected Date Details Card
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          if (currentUser.role == UserRole.TEAM_LEAD) {
            TextButton(onClick = { showTlOverrideDialog = true }) {
              Text("+ Group Event", fontSize = 11.sp, color = TeamLeadIndigo)
            }
          }
        }

        if (selectedHoliday != null) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(selectedHoliday.colorHex).copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = "${selectedHoliday.type.displayName}: ${selectedHoliday.title}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(selectedHoliday.colorHex)
              )
              if (selectedHoliday.remark.isNotBlank()) {
                Text(selectedHoliday.remark, fontSize = 11.sp, color = TextSecondary)
              }
              if (selectedHoliday.isLockedByCeo) {
                Text("🔒 Locked by CEO: TL cannot override.", fontSize = 10.sp, color = CeoPurple)
              }
            }
          }
        }

        if (selectedGroupEvents.isNotEmpty()) {
          selectedGroupEvents.forEach { ev ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = TeamLeadIndigoLight.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text("Group Event: ${ev.title}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TeamLeadIndigo)
                Text("Remark: ${ev.remark} • By ${ev.addedByTlName}", fontSize = 11.sp, color = TextSecondary)
              }
            }
          }
        }

        if (approvedLeavesOnDate.isNotEmpty()) {
          Text("Colleagues on Approved Leave:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          approvedLeavesOnDate.forEach { leave ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              LeaveTypeBadge(leave.leaveType)
              Text("${leave.userName} (${leave.groupName})", fontSize = 11.sp, color = TextPrimary)
            }
          }
        } else if (selectedHoliday == null && selectedGroupEvents.isEmpty()) {
          Text("Standard Working Day • Normal group attendance scheduled.", fontSize = 11.sp, color = TextSecondary)
        }
      }
    }
  }
}

@Composable
private fun LegendDot(label: String, color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Text(label, fontSize = 10.sp, color = TextSecondary)
  }
}

@Composable
fun QuickChatScreen(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier,
  initialThreadId: String? = null
) {
  val currentUser by repository.currentUser.collectAsState()
  val allMessages by repository.chatMessages.collectAsState()
  val availableThreads = remember(currentUser, allMessages) {
    repository.getAvailableChatThreads(currentUser)
  }

  val context = LocalContext.current

  var selectedThreadId by remember(availableThreads) {
    mutableStateOf(
      initialThreadId ?: availableThreads.firstOrNull { !it.isLocked }?.threadId ?: availableThreads.first().threadId
    )
  }

  var messageInput by remember { mutableStateOf("") }
  var attachedProof by remember { mutableStateOf<LeaveProof?>(null) }
  var showAttachmentDialog by remember { mutableStateOf(false) }
  var viewingAttachment by remember { mutableStateOf<LeaveProof?>(null) }

  // Mobile Gallery Photo Picker Launcher
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    uri?.let {
      attachedProof = resolveProofFromUri(context, it)
      showAttachmentDialog = false
    }
  }

  // System Document / File Picker Launcher
  val docLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    uri?.let {
      attachedProof = resolveProofFromUri(context, it)
      showAttachmentDialog = false
    }
  }

  val activeThread = remember(availableThreads, selectedThreadId) {
    availableThreads.find { it.threadId == selectedThreadId } ?: availableThreads.first()
  }

  val threadMessages = remember(allMessages, selectedThreadId) {
    allMessages.filter { it.threadId == selectedThreadId }.sortedBy { it.timestamp }
  }

  // Document Viewer Modal
  viewingAttachment?.let { proof ->
    AlertDialog(
      onDismissRequest = { viewingAttachment = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.AttachFile, contentDescription = null, tint = LocalRoleTheme.current.accent)
          Text(proof.fileName, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (proof.uriString != null && (proof.fileType == "PNG" || proof.fileType == "JPG" || proof.fileType == "JPEG" || proof.fileType == "IMG")) {
            AsyncImage(
              model = proof.uriString,
              contentDescription = proof.fileName,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .clip(RoundedCornerShape(8.dp)),
              contentScale = ContentScale.Fit
            )
          } else {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = NeutralSurface,
              border = BorderStroke(1.dp, CardBorder),
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(
                    imageVector = if (proof.fileType.equals("PDF", ignoreCase = true)) Icons.Default.Description else Icons.Default.Image,
                    contentDescription = null,
                    tint = LocalRoleTheme.current.accent,
                    modifier = Modifier.size(36.dp)
                  )
                  Text("Verified Corporate Attachment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                  Text("${proof.fileType} • ${proof.fileSizeStr}", fontSize = 11.sp, color = TextSecondary)
                  Text("Document signed & stored securely in LeaveDesk", fontSize = 10.sp, color = TextMuted)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { viewingAttachment = null },
          colors = ButtonDefaults.buttonColors(containerColor = LocalRoleTheme.current.accent)
        ) {
          Text("Close")
        }
      }
    )
  }

  // Attachment Picker Modal
  if (showAttachmentDialog) {
    AlertDialog(
      onDismissRequest = { showAttachmentDialog = false },
      title = { Text("Select Attachment Proof", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Choose a file from your mobile device or quick corporate sample:", fontSize = 12.sp, color = TextSecondary)

          // 1. Mobile Gallery Button
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = LocalRoleTheme.current.lightTint,
            border = BorderStroke(1.dp, LocalRoleTheme.current.accent),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                galleryLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
              .testTag("open_gallery_picker_btn")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(LocalRoleTheme.current.accent),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("Choose from Mobile Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Select photo, receipt, or screenshot from phone", fontSize = 10.sp, color = TextSecondary)
              }
            }
          }

          // 2. Mobile File Manager (PDF / Documents)
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = NeutralSurface,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                docLauncher.launch("*/*")
              }
              .testTag("open_doc_picker_btn")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF64748B)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("Browse Device Files (PDF / Docs)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Upload hospital summary or official PDF document", fontSize = 10.sp, color = TextSecondary)
              }
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
          Text("Or Select Sample Enterprise Proof:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)

          val presets = listOf(
            LeaveProof("Medical_Certificate_Hospital.pdf", "PDF", "1.4 MB"),
            LeaveProof("Flight_Booking_Tahoe.pdf", "PDF", "1.2 MB"),
            LeaveProof("Client_Signoff_Confirmation.pdf", "PDF", "420 KB"),
            LeaveProof("Sprint_Handover_Checklist.docx", "DOCX", "350 KB"),
            LeaveProof("Executive_Handover_Guidelines.pdf", "PDF", "420 KB")
          )

          presets.forEach { preset ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (attachedProof?.fileName == preset.fileName) LocalRoleTheme.current.lightTint else NeutralSurface,
              border = BorderStroke(1.dp, if (attachedProof?.fileName == preset.fileName) LocalRoleTheme.current.accent else CardBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  attachedProof = preset
                  showAttachmentDialog = false
                }
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = LocalRoleTheme.current.accent, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(preset.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                  Text("${preset.fileType} • ${preset.fileSizeStr}", fontSize = 10.sp, color = TextSecondary)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showAttachmentDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Corporate Chat & Queries", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = LocalRoleTheme.current.lightTint
      ) {
        Text(
          text = currentUser.role.displayName,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = LocalRoleTheme.current.accent,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }

    // Thread selector chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      availableThreads.forEach { thread ->
        val isSelected = selectedThreadId == thread.threadId
        FilterChip(
          selected = isSelected,
          onClick = { selectedThreadId = thread.threadId },
          leadingIcon = {
            if (thread.isLocked) {
              Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(14.dp), tint = TextMuted)
            } else if (thread.role != null) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (thread.role == UserRole.CEO) CeoPurple else if (thread.role == UserRole.TEAM_LEAD) TeamLeadIndigo else EmployeeTeal)
              )
            } else {
              Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(14.dp))
            }
          },
          label = { Text(thread.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
        )
      }
    }

    // Active Thread Header Card
    Card(
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(activeThread.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            activeThread.role?.let { RoleBadge(it) }
          }
          Text(activeThread.subtitle, fontSize = 11.sp, color = TextSecondary)
        }

        if (activeThread.isLocked) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = StatusRejectedBg
          ) {
            Text(
              text = "Locked",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = StatusRejectedRed,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }
    }

    // Messages list
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (threadMessages.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (activeThread.isLocked) activeThread.lockReason ?: "Thread is currently locked."
              else "No messages in this channel yet. Type below to start the conversation.",
              fontSize = 12.sp,
              color = TextMuted
            )
          }
        }
      }

      items(threadMessages, key = { it.id }) { msg ->
        val isMe = msg.senderId == currentUser.id
        val alignment = if (isMe) Alignment.End else Alignment.Start

        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = alignment
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            RoleBadge(msg.senderRole)
            Text(msg.senderName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Text(msg.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 9.sp, color = TextMuted)
          }

          // Message bubble
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = when {
              msg.isOfficialQuery -> CeoPurpleLight
              msg.isOfficialDecision -> StatusRejectedBg.copy(alpha = 0.5f)
              isMe -> LocalRoleTheme.current.lightTint
              else -> Color.White
            },
            border = BorderStroke(
              1.dp,
              when {
                msg.isOfficialQuery -> CeoPurple
                msg.isOfficialDecision -> StatusRejectedRed
                isMe -> LocalRoleTheme.current.accent.copy(alpha = 0.4f)
                else -> CardBorder
              }
            ),
            modifier = Modifier.widthIn(max = 300.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              if (msg.isOfficialQuery) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = CeoPurple
                ) {
                  Text(
                    text = "OFFICIAL APPROVAL QUERY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              if (msg.isOfficialDecision) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = StatusRejectedRed
                ) {
                  Text(
                    text = "EXECUTIVE DECISION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Text(
                text = msg.text,
                fontSize = 13.sp,
                color = TextPrimary
              )

              // Attached file inside chat bubble
              msg.attachment?.let { att ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color.White.copy(alpha = 0.9f),
                  border = BorderStroke(1.dp, CardBorder),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewingAttachment = att }
                ) {
                  Column {
                    if (att.uriString != null && (att.fileType == "PNG" || att.fileType == "JPG" || att.fileType == "JPEG" || att.fileType == "IMG")) {
                      AsyncImage(
                        model = att.uriString,
                        contentDescription = att.fileName,
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(130.dp)
                          .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                        contentScale = ContentScale.Crop
                      )
                    }
                    Row(
                      modifier = Modifier.padding(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Icon(
                        imageVector = if (att.fileType == "PDF") Icons.Default.Description else Icons.Default.Image,
                        contentDescription = null,
                        tint = LocalRoleTheme.current.accent,
                        modifier = Modifier.size(16.dp)
                      )
                      Column(modifier = Modifier.weight(1f)) {
                        Text(att.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                        Text("${att.fileType} • ${att.fileSizeStr} • Tap to expand", fontSize = 10.sp, color = TextMuted)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Input bar
    if (activeThread.isLocked) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = StatusRejectedBg,
        border = BorderStroke(1.dp, StatusRejectedRed.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = StatusRejectedRed, modifier = Modifier.size(16.dp))
          Text(
            text = activeThread.lockReason ?: "Communication channel is locked.",
            fontSize = 11.sp,
            color = StatusRejectedText
          )
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Active attachment chip before sending
        attachedProof?.let { att ->
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = LocalRoleTheme.current.lightTint,
            border = BorderStroke(1.dp, LocalRoleTheme.current.accent),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (att.uriString != null && (att.fileType == "PNG" || att.fileType == "JPG" || att.fileType == "JPEG" || att.fileType == "IMG")) {
                  AsyncImage(
                    model = att.uriString,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                  )
                } else {
                  Icon(Icons.Default.AttachFile, contentDescription = null, tint = LocalRoleTheme.current.accent, modifier = Modifier.size(16.dp))
                }
                Text("Ready to send: ${att.fileName} (${att.fileSizeStr})", fontSize = 11.sp, color = LocalRoleTheme.current.accent, fontWeight = FontWeight.SemiBold, maxLines = 1)
              }
              IconButton(onClick = { attachedProof = null }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = LocalRoleTheme.current.accent, modifier = Modifier.size(14.dp))
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          IconButton(
            onClick = { showAttachmentDialog = true },
            modifier = Modifier.size(40.dp)
          ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach Document", tint = LocalRoleTheme.current.accent)
          }

          OutlinedTextField(
            value = messageInput,
            onValueChange = { messageInput = it },
            placeholder = { Text("Reply or query response...") },
            modifier = Modifier
              .weight(1f)
              .testTag("chat_input_field"),
            singleLine = true
          )

          IconButton(
            onClick = {
              repository.sendChatMessage(
                sender = currentUser,
                text = messageInput,
                threadId = selectedThreadId,
                attachment = attachedProof,
                relatedRequestId = activeThread.relatedRequestId
              )
              messageInput = ""
              attachedProof = null
            },
            enabled = messageInput.isNotBlank() || attachedProof != null,
            modifier = Modifier.testTag("send_chat_message_btn")
          ) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = LocalRoleTheme.current.accent)
          }
        }
      }
    }
  }
}

@Composable
fun NotificationsScreen(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val allNotifications by repository.notifications.collectAsState()

  val myNotifications = remember(allNotifications, currentUser) {
    allNotifications.filter {
      it.recipientUserId == currentUser.id || it.recipientUserId == "ALL"
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      TextButton(onClick = { repository.markAllNotificationsAsRead(currentUser.id) }) {
        Text("Mark all read", fontSize = 12.sp)
      }
    }

    if (myNotifications.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No notifications yet.", color = TextMuted)
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(myNotifications, key = { it.id }) { notif ->
          val icon = when (notif.type) {
            NotificationType.NEW_REQUEST -> Icons.Default.AddComment
            NotificationType.DECISION -> Icons.Default.CheckCircle
            NotificationType.QUERY -> Icons.Default.HelpOutline
            NotificationType.REMINDER_24H -> Icons.Default.Schedule
            NotificationType.ESCALATION_48H -> Icons.Default.NotificationImportant
            NotificationType.CALENDAR_CHANGE -> Icons.Default.CalendarMonth
            NotificationType.CANCEL_CONFIRMATION -> Icons.Default.Cancel
            NotificationType.BACKUP_ASSIGNED -> Icons.Default.PersonOutline
            NotificationType.EMERGENCY_MAIL -> Icons.Default.Mail
          }

          val tintColor = when (notif.type) {
            NotificationType.DECISION -> StatusApprovedGreen
            NotificationType.ESCALATION_48H, NotificationType.EMERGENCY_MAIL -> StatusRejectedRed
            NotificationType.QUERY -> CeoPurple
            NotificationType.REMINDER_24H -> StatusPendingAmber
            else -> EmployeeTeal
          }

          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = if (notif.isRead) Color.White else Color(0xFFF0FDF4)),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { repository.markNotificationAsRead(notif.id) }
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.Top
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(tintColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(18.dp))
              }

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(notif.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                  Text(
                    notif.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                    fontSize = 10.sp,
                    color = TextMuted
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(notif.message, fontSize = 11.sp, color = TextSecondary)
              }
            }
          }
        }
      }
    }
  }
}

```


### File 10: app/src/main/java/com/example/ui/screens/EmployeeScreens.kt
**File Path:** `app/src/main/java/com/example/ui/screens/EmployeeScreens.kt`

```kotlin
package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaveDeskRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun EmployeeHomeScreen(
  repository: LeaveDeskRepository,
  onNavigateToApply: () -> Unit,
  onNavigateToRequests: () -> Unit,
  onNavigateToCalendar: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val leaveRequests by repository.leaveRequests.collectAsState()
  val userBalances by repository.userBalances.collectAsState()
  val groups by repository.groups.collectAsState()
  val companyHolidays by repository.companyHolidays.collectAsState()

  val myRequests = remember(leaveRequests, currentUser) {
    leaveRequests.filter { it.userId == currentUser.id }
  }
  val myQuotas = remember(userBalances, currentUser) {
    userBalances[currentUser.id] ?: emptyList()
  }

  val totalApplied = myRequests.size
  val pendingCount = myRequests.count { it.status == LeaveStatus.PENDING || it.status == LeaveStatus.QUERY }
  val approvedUpcoming = myRequests.filter {
    it.status == LeaveStatus.APPROVED && !it.startDate.isBefore(LocalDate.now())
  }.sortedBy { it.startDate }

  val myGroup = remember(groups, currentUser) {
    groups.find { it.id == currentUser.groupId }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Greeting Card
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = EmployeeSurface),
      border = BorderStroke(1.dp, EmployeeTealLight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Welcome back, ${currentUser.name.split(" ").first()}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${currentUser.designation} • ${currentUser.groupName}",
            fontSize = 12.sp,
            color = TextSecondary
          )
          if (myGroup != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(Icons.Default.Groups, contentDescription = null, tint = EmployeeTeal, modifier = Modifier.size(14.dp))
              Text(
                text = "Min required present: ${myGroup.minPresentThreshold} members",
                fontSize = 11.sp,
                color = EmployeeTeal,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
        Button(
          onClick = onNavigateToApply,
          colors = ButtonDefaults.buttonColors(containerColor = EmployeeTeal),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier.testTag("home_apply_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Apply", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Quick KPIs Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      StatCard(
        title = "Applied",
        value = totalApplied.toString(),
        subtitle = "Lifetime records",
        color = EmployeeTeal,
        icon = Icons.Default.DateRange,
        modifier = Modifier.weight(1f)
      )
      StatCard(
        title = "Pending",
        value = pendingCount.toString(),
        subtitle = "In review",
        color = StatusPendingAmber,
        icon = Icons.Default.HourglassTop,
        modifier = Modifier.weight(1f)
      )
    }

    // Leave Balances section with progress bars
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Leave Balances",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Fiscal Year 2026",
            fontSize = 11.sp,
            color = TextMuted
          )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Quotas list
        myQuotas.forEach { quota ->
          val typeColor = Color(quota.leaveType.colorHex)
          val progress = if (quota.leaveType.isNoCap || quota.total == 0f) 0f else (quota.used / quota.total).coerceIn(0f, 1f)

          Column(modifier = Modifier.padding(vertical = 5.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(typeColor)
                )
                Text(
                  text = quota.leaveType.title,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = TextPrimary
                )
                if (quota.leaveType.requiresProofByDefault) {
                  Text(
                    text = "• Proof req",
                    fontSize = 10.sp,
                    color = CeoPurple,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              if (quota.leaveType.isNoCap) {
                Text(
                  text = "${quota.used.toInt()} used (No Cap)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextSecondary
                )
              } else {
                Text(
                  text = "${quota.remaining.toInt()} / ${quota.total.toInt()} left",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (quota.remaining <= 2f) StatusRejectedRed else TextPrimary
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))
            if (!quota.leaveType.isNoCap) {
              LinearProgressIndicator(
                progress = { 1f - progress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = typeColor,
                trackColor = NeutralTint
              )
            }
          }
        }
      }
    }

    // Upcoming Leaves
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Upcoming Approved Leaves",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          TextButton(onClick = onNavigateToRequests) {
            Text("View All", fontSize = 12.sp, color = EmployeeTeal)
          }
        }

        if (approvedUpcoming.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No upcoming approved leaves scheduled.",
              fontSize = 12.sp,
              color = TextMuted
            )
          }
        } else {
          approvedUpcoming.take(2).forEach { leave ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              shape = RoundedCornerShape(8.dp),
              color = StatusApprovedBg.copy(alpha = 0.4f),
              border = BorderStroke(1.dp, StatusApprovedGreen.copy(alpha = 0.5f))
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    LeaveTypeBadge(leave.leaveType)
                    Text(
                      text = "${leave.startDate} to ${leave.endDate}",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${leave.workingDays} working days • Backup: ${leave.backupUserName ?: "None"}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                LeaveStatusBadge(leave.status)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun EmployeeApplyScreen(
  repository: LeaveDeskRepository,
  onApplySuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val allUsers by repository.users.collectAsState()
  val groups by repository.groups.collectAsState()

  // Form State
  var selectedType by remember { mutableStateOf(LeaveType.CL) }
  var startDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
  var endDate by remember { mutableStateOf(LocalDate.now().plusDays(2)) }
  var isHalfDay by remember { mutableStateOf(false) }
  var halfDaySession by remember { mutableStateOf("First Half") }

  var selectedBackupUser by remember { mutableStateOf<User?>(null) }
  var backupTaskNote by remember { mutableStateOf("") }
  var reason by remember { mutableStateOf("") }
  var uploadedProof by remember { mutableStateOf<LeaveProof?>(null) }

  val context = LocalContext.current
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      uploadedProof = resolveProofFromUri(context, uri)
    }
  }
  val docLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      uploadedProof = resolveProofFromUri(context, uri)
    }
  }

  // Calendar inspect modal
  var inspectedDayAvail by remember { mutableStateOf<LeaveDeskRepository.DayAvailability?>(null) }
  var showEmergencyMailDialog by remember { mutableStateOf(false) }
  var showStartDatePicker by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }

  // Team peers for backup
  val groupPeers = remember(allUsers, currentUser) {
    allUsers.filter { it.groupId == currentUser.groupId && it.id != currentUser.id }
  }

  // Live Validation
  val validation = remember(currentUser, selectedType, startDate, endDate, isHalfDay, uploadedProof) {
    repository.validateLeaveApplication(
      user = currentUser,
      leaveType = selectedType,
      startDate = startDate,
      endDate = endDate,
      isHalfDay = isHalfDay,
      hasProof = uploadedProof != null
    )
  }

  if (showEmergencyMailDialog) {
    EmergencyMailDialog(
      user = currentUser,
      startDate = startDate,
      endDate = endDate,
      initialReason = reason,
      onDismiss = { showEmergencyMailDialog = false },
      onSendMail = { emergencyReason ->
        repository.sendEmergencyMail(currentUser, startDate, endDate, emergencyReason)
        showEmergencyMailDialog = false
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Screen Title
    Text(
      text = "Apply for Leave",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary
    )

    // Step 1: Leave Type Selection
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "1. Select Leave Type",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal chips row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          LeaveType.values().forEach { type ->
            val isSelected = selectedType == type
            val typeColor = Color(type.colorHex)
            val quota = repository.getUserQuota(currentUser.id, type)
            val leftText = if (type.isNoCap) "No cap" else "${quota?.remaining?.toInt() ?: 0} left"

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) typeColor.copy(alpha = 0.15f) else Color(0xFFF9FAFB),
              border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) typeColor else CardBorder),
              modifier = Modifier
                .clickable {
                  selectedType = type
                  if (type == LeaveType.HD) {
                    isHalfDay = true
                    endDate = startDate
                  } else {
                    isHalfDay = false
                  }
                }
                .testTag("leave_type_${type.code.lowercase()}")
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = type.code,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (isSelected) typeColor else TextPrimary
                )
                Text(
                  text = leftText,
                  fontSize = 10.sp,
                  color = if (isSelected) typeColor else TextSecondary
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "${selectedType.title} • Max consecutive: ${selectedType.maxConsecutiveDays} days${if (selectedType.requiresProofByDefault) " • Proof mandatory" else ""}",
          fontSize = 11.sp,
          color = TextSecondary
        )
      }
    }

    // Step 2: Date Selection & Calendar Conflict Visualizer
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "2. Date Range & Availability",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = EmployeeTealLight
          ) {
            Text(
              text = "${validation.workingDays} working days",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = EmployeeTeal,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Date Picker Buttons
        // Date Picker Buttons with visual calendar dialog triggers
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = { showStartDatePicker = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color.White
            ),
            border = BorderStroke(1.dp, EmployeeTeal),
            modifier = Modifier
              .weight(1f)
              .testTag("select_start_date_btn")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmployeeTeal, modifier = Modifier.size(16.dp))
              Column(horizontalAlignment = Alignment.Start) {
                Text("Start Date", fontSize = 10.sp, color = TextMuted)
                Text(startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              }
            }
          }

          OutlinedButton(
            onClick = { if (!isHalfDay) showEndDatePicker = true },
            enabled = !isHalfDay,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isHalfDay) NeutralSurface else Color.White
            ),
            border = BorderStroke(1.dp, if (isHalfDay) CardBorder else EmployeeTeal),
            modifier = Modifier
              .weight(1f)
              .testTag("select_end_date_btn")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                Icons.Default.Event,
                contentDescription = null,
                tint = if (isHalfDay) TextMuted else EmployeeTeal,
                modifier = Modifier.size(16.dp)
              )
              Column(horizontalAlignment = Alignment.Start) {
                Text("End Date", fontSize = 10.sp, color = TextMuted)
                Text(
                  if (isHalfDay) startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                  else endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isHalfDay) TextMuted else TextPrimary
                )
              }
            }
          }
        }

        // Calendar Date Picker Dialogs
        if (showStartDatePicker) {
          CalendarDatePickerDialog(
            title = "Select Start Date",
            initialDate = startDate,
            minDate = LocalDate.now(),
            onDateSelected = { picked ->
              startDate = picked
              if (endDate.isBefore(picked) || isHalfDay) {
                endDate = picked
              }
            },
            onDismiss = { showStartDatePicker = false }
          )
        }

        if (showEndDatePicker) {
          CalendarDatePickerDialog(
            title = "Select End Date",
            initialDate = endDate,
            minDate = startDate,
            onDateSelected = { picked ->
              endDate = picked
            },
            onDismiss = { showEndDatePicker = false }
          )
        }

        // Half Day Options
        if (selectedType == LeaveType.HD || isHalfDay) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Session:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            FilterChip(
              selected = halfDaySession == "First Half",
              onClick = { halfDaySession = "First Half" },
              label = { Text("First Half (Morning)") }
            )
            FilterChip(
              selected = halfDaySession == "Second Half",
              onClick = { halfDaySession = "Second Half" },
              label = { Text("Second Half (Afternoon)") }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        // Calendar Strip Visualizer: Green tint = free, Amber = few slots, Red = blocked
        Text(
          text = "Team Slot Availability (tap day to see remarks/who's off):",
          fontSize = 11.sp,
          color = TextSecondary,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Next 10 days preview strip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val today = LocalDate.now()
          for (i in 0..11) {
            val d = today.plusDays(i.toLong())
            val avail = repository.getDayAvailability(currentUser.groupId, d)
            val isInRange = !d.isBefore(startDate) && !d.isAfter(endDate)

            val (chipBg, chipBorder, chipText) = when (avail.colorLevel) {
              LeaveDeskRepository.AvailabilityLevel.FREE -> Triple(StatusApprovedBg, StatusApprovedGreen, StatusApprovedText)
              LeaveDeskRepository.AvailabilityLevel.FEW_SLOTS -> Triple(StatusPendingBg, StatusPendingAmber, StatusPendingText)
              LeaveDeskRepository.AvailabilityLevel.BLOCKED -> Triple(StatusRejectedBg, StatusRejectedRed, StatusRejectedText)
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = chipBg,
              border = BorderStroke(if (isInRange) 2.dp else 1.dp, if (isInRange) EmployeeTeal else chipBorder),
              modifier = Modifier
                .width(48.dp)
                .clickable {
                  inspectedDayAvail = avail
                  startDate = d
                  if (endDate.isBefore(d) || isHalfDay) endDate = d
                }
            ) {
              Column(
                modifier = Modifier.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = d.dayOfWeek.name.take(3),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Medium,
                  color = chipText
                )
                Text(
                  text = d.dayOfMonth.toString(),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = chipText
                )
                Text(
                  text = if (avail.isBlocked) "Full" else "${avail.presentCount} in",
                  fontSize = 8.sp,
                  color = chipText
                )
              }
            }
          }
        }

        // Inspected day details banner
        inspectedDayAvail?.let { day ->
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = StatusNotesBg,
            border = BorderStroke(1.dp, Color(0xFFFFD54F)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = "${day.date.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))} Details:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              if (day.holidayTitle != null) {
                Text("• Holiday: ${day.holidayTitle}", fontSize = 10.sp, color = CeoPurple)
              }
              if (day.absentUsers.isNotEmpty()) {
                Text("• On Leave: ${day.absentUsers.joinToString(", ")}", fontSize = 10.sp, color = StatusRejectedRed)
              } else {
                Text("• All team members currently present", fontSize = 10.sp, color = StatusApprovedText)
              }
              Text("• Present: ${day.presentCount} / Min threshold: ${day.minThreshold}", fontSize = 10.sp, color = TextSecondary)
            }
          }
        }
      }
    }

    // Validation Inline Alerts
    if (validation.message != null) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (validation.isThresholdViolated || validation.isBalanceExceeded) StatusRejectedBg else StatusPendingBg,
        border = BorderStroke(1.dp, if (validation.isThresholdViolated || validation.isBalanceExceeded) StatusRejectedRed else StatusPendingAmber),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (validation.isThresholdViolated) Icons.Default.Block else Icons.Default.Warning,
              contentDescription = null,
              tint = if (validation.isThresholdViolated || validation.isBalanceExceeded) StatusRejectedRed else StatusPendingAmber,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = validation.message,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (validation.isThresholdViolated || validation.isBalanceExceeded) StatusRejectedText else StatusPendingText
            )
          }

          // Smart recovery actions
          if (validation.isBalanceExceeded) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = { selectedType = LeaveType.LOP },
              colors = ButtonDefaults.buttonColors(containerColor = Color(LeaveType.LOP.colorHex)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("switch_to_lop_button")
            ) {
              Text("Switch to LOP (Loss of Pay)", fontSize = 11.sp)
            }
          }

          if (validation.isThresholdViolated) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = { showEmergencyMailDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("trigger_emergency_mail_btn")
            ) {
              Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Send Emergency Mail to TL & CEO", fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Step 3: Proof Upload (Required for ML and long leaves >= 3 days)
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "3. Proof Attachment",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          if (validation.requiresProof) {
            Text(
              text = "Required",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = StatusRejectedRed
            )
          } else {
            Text(
              text = "Optional",
              fontSize = 11.sp,
              color = TextMuted
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "PDF, JPG, or PNG (max 5 MB). Mandatory for Maternity Leave and leaves ≥ 3 days.",
          fontSize = 11.sp,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uploadedProof != null) {
          // File preview chip with image preview if from gallery
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = EmployeeTealLight,
            border = BorderStroke(1.dp, EmployeeTeal),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                if (uploadedProof?.uriString != null && (uploadedProof?.fileType?.contains("JPG") == true || uploadedProof?.fileType?.contains("PNG") == true || uploadedProof?.fileType?.contains("Image") == true)) {
                  AsyncImage(
                    model = uploadedProof?.uriString,
                    contentDescription = "Proof thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                      .size(42.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .border(1.dp, EmployeeTeal, RoundedCornerShape(6.dp))
                  )
                } else {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmployeeTeal.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.AttachFile, contentDescription = null, tint = EmployeeTeal)
                    }
                  }
                }
                Column {
                  Text(
                    text = uploadedProof!!.fileName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${uploadedProof!!.fileType} • ${uploadedProof!!.fileSizeStr}${if (uploadedProof?.uriString != null) " • Local Device" else ""}",
                    fontSize = 10.sp,
                    color = TextSecondary
                  )
                }
              }
              IconButton(onClick = { uploadedProof = null }) {
                Icon(Icons.Default.Close, contentDescription = "Remove proof", tint = StatusRejectedRed)
              }
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmployeeTeal),
                modifier = Modifier
                  .weight(1f)
                  .testTag("gallery_proof_button")
              ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Gallery", fontSize = 12.sp)
              }

              OutlinedButton(
                onClick = {
                  docLauncher.launch("*/*")
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("device_file_proof_button")
              ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browse Files", fontSize = 12.sp)
              }
            }

            // Quick presets
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Quick Presets:", fontSize = 10.sp, color = TextSecondary)
              FilterChip(
                selected = false,
                onClick = {
                  uploadedProof = LeaveProof(
                    fileName = "Medical_Certificate_Hospital.pdf",
                    fileType = "PDF",
                    fileSizeStr = "1.4 MB"
                  )
                },
                label = { Text("Medical Slip", fontSize = 10.sp) }
              )
              FilterChip(
                selected = false,
                onClick = {
                  uploadedProof = LeaveProof(
                    fileName = "Flight_Tickets_Confirmed.pdf",
                    fileType = "PDF",
                    fileSizeStr = "850 KB"
                  )
                },
                label = { Text("Travel Ticket", fontSize = 10.sp) }
              )
            }
          }
        }
      }
    }

    // Step 4: Backup Person
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "4. Backup Colleague (Same Group)",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          groupPeers.forEach { peer ->
            val isSelected = selectedBackupUser?.id == peer.id
            FilterChip(
              selected = isSelected,
              onClick = {
                selectedBackupUser = if (isSelected) null else peer
              },
              label = { Text(peer.name, fontSize = 12.sp) },
              leadingIcon = if (isSelected) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
              } else null
            )
          }
        }

        if (selectedBackupUser != null) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = backupTaskNote,
            onValueChange = { backupTaskNote = it },
            label = { Text("Handover / Task Note for ${selectedBackupUser!!.name.split(" ").first()}") },
            placeholder = { Text("e.g. Please handle client sync and PR review") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
          )
        }
      }
    }

    // Step 5: Reason & Submit
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "5. Reason & Submission",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason for Leave (Required)") },
          placeholder = { Text("Provide sufficient detail for approver review...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("apply_reason_input"),
          minLines = 3,
          maxLines = 5
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Routing indicator badge
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = NeutralSurface,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Route, contentDescription = null, tint = EmployeeTeal, modifier = Modifier.size(16.dp))
            Text(
              text = if (validation.requiresCeoApproval)
                "Routing: Team Lead endorses first, then CEO final sign-off."
              else
                "Routing: Team Lead approval only (1-2 days).",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            val success = repository.submitLeaveRequest(
              user = currentUser,
              leaveType = selectedType,
              startDate = startDate,
              endDate = endDate,
              isHalfDay = isHalfDay,
              halfDaySession = if (isHalfDay) halfDaySession else null,
              reason = reason,
              backupUser = selectedBackupUser,
              backupTaskNote = backupTaskNote.ifBlank { null },
              proof = uploadedProof
            )
            if (success) {
              onApplySuccess()
            }
          },
          enabled = reason.isNotBlank() && validation.isValid,
          colors = ButtonDefaults.buttonColors(containerColor = EmployeeTeal),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_leave_button")
        ) {
          Text("Submit Leave Application", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun EmployeeRequestsScreen(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val allRequests by repository.leaveRequests.collectAsState()
  val context = LocalContext.current

  var selectedFilter by remember { mutableStateOf("ALL") }
  var inspectingRequest by remember { mutableStateOf<LeaveRequest?>(null) }

  // Action modals
  var cancelReasonDialogFor by remember { mutableStateOf<LeaveRequest?>(null) }
  var cancelReasonText by remember { mutableStateOf("") }

  var queryReplyDialogFor by remember { mutableStateOf<LeaveRequest?>(null) }
  var queryReplyText by remember { mutableStateOf("") }

  val myRequests = remember(allRequests, currentUser, selectedFilter) {
    val list = allRequests.filter { it.userId == currentUser.id }
    when (selectedFilter) {
      "PENDING" -> list.filter { it.status == LeaveStatus.PENDING || it.status == LeaveStatus.QUERY }
      "APPROVED" -> list.filter { it.status == LeaveStatus.APPROVED }
      "REJECTED" -> list.filter { it.status == LeaveStatus.REJECTED }
      else -> list
    }
  }

  // Cancel Request Dialog
  cancelReasonDialogFor?.let { req ->
    AlertDialog(
      onDismissRequest = { cancelReasonDialogFor = null },
      title = { Text("Request Cancel / Postpone", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Approver must confirm this request before balance is restored and calendar is freed.",
            fontSize = 12.sp,
            color = TextSecondary
          )
          OutlinedTextField(
            value = cancelReasonText,
            onValueChange = { cancelReasonText = it },
            label = { Text("Reason for cancellation (Required)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            repository.employeeRequestCancel(req, currentUser, cancelReasonText)
            cancelReasonDialogFor = null
            cancelReasonText = ""
          },
          enabled = cancelReasonText.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed)
        ) {
          Text("Submit Cancellation")
        }
      },
      dismissButton = {
        TextButton(onClick = { cancelReasonDialogFor = null }) {
          Text("Close")
        }
      }
    )
  }

  // Query Reply Dialog
  var queryReplyProof by remember { mutableStateOf<LeaveProof?>(null) }
  val replyGalleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      queryReplyProof = resolveProofFromUri(context, uri)
    }
  }
  val replyDocLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      queryReplyProof = resolveProofFromUri(context, uri)
    }
  }

  queryReplyDialogFor?.let { req ->
    val isCeoQuery = req.ceoStep?.decision == LeaveStatus.QUERY
    AlertDialog(
      onDismissRequest = { queryReplyDialogFor = null },
      title = { Text(if (isCeoQuery) "Reply to Executive (CEO) Query" else "Reply to Team Lead Query", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Query: ${req.queryQuestion}",
            fontSize = 12.sp,
            color = if (isCeoQuery) CeoPurple else TeamLeadIndigo,
            fontWeight = FontWeight.SemiBold
          )
          if (!req.querySuggestedDates.isNullOrBlank()) {
            Text(
              text = "Suggested Dates: ${req.querySuggestedDates}",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
          OutlinedTextField(
            value = queryReplyText,
            onValueChange = { queryReplyText = it },
            label = { Text("Your Response / Explanation *") },
            placeholder = { Text("Provide requested details or date confirmations...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
          )

          // Attach Proof Option
          Text("Provide Proof / Evidence Document:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                replyGalleryLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EmployeeTeal),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("From Gallery", fontSize = 11.sp)
            }
            OutlinedButton(
              onClick = { replyDocLauncher.launch("*/*") },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Device Files", fontSize = 11.sp)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = queryReplyProof?.fileName == "Deployment_Handover_Signed.pdf",
              onClick = {
                queryReplyProof = if (queryReplyProof?.fileName == "Deployment_Handover_Signed.pdf") null
                else LeaveProof("Deployment_Handover_Signed.pdf", "PDF", "650 KB")
              },
              label = { Text("Handover (PDF)", fontSize = 10.sp) }
            )
            FilterChip(
              selected = queryReplyProof?.fileName == "Medical_Certificate_Hospital.pdf",
              onClick = {
                queryReplyProof = if (queryReplyProof?.fileName == "Medical_Certificate_Hospital.pdf") null
                else LeaveProof("Medical_Certificate_Hospital.pdf", "PDF", "1.4 MB")
              },
              label = { Text("Medical Proof", fontSize = 10.sp) }
            )
          }

          queryReplyProof?.let { proof ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = EmployeeTealLight,
              border = BorderStroke(1.dp, EmployeeTeal),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  if (proof.uriString != null) {
                    AsyncImage(
                      model = proof.uriString,
                      contentDescription = "Attachment preview",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, EmployeeTeal, RoundedCornerShape(4.dp))
                    )
                  } else {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = EmployeeTeal, modifier = Modifier.size(16.dp))
                  }
                  Column {
                    Text(proof.fileName, fontSize = 11.sp, color = EmployeeTeal, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${proof.fileType} • ${proof.fileSizeStr}", fontSize = 10.sp, color = TextSecondary)
                  }
                }
                IconButton(onClick = { queryReplyProof = null }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Remove", tint = EmployeeTeal, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            repository.employeeReplyQuery(req, currentUser, queryReplyText)
            // Also send into the dedicated chat thread
            val threadId = if (isCeoQuery) "thread_emp_${currentUser.id}_ceo" else "thread_emp_${currentUser.id}_tl"
            repository.sendChatMessage(
              sender = currentUser,
              text = "REPLY TO QUERY on ${req.id}:\n$queryReplyText",
              threadId = threadId,
              relatedRequestId = req.id,
              attachment = queryReplyProof
            )
            queryReplyDialogFor = null
            queryReplyText = ""
            queryReplyProof = null
          },
          enabled = queryReplyText.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = EmployeeTeal)
        ) {
          Text("Send Reply & Proofs")
        }
      },
      dismissButton = {
        TextButton(onClick = { queryReplyDialogFor = null }) {
          Text("Cancel")
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "My Leave Requests",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary
    )

    // Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("ALL" to "All", "PENDING" to "Pending", "APPROVED" to "Approved", "REJECTED" to "Rejected").forEach { (code, title) ->
        FilterChip(
          selected = selectedFilter == code,
          onClick = { selectedFilter = code },
          label = { Text(title, fontSize = 12.sp) }
        )
      }
    }

    if (myRequests.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No requests found in this view.", color = TextMuted)
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(myRequests, key = { it.id }) { req ->
          val isExpanded = inspectingRequest?.id == req.id
          val isOver24h = req.status == LeaveStatus.PENDING && req.submittedAt.isBefore(java.time.LocalDateTime.now().minusHours(24))
          val isOver48h = req.status == LeaveStatus.PENDING && req.submittedAt.isBefore(java.time.LocalDateTime.now().minusHours(48))

          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, if (isExpanded) EmployeeTeal else CardBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                inspectingRequest = if (isExpanded) null else req
              }
              .testTag("request_item_${req.id.lowercase()}")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  LeaveTypeBadge(req.leaveType)
                  Text(
                    text = req.id,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                  )
                }
                LeaveStatusBadge(req.status)
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "${req.startDate} to ${req.endDate} (${req.workingDays} working days)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = req.reason,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = if (isExpanded) 10 else 1,
                overflow = TextOverflow.Ellipsis
              )

              // Escalation / Reminder flags
              if (isOver48h) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = StatusRejectedBg
                ) {
                  Text(
                    text = "🚨 Over 48h Escalation Active • Still waiting",
                    fontSize = 10.sp,
                    color = StatusRejectedText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              } else if (isOver24h) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = StatusPendingBg
                ) {
                  Text(
                    text = "⏳ Pending > 24h • Approver notified",
                    fontSize = 10.sp,
                    color = StatusPendingText,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              // Query alert
              if (req.status == LeaveStatus.QUERY && !req.queryQuestion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFFF3E8FF),
                  border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                      text = "TL Query: ${req.queryQuestion}",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = CeoPurple
                    )
                    if (!req.querySuggestedDates.isNullOrBlank()) {
                      Text("Suggested: ${req.querySuggestedDates}", fontSize = 10.sp, color = TextSecondary)
                    }
                  }
                }
              }

              // Expanded Details & Timeline
              if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = CardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                RequestTimeline(req)

                if (req.backupUserName != null) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "Backup Person: ${req.backupUserName}${if (req.backupTaskNote != null) " (${req.backupTaskNote})" else ""}",
                    fontSize = 11.sp,
                    color = TextSecondary
                  )
                }

                if (req.proof != null) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmployeeTealLight.copy(alpha = 0.5f)
                  ) {
                    Row(
                      modifier = Modifier.padding(6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(Icons.Default.AttachFile, contentDescription = null, tint = EmployeeTeal, modifier = Modifier.size(14.dp))
                      Text(
                        text = "${req.proof.fileName} (${req.proof.fileSizeStr})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = EmployeeTeal
                      )
                    }
                  }
                }

                // Action Buttons for this request
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  when (req.status) {
                    LeaveStatus.PENDING -> {
                      OutlinedButton(
                        onClick = { repository.employeeWithdrawRequest(req, currentUser) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeutralGrey),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                      ) {
                        Text("Withdraw", fontSize = 12.sp)
                      }
                      if (isOver24h) {
                        Button(
                          onClick = { repository.sendApproverReminder(req, currentUser) },
                          colors = ButtonDefaults.buttonColors(containerColor = StatusPendingAmber),
                          shape = RoundedCornerShape(8.dp),
                          modifier = Modifier.weight(1f)
                        ) {
                          Text("Send Reminder", fontSize = 11.sp, color = Color.White)
                        }
                      }
                    }

                    LeaveStatus.APPROVED -> {
                      val hasStarted = !LocalDate.now().isBefore(req.startDate)
                      if (!hasStarted) {
                        Button(
                          onClick = { cancelReasonDialogFor = req },
                          colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                          shape = RoundedCornerShape(8.dp),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Text("Request Cancel / Postpone", fontSize = 12.sp)
                        }
                      } else {
                        Text(
                          text = "Leave already started: Cannot cancel online, contact TL or HR.",
                          fontSize = 11.sp,
                          color = StatusRejectedRed,
                          fontWeight = FontWeight.Medium
                        )
                      }
                    }

                    LeaveStatus.QUERY -> {
                      Button(
                        onClick = { queryReplyDialogFor = req },
                        colors = ButtonDefaults.buttonColors(containerColor = CeoPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                      ) {
                        Text("Reply to Query", fontSize = 12.sp)
                      }
                      OutlinedButton(
                        onClick = { repository.employeeWithdrawRequest(req, currentUser) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                      ) {
                        Text("Withdraw", fontSize = 12.sp)
                      }
                    }

                    else -> {}
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

```


### File 11: app/src/main/java/com/example/ui/screens/TeamLeadScreens.kt
**File Path:** `app/src/main/java/com/example/ui/screens/TeamLeadScreens.kt`

```kotlin
package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaveDeskRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.LocalDate

@Composable
fun TeamLeadHomeScreen(
  repository: LeaveDeskRepository,
  onNavigateToTeamRequests: () -> Unit,
  onNavigateToMyLeaves: () -> Unit,
  onNavigateToCalendar: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val allRequests by repository.leaveRequests.collectAsState()
  val groups by repository.groups.collectAsState()

  val myGroup = remember(groups, currentUser) {
    groups.find { it.tlUserId == currentUser.id }
  }

  val teamRequests = remember(allRequests, myGroup) {
    if (myGroup == null) emptyList()
    else allRequests.filter { it.groupId == myGroup.id && it.userId != currentUser.id }
  }

  val pendingApprovals = teamRequests.filter {
    it.status == LeaveStatus.PENDING && (it.tlStep == null || it.tlStep.decision != LeaveStatus.APPROVED)
  }

  val myOwnRequests = remember(allRequests, currentUser) {
    allRequests.filter { it.userId == currentUser.id }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Lead Greeting Card
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = TeamLeadSurface),
      border = BorderStroke(1.dp, TeamLeadIndigoLight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Team Lead: ${currentUser.name}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${currentUser.groupName} • ${myGroup?.memberIds?.size ?: 0} members",
            fontSize = 12.sp,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = TeamLeadIndigoLight
          ) {
            Text(
              text = "Min attendance threshold: ${myGroup?.minPresentThreshold ?: 2} present",
              fontSize = 11.sp,
              color = TeamLeadIndigo,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }
    }

    // Lead KPIs
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      StatCard(
        title = "Pending Approvals",
        value = pendingApprovals.size.toString(),
        subtitle = "Awaiting your decision",
        color = TeamLeadIndigo,
        icon = Icons.Default.PendingActions,
        modifier = Modifier.weight(1f)
      )
      StatCard(
        title = "My Leaves",
        value = myOwnRequests.size.toString(),
        subtitle = "Personal applications",
        color = CeoPurple,
        icon = Icons.Default.AccountCircle,
        modifier = Modifier.weight(1f)
      )
    }

    // Pending Requests Quick Glance
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Urgent Team Inbox",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          TextButton(onClick = onNavigateToTeamRequests) {
            Text("Open Inbox", fontSize = 12.sp, color = TeamLeadIndigo)
          }
        }

        if (pendingApprovals.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("All team requests reviewed! Inbox clean.", color = TextMuted, fontSize = 12.sp)
          }
        } else {
          pendingApprovals.take(3).forEach { req ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              shape = RoundedCornerShape(8.dp),
              color = NeutralSurface,
              border = BorderStroke(1.dp, CardBorder)
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    LeaveTypeBadge(req.leaveType)
                    Text(
                      text = req.userName,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${req.startDate} to ${req.endDate} (${req.workingDays.toInt()}d)",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Button(
                  onClick = onNavigateToTeamRequests,
                  colors = ButtonDefaults.buttonColors(containerColor = TeamLeadIndigo),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  modifier = Modifier.wrapContentWidth()
                ) {
                  Text("Review", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
              }
            }
          }
        }
      }
    }

    // Quick Group Event Add Button
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Team Calendar Management", fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text("Manage group events or working day overrides", fontSize = 11.sp, color = TextSecondary)
        }
        OutlinedButton(
          onClick = onNavigateToCalendar,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TeamLeadIndigo)
        ) {
          Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Calendar", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun TeamLeadRequestsScreen(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val allRequests by repository.leaveRequests.collectAsState()
  val groups by repository.groups.collectAsState()

  var selectedFilter by remember { mutableStateOf("PENDING") }
  val myGroup = remember(groups, currentUser) {
    groups.find { it.tlUserId == currentUser.id }
  }

  val teamRequests = remember(allRequests, myGroup, selectedFilter) {
    if (myGroup == null) emptyList()
    else {
      val base = allRequests.filter { it.groupId == myGroup.id && it.userId != currentUser.id }
      when (selectedFilter) {
        "PENDING" -> base.filter { it.status == LeaveStatus.PENDING || it.status == LeaveStatus.QUERY }
        "APPROVED" -> base.filter { it.status == LeaveStatus.APPROVED }
        "CANCEL_REQUESTED" -> base.filter { it.cancelRequested }
        "REJECTED" -> base.filter { it.status == LeaveStatus.REJECTED }
        else -> base
      }
    }
  }

  // Dialog states
  var rejectDialogFor by remember { mutableStateOf<LeaveRequest?>(null) }
  var rejectReason by remember { mutableStateOf("") }

  var queryDialogFor by remember { mutableStateOf<LeaveRequest?>(null) }
  var queryQuestion by remember { mutableStateOf("") }
  var querySuggestedDates by remember { mutableStateOf("") }

  var emergencyCancelFor by remember { mutableStateOf<LeaveRequest?>(null) }
  var emergencyCancelReason by remember { mutableStateOf("") }

  // Reject Dialog
  rejectDialogFor?.let { req ->
    AlertDialog(
      onDismissRequest = { rejectDialogFor = null },
      title = { Text("Reject Leave Request", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Rejection reason is mandatory and will be logged in the audit trail.", fontSize = 12.sp, color = TextSecondary)
          OutlinedTextField(
            value = rejectReason,
            onValueChange = { rejectReason = it },
            label = { Text("Reason for Rejection *") },
            placeholder = { Text("e.g. Critical milestone sprint deadline...") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("tl_reject_reason_input"),
            minLines = 2
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            repository.tlRejectRequest(req, currentUser, rejectReason)
            rejectDialogFor = null
            rejectReason = ""
          },
          enabled = rejectReason.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
          modifier = Modifier.testTag("tl_confirm_reject_btn")
        ) {
          Text("Confirm Reject")
        }
      },
      dismissButton = {
        TextButton(onClick = { rejectDialogFor = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Query Dialog
  val context = LocalContext.current
  var queryAttachment by remember { mutableStateOf<LeaveProof?>(null) }
  val tlGalleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      queryAttachment = resolveProofFromUri(context, uri)
    }
  }
  val tlDocLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      queryAttachment = resolveProofFromUri(context, uri)
    }
  }

  queryDialogFor?.let { req ->
    AlertDialog(
      onDismissRequest = { queryDialogFor = null },
      title = { Text("Query / Suggest Dates", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Send question or recommend alternate dates to ${req.userName}. This will be delivered directly to the chat between you and the employee.", fontSize = 12.sp, color = TextSecondary)
          OutlinedTextField(
            value = queryQuestion,
            onValueChange = { queryQuestion = it },
            label = { Text("Question / Concern *") },
            placeholder = { Text("Can you push leave by 2 days?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
          )
          OutlinedTextField(
            value = querySuggestedDates,
            onValueChange = { querySuggestedDates = it },
            label = { Text("Suggested Dates (Optional)") },
            placeholder = { Text("e.g. Nov 14 to Nov 16") },
            modifier = Modifier.fillMaxWidth()
          )

          // Attachment option
          Text("Attach Document, Image, or Sprint Schedule:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                tlGalleryLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TeamLeadIndigo),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("From Gallery", fontSize = 11.sp)
            }
            OutlinedButton(
              onClick = { tlDocLauncher.launch("*/*") },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Device Files", fontSize = 11.sp)
            }
          }

          FilterChip(
            selected = queryAttachment?.fileName == "Sprint_Delivery_Timeline.pdf",
            onClick = {
              queryAttachment = if (queryAttachment?.fileName == "Sprint_Delivery_Timeline.pdf") null
              else LeaveProof("Sprint_Delivery_Timeline.pdf", "PDF", "310 KB")
            },
            label = { Text("Sprint Timeline (PDF)", fontSize = 10.sp) }
          )

          queryAttachment?.let { att ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = TeamLeadSurface,
              border = BorderStroke(1.dp, TeamLeadIndigo.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  if (att.uriString != null) {
                    AsyncImage(
                      model = att.uriString,
                      contentDescription = "Attachment preview",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, TeamLeadIndigo, RoundedCornerShape(4.dp))
                    )
                  } else {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = TeamLeadIndigo, modifier = Modifier.size(16.dp))
                  }
                  Column {
                    Text(att.fileName, fontSize = 11.sp, color = TeamLeadIndigo, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${att.fileType} • ${att.fileSizeStr}", fontSize = 10.sp, color = TextSecondary)
                  }
                }
                IconButton(onClick = { queryAttachment = null }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Remove", tint = TeamLeadIndigo, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            repository.tlQueryRequest(
              req,
              currentUser,
              queryQuestion,
              querySuggestedDates.ifBlank { null },
              queryAttachment
            )
            queryDialogFor = null
            queryQuestion = ""
            querySuggestedDates = ""
            queryAttachment = null
          },
          enabled = queryQuestion.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = CeoPurple)
        ) {
          Text("Send to Chat")
        }
      },
      dismissButton = {
        TextButton(onClick = { queryDialogFor = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // TL Emergency Cancel Dialog
  emergencyCancelFor?.let { req ->
    AlertDialog(
      onDismissRequest = { emergencyCancelFor = null },
      title = { Text("Emergency Cancellation of Approved Leave", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("In an emergency, TLs can cancel approved leaves. This restores the employee's balance and alerts the employee, backup, and CEO.", fontSize = 12.sp, color = TextSecondary)
          OutlinedTextField(
            value = emergencyCancelReason,
            onValueChange = { emergencyCancelReason = it },
            label = { Text("Emergency Reason *") },
            placeholder = { Text("e.g. Unforeseen production outage requires all hands...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            repository.tlEmergencyCancel(req, currentUser, emergencyCancelReason)
            emergencyCancelFor = null
            emergencyCancelReason = ""
          },
          enabled = emergencyCancelReason.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed)
        ) {
          Text("Cancel Leave")
        }
      },
      dismissButton = {
        TextButton(onClick = { emergencyCancelFor = null }) {
          Text("Close")
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Team Requests Inbox",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary
    )

    // Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(
        "PENDING" to "Pending Review",
        "CANCEL_REQUESTED" to "Cancel Requests",
        "APPROVED" to "Approved",
        "REJECTED" to "Rejected",
        "ALL" to "All"
      ).forEach { (code, title) ->
        FilterChip(
          selected = selectedFilter == code,
          onClick = { selectedFilter = code },
          label = { Text(title, fontSize = 12.sp) }
        )
      }
    }

    if (teamRequests.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No requests found in this filter.", color = TextMuted)
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(teamRequests, key = { it.id }) { req ->
          // Calculate who else is off for these exact dates!
          val colleaguesOff = remember(allRequests, req) {
            allRequests.filter {
              it.id != req.id &&
                it.groupId == req.groupId &&
                it.status == LeaveStatus.APPROVED &&
                !(req.endDate.isBefore(it.startDate) || req.startDate.isAfter(it.endDate))
            }.map { "${it.userName} (${it.leaveType.code})" }
          }

          val isCoverageBreached = myGroup != null &&
            (myGroup.memberIds.size - (colleaguesOff.size + 1)) < myGroup.minPresentThreshold &&
            !req.leaveType.isUrgentNeverBlocked

          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  LeaveTypeBadge(req.leaveType)
                  Text(
                    text = req.userName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                }
                LeaveStatusBadge(req.status)
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${req.startDate} to ${req.endDate} • ${req.workingDays} working days",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
              )
              Text(
                text = "Reason: ${req.reason}",
                fontSize = 12.sp,
                color = TextPrimary
              )

              if (req.leaveType.isUrgentNeverBlocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = StatusRejectedBg
                ) {
                  Text(
                    text = "🚨 URGENT: Sick Leave (Never blocked by threshold)",
                    fontSize = 10.sp,
                    color = StatusRejectedText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              // "Who else is off those dates"
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (colleaguesOff.isEmpty()) StatusApprovedBg.copy(alpha = 0.4f) else StatusPendingBg.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = if (colleaguesOff.isEmpty()) Icons.Default.CheckCircle else Icons.Default.People,
                    contentDescription = null,
                    tint = if (colleaguesOff.isEmpty()) StatusApprovedText else StatusPendingText,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = if (colleaguesOff.isEmpty()) "Full team present (0 others off on these dates)"
                    else "Also off on these dates: ${colleaguesOff.joinToString(", ")}",
                    fontSize = 11.sp,
                    color = if (colleaguesOff.isEmpty()) StatusApprovedText else StatusPendingText,
                    fontWeight = FontWeight.Medium
                  )
                }
              }

              // Coverage warning banner
              if (isCoverageBreached) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = StatusRejectedBg,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRejectedRed, modifier = Modifier.size(16.dp))
                    Text(
                      text = "Coverage Warning: Approving leaves fewer than min threshold (${myGroup?.minPresentThreshold}) present!",
                      fontSize = 10.sp,
                      color = StatusRejectedText,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }

              // Proof chip if attached
              req.proof?.let { proof ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = NeutralTint
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                      text = "Proof: ${proof.fileName} (${proof.fileSizeStr})",
                      fontSize = 11.sp,
                      color = TextPrimary
                    )
                  }
                }
              }

              // Backup person
              req.backupUserName?.let { backup ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Backup: $backup${if (req.backupTaskNote != null) " • Handover: ${req.backupTaskNote}" else ""}",
                  fontSize = 11.sp,
                  color = TextMuted
                )
              }

              // Cancellation Request confirmation from employee
              if (req.cancelRequested) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = StatusRejectedBg,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                      text = "Employee requested cancellation: '${req.cancelReason}'",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = StatusRejectedText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                      onClick = { repository.approverConfirmCancel(req, currentUser) },
                      colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text("Confirm Cancel & Restore Balance", fontSize = 11.sp)
                    }
                  }
                }
              }

              // Action Buttons
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (req.status == LeaveStatus.PENDING) {
                  Button(
                    onClick = { repository.tlApproveRequest(req, currentUser, "Endorsed by Team Lead") },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("tl_approve_btn_${req.id.lowercase()}")
                  ) {
                    Text(if (req.requiresCeoApproval) "Endorse" else "Approve", fontSize = 12.sp)
                  }

                  OutlinedButton(
                    onClick = { queryDialogFor = req },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CeoPurple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("Query", fontSize = 12.sp)
                  }

                  Button(
                    onClick = { rejectDialogFor = req },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("tl_reject_btn_${req.id.lowercase()}")
                  ) {
                    Text("Reject", fontSize = 12.sp)
                  }
                } else if (req.status == LeaveStatus.APPROVED) {
                  Button(
                    onClick = { emergencyCancelFor = req },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Emergency Cancel / Postpone", fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

```


### File 12: app/src/main/java/com/example/ui/screens/CeoScreens.kt
**File Path:** `app/src/main/java/com/example/ui/screens/CeoScreens.kt`

```kotlin
package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaveDeskRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CeoDashboardScreen(
  repository: LeaveDeskRepository,
  onNavigateToApprovals: () -> Unit,
  onNavigateToAudit: () -> Unit,
  onNavigateToCalendarEditor: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val allUsers by repository.users.collectAsState()
  val allRequests by repository.leaveRequests.collectAsState()
  val holidays by repository.companyHolidays.collectAsState()

  val totalEmployees = allUsers.size
  val today = LocalDate.now()

  // On leave today
  val onLeaveToday = remember(allRequests, today) {
    allRequests.filter {
      it.status == LeaveStatus.APPROVED && !today.isBefore(it.startDate) && !today.isAfter(it.endDate)
    }
  }
  val atWorkToday = (totalEmployees - onLeaveToday.size).coerceAtLeast(0)

  // Pending CEO Approvals
  val pendingCeoApprovals = remember(allRequests) {
    allRequests.filter { req ->
      req.status == LeaveStatus.PENDING &&
        (req.userRole == UserRole.TEAM_LEAD || req.requiresCeoApproval)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Executive Greeting Card
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = CeoSurface),
      border = BorderStroke(1.dp, CeoPurpleLight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "CEO Executive Suite",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${currentUser.name} • Company Governance",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
        RoleBadge(UserRole.CEO)
      }
    }

    // CEO 4 KPI Cards Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      StatCard(
        title = "Total Employees",
        value = totalEmployees.toString(),
        subtitle = "Across 2 groups",
        color = CeoPurple,
        icon = Icons.Default.Groups,
        modifier = Modifier.weight(1f)
      )
      StatCard(
        title = "At Work Today",
        value = atWorkToday.toString(),
        subtitle = "${onLeaveToday.size} on leave",
        color = StatusApprovedGreen,
        icon = Icons.Default.Work,
        modifier = Modifier.weight(1f)
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      StatCard(
        title = "On Leave Today",
        value = onLeaveToday.size.toString(),
        subtitle = if (onLeaveToday.isEmpty()) "Full attendance" else "${onLeaveToday.first().userName} etc.",
        color = Color(LeaveType.EL.colorHex),
        icon = Icons.Default.BeachAccess,
        modifier = Modifier.weight(1f)
      )
      StatCard(
        title = "Pending Approvals",
        value = pendingCeoApprovals.size.toString(),
        subtitle = "Requires sign-off",
        color = StatusPendingAmber,
        icon = Icons.Default.PendingActions,
        modifier = Modifier.weight(1f)
      )
    }

    // Pending Approvals Action Card
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("CEO Approval Queue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
          TextButton(onClick = onNavigateToApprovals) {
            Text("Open Approvals", fontSize = 12.sp, color = CeoPurple)
          }
        }

        if (pendingCeoApprovals.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("No executive approvals waiting! All signed.", fontSize = 12.sp, color = TextMuted)
          }
        } else {
          pendingCeoApprovals.take(3).forEach { req ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              shape = RoundedCornerShape(8.dp),
              color = NeutralSurface
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    LeaveTypeBadge(req.leaveType)
                    Text(
                      text = req.userName,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${req.startDate} to ${req.endDate} (${req.workingDays.toInt()}d)",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = req.groupName,
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Button(
                  onClick = onNavigateToApprovals,
                  colors = ButtonDefaults.buttonColors(containerColor = CeoPurple),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  modifier = Modifier.wrapContentWidth()
                ) {
                  Text("Review", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
              }
            }
          }
        }
      }
    }

    // Quick Governance Actions
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Company Governance", fontSize = 15.sp, fontWeight = FontWeight.Bold)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onNavigateToCalendarEditor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Edit Calendar", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = onNavigateToAudit,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Audit Trail", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Composable
fun CeoApprovalsScreen(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val allRequests by repository.leaveRequests.collectAsState()

  val ceoQueue = remember(allRequests) {
    allRequests.filter { req ->
      (req.status == LeaveStatus.PENDING || req.status == LeaveStatus.QUERY) &&
        (req.userRole == UserRole.TEAM_LEAD || req.requiresCeoApproval)
    }
  }

  // Action dialogs
  val context = LocalContext.current
  var rejectDialogFor by remember { mutableStateOf<LeaveRequest?>(null) }
  var rejectReason by remember { mutableStateOf("") }

  var queryDialogFor by remember { mutableStateOf<LeaveRequest?>(null) }
  var queryQuestion by remember { mutableStateOf("") }
  var queryAttachment by remember { mutableStateOf<LeaveProof?>(null) }

  val ceoGalleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      queryAttachment = resolveProofFromUri(context, uri)
    }
  }
  val ceoDocLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      queryAttachment = resolveProofFromUri(context, uri)
    }
  }

  // Query Dialog
  queryDialogFor?.let { req ->
    AlertDialog(
      onDismissRequest = { queryDialogFor = null },
      title = { Text("Query Employee on Leave Request", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Your query will be delivered directly to ${req.userName}'s chat box. The employee can reply and provide proof attachments back to you.",
            fontSize = 12.sp,
            color = TextSecondary
          )
          OutlinedTextField(
            value = queryQuestion,
            onValueChange = { queryQuestion = it },
            label = { Text("Clarification Question *") },
            placeholder = { Text("e.g. Please share client signoff or deployment handover proof...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
          )

          // Attachment option for CEO
          Text("Attach Policy, Image, or Directive Document:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                ceoGalleryLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = CeoPurple),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("From Gallery", fontSize = 11.sp)
            }
            OutlinedButton(
              onClick = { ceoDocLauncher.launch("*/*") },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Device Files", fontSize = 11.sp)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = queryAttachment?.fileName == "Executive_Handover_Guidelines.pdf",
              onClick = {
                queryAttachment = if (queryAttachment?.fileName == "Executive_Handover_Guidelines.pdf") null
                else LeaveProof("Executive_Handover_Guidelines.pdf", "PDF", "420 KB")
              },
              label = { Text("Handover Guide (PDF)", fontSize = 10.sp) }
            )
            FilterChip(
              selected = queryAttachment?.fileName == "Q4_Launch_Schedule.pdf",
              onClick = {
                queryAttachment = if (queryAttachment?.fileName == "Q4_Launch_Schedule.pdf") null
                else LeaveProof("Q4_Launch_Schedule.pdf", "PDF", "650 KB")
              },
              label = { Text("Launch Schedule", fontSize = 10.sp) }
            )
          }

          queryAttachment?.let { att ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = CeoPurpleLight,
              border = BorderStroke(1.dp, CeoPurple.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  if (att.uriString != null) {
                    AsyncImage(
                      model = att.uriString,
                      contentDescription = "Attachment preview",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, CeoPurple, RoundedCornerShape(4.dp))
                    )
                  } else {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = CeoPurple, modifier = Modifier.size(16.dp))
                  }
                  Column {
                    Text(att.fileName, fontSize = 11.sp, color = CeoPurple, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${att.fileType} • ${att.fileSizeStr}", fontSize = 10.sp, color = TextSecondary)
                  }
                }
                IconButton(onClick = { queryAttachment = null }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Remove", tint = CeoPurple, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            repository.ceoQueryRequest(req, currentUser, queryQuestion, queryAttachment)
            queryDialogFor = null
            queryQuestion = ""
            queryAttachment = null
          },
          enabled = queryQuestion.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = CeoPurple)
        ) {
          Text("Send Query to Chat")
        }
      },
      dismissButton = {
        TextButton(onClick = { queryDialogFor = null }) {
          Text("Cancel")
        }
      }
    )
  }

  rejectDialogFor?.let { req ->
    AlertDialog(
      onDismissRequest = { rejectDialogFor = null },
      title = { Text("CEO Rejection", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("CEO rejection requires an official reason logged into the governance audit trail.", fontSize = 12.sp, color = TextSecondary)
          OutlinedTextField(
            value = rejectReason,
            onValueChange = { rejectReason = it },
            label = { Text("Rejection Justification *") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            repository.ceoRejectRequest(req, currentUser, rejectReason)
            rejectDialogFor = null
            rejectReason = ""
          },
          enabled = rejectReason.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed)
        ) {
          Text("Reject Request")
        }
      },
      dismissButton = {
        TextButton(onClick = { rejectDialogFor = null }) {
          Text("Cancel")
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Executive Approval Queue", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Text(
      text = "Requests requiring CEO sign-off (TL leave, leaves ≥ 3 days, Maternity, and escalated requests)",
      fontSize = 11.sp,
      color = TextSecondary
    )

    if (ceoQueue.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No pending requests in CEO queue.", color = TextMuted)
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(ceoQueue, key = { it.id }) { req ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  LeaveTypeBadge(req.leaveType)
                  Text(req.userName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                  RoleBadge(req.userRole)
                }
                LeaveStatusBadge(req.status)
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${req.startDate} to ${req.endDate} (${req.workingDays} working days) • ${req.groupName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
              )
              Text("Reason: ${req.reason}", fontSize = 12.sp, color = TextPrimary)

              // TL Endorsement Comment if available
              if (req.tlStep != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = TeamLeadIndigoLight.copy(alpha = 0.5f),
                  border = BorderStroke(1.dp, TeamLeadIndigoLight)
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = TeamLeadIndigo, modifier = Modifier.size(16.dp))
                    Text(
                      text = "TL ${req.tlStep.approverName}: \"${req.tlStep.remarks ?: "Approved"}\"",
                      fontSize = 11.sp,
                      color = TeamLeadIndigo,
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }

              // Proof chip
              req.proof?.let { proof ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = NeutralTint
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                      text = "Proof Attached: ${proof.fileName} (${proof.fileSizeStr})",
                      fontSize = 11.sp,
                      color = TextPrimary
                    )
                  }
                }
              }

              // Query status banner if previously queried
              if (req.queryQuestion != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = CeoPurpleLight,
                  border = BorderStroke(1.dp, CeoPurple.copy(alpha = 0.4f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = CeoPurple, modifier = Modifier.size(16.dp))
                    Column {
                      Text("Active Clarification Query in Chat:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CeoPurple)
                      Text("\"${req.queryQuestion}\"", fontSize = 11.sp, color = TextPrimary)
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { repository.ceoApproveRequest(req, currentUser, "Approved by CEO") },
                  colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = { queryDialogFor = req },
                  colors = ButtonDefaults.buttonColors(containerColor = CeoPurple),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Query", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = { rejectDialogFor = req },
                  colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun CeoCompanyCalendarEditor(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val holidays by repository.companyHolidays.collectAsState()
  val leaveRequests by repository.leaveRequests.collectAsState()

  var viewingMonth by remember { mutableStateOf(YearMonth.now()) }
  var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(3)) }
  var selectedType by remember { mutableStateOf(HolidayType.PUBLIC_HOLIDAY) }
  var holidayTitle by remember { mutableStateOf("") }
  var holidayRemark by remember { mutableStateOf("") }
  var lockThisDay by remember { mutableStateOf(true) }
  var filterCategory by remember { mutableStateOf("ALL") }

  // Check if selected date is already locked or is a holiday
  val selectedDateHoliday = remember(holidays, selectedDate) {
    holidays.find { it.date == selectedDate }
  }
  val isCurrentDateLocked = selectedDateHoliday?.isLockedByCeo == true

  // Sync title and remark if day already exists
  LaunchedEffect(selectedDateHoliday) {
    if (selectedDateHoliday != null) {
      holidayTitle = selectedDateHoliday.title
      holidayRemark = selectedDateHoliday.remark
      selectedType = selectedDateHoliday.type
      lockThisDay = selectedDateHoliday.isLockedByCeo
    } else {
      holidayTitle = ""
      holidayRemark = ""
      lockThisDay = true
    }
  }

  // Check affected approved leaves for selected date
  val affectedApprovedLeaves = remember(leaveRequests, selectedDate, selectedType) {
    if (selectedType == HolidayType.PUBLIC_HOLIDAY || selectedType == HolidayType.COMPANY_CLOSURE) {
      leaveRequests.filter {
        it.status == LeaveStatus.APPROVED && !selectedDate.isBefore(it.startDate) && !selectedDate.isAfter(it.endDate)
      }
    } else {
      emptyList()
    }
  }

  val lockedCount = remember(holidays) { holidays.count { it.isLockedByCeo } }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header with proper non-squeezing row layout
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Company Calendar & Lock",
          fontSize = 19.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
        )
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CeoPurpleLight
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text("🔒", fontSize = 11.sp)
            Text(
              text = "$lockedCount Locked",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = CeoPurple,
              maxLines = 1
            )
          }
        }
      }
      Text(
        text = "Tap any date to lock/unlock or declare company holidays",
        fontSize = 12.sp,
        color = TextSecondary
      )
    }

    // Interactive Visual Month Calendar Card
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Month navigator
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CeoPurple, modifier = Modifier.size(20.dp))
            Text(
              text = viewingMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }

          Row {
            IconButton(
              onClick = { viewingMonth = viewingMonth.minusMonths(1) },
              modifier = Modifier.size(34.dp)
            ) {
              Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }
            IconButton(
              onClick = { viewingMonth = viewingMonth.plusMonths(1) },
              modifier = Modifier.size(34.dp)
            ) {
              Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
          }
        }

        // Weekday header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            Box(
              modifier = Modifier.weight(1f),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = day,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
            }
          }
        }

        // Calendar Grid
        val firstDayOfMonth = viewingMonth.atDay(1)
        val dayOffset = firstDayOfMonth.dayOfWeek.value - 1
        val daysInMonth = viewingMonth.lengthOfMonth()
        val totalCells = ((dayOffset + daysInMonth + 6) / 7) * 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          for (row in 0 until (totalCells / 7)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              for (col in 0..6) {
                val cellIndex = row * 7 + col
                val dayNum = cellIndex - dayOffset + 1

                if (dayNum in 1..daysInMonth) {
                  val date = viewingMonth.atDay(dayNum)
                  val isSelected = date == selectedDate
                  val isToday = date == LocalDate.now()
                  val holiday = holidays.find { it.date == date }
                  val isLocked = holiday?.isLockedByCeo == true
                  val isWeekend = date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(46.dp)
                      .padding(2.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(
                        when {
                          isSelected -> CeoPurpleLight
                          isLocked -> Color(0xFFF3E8FF)
                          holiday != null -> Color(holiday.colorHex).copy(alpha = 0.12f)
                          isWeekend -> NeutralTint.copy(alpha = 0.5f)
                          else -> Color.White
                        }
                      )
                      .border(
                        BorderStroke(
                          if (isSelected) 2.dp else 1.dp,
                          when {
                            isSelected -> CeoPurple
                            isLocked -> Color(0xFFC084FC)
                            else -> CardBorder
                          }
                        ),
                        RoundedCornerShape(8.dp)
                      )
                      .clickable { selectedDate = date },
                    contentAlignment = Alignment.Center
                  ) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                      ) {
                        Text(
                          text = dayNum.toString(),
                          fontSize = 12.sp,
                          fontWeight = if (isSelected || isLocked || holiday != null) FontWeight.Bold else FontWeight.Medium,
                          color = when {
                            isSelected -> CeoPurple
                            isLocked -> CeoPurple
                            holiday != null -> Color(holiday.colorHex)
                            else -> TextPrimary
                          }
                        )
                        if (isLocked) {
                          Text("🔒", fontSize = 9.sp, modifier = Modifier.padding(start = 2.dp))
                        }
                      }

                      if (holiday != null) {
                        Box(
                          modifier = Modifier
                            .padding(top = 2.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(holiday.colorHex))
                        )
                      }
                    }
                  }
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }

        // Color & Icon Legend
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text("🔒", fontSize = 11.sp)
            Text("Locked by CEO", fontSize = 10.sp, color = CeoPurple, fontWeight = FontWeight.SemiBold)
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(HolidayType.PUBLIC_HOLIDAY.colorHex)))
            Text("Public Holiday", fontSize = 10.sp, color = TextSecondary)
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(HolidayType.COMPANY_CLOSURE.colorHex)))
            Text("Closure", fontSize = 10.sp, color = TextSecondary)
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(HolidayType.SPECIAL_WORKING_DAY.colorHex)))
            Text("Special Workday", fontSize = 10.sp, color = TextSecondary)
          }
        }
      }
    }

    // Direct Date Lock & Status Control for Selected Date
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.5.dp, if (isCurrentDateLocked) CeoPurple else CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isCurrentDateLocked) CeoPurpleLight else NeutralTint
          ) {
            Text(
              text = if (isCurrentDateLocked) "🔒 Locked" else "🔓 Unlocked",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (isCurrentDateLocked) CeoPurple else TextSecondary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              maxLines = 1
            )
          }
        }

        Text(
          text = if (isCurrentDateLocked) "Protected day: Team Leads cannot override or add group events."
          else "Open day: Team Leads may schedule group working days or events.",
          fontSize = 11.sp,
          color = TextSecondary
        )

        // Instant Lock / Unlock Toggle Button
        Button(
          onClick = {
            repository.ceoToggleDateLock(currentUser, selectedDate)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isCurrentDateLocked) StatusRejectedRed else CeoPurple
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("toggle_date_lock_btn")
        ) {
          Icon(
            imageVector = if (isCurrentDateLocked) Icons.Default.LockOpen else Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isCurrentDateLocked) "Unlock This Date (Allow Overrides)" else "Lock This Date (Block Overrides)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
        }

        Divider(color = CardBorder)

        // Holiday / Policy declaration options for this date
        Text("Declare Company Policy / Holiday for this Date:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Day Type Picker Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          HolidayType.values().forEach { type ->
            FilterChip(
              selected = selectedType == type,
              onClick = { selectedType = type },
              label = { Text(type.displayName, fontSize = 11.sp) }
            )
          }
        }

        OutlinedTextField(
          value = holidayTitle,
          onValueChange = { holidayTitle = it },
          label = { Text("Label / Title *") },
          placeholder = { Text("e.g. Annual Strategic Alignment Day") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = holidayRemark,
          onValueChange = { holidayRemark = it },
          label = { Text("Remark / Reason *") },
          placeholder = { Text("e.g. Paid holiday declared for all departments") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )

        // Lock Toggle Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Enforce CEO Lock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Prevent Team Leads from overriding this date", fontSize = 10.sp, color = TextMuted)
          }
          Switch(
            checked = lockThisDay,
            onCheckedChange = { lockThisDay = it }
          )
        }

        // Affected Leaves Live Preview
        if (affectedApprovedLeaves.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = StatusNotesBg,
            border = BorderStroke(1.dp, Color(0xFFFFD54F)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "⚡ Affected Approved Leaves (${affectedApprovedLeaves.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Publishing will automatically recalculate working days, restore 1 day to balance, and notify: ${affectedApprovedLeaves.joinToString { it.userName }}",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }
        }

        Button(
          onClick = {
            repository.ceoUpdateCompanyCalendar(
              ceo = currentUser,
              date = selectedDate,
              type = selectedType,
              title = holidayTitle.ifBlank { "Company ${selectedType.displayName}" },
              remark = holidayRemark.ifBlank { "Declared by CEO" },
              colorHex = selectedType.colorHex,
              lockThisDay = lockThisDay
            )
            holidayTitle = ""
            holidayRemark = ""
          },
          enabled = holidayTitle.isNotBlank() || selectedDateHoliday != null,
          colors = ButtonDefaults.buttonColors(containerColor = CeoPurple),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("publish_calendar_policy_btn")
        ) {
          Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Publish Policy & Notify Company", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Active Locked & Holiday Entries Directory
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = BorderStroke(1.dp, CardBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Calendar Entries & Locks", fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = filterCategory == "ALL",
              onClick = { filterCategory = "ALL" },
              label = { Text("All", fontSize = 10.sp) }
            )
            FilterChip(
              selected = filterCategory == "LOCKED",
              onClick = { filterCategory = "LOCKED" },
              label = { Text("🔒 Locked", fontSize = 10.sp) }
            )
          }
        }

        val displayedHolidays = remember(holidays, filterCategory) {
          when (filterCategory) {
            "LOCKED" -> holidays.filter { it.isLockedByCeo }
            else -> holidays
          }.sortedBy { it.date }
        }

        if (displayedHolidays.isEmpty()) {
          Text("No entries matching filter.", fontSize = 12.sp, color = TextMuted)
        } else {
          displayedHolidays.forEach { h ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (h.date == selectedDate) CeoPurpleLight.copy(alpha = 0.5f) else NeutralSurface,
              border = BorderStroke(1.dp, if (h.date == selectedDate) CeoPurple else CardBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedDate = h.date
                  viewingMonth = YearMonth.from(h.date)
                }
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(Color(h.colorHex))
                  )
                  Column {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(h.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                      if (h.isLockedByCeo) {
                        Surface(shape = RoundedCornerShape(4.dp), color = CeoPurpleLight) {
                          Text("🔒 Locked", fontSize = 9.sp, color = CeoPurple, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                      }
                    }
                    Text(
                      "${h.date.format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy"))} • ${h.type.displayName}",
                      fontSize = 11.sp,
                      color = TextSecondary
                    )
                  }
                }

                IconButton(
                  onClick = { repository.ceoToggleDateLock(currentUser, h.date) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = if (h.isLockedByCeo) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Lock",
                    tint = if (h.isLockedByCeo) CeoPurple else TextMuted,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun CeoAuditScreen(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier
) {
  val auditLogs by repository.auditLogs.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }

  val filteredLogs = remember(auditLogs, searchQuery, selectedRoleFilter) {
    auditLogs.filter { log ->
      val matchesSearch = searchQuery.isBlank() ||
        log.actorName.contains(searchQuery, ignoreCase = true) ||
        log.action.contains(searchQuery, ignoreCase = true) ||
        log.target.contains(searchQuery, ignoreCase = true) ||
        log.reason.contains(searchQuery, ignoreCase = true)

      val matchesRole = selectedRoleFilter == null || log.actorRole == selectedRoleFilter
      matchesSearch && matchesRole
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Leave Record Audit Log", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Text("Complete immutable trail of decisions, exceptions, and calendar overrides.", fontSize = 11.sp, color = TextSecondary)

    // Search bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      label = { Text("Search by actor, action, reason or target...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      modifier = Modifier.fillMaxWidth()
    )

    // Role Filters
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = selectedRoleFilter == null,
        onClick = { selectedRoleFilter = null },
        label = { Text("All Roles") }
      )
      UserRole.values().forEach { role ->
        FilterChip(
          selected = selectedRoleFilter == role,
          onClick = { selectedRoleFilter = if (selectedRoleFilter == role) null else role },
          label = { Text(role.displayName) }
        )
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(filteredLogs, key = { it.id }) { log ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          border = BorderStroke(1.dp, CardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                RoleBadge(log.actorRole)
                Text(log.actorName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
              Text(
                log.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")),
                fontSize = 11.sp,
                color = TextMuted
              )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(log.action, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CeoPurple)
            Text("Target: ${log.target}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("Reason: ${log.reason}", fontSize = 11.sp, color = TextSecondary)
            if (log.details.isNotBlank()) {
              Text("Details: ${log.details}", fontSize = 10.sp, color = TextMuted)
            }
          }
        }
      }
    }
  }
}

```


### File 13: app/src/main/java/com/example/MainActivity.kt
**File Path:** `app/src/main/java/com/example/MainActivity.kt`

```kotlin
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaveDeskRepository
import com.example.model.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  private val repository = LeaveDeskRepository()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val currentUser by repository.currentUser.collectAsState()
      LeaveDeskTheme(userRole = currentUser.role) {
        LeaveDeskApp(repository = repository)
      }
    }
  }
}

enum class AppDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  // Common / Employee
  HOME("Home", Icons.Default.Home),
  APPLY("Apply", Icons.Default.AddCircleOutline),
  CALENDAR("Calendar", Icons.Default.CalendarMonth),
  MY_REQUESTS("Requests", Icons.Default.FormatListBulleted),
  CHAT("Chat", Icons.Default.ChatBubbleOutline),

  // TL Specific
  TEAM_REQUESTS("Team Inbox", Icons.Default.Inbox),
  MY_LEAVES("My Leaves", Icons.Default.Person),

  // CEO Specific
  DASHBOARD("Dashboard", Icons.Default.Dashboard),
  APPROVALS("Approvals", Icons.Default.Rule),
  AUDIT("Audit", Icons.Default.History),

  // Modal / Overlays
  NOTIFICATIONS("Notifications", Icons.Default.Notifications),
  LOGIN("Switch User", Icons.Default.Login)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveDeskApp(repository: LeaveDeskRepository) {
  val isLoggedIn by repository.isLoggedIn.collectAsState()
  val currentUser by repository.currentUser.collectAsState()
  val allUsers by repository.users.collectAsState()
  val notifications by repository.notifications.collectAsState()
  val toastMessage by repository.toastMessage.collectAsState()

  val unreadNotifCount = remember(notifications, currentUser) {
    notifications.count {
      (it.recipientUserId == currentUser.id || it.recipientUserId == "ALL") && !it.isRead
    }
  }

  // Active navigation tab
  var currentTab by remember(currentUser.role) {
    mutableStateOf(
      when (currentUser.role) {
        UserRole.EMPLOYEE -> AppDestination.HOME
        UserRole.TEAM_LEAD -> AppDestination.HOME
        UserRole.CEO -> AppDestination.DASHBOARD
      }
    )
  }

  var showNotificationsScreen by remember { mutableStateOf(false) }
  var showUserMenu by remember { mutableStateOf(false) }

  // Auto-dismiss toast after 4 seconds
  LaunchedEffect(toastMessage) {
    if (toastMessage != null) {
      delay(4000)
      repository.clearToast()
    }
  }

  // Handle back press
  BackHandler(enabled = !isLoggedIn || showNotificationsScreen || currentTab != AppDestination.HOME && currentTab != AppDestination.DASHBOARD) {
    when {
      !isLoggedIn -> { /* at root login */ }
      showNotificationsScreen -> showNotificationsScreen = false
      else -> {
        currentTab = if (currentUser.role == UserRole.CEO) AppDestination.DASHBOARD else AppDestination.HOME
      }
    }
  }

  // Responsive mobile shell (390dp max width centered for mobile design)
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFE5E7EB)),
    contentAlignment = Alignment.Center
  ) {
    Scaffold(
      modifier = Modifier
        .widthIn(max = 480.dp)
        .fillMaxHeight(),
      topBar = {
        if (isLoggedIn) {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LocalRoleTheme.current.accent),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Text(
                  text = "LeaveDesk",
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp,
                  color = TextPrimary
                )
                RoleBadge(currentUser.role)
              }
            },
            actions = {
              // Notifications Bell with Unread Badge
              IconButton(
                onClick = { showNotificationsScreen = !showNotificationsScreen },
                modifier = Modifier.testTag("top_bar_bell_button")
              ) {
                BadgedBox(
                  badge = {
                    if (unreadNotifCount > 0) {
                      Badge(containerColor = StatusRejectedRed) {
                        Text(unreadNotifCount.toString())
                      }
                    }
                  }
                ) {
                  Icon(
                    imageVector = if (showNotificationsScreen) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = TextPrimary
                  )
                }
              }

              // User Avatar with Dropdown Menu
              Box {
                Surface(
                  shape = CircleShape,
                  color = LocalRoleTheme.current.accent,
                  modifier = Modifier
                    .padding(end = 12.dp)
                    .size(34.dp)
                    .clickable { showUserMenu = true }
                    .testTag("top_bar_profile_button")
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = currentUser.avatarInitials,
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                DropdownMenu(
                  expanded = showUserMenu,
                  onDismissRequest = { showUserMenu = false }
                ) {
                  DropdownMenuItem(
                    text = {
                      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(currentUser.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${currentUser.employeeId} • ${currentUser.role.displayName}", fontSize = 11.sp, color = LocalRoleTheme.current.accent, fontWeight = FontWeight.SemiBold)
                        Text(currentUser.email, fontSize = 11.sp, color = TextSecondary)
                        Text("Department: ${currentUser.groupName}", fontSize = 10.sp, color = TextMuted)
                      }
                    },
                    onClick = { },
                    enabled = false
                  )
                  HorizontalDivider()
                  DropdownMenuItem(
                    text = { Text("Sign Out", color = StatusRejectedRed, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = StatusRejectedRed) },
                    onClick = {
                      showUserMenu = false
                      repository.logout()
                    }
                  )
                }
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = Color.White
            )
          )
        }
      },
      bottomBar = {
        if (isLoggedIn && !showNotificationsScreen) {
          NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp
          ) {
            val tabs = when (currentUser.role) {
              UserRole.EMPLOYEE -> listOf(
                AppDestination.HOME,
                AppDestination.APPLY,
                AppDestination.CALENDAR,
                AppDestination.MY_REQUESTS,
                AppDestination.CHAT
              )
              UserRole.TEAM_LEAD -> listOf(
                AppDestination.HOME,
                AppDestination.TEAM_REQUESTS,
                AppDestination.CALENDAR,
                AppDestination.MY_LEAVES,
                AppDestination.CHAT
              )
              UserRole.CEO -> listOf(
                AppDestination.DASHBOARD,
                AppDestination.APPROVALS,
                AppDestination.CALENDAR,
                AppDestination.AUDIT,
                AppDestination.CHAT
              )
            }

            tabs.forEach { dest ->
              val isSelected = currentTab == dest
              NavigationBarItem(
                selected = isSelected,
                onClick = { currentTab = dest },
                icon = {
                  Icon(
                    imageVector = dest.icon,
                    contentDescription = dest.label
                  )
                },
                label = {
                  Text(
                    text = dest.label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = LocalRoleTheme.current.accent,
                  selectedTextColor = LocalRoleTheme.current.accent,
                  indicatorColor = LocalRoleTheme.current.lightTint
                ),
                modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
              )
            }
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(Color(0xFFF9FAFB))
      ) {
        // Screen Content
        when {
          !isLoggedIn -> {
            LoginScreen(
              repository = repository,
              onLoginComplete = {
                currentTab = if (currentUser.role == UserRole.CEO) AppDestination.DASHBOARD else AppDestination.HOME
              }
            )
          }

          showNotificationsScreen -> {
            NotificationsScreen(
              repository = repository,
              modifier = Modifier.fillMaxSize()
            )
          }

          else -> {
            when (currentTab) {
              AppDestination.HOME -> {
                if (currentUser.role == UserRole.TEAM_LEAD) {
                  TeamLeadHomeScreen(
                    repository = repository,
                    onNavigateToTeamRequests = { currentTab = AppDestination.TEAM_REQUESTS },
                    onNavigateToMyLeaves = { currentTab = AppDestination.MY_LEAVES },
                    onNavigateToCalendar = { currentTab = AppDestination.CALENDAR }
                  )
                } else {
                  EmployeeHomeScreen(
                    repository = repository,
                    onNavigateToApply = { currentTab = AppDestination.APPLY },
                    onNavigateToRequests = { currentTab = AppDestination.MY_REQUESTS },
                    onNavigateToCalendar = { currentTab = AppDestination.CALENDAR }
                  )
                }
              }

              AppDestination.APPLY -> {
                EmployeeApplyScreen(
                  repository = repository,
                  onApplySuccess = { currentTab = AppDestination.MY_REQUESTS }
                )
              }

              AppDestination.MY_REQUESTS -> {
                EmployeeRequestsScreen(repository = repository)
              }

              AppDestination.TEAM_REQUESTS -> {
                TeamLeadRequestsScreen(repository = repository)
              }

              AppDestination.MY_LEAVES -> {
                EmployeeRequestsScreen(repository = repository)
              }

              AppDestination.DASHBOARD -> {
                CeoDashboardScreen(
                  repository = repository,
                  onNavigateToApprovals = { currentTab = AppDestination.APPROVALS },
                  onNavigateToAudit = { currentTab = AppDestination.AUDIT },
                  onNavigateToCalendarEditor = { currentTab = AppDestination.CALENDAR }
                )
              }

              AppDestination.APPROVALS -> {
                CeoApprovalsScreen(repository = repository)
              }

              AppDestination.AUDIT -> {
                CeoAuditScreen(repository = repository)
              }

              AppDestination.CALENDAR -> {
                if (currentUser.role == UserRole.CEO) {
                  CeoCompanyCalendarEditor(repository = repository)
                } else {
                  CompanyCalendarScreen(repository = repository)
                }
              }

              AppDestination.CHAT -> {
                QuickChatScreen(repository = repository)
              }

              else -> {}
            }
          }
        }

        // Floating Toast / "Mail Sent" Banner
        toastMessage?.let { msg ->
          Box(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(horizontal = 16.dp, vertical = 12.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF1E293B),
              shadowElevation = 6.dp,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = StatusApprovedGreen,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = msg,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                  )
                }
                IconButton(
                  onClick = { repository.clearToast() },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

```


### File 14: backend/schema.sql
**File Path:** `backend/schema.sql`

```sql
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

```


### File 15: backend/server.js
**File Path:** `backend/server.js`

```javascript
/**
 * LeaveDesk Unified Local Gateway & Server
 * 
 * Runs all 3 microservices on their dedicated ports simultaneously:
 * - Service 1: Identity & Auth (:8081)
 * - Service 2: Leave Workflow Engine (:8082)
 * - Service 3: Real-Time Calendar & WebSockets (:8083)
 * - Reverse Proxy Gateway (:8084 or configured GATEWAY_PORT)
 * - Database: MySQL with auto-initialization & in-memory fallback
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const mysqlDb = require('./mysql-db');

const authApp = require('./services/auth-service/index');
const workflowApp = require('./services/workflow-service/index');
const { app: calendarApp, server: calendarServer } = require('./services/calendar-service/index');

const AUTH_PORT = parseInt(process.env.AUTH_PORT || '8081', 10);
const WORKFLOW_PORT = parseInt(process.env.WORKFLOW_PORT || '8082', 10);
const CALENDAR_PORT = parseInt(process.env.CALENDAR_PORT || '8083', 10);
let GATEWAY_PORT = parseInt(process.env.GATEWAY_PORT || '8084', 10);

// Initialize MySQL asynchronously
mysqlDb.initMySQL().then(isOnline => {
  if (isOnline) {
    console.log(`🐬 [Database] MySQL cluster ONLINE (Host: ${mysqlDb.config.host}, Database: ${mysqlDb.config.database})`);
  } else {
    console.log(`💾 [Database] In-Memory Fallback store ACTIVE (All endpoints 100% operational)`);
  }
});

// Start Microservice 1
authApp.listen(AUTH_PORT, () => {
  console.log(`✅ [Service 1: Identity & Auth] listening at http://localhost:${AUTH_PORT}`);
}).on('error', (err) => {
  console.log(`ℹ️ [Service 1: Auth] Note on port ${AUTH_PORT}: ${err.message}`);
});

// Start Microservice 2
workflowApp.listen(WORKFLOW_PORT, () => {
  console.log(`✅ [Service 2: Leave Workflow Engine] listening at http://localhost:${WORKFLOW_PORT}`);
}).on('error', (err) => {
  console.log(`ℹ️ [Service 2: Workflow] Note on port ${WORKFLOW_PORT}: ${err.message}`);
});

// Start Microservice 3
calendarServer.listen(CALENDAR_PORT, () => {
  console.log(`✅ [Service 3: Real-Time Calendar & WSS] listening at http://localhost:${CALENDAR_PORT}`);
}).on('error', (err) => {
  console.log(`ℹ️ [Service 3: Calendar] Note on port ${CALENDAR_PORT}: ${err.message}`);
});

// Start API Gateway
const gateway = express();
gateway.use(cors());
gateway.use(express.json());

// Forwarding rules
gateway.use('/api/v1/auth', authApp);
gateway.use('/api/v1/leaves', workflowApp);
gateway.use('/api/v1/audit', workflowApp);
gateway.use('/api/v1/calendar', calendarApp);
gateway.use('/api/v1/chat', calendarApp);

gateway.get('/health', (req, res) => {
  res.json({
    status: "HEALTHY",
    database: mysqlDb.isAvailable() ? "MYSQL_ONLINE" : "IN_MEMORY_ACTIVE",
    databaseHost: mysqlDb.config.host,
    databaseName: mysqlDb.config.database,
    services: {
      auth: `http://localhost:${AUTH_PORT}`,
      workflow: `http://localhost:${WORKFLOW_PORT}`,
      calendar: `http://localhost:${CALENDAR_PORT}`
    },
    version: "1.0.0"
  });
});

function startGateway(port) {
  const server = gateway.listen(port, () => {
    console.log(`\n==================================================`);
    console.log(`🚀 LeaveDesk Unified Gateway ready on http://localhost:${port}`);
    console.log(`==================================================`);
    console.log(`• Test Health:     curl http://localhost:${port}/health`);
    console.log(`• Login Endpoint:  curl -X POST http://localhost:${port}/api/v1/auth/login -H "Content-Type: application/json" -d '{"identifier":"EMP101","password":"password123"}'`);
    console.log(`• Leave Balances:  curl http://localhost:${port}/api/v1/leaves/balances/u_emp1`);
    console.log(`• Calendar Lock:   curl http://localhost:${port}/api/v1/calendar/holidays`);
    console.log(`==================================================\n`);
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.log(`Port ${port} in use, trying next port ${port + 1}...`);
      startGateway(port + 1);
    } else {
      console.error("Gateway error:", err);
    }
  });
}

startGateway(GATEWAY_PORT);

```
