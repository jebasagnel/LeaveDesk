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

enum class BackendStatus(val label: String) {
  ONLINE("Connected (Live Backend)"),
  CONNECTING("Connecting..."),
  OFFLINE_CACHE("Local Storage Active (Offline Safe)")
}

data class BackendConnectionState(
  val status: BackendStatus = BackendStatus.ONLINE,
  val serverUrl: String = "http://10.0.2.2:8084/",
  val databaseEngine: String = "MySQL 8.0 (InnoDB) / Live Microservices",
  val lastSyncTime: String? = null,
  val message: String = "Live link online"
)

data class ChatThreadInfo(
  val threadId: String,
  val title: String,
  val subtitle: String,
  val role: UserRole?,
  val targetUserId: String? = null,
  val relatedRequestId: String? = null,
  val isLocked: Boolean = false,
  val lockReason: String? = null,
  val groupId: String? = null,
  val groupName: String? = null,
  val employeeId: String? = null,
  val designation: String? = null,
  val avatarInitials: String? = null
)
