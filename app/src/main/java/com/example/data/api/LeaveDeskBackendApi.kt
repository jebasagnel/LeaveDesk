package com.example.data.api

import com.example.model.*
import java.time.LocalDate

/**
 * LeaveDesk Production Backend Contract Architecture
 *
 * In a production / Calide desktop deployment, the application communicates
 * across 3 distinct backend microservices:
 *
 * 1. IDENTITY & AUTHENTICATION SERVICE (Port :8081)
 *    - Validates employee corporate credentials (EMP101, TL201, CEO001).
 *    - Issues signed JWT access/refresh tokens with role claims (EMPLOYEE, TEAM_LEAD, CEO).
 *    - Manages corporate organization structure (users, teams, departments, reporting lines).
 *
 * 2. LEAVE WORKFLOW & APPROVAL ENGINE (Port :8082)
 *    - Executes business logic rules (balance deductions, max consecutive days).
 *    - Evaluates team minimum-presence thresholds against concurrent team requests.
 *    - State machine routing: Short leave -> TL approval; Long/frequent leave -> TL then CEO.
 *    - Implements emergency overrides, balance restorations, queries, and immutable audit logs.
 *
 * 3. REAL-TIME CALENDAR & NOTIFICATION DISPATCHER (Port :8083)
 *    - WebSocket/SSE pub-sub hub for live notifications and instant chat.
 *    - Synchronizes company holidays, group overrides, and CEO date locks.
 *    - Dispatches SMTP corporate emails ("mail sent" simulator or real SMTP server) and push alerts.
 */

// MARK: - 1. Identity & Auth Service Contracts
data class LoginRequest(
  val identifier: String, // Employee ID (EMP101) or Email
  val password: String
)

data class AuthSessionResponse(
  val token: String,
  val refreshToken: String,
  val user: User
)

interface LeaveDeskAuthApi {
  suspend fun login(request: LoginRequest): Result<AuthSessionResponse>
  suspend fun refreshToken(token: String): Result<AuthSessionResponse>
  suspend fun fetchCurrentUser(): Result<User>
  suspend fun listTeamMembers(groupId: String): Result<List<User>>
  suspend fun logout(): Result<Unit>
}

// MARK: - 2. Leave Workflow & Approval Engine Contracts
data class ApplyLeaveRequest(
  val userId: String,
  val leaveType: LeaveType,
  val startDate: String, // ISO-8601 YYYY-MM-DD
  val endDate: String,
  val isHalfDay: Boolean,
  val reason: String,
  val backupUserId: String?,
  val backupTaskNote: String?,
  val proofFileName: String?,
  val proofFileSize: Long?
)

data class ApprovalActionRequest(
  val approverUserId: String,
  val decision: LeaveStatus, // APPROVED or REJECTED
  val comment: String?,
  val suggestedDates: String? = null
)

data class QueryActionRequest(
  val approverUserId: String,
  val queryQuestion: String,
  val suggestedStartDate: String?,
  val suggestedEndDate: String?
)

data class QueryReplyRequest(
  val employeeUserId: String,
  val replyText: String,
  val updatedStartDate: String?,
  val updatedEndDate: String?
)

data class EmergencyCancelRequest(
  val actorUserId: String,
  val reason: String
)

interface LeaveDeskWorkflowApi {
  suspend fun getLeaveQuotas(userId: String): Result<List<LeaveQuota>>
  suspend fun getMyRequests(userId: String): Result<List<LeaveRequest>>
  suspend fun submitLeave(request: ApplyLeaveRequest): Result<LeaveRequest>
  suspend fun withdrawLeave(requestId: String, userId: String): Result<LeaveRequest>
  suspend fun requestCancellation(requestId: String, userId: String, reason: String): Result<LeaveRequest>
  
  // Approver endpoints
  suspend fun getTeamInbox(tlUserId: String): Result<List<LeaveRequest>>
  suspend fun getCeoInbox(): Result<List<LeaveRequest>>
  suspend fun processDecision(requestId: String, action: ApprovalActionRequest): Result<LeaveRequest>
  suspend fun submitQuery(requestId: String, action: QueryActionRequest): Result<LeaveRequest>
  suspend fun replyToQuery(requestId: String, action: QueryReplyRequest): Result<LeaveRequest>
  suspend fun emergencyCancel(requestId: String, action: EmergencyCancelRequest): Result<LeaveRequest>
  suspend fun getAuditLogs(): Result<List<AuditLogEntry>>
}

// MARK: - 3. Calendar & Real-Time Notification Dispatcher Contracts
data class DateLockRequest(
  val date: String,
  val reason: String,
  val isLocked: Boolean
)

data class SendChatMessageRequest(
  val threadId: String,
  val senderId: String,
  val message: String,
  val relatedRequestId: String? = null
)

interface LeaveDeskCalendarNotificationApi {
  suspend fun getHolidays(year: Int): Result<List<CompanyHoliday>>
  suspend fun getGroupCalendarOverrides(groupId: String): Result<List<GroupCalendarEvent>>
  suspend fun toggleCeoDateLock(request: DateLockRequest): Result<CompanyHoliday>
  suspend fun getNotifications(userId: String): Result<List<AppNotification>>
  suspend fun markNotificationRead(notificationId: String): Result<Unit>
  suspend fun getChatMessages(threadId: String): Result<List<ChatMessage>>
  suspend fun sendChatMessage(request: SendChatMessageRequest): Result<ChatMessage>
}
