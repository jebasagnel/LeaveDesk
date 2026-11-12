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
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("submit_leave_button")
        ) {
          Text("Submit Leave Application", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                          .weight(1f)
                          .defaultMinSize(minHeight = 44.dp)
                      ) {
                        Text("Withdraw", fontSize = 12.sp, maxLines = 1)
                      }
                      if (isOver24h) {
                        Button(
                          onClick = { repository.sendApproverReminder(req, currentUser) },
                          colors = ButtonDefaults.buttonColors(containerColor = StatusPendingAmber),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                          modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                        ) {
                          Text("Send Reminder", fontSize = 11.sp, color = Color.White, maxLines = 1)
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
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                          modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp)
                        ) {
                          Text("Request Cancel / Postpone", fontSize = 12.sp, maxLines = 1)
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
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                          .weight(1f)
                          .defaultMinSize(minHeight = 44.dp)
                      ) {
                        Text("Reply to Query", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                      }
                      OutlinedButton(
                        onClick = { repository.employeeWithdrawRequest(req, currentUser) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                          .weight(1f)
                          .defaultMinSize(minHeight = 44.dp)
                      ) {
                        Text("Withdraw", fontSize = 12.sp, maxLines = 1)
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
