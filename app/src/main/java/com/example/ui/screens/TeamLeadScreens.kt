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
