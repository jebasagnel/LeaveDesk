package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import com.example.data.LeaveDeskRepository
import com.example.model.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  private val repository = LeaveDeskRepository()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    repository.initContext(applicationContext)
    enableEdgeToEdge()
    setContent {
      val currentUser by repository.currentUser.collectAsState()
      LeaveDeskTheme(userRole = currentUser.role) {
        LeaveDeskApp(repository = repository)
      }
    }
  }
}

enum class AppDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  // Common / Employee
  HOME("Home", Icons.Default.Home),
  APPLY("Apply", Icons.Default.AddCircleOutline),
  CALENDAR("Calendar", Icons.Default.CalendarMonth),
  MY_REQUESTS("Requests", Icons.Default.FormatListBulleted),
  CHAT("Chat", Icons.Default.ChatBubbleOutline),

  // TL Specific
  TEAM_REQUESTS("Team Inbox", Icons.Default.Inbox),
  MY_LEAVES("My Leaves", Icons.Default.Person),

  // CEO Specific
  DASHBOARD("Dashboard", Icons.Default.Dashboard),
  APPROVALS("Approvals", Icons.Default.Rule),
  AUDIT("Audit", Icons.Default.History),

  // Modal / Overlays
  NOTIFICATIONS("Notifications", Icons.Default.Notifications),
  LOGIN("Switch User", Icons.Default.Login)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveDeskApp(repository: LeaveDeskRepository) {
  val isLoggedIn by repository.isLoggedIn.collectAsState()
  val currentUser by repository.currentUser.collectAsState()
  val allUsers by repository.users.collectAsState()
  val notifications by repository.notifications.collectAsState()
  val toastMessage by repository.toastMessage.collectAsState()

  val unreadNotifCount = remember(notifications, currentUser) {
    notifications.count {
      (it.recipientUserId == currentUser.id || it.recipientUserId == "ALL") && !it.isRead
    }
  }

  // Active navigation tab
  var currentTab by remember(currentUser.role) {
    mutableStateOf(
      when (currentUser.role) {
        UserRole.EMPLOYEE -> AppDestination.HOME
        UserRole.TEAM_LEAD -> AppDestination.HOME
        UserRole.CEO -> AppDestination.DASHBOARD
      }
    )
  }

  var showNotificationsScreen by remember { mutableStateOf(false) }
  var showUserMenu by remember { mutableStateOf(false) }
  var showBackendDialog by remember { mutableStateOf(false) }
  val backendState by repository.backendState.collectAsState()
  var backendUrlInput by remember(backendState.serverUrl) { mutableStateOf(backendState.serverUrl) }

  // Auto-dismiss toast after 4 seconds
  LaunchedEffect(toastMessage) {
    if (toastMessage != null) {
      delay(4000)
      repository.clearToast()
    }
  }

  // Handle back press
  BackHandler(enabled = !isLoggedIn || showNotificationsScreen || currentTab != AppDestination.HOME && currentTab != AppDestination.DASHBOARD) {
    when {
      !isLoggedIn -> { /* at root login */ }
      showNotificationsScreen -> showNotificationsScreen = false
      else -> {
        currentTab = if (currentUser.role == UserRole.CEO) AppDestination.DASHBOARD else AppDestination.HOME
      }
    }
  }

  // Responsive mobile shell (390dp max width centered for mobile design)
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFE5E7EB)),
    contentAlignment = Alignment.Center
  ) {
    if (showBackendDialog) {
      AlertDialog(
        onDismissRequest = { showBackendDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = LocalRoleTheme.current.accent)
            Text("Backend & Database Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (backendState.status == com.example.model.BackendStatus.ONLINE) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
              border = BorderStroke(1.dp, if (backendState.status == com.example.model.BackendStatus.ONLINE) Color(0xFF86EFAC) else CardBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (backendState.status == com.example.model.BackendStatus.ONLINE) Color(0xFF16A34A) else Color(0xFF64748B)))
                  Text("Status: ${backendState.status.label}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (backendState.status == com.example.model.BackendStatus.ONLINE) Color(0xFF166534) else Color(0xFF475569))
                }
                Text("Engine: ${backendState.databaseEngine}", fontSize = 11.sp, color = TextSecondary)
                Text("Telemetry: ${backendState.message}", fontSize = 10.sp, color = TextMuted)
                backendState.lastSyncTime?.let {
                  Text("Last Synced: $it", fontSize = 10.sp, color = TextMuted)
                }
              }
            }

            Text("Gateway Host URL:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            OutlinedTextField(
              value = backendUrlInput,
              onValueChange = { backendUrlInput = it },
              placeholder = { Text("e.g. http://10.0.2.2:8084/ or http://192.168.1.100:8084/") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Text("Tip: In emulator use http://10.0.2.2:8084/. On a physical phone on Wi-Fi, enter your PC's IP address.", fontSize = 10.sp, color = TextSecondary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = {
                  repository.updateBackendUrl(backendUrlInput)
                  repository.testBackendConnection()
                },
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Ping", fontSize = 11.sp)
              }
              Button(
                onClick = {
                  repository.updateBackendUrl(backendUrlInput)
                  repository.syncWithLiveBackend()
                  showBackendDialog = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = LocalRoleTheme.current.accent),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save & Sync", fontSize = 11.sp)
              }
            }
          }
        },
        confirmButton = {},
        dismissButton = {
          TextButton(onClick = { showBackendDialog = false }) {
            Text("Close")
          }
        }
      )
    }

    Scaffold(
      modifier = Modifier
        .widthIn(max = 480.dp)
        .fillMaxHeight(),
      topBar = {
        if (isLoggedIn) {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LocalRoleTheme.current.accent),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Text(
                  text = "LeaveDesk",
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp,
                  color = TextPrimary
                )
                RoleBadge(currentUser.role)
              }
            },
            actions = {
              // Backend Connection Status Indicator
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (backendState.status == com.example.model.BackendStatus.ONLINE) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (backendState.status == com.example.model.BackendStatus.ONLINE) Color(0xFF16A34A) else Color(0xFF94A3B8)),
                modifier = Modifier
                  .clickable { showBackendDialog = true }
                  .testTag("top_bar_backend_chip")
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
                      .background(if (backendState.status == com.example.model.BackendStatus.ONLINE) Color(0xFF16A34A) else Color(0xFF64748B))
                  )
                  Text(
                    text = if (backendState.status == com.example.model.BackendStatus.ONLINE) "Live DB" else "Local DB",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (backendState.status == com.example.model.BackendStatus.ONLINE) Color(0xFF166534) else Color(0xFF475569)
                  )
                }
              }

              // Notifications Bell with Unread Badge
              IconButton(
                onClick = { showNotificationsScreen = !showNotificationsScreen },
                modifier = Modifier.testTag("top_bar_bell_button")
              ) {
                BadgedBox(
                  badge = {
                    if (unreadNotifCount > 0) {
                      Badge(containerColor = StatusRejectedRed) {
                        Text(unreadNotifCount.toString())
                      }
                    }
                  }
                ) {
                  Icon(
                    imageVector = if (showNotificationsScreen) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = TextPrimary
                  )
                }
              }

              // User Avatar with Dropdown Menu
              Box {
                Surface(
                  shape = CircleShape,
                  color = LocalRoleTheme.current.accent,
                  modifier = Modifier
                    .padding(end = 12.dp)
                    .size(34.dp)
                    .clickable { showUserMenu = true }
                    .testTag("top_bar_profile_button")
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = currentUser.avatarInitials,
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                DropdownMenu(
                  expanded = showUserMenu,
                  onDismissRequest = { showUserMenu = false }
                ) {
                  DropdownMenuItem(
                    text = {
                      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(currentUser.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${currentUser.employeeId} • ${currentUser.role.displayName}", fontSize = 11.sp, color = LocalRoleTheme.current.accent, fontWeight = FontWeight.SemiBold)
                        Text(currentUser.email, fontSize = 11.sp, color = TextSecondary)
                        Text("Department: ${currentUser.groupName}", fontSize = 10.sp, color = TextMuted)
                      }
                    },
                    onClick = { },
                    enabled = false
                  )
                  HorizontalDivider()
                  DropdownMenuItem(
                    text = { Text("Sign Out", color = StatusRejectedRed, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = StatusRejectedRed) },
                    onClick = {
                      showUserMenu = false
                      repository.logout()
                    }
                  )
                }
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = Color.White
            )
          )
        }
      },
      bottomBar = {
        if (isLoggedIn && !showNotificationsScreen) {
          NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp
          ) {
            val tabs = when (currentUser.role) {
              UserRole.EMPLOYEE -> listOf(
                AppDestination.HOME,
                AppDestination.APPLY,
                AppDestination.CALENDAR,
                AppDestination.MY_REQUESTS,
                AppDestination.CHAT
              )
              UserRole.TEAM_LEAD -> listOf(
                AppDestination.HOME,
                AppDestination.TEAM_REQUESTS,
                AppDestination.CALENDAR,
                AppDestination.MY_LEAVES,
                AppDestination.CHAT
              )
              UserRole.CEO -> listOf(
                AppDestination.DASHBOARD,
                AppDestination.APPROVALS,
                AppDestination.CALENDAR,
                AppDestination.AUDIT,
                AppDestination.CHAT
              )
            }

            tabs.forEach { dest ->
              val isSelected = currentTab == dest
              NavigationBarItem(
                selected = isSelected,
                onClick = { currentTab = dest },
                icon = {
                  Icon(
                    imageVector = dest.icon,
                    contentDescription = dest.label
                  )
                },
                label = {
                  Text(
                    text = dest.label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = LocalRoleTheme.current.accent,
                  selectedTextColor = LocalRoleTheme.current.accent,
                  indicatorColor = LocalRoleTheme.current.lightTint
                ),
                modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
              )
            }
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(Color(0xFFF9FAFB))
      ) {
        // Screen Content
        when {
          !isLoggedIn -> {
            LoginScreen(
              repository = repository,
              onLoginComplete = {
                currentTab = if (currentUser.role == UserRole.CEO) AppDestination.DASHBOARD else AppDestination.HOME
              }
            )
          }

          showNotificationsScreen -> {
            NotificationsScreen(
              repository = repository,
              modifier = Modifier.fillMaxSize()
            )
          }

          else -> {
            when (currentTab) {
              AppDestination.HOME -> {
                if (currentUser.role == UserRole.TEAM_LEAD) {
                  TeamLeadHomeScreen(
                    repository = repository,
                    onNavigateToTeamRequests = { currentTab = AppDestination.TEAM_REQUESTS },
                    onNavigateToMyLeaves = { currentTab = AppDestination.MY_LEAVES },
                    onNavigateToCalendar = { currentTab = AppDestination.CALENDAR }
                  )
                } else {
                  EmployeeHomeScreen(
                    repository = repository,
                    onNavigateToApply = { currentTab = AppDestination.APPLY },
                    onNavigateToRequests = { currentTab = AppDestination.MY_REQUESTS },
                    onNavigateToCalendar = { currentTab = AppDestination.CALENDAR }
                  )
                }
              }

              AppDestination.APPLY -> {
                EmployeeApplyScreen(
                  repository = repository,
                  onApplySuccess = { currentTab = AppDestination.MY_REQUESTS }
                )
              }

              AppDestination.MY_REQUESTS -> {
                EmployeeRequestsScreen(repository = repository)
              }

              AppDestination.TEAM_REQUESTS -> {
                TeamLeadRequestsScreen(repository = repository)
              }

              AppDestination.MY_LEAVES -> {
                TeamLeadMyLeavesScreen(repository = repository)
              }

              AppDestination.DASHBOARD -> {
                CeoDashboardScreen(
                  repository = repository,
                  onNavigateToApprovals = { currentTab = AppDestination.APPROVALS },
                  onNavigateToAudit = { currentTab = AppDestination.AUDIT },
                  onNavigateToCalendarEditor = { currentTab = AppDestination.CALENDAR }
                )
              }

              AppDestination.APPROVALS -> {
                CeoApprovalsScreen(repository = repository)
              }

              AppDestination.AUDIT -> {
                CeoAuditScreen(repository = repository)
              }

              AppDestination.CALENDAR -> {
                if (currentUser.role == UserRole.CEO) {
                  CeoCompanyCalendarEditor(repository = repository)
                } else {
                  CompanyCalendarScreen(repository = repository)
                }
              }

              AppDestination.CHAT -> {
                QuickChatScreen(repository = repository)
              }

              else -> {}
            }
          }
        }

        // Floating Toast / "Mail Sent" Banner
        toastMessage?.let { msg ->
          Box(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(horizontal = 16.dp, vertical = 12.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF1E293B),
              shadowElevation = 6.dp,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = StatusApprovedGreen,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = msg,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                  )
                }
                IconButton(
                  onClick = { repository.clearToast() },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
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
