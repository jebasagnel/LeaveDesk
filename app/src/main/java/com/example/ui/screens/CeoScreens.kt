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
