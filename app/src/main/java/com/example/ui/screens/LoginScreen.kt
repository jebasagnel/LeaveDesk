package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaveDeskRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  repository: LeaveDeskRepository,
  onLoginComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val focusManager = LocalFocusManager.current
  val scrollState = rememberScrollState()

  var organization by remember { mutableStateOf("leavedesk.corp") }
  var identifier by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  fun performLogin() {
    val cleanId = identifier.trim()
    val cleanPass = password.trim()
    val cleanOrg = organization.trim()

    if (cleanId.isEmpty() || cleanPass.isEmpty()) {
      errorMessage = "Please enter your Employee ID / Work Email and Password."
      return
    }

    isSubmitting = true
    errorMessage = null
    val success = repository.login(cleanId, cleanPass, cleanOrg)
    if (success) {
      errorMessage = null
      onLoginComplete()
    } else {
      errorMessage = "Authentication failed. Please verify your Employee ID or Work Email, Password, and Organization Domain."
    }
    isSubmitting = false
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFF8FAFC))
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(20.dp))

    // Corporate App Header
    Box(
      modifier = Modifier
        .size(68.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(EmployeeTeal),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.CalendarMonth,
        contentDescription = "LeaveDesk Logo",
        tint = Color.White,
        modifier = Modifier.size(38.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "LeaveDesk",
      fontSize = 26.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary
    )
    Text(
      text = "Enterprise Leave & Attendance Management",
      fontSize = 13.sp,
      color = TextSecondary,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(28.dp))

    // Production Login Form Card
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color.White,
      border = BorderStroke(1.dp, CardBorder),
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Corporate Sign In",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Enter your work credentials to access your portal",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }

        // Error message banner
        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = StatusRejectedRed,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = errorMessage!!,
                color = StatusRejectedRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Organization / Domain Field
        OutlinedTextField(
          value = organization,
          onValueChange = {
            organization = it
            errorMessage = null
          },
          label = { Text("Organization Domain / Org Code") },
          placeholder = { Text("e.g. leavedesk.corp or CORP01") },
          leadingIcon = {
            Icon(Icons.Default.Business, contentDescription = null, tint = TextMuted)
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_organization")
        )

        // Employee ID / Work Email Field
        OutlinedTextField(
          value = identifier,
          onValueChange = {
            identifier = it
            errorMessage = null
          },
          label = { Text("Employee ID or Corporate Email") },
          placeholder = { Text("e.g. EMP101 or alex.chen@leavedesk.corp") },
          leadingIcon = {
            Icon(Icons.Default.Badge, contentDescription = null, tint = TextMuted)
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_identifier")
        )

        // Password Field
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            errorMessage = null
          },
          label = { Text("Password") },
          placeholder = { Text("Enter your password") },
          leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
          },
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                tint = TextMuted
              )
            }
          },
          singleLine = true,
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              focusManager.clearFocus()
              performLogin()
            }
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_password")
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Submit Button
        Button(
          onClick = {
            focusManager.clearFocus()
            performLogin()
          },
          enabled = !isSubmitting,
          colors = ButtonDefaults.buttonColors(containerColor = EmployeeTeal),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("login_submit_button")
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
              Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Enterprise Security Footer
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = "Encrypted Connection",
        tint = TextMuted,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = "Enterprise SSO & Directory Services • 256-bit SSL Protected",
        fontSize = 11.sp,
        color = TextMuted,
        textAlign = TextAlign.Center
      )
    }
  }
}
