package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickChatScreen(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier,
  initialThreadId: String? = null
) {
  val currentUser by repository.currentUser.collectAsState()
  val allMessages by repository.chatMessages.collectAsState()
  val allUsers by repository.users.collectAsState()
  val allGroups by repository.groups.collectAsState()
  val allRequests by repository.leaveRequests.collectAsState()

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

  // Intelligent Slide Filter & Directory States
  var showDirectoryFilter by remember { mutableStateOf(false) }
  var directorySearchQuery by remember { mutableStateOf("") }
  var expandedGroupIds by remember { mutableStateOf(setOf("g_eng", "g_ops", "g_exec")) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  fun selectDirectoryUser(targetUser: User) {
    val match = availableThreads.find { it.targetUserId == targetUser.id }
    if (match != null) {
      selectedThreadId = match.threadId
    } else {
      val computedId = when (currentUser.role) {
        UserRole.CEO -> {
          if (targetUser.role == UserRole.TEAM_LEAD) "thread_tl_${targetUser.id}_ceo"
          else "thread_emp_${targetUser.id}_ceo"
        }
        UserRole.TEAM_LEAD -> "thread_emp_${targetUser.id}_tl"
        UserRole.EMPLOYEE -> {
          if (targetUser.role == UserRole.CEO) "thread_emp_${currentUser.id}_ceo"
          else "thread_emp_${currentUser.id}_tl"
        }
      }
      selectedThreadId = computedId
    }
    showDirectoryFilter = false
    directorySearchQuery = ""
  }

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
      Column {
        Text("Corporate Chat & Queries", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Live approvals, query proofs & team directory", fontSize = 11.sp, color = TextSecondary)
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // Corner Intelligent Slide Filter Button (CEO and TL only - Employees do not need a filter)
        if (currentUser.role != UserRole.EMPLOYEE) {
          FilledTonalButton(
            onClick = { showDirectoryFilter = true },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = LocalRoleTheme.current.accent,
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("chat_slide_filter_corner_btn")
          ) {
            Icon(Icons.Default.Tune, contentDescription = "Slide Filter", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (currentUser.role == UserRole.TEAM_LEAD) "Team Filter" else "Directory", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Thread selector chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // First Chip: Quick Search & Directory Trigger (CEO and TL only)
      if (currentUser.role != UserRole.EMPLOYEE) {
        FilterChip(
          selected = showDirectoryFilter,
          onClick = { showDirectoryFilter = true },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search Directory", modifier = Modifier.size(14.dp), tint = LocalRoleTheme.current.accent)
          },
          label = {
            Text(
              if (currentUser.role == UserRole.TEAM_LEAD) "Filter Team ▾" else "Search & Groups ▾",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = LocalRoleTheme.current.accent
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            containerColor = LocalRoleTheme.current.lightTint
          ),
          modifier = Modifier.testTag("chat_search_groups_chip")
        )
      }

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
          val metaInfo = listOfNotNull(
            activeThread.groupName?.takeIf { it.isNotBlank() },
            activeThread.designation?.takeIf { it.isNotBlank() }
          ).joinToString(" • ")
          if (metaInfo.isNotBlank()) {
            Text(metaInfo, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = LocalRoleTheme.current.accent)
          }
          Text(activeThread.subtitle, fontSize = 11.sp, color = TextSecondary)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
          if (currentUser.role != UserRole.EMPLOYEE) {
            OutlinedButton(
              onClick = { showDirectoryFilter = true },
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(2.dp))
              Text("Switch", fontSize = 10.sp)
            }
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

  // Intelligent Slide Filter & Directory Hierarchy Modal
  if (showDirectoryFilter) {
    ModalBottomSheet(
      onDismissRequest = {
        showDirectoryFilter = false
        directorySearchQuery = ""
      },
      sheetState = sheetState,
      containerColor = Color(0xFFF8FAFC),
      dragHandle = { BottomSheetDefaults.DragHandle() },
      modifier = Modifier.testTag("chat_slide_filter_modal")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 6.dp)
          .heightIn(max = 620.dp)
      ) {
        // Title & Close Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LocalRoleTheme.current.lightTint),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.AccountTree, contentDescription = null, tint = LocalRoleTheme.current.accent, modifier = Modifier.size(20.dp))
            }
            Column {
              Text("Intelligent Directory & Hierarchy", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              Text("Slide filter • Live search or group hierarchy", fontSize = 11.sp, color = TextSecondary)
            }
          }
          IconButton(onClick = {
            showDirectoryFilter = false
            directorySearchQuery = ""
          }) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(Modifier.height(10.dp))

        // Search Input
        OutlinedTextField(
          value = directorySearchQuery,
          onValueChange = { directorySearchQuery = it },
          placeholder = { Text("Search by name, ID, title, or character (e.g. 'A', 'David', 'Eng')...", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LocalRoleTheme.current.accent) },
          trailingIcon = {
            if (directorySearchQuery.isNotEmpty()) {
              IconButton(onClick = { directorySearchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = LocalRoleTheme.current.accent,
            unfocusedBorderColor = CardBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("directory_search_input")
        )

        Spacer(Modifier.height(10.dp))

        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Scope candidate users based on role
          // CEO: all employees & TLs across company
          // Team Lead: ONLY their own team members and the CEO
          val scopeCandidateUsers = when (currentUser.role) {
            UserRole.CEO -> allUsers.filter { it.id != currentUser.id }
            UserRole.TEAM_LEAD -> allUsers.filter {
              (it.groupId == currentUser.groupId && it.role == UserRole.EMPLOYEE) || it.role == UserRole.CEO
            }
            UserRole.EMPLOYEE -> emptyList()
          }

          val scopeCandidateGroups = when (currentUser.role) {
            UserRole.CEO -> allGroups
            UserRole.TEAM_LEAD -> allGroups.filter { it.id == currentUser.groupId }
            UserRole.EMPLOYEE -> emptyList()
          }

          // If search query is entered, show matched employees and TLs
          if (directorySearchQuery.isNotBlank()) {
            val query = directorySearchQuery.trim().lowercase()
            val matchedUsers = scopeCandidateUsers.filter {
              it.name.lowercase().contains(query) ||
              it.designation.lowercase().contains(query) ||
              it.employeeId.lowercase().contains(query) ||
              it.groupName.lowercase().contains(query) ||
              it.role.displayName.lowercase().contains(query)
            }

            item {
              Text(
                text = "SEARCH RESULTS (${matchedUsers.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
              )
            }

            if (matchedUsers.isEmpty()) {
              item {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (currentUser.role == UserRole.TEAM_LEAD) "No team members match '$directorySearchQuery'" else "No employees or TLs match '$directorySearchQuery'",
                    fontSize = 12.sp,
                    color = TextMuted
                  )
                }
              }
            } else {
              items(matchedUsers, key = { it.id }) { user ->
                val userHasActiveQuery = allRequests.any {
                  it.userId == user.id && (it.status == LeaveStatus.QUERY || it.ceoStep?.decision == LeaveStatus.QUERY)
                }
                val pendingLeavesCount = allRequests.count {
                  it.userId == user.id && it.status == LeaveStatus.PENDING
                }

                DirectoryUserCard(
                  user = user,
                  hasActiveQuery = userHasActiveQuery,
                  pendingCount = pendingLeavesCount,
                  onClick = { selectDirectoryUser(user) }
                )
              }
            }
          } else {
            // Default: Department hierarchy tree view!
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (currentUser.role == UserRole.TEAM_LEAD) "MY TEAM DIRECTORY" else "DEPARTMENT HIERARCHY TREE",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextMuted
                )
                Text(
                  text = if (currentUser.role == UserRole.TEAM_LEAD) "Select team member to chat" else "Click group to expand TL & members",
                  fontSize = 10.sp,
                  color = TextSecondary
                )
              }
            }

            // Group tree cards
            items(scopeCandidateGroups, key = { it.id }) { group ->
              val isExpanded = expandedGroupIds.contains(group.id)
              val groupTl = allUsers.find { it.id == group.tlUserId }
              val groupEmployees = allUsers.filter { it.groupId == group.id && it.role == UserRole.EMPLOYEE }
              val groupPendingCount = allRequests.count {
                it.groupId == group.id && it.status == LeaveStatus.PENDING
              }
              val groupQueryCount = allRequests.count {
                it.groupId == group.id && (it.status == LeaveStatus.QUERY || it.ceoStep?.decision == LeaveStatus.QUERY)
              }

              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, if (isExpanded) LocalRoleTheme.current.accent.copy(alpha = 0.5f) else CardBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  // Group Header Clickable Row
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        expandedGroupIds = if (isExpanded) {
                          expandedGroupIds - group.id
                        } else {
                          expandedGroupIds + group.id
                        }
                      }
                      .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Box(
                        modifier = Modifier
                          .size(32.dp)
                          .clip(CircleShape)
                          .background(if (group.id == "g_exec") CeoPurpleLight else if (group.id == "g_eng") EmployeeTealLight else TeamLeadIndigoLight),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = if (group.id == "g_exec") Icons.Default.CorporateFare else Icons.Default.AccountTree,
                          contentDescription = null,
                          tint = if (group.id == "g_exec") CeoPurple else if (group.id == "g_eng") EmployeeTeal else TeamLeadIndigo,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                      Column {
                        Text(group.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${groupEmployees.size + (if (groupTl != null) 1 else 0)} Members • Min attendance: ${group.minPresentThreshold}", fontSize = 10.sp, color = TextSecondary)
                      }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      if (groupQueryCount > 0) {
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = StatusQueryAmberLight
                        ) {
                          Text("$groupQueryCount Query", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusQueryAmber, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                      } else if (groupPendingCount > 0) {
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = NeutralSurface
                        ) {
                          Text("$groupPendingCount Pending", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                      }
                      Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary
                      )
                    }
                  }

                  // Animated Expanded Hierarchy
                  AnimatedVisibility(visible = isExpanded) {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 4.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      // 1. PINNED TEAM LEAD AT TOP
                      if (groupTl != null) {
                        Text("👑 TEAM LEAD (PINNED)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TeamLeadIndigo)
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = TeamLeadIndigoLight.copy(alpha = 0.6f),
                          border = BorderStroke(1.dp, TeamLeadIndigo.copy(alpha = 0.4f)),
                          modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectDirectoryUser(groupTl) }
                        ) {
                          Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                          ) {
                            Box(
                              modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TeamLeadIndigo),
                              contentAlignment = Alignment.Center
                            ) {
                              Text(groupTl.avatarInitials, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(groupTl.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Surface(
                                  shape = RoundedCornerShape(4.dp),
                                  color = TeamLeadIndigo
                                ) {
                                  Text("TL", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                              }
                              Text("${groupTl.designation} • ${groupTl.employeeId}", fontSize = 10.sp, color = TextSecondary)
                            }
                            Icon(Icons.Default.Chat, contentDescription = "Chat", tint = TeamLeadIndigo, modifier = Modifier.size(18.dp))
                          }
                        }
                      }

                      // 2. EMPLOYEES LISTED UNDERNEATH
                      if (groupEmployees.isNotEmpty()) {
                        Text("TEAM MEMBERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        groupEmployees.forEach { emp ->
                          val hasQuery = allRequests.any {
                            it.userId == emp.id && (it.status == LeaveStatus.QUERY || it.ceoStep?.decision == LeaveStatus.QUERY)
                          }
                          Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasQuery) StatusQueryAmberLight.copy(alpha = 0.5f) else NeutralSurface,
                            border = BorderStroke(1.dp, if (hasQuery) StatusQueryAmber.copy(alpha = 0.4f) else CardBorder),
                            modifier = Modifier
                              .fillMaxWidth()
                              .clickable { selectDirectoryUser(emp) }
                          ) {
                            Row(
                              modifier = Modifier.padding(8.dp),
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                              Box(
                                modifier = Modifier
                                  .size(30.dp)
                                  .clip(CircleShape)
                                  .background(EmployeeTeal),
                                contentAlignment = Alignment.Center
                              ) {
                                Text(emp.avatarInitials, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                              }
                              Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                  Text(emp.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                  if (hasQuery) {
                                    Text("⚠️ Query Pending", fontSize = 9.sp, color = StatusQueryAmber, fontWeight = FontWeight.Bold)
                                  }
                                }
                                Text("${emp.designation} • ${emp.employeeId}", fontSize = 10.sp, color = TextSecondary)
                              }
                              Icon(Icons.Default.Chat, contentDescription = "Chat", tint = EmployeeTeal, modifier = Modifier.size(16.dp))
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

@Composable
private fun DirectoryUserCard(
  user: User,
  hasActiveQuery: Boolean,
  pendingCount: Int,
  onClick: () -> Unit
) {
  val avatarBg = when (user.role) {
    UserRole.CEO -> CeoPurple
    UserRole.TEAM_LEAD -> TeamLeadIndigo
    UserRole.EMPLOYEE -> EmployeeTeal
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (hasActiveQuery) StatusQueryAmberLight.copy(alpha = 0.5f) else Color.White,
    border = BorderStroke(1.dp, if (hasActiveQuery) StatusQueryAmber.copy(alpha = 0.6f) else CardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(avatarBg),
        contentAlignment = Alignment.Center
      ) {
        Text(user.avatarInitials, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(user.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          RoleBadge(user.role)
          if (hasActiveQuery) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = StatusQueryAmberLight
            ) {
              Text("⚠️ Query", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusQueryAmber, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            }
          }
        }
        Text("${user.designation} • ${user.groupName} (${user.employeeId})", fontSize = 11.sp, color = TextSecondary)
      }

      Icon(Icons.Default.Chat, contentDescription = "Open Chat", tint = avatarBg, modifier = Modifier.size(18.dp))
    }
  }
}
