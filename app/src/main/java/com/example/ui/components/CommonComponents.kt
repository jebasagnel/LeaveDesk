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

