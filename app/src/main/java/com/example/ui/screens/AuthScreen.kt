package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.CampusCard
import com.example.ui.components.StatusPill

@Composable
fun AuthScreen(
  externalError: String? = null,
  onClearExternalError: () -> Unit = {},
  onSignInCompleted: (requestedAdminPortal: Boolean) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoadingStudent by remember { mutableStateOf(false) }
  var isLoadingAdmin by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { onSignInCompleted(false) },
      onUnauthenticated = {},
      scope = scope,
    )
  }

  val displayedError = externalError ?: errorMessage

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(12.dp))

      // Hero Academic Crest Card
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 520.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
              Brush.linearGradient(
                listOf(
                  Color(0xFF152B52),
                  Color(0xFF284E91),
                )
              )
            )
            .padding(24.dp)
      ) {
        Column(horizontalAlignment = Alignment.Start) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.size(52.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.School,
                contentDescription = "College Core Emblem",
                tint = Color.White,
                modifier = Modifier.size(30.dp),
              )
            }
            Spacer(Modifier.width(14.dp))
            Column {
              StatusPill(
                text = "ITG GOPESHWAR",
                containerColor = Color(0xFFF59E0B),
                contentColor = Color(0xFF1E1B18),
              )
              Spacer(Modifier.height(4.dp))
              Text(
                text = "College Core",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
              )
            }
          }
          Spacer(Modifier.height(14.dp))
          Text(
            text = "Govt. Institute of Technology (ITG) Gopeshwar",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.95f),
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text =
              "Centralized academic portal for notices, syllabus, study notes, previous exam papers, and campus gallery.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f),
          )
        }
      }

      Spacer(Modifier.height(20.dp))

      if (!displayedError.isNullOrBlank()) {
        CampusCard(
          modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
          containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
          borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
        ) {
          Text(
            text = displayedError,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
        Spacer(Modifier.height(14.dp))
      }

      // Student Sign-In Card
      CampusCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.VerifiedUser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.width(10.dp))
          Text(
            text = "Student Portal Sign-In",
            style = MaterialTheme.typography.titleLarge,
          )
        }
        Spacer(Modifier.height(8.dp))
        Text(
          text =
            "Sign in with your Google / college-issued account to view notices, syllabus, notes, exam papers, and campus updates tailored to your course and semester.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(
          onClick = {
            errorMessage = null
            onClearExternalError()
            isLoadingStudent = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoadingStudent = false
                onSignInCompleted(false)
              },
              onAuthError = { msg ->
                isLoadingStudent = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isLoadingStudent = false },
            )
          },
          enabled = !isLoadingStudent && !isLoadingAdmin,
          modifier =
            Modifier.fillMaxWidth()
              .defaultMinSize(minHeight = 50.dp)
              .testTag("student_google_signin_button"),
        ) {
          if (isLoadingStudent) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
            Text("Signing in...")
          } else {
            Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
          }
        }
      }

      Spacer(Modifier.height(14.dp))

      // Separate Protected Admin Portal Sign-In Card
      CampusCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.AdminPanelSettings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
          )
          Spacer(Modifier.width(10.dp))
          Text(
            text = "Administrator Portal",
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(Modifier.weight(1f))
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = "Protected Admin Access",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
        Spacer(Modifier.height(8.dp))
        Text(
          text =
            "Restricted to authorized ITG Gopeshwar administrators only. Public admin sign-up is disabled and verified by server-side security rules.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
          onClick = {
            errorMessage = null
            onClearExternalError()
            isLoadingAdmin = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoadingAdmin = false
                onSignInCompleted(true)
              },
              onAuthError = { msg ->
                isLoadingAdmin = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isLoadingAdmin = false },
            )
          },
          enabled = !isLoadingStudent && !isLoadingAdmin,
          modifier =
            Modifier.fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("admin_google_signin_button"),
        ) {
          if (isLoadingAdmin) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Verifying admin access...")
          } else {
            Text("Admin Sign-In with Google")
          }
        }
      }

      Spacer(Modifier.height(20.dp))
      Text(
        text = "Secured by Firebase Authentication & Cloud Firestore Rules",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}
