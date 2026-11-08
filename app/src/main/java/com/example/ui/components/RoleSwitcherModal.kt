package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun RoleSwitcherModal(
  currentUser: User,
  allUsers: List<User>,
  onSelectUser: (User) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = EmployeeTeal)
        Text("Switch Role / Persona", fontSize = 17.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Instantly test approvals and policies across all three LeaveDesk roles:",
          fontSize = 12.sp,
          color = TextSecondary
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 340.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(allUsers) { user ->
            val isSelected = user.id == currentUser.id
            val (roleColor, roleBg) = when (user.role) {
              UserRole.EMPLOYEE -> EmployeeTeal to EmployeeTealLight
              UserRole.TEAM_LEAD -> TeamLeadIndigo to TeamLeadIndigoLight
              UserRole.CEO -> CeoPurple to CeoPurpleLight
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) roleBg else NeutralSurface,
              border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) roleColor else CardBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onSelectUser(user)
                  onDismiss()
                }
                .testTag("switch_user_${user.id}")
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(34.dp)
                      .clip(CircleShape)
                      .background(roleColor),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = user.avatarInitials,
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                  }

                  Column {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(
                        text = user.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                      )
                      RoleBadge(user.role)
                    }
                    Text(
                      text = "${user.designation} • ${user.groupName}",
                      fontSize = 11.sp,
                      color = TextSecondary
                    )
                  }
                }

                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active user",
                    tint = roleColor,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}
