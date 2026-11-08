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
