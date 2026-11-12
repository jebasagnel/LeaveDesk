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

    // Responsive Filter Chips Container (Wraps neatly across screen sizes)
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(
        "PENDING" to "Pending Review",
        "CANCEL_REQUESTED" to "Cancel Requests",
        "APPROVED" to "Approved",
        "REJECTED" to "Rejected",
        "ALL" to "All"
      ).forEach { (code, title) ->
        val count = when (code) {
          "PENDING" -> allRequests.count { it.groupId == myGroup?.id && it.userId != currentUser.id && (it.status == LeaveStatus.PENDING || it.status == LeaveStatus.QUERY) }
          "CANCEL_REQUESTED" -> allRequests.count { it.groupId == myGroup?.id && it.userId != currentUser.id && it.cancelRequested }
          "APPROVED" -> allRequests.count { it.groupId == myGroup?.id && it.userId != currentUser.id && it.status == LeaveStatus.APPROVED }
          "REJECTED" -> allRequests.count { it.groupId == myGroup?.id && it.userId != currentUser.id && it.status == LeaveStatus.REJECTED }
          else -> allRequests.count { it.groupId == myGroup?.id && it.userId != currentUser.id }
        }
        FilterChip(
          selected = selectedFilter == code,
          onClick = { selectedFilter = code },
          label = {
            Text(
              text = if (count > 0) "$title ($count)" else title,
              fontSize = 11.5.sp,
              maxLines = 1
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TeamLeadIndigoLight,
            selectedLabelColor = TeamLeadIndigo
          )
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                if (req.status == LeaveStatus.PENDING) {
                  Button(
                    onClick = { repository.tlApproveRequest(req, currentUser, "Endorsed by Team Lead") },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 44.dp)
                      .testTag("tl_approve_btn_${req.id.lowercase()}")
                  ) {
                    Text(
                      text = if (req.requiresCeoApproval) "Endorse" else "Approve",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1
                    )
                  }

                  OutlinedButton(
                    onClick = { queryDialogFor = req },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CeoPurple),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 44.dp)
                  ) {
                    Text("Query", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  }

                  Button(
                    onClick = { rejectDialogFor = req },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    modifier = Modifier
                      .weight(1f)
                      .defaultMinSize(minHeight = 44.dp)
                      .testTag("tl_reject_btn_${req.id.lowercase()}")
                  ) {
                    Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  }
                } else if (req.status == LeaveStatus.APPROVED) {
                  Button(
                    onClick = { emergencyCancelFor = req },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .defaultMinSize(minHeight = 44.dp)
                  ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Emergency Cancel / Postpone", fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
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
fun TeamLeadMyLeavesScreen(
  repository: LeaveDeskRepository,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  val allRequests by repository.leaveRequests.collectAsState()
  val allUsers by repository.users.collectAsState()
  val userBalances by repository.userBalances.collectAsState()
  val context = LocalContext.current

  // 0 = My History, 1 = Apply for Leave
  var activeTab by remember { mutableStateOf(0) }
  var selectedFilter by remember { mutableStateOf("ALL") }

  // Form State for TL Applying for Leave
  var selectedType by remember { mutableStateOf(LeaveType.CL) }
  var startDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
  var endDate by remember { mutableStateOf(LocalDate.now().plusDays(2)) }
  var isHalfDay by remember { mutableStateOf(false) }
  var halfDaySession by remember { mutableStateOf("First Half") }
  var selectedBackupUser by remember { mutableStateOf<User?>(null) }
  var backupTaskNote by remember { mutableStateOf("") }
  var reason by remember { mutableStateOf("") }
  var uploadedProof by remember { mutableStateOf<LeaveProof?>(null) }

  var showStartDatePicker by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }

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

  val myOwnRequests = remember(allRequests, currentUser, selectedFilter) {
    val list = allRequests.filter { it.userId == currentUser.id }
    when (selectedFilter) {
      "PENDING" -> list.filter { it.status == LeaveStatus.PENDING || it.status == LeaveStatus.QUERY }
      "APPROVED" -> list.filter { it.status == LeaveStatus.APPROVED }
      "REJECTED" -> list.filter { it.status == LeaveStatus.REJECTED }
      else -> list
    }
  }

  val myQuotas = remember(userBalances, currentUser) {
    userBalances[currentUser.id] ?: emptyList()
  }

  val peerCandidates = remember(allUsers, currentUser) {
    allUsers.filter { it.id != currentUser.id }
  }

  // Dialog states for TL's own requests
  var cancelReasonDialogFor by remember { mutableStateOf<LeaveRequest?>(null) }
  var cancelReasonText by remember { mutableStateOf("") }

  cancelReasonDialogFor?.let { req ->
    AlertDialog(
      onDismissRequest = { cancelReasonDialogFor = null },
      title = { Text("Request Leave Cancellation", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("A cancellation request will be submitted directly to CEO for approval.", fontSize = 12.sp, color = TextSecondary)
          OutlinedTextField(
            value = cancelReasonText,
            onValueChange = { cancelReasonText = it },
            label = { Text("Reason for cancellation *") },
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
          Text("Confirm Cancel")
        }
      },
      dismissButton = {
        TextButton(onClick = { cancelReasonDialogFor = null }) {
          Text("Dismiss")
        }
      }
    )
  }

  if (showStartDatePicker) {
    CalendarDatePickerDialog(
      title = "Select Start Date",
      initialDate = startDate,
      onDateSelected = {
        startDate = it
        if (endDate.isBefore(startDate)) endDate = startDate
      },
      onDismiss = { showStartDatePicker = false }
    )
  }

  if (showEndDatePicker) {
    CalendarDatePickerDialog(
      title = "Select End Date",
      initialDate = endDate,
      minDate = startDate,
      onDateSelected = {
        if (!it.isBefore(startDate)) endDate = it
      },
      onDismiss = { showEndDatePicker = false }
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      if (activeTab == 0) {
        ExtendedFloatingActionButton(
          onClick = { activeTab = 1 },
          containerColor = TeamLeadIndigo,
          contentColor = Color.White,
          icon = { Icon(Icons.Default.Add, contentDescription = "Apply Leave") },
          text = { Text("Apply for Leave", fontWeight = FontWeight.Bold, maxLines = 1) },
          modifier = Modifier.testTag("tl_fab_apply_leave")
        )
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header & Navigation Toggle
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TeamLeadSurface),
        border = BorderStroke(1.dp, TeamLeadIndigoLight),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "My Personal Leaves",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Direct CEO Escalation • Bypasses Peer Review",
                fontSize = 11.5.sp,
                color = TeamLeadIndigo,
                fontWeight = FontWeight.SemiBold
              )
            }
            RoleBadge(UserRole.TEAM_LEAD)
          }

          // Tab Selector Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = activeTab == 0,
              onClick = { activeTab = 0 },
              label = {
                Text(
                  text = "My History (${myOwnRequests.size})",
                  fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal
                )
              },
              leadingIcon = {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.weight(1f)
            )

            FilterChip(
              selected = activeTab == 1,
              onClick = { activeTab = 1 },
              label = {
                Text(
                  text = "Apply for Leave",
                  fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal
                )
              },
              leadingIcon = {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TeamLeadIndigo,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
              ),
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      if (activeTab == 0) {
        // --- TAB 0: TL LEAVE HISTORY ---
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(
            "ALL" to "All",
            "PENDING" to "Pending CEO",
            "APPROVED" to "Approved",
            "REJECTED" to "Rejected"
          ).forEach { (code, label) ->
            FilterChip(
              selected = selectedFilter == code,
              onClick = { selectedFilter = code },
              label = { Text(label, fontSize = 11.sp) }
            )
          }
        }

        if (myOwnRequests.isEmpty()) {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Text("No personal leave requests found.", color = TextMuted, fontSize = 14.sp)
                Button(
                  onClick = { activeTab = 1 },
                  colors = ButtonDefaults.buttonColors(containerColor = TeamLeadIndigo),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Apply Now (Direct to CEO)")
                }
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(myOwnRequests, key = { it.id }) { req ->
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        color = TextPrimary
                      )
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CeoPurple.copy(alpha = 0.12f)
                      ) {
                        Text(
                          text = "Routed to CEO",
                          fontSize = 10.sp,
                          color = CeoPurple,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                    LeaveStatusBadge(req.status)
                  }

                  Text(
                    text = "${req.startDate} to ${req.endDate} (${req.workingDays} working days${if (req.isHalfDay) ", ${req.halfDaySession ?: "Half-Day"}" else ""})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                  )

                  Text(
                    text = "Reason: ${req.reason}",
                    fontSize = 12.sp,
                    color = TextSecondary
                  )

                  if (!req.backupUserName.isNullOrBlank()) {
                    Text(
                      text = "Handover: ${req.backupUserName} • ${req.backupTaskNote ?: "No note"}",
                      fontSize = 11.sp,
                      color = TextMuted
                    )
                  }

                  if (req.ceoStep != null) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = CeoPurpleLight.copy(alpha = 0.5f),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = CeoPurple, modifier = Modifier.size(16.dp))
                        Text(
                          text = "CEO ${req.ceoStep.approverName}: \"${req.ceoStep.remarks ?: req.ceoStep.decision.name}\"",
                          fontSize = 11.5.sp,
                          color = CeoPurple,
                          fontWeight = FontWeight.Medium
                        )
                      }
                    }
                  }

                  // Action Buttons
                  if (req.status == LeaveStatus.PENDING) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.End
                    ) {
                      OutlinedButton(
                        onClick = { repository.employeeWithdrawRequest(req, currentUser) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejectedRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                      ) {
                        Text("Withdraw Request", fontSize = 11.5.sp)
                      }
                    }
                  } else if (req.status == LeaveStatus.APPROVED && !req.cancelRequested) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.End
                    ) {
                      OutlinedButton(
                        onClick = { cancelReasonDialogFor = req },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejectedRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                      ) {
                        Text("Request Cancellation", fontSize = 11.5.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        // --- TAB 1: TL LEAVE APPLICATION FORM (DIRECT TO CEO) ---
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Executive Bypassing Notice Banner
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = CeoPurple.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, CeoPurple.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.Security, contentDescription = null, tint = CeoPurple, modifier = Modifier.size(24.dp))
              Column {
                Text(
                  text = "Direct CEO Escalation",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = CeoPurple
                )
                Text(
                  text = "As Team Lead, your application bypasses peer review and is submitted directly to Chief Executive Officer Robert Sterling.",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }
          }

          // Leave Type Selector Card
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Select Leave Category", fontSize = 14.sp, fontWeight = FontWeight.Bold)
              @OptIn(ExperimentalLayoutApi::class)
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                LeaveType.entries.forEach { type ->
                  val quota = myQuotas.find { it.leaveType == type }
                  val remaining = quota?.remaining ?: 0f
                  FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = {
                      Text(
                        text = if (type.isNoCap) type.code else "${type.code} (${remaining.toInt()})",
                        fontSize = 11.5.sp
                      )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = TeamLeadIndigo,
                      selectedLabelColor = Color.White
                    )
                  )
                }
              }
            }
          }

          // Dates Card
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Dates & Duration", fontSize = 14.sp, fontWeight = FontWeight.Bold)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedButton(
                  onClick = { showStartDatePicker = true },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Start Date", fontSize = 10.sp, color = TextMuted)
                    Text(startDate.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  }
                }

                OutlinedButton(
                  onClick = { if (!isHalfDay) showEndDatePicker = true },
                  enabled = !isHalfDay,
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("End Date", fontSize = 10.sp, color = TextMuted)
                    Text(if (isHalfDay) startDate.toString() else endDate.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Checkbox(
                    checked = isHalfDay,
                    onCheckedChange = {
                      isHalfDay = it
                      if (it) endDate = startDate
                    }
                  )
                  Text("Half-Day Session", fontSize = 13.sp)
                }

                if (isHalfDay) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                      selected = halfDaySession == "First Half",
                      onClick = { halfDaySession = "First Half" },
                      label = { Text("1st Half", fontSize = 10.sp) }
                    )
                    FilterChip(
                      selected = halfDaySession == "Second Half",
                      onClick = { halfDaySession = "Second Half" },
                      label = { Text("2nd Half", fontSize = 10.sp) }
                    )
                  }
                }
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeutralSurface,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("Calculated Working Days:", fontSize = 12.sp, color = TextSecondary)
                  Text(
                    text = "${validation.workingDays} Day(s)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeamLeadIndigo
                  )
                }
              }
            }
          }

          // Handover Peer Selection
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Task Handover / Stand-In Peer", fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Assign a peer lead or team member to handle escalations while away.", fontSize = 11.5.sp, color = TextSecondary)

              @OptIn(ExperimentalLayoutApi::class)
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                peerCandidates.take(6).forEach { user ->
                  FilterChip(
                    selected = selectedBackupUser?.id == user.id,
                    onClick = {
                      selectedBackupUser = if (selectedBackupUser?.id == user.id) null else user
                    },
                    label = { Text("${user.name} (${user.role.displayName})", fontSize = 11.sp) }
                  )
                }
              }

              OutlinedTextField(
                value = backupTaskNote,
                onValueChange = { backupTaskNote = it },
                label = { Text("Handover Notes / On-Call Protocol") },
                placeholder = { Text("e.g. Elena will cover operational escalations...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
              )
            }
          }

          // Reason & Document Proof
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Reason & Documentation", fontSize = 14.sp, fontWeight = FontWeight.Bold)

              OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for Leave *") },
                placeholder = { Text("Provide details for CEO review...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Attach Photo", fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                  onClick = { docLauncher.launch("*/*") },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Attach File", fontSize = 11.sp, maxLines = 1)
                }
              }

              uploadedProof?.let { proof ->
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = TeamLeadSurface,
                  border = BorderStroke(1.dp, TeamLeadIndigo.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("Attached: ${proof.fileName} (${proof.fileSizeStr})", fontSize = 11.sp, color = TeamLeadIndigo)
                    IconButton(onClick = { uploadedProof = null }, modifier = Modifier.size(24.dp)) {
                      Icon(Icons.Default.Close, contentDescription = "Remove proof", modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }
          }

          // Validation Message Banner
          if (validation.message != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (validation.isValid) Color(0xFFFEF3C7) else Color(0xFFFEE2E2),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = validation.message,
                fontSize = 11.5.sp,
                color = if (validation.isValid) Color(0xFF92400E) else StatusRejectedRed,
                modifier = Modifier.padding(10.dp)
              )
            }
          }

          // Dynamic Scaling Action Button
          Button(
            onClick = {
              val ok = repository.submitLeaveRequest(
                user = currentUser,
                leaveType = selectedType,
                startDate = startDate,
                endDate = endDate,
                isHalfDay = isHalfDay,
                halfDaySession = if (isHalfDay) halfDaySession else null,
                reason = reason.ifBlank { "${selectedType.title} Application" },
                backupUser = selectedBackupUser,
                backupTaskNote = backupTaskNote.ifBlank { null },
                proof = uploadedProof
              )
              if (ok) {
                repository.showToast("Leave application submitted directly to CEO for approval!")
                activeTab = 0 // Switch to History
              }
            },
            enabled = validation.isValid && reason.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = TeamLeadIndigo),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("tl_submit_leave_btn")
          ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit Application to CEO", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
          }

          OutlinedButton(
            onClick = { activeTab = 0 },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 44.dp)
          ) {
            Text("Cancel and View History", fontSize = 13.sp)
          }

          Spacer(modifier = Modifier.height(20.dp))
        }
      }
    }
  }
}

