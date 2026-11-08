package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.UserRole

data class RoleThemeColors(
  val accent: Color,
  val lightTint: Color,
  val surfaceBg: Color,
  val role: UserRole
)

val LocalRoleTheme = staticCompositionLocalOf {
  RoleThemeColors(
    accent = EmployeeTeal,
    lightTint = EmployeeTealLight,
    surfaceBg = EmployeeSurface,
    role = UserRole.EMPLOYEE
  )
}

@Composable
fun LeaveDeskTheme(
  userRole: UserRole = UserRole.EMPLOYEE,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val roleColors = when (userRole) {
    UserRole.EMPLOYEE -> RoleThemeColors(EmployeeTeal, EmployeeTealLight, EmployeeSurface, UserRole.EMPLOYEE)
    UserRole.TEAM_LEAD -> RoleThemeColors(TeamLeadIndigo, TeamLeadIndigoLight, TeamLeadSurface, UserRole.TEAM_LEAD)
    UserRole.CEO -> RoleThemeColors(CeoPurple, CeoPurpleLight, CeoSurface, UserRole.CEO)
  }

  val lightScheme = lightColorScheme(
    primary = roleColors.accent,
    onPrimary = Color.White,
    primaryContainer = roleColors.lightTint,
    onPrimaryContainer = roleColors.accent,
    secondary = NeutralGrey,
    background = Color(0xFFFCFDFD),
    surface = Color.White,
    surfaceVariant = roleColors.surfaceBg,
    onSurface = TextPrimary,
    outline = CardBorder
  )

  CompositionLocalProvider(LocalRoleTheme provides roleColors) {
    MaterialTheme(
      colorScheme = lightScheme,
      typography = Typography,
      content = content
    )
  }
}
