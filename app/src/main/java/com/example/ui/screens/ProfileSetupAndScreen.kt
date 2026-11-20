package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AcademicBranch
import com.example.model.AcademicCourse
import com.example.model.StudentProfile
import com.example.ui.components.CampusCard
import com.example.ui.components.SimpleDropdownField
import com.example.ui.components.StatusPill
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun StudentProfileSetupScreen(
  existingProfile: StudentProfile?,
  courses: List<AcademicCourse>,
  branches: List<AcademicBranch>,
  isBusy: Boolean,
  isAdmin: Boolean,
  onSaveProfile: (
    fullName: String,
    rollNumber: String,
    course: AcademicCourse,
    branch: AcademicBranch?,
    academicYear: Long,
    semester: Long,
    photoUrl: String,
  ) -> Unit,
  onSeedDemoData: () -> Unit,
  onOpenAdminDashboard: () -> Unit,
  onSignOut: () -> Unit,
  onCancelEdit: (() -> Unit)? = null,
) {
  val currentUser = Firebase.auth.currentUser
  var fullName by remember(existingProfile) {
    mutableStateOf(existingProfile?.fullName ?: currentUser?.displayName.orEmpty())
  }
  var rollNumber by remember(existingProfile) {
    mutableStateOf(existingProfile?.rollNumber ?: "")
  }
  var photoUrl by remember(existingProfile) {
    mutableStateOf(existingProfile?.photoUrl ?: currentUser?.photoUrl?.toString().orEmpty())
  }

  val fallbackCourse = remember {
    AcademicCourse(
      id = "default_btech",
      name = "B.Tech",
      code = "BTECH",
      durationYears = 4L,
      totalSemesters = 8L,
      hasBranches = true,
    )
  }
  val availableCourses = courses.ifEmpty { listOf(fallbackCourse) }

  var selectedCourse by remember(availableCourses, existingProfile) {
    mutableStateOf(
      availableCourses.firstOrNull { it.id == existingProfile?.courseId } ?: availableCourses.first()
    )
  }

  val courseBranches =
    remember(branches, selectedCourse) {
      branches.filter { it.courseId == selectedCourse.id }
    }

  var selectedBranch by remember(courseBranches, existingProfile) {
    mutableStateOf(
      courseBranches.firstOrNull { it.id == existingProfile?.branchId } ?: courseBranches.firstOrNull()
    )
  }

  var selectedYear by remember(existingProfile, selectedCourse) {
    mutableLongStateOf(
      (existingProfile?.academicYear ?: 1L).coerceIn(1L, selectedCourse.durationYears)
    )
  }
  var selectedSemester by remember(existingProfile, selectedCourse) {
    mutableLongStateOf(
      (existingProfile?.semester ?: 1L).coerceIn(1L, selectedCourse.totalSemesters)
    )
  }

  var courseExpanded by remember { mutableStateOf(false) }
  var branchExpanded by remember { mutableStateOf(false) }
  var yearExpanded by remember { mutableStateOf(false) }
  var semExpanded by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CampusCard(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
      Text(
        text = if (existingProfile == null) "Complete Student Profile" else "Edit Student Profile",
        style = MaterialTheme.typography.headlineSmall,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = "Your notices, syllabus, notes, and exam papers are personalized using your course, branch, year, and semester.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (courses.isEmpty()) {
        Spacer(Modifier.height(12.dp))
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
          shape = MaterialTheme.shapes.small,
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text =
                if (isAdmin) {
                  "No courses configured yet. Seed [DEMO] ITG Gopeshwar courses or configure them in Admin Dashboard."
                } else {
                  "Academic courses are not configured by the administrator yet. You can save with default B.Tech and update later."
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (isAdmin) {
              Spacer(Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onSeedDemoData, enabled = !isBusy) {
                  Text("Seed [DEMO] Data")
                }
                OutlinedButton(onClick = onOpenAdminDashboard) {
                  Text("Admin Dashboard")
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(16.dp))
      OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = { Text("Full Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
      )
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = rollNumber,
        onValueChange = { rollNumber = it },
        label = { Text("Roll Number / Student ID") },
        placeholder = { Text("e.g., ITG2026045") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("profile_roll_input"),
      )
      Spacer(Modifier.height(12.dp))

      SimpleDropdownField(
        label = "Course",
        selectedLabel = "${selectedCourse.name} (${selectedCourse.durationYears} Years)",
        options = availableCourses,
        optionLabel = { "${it.name} (${it.durationYears} Yrs / ${it.totalSemesters} Sems)" },
        onSelect = { course ->
          selectedCourse = course
          selectedYear = selectedYear.coerceIn(1L, course.durationYears)
          selectedSemester = selectedSemester.coerceIn(1L, course.totalSemesters)
          selectedBranch = branches.firstOrNull { it.courseId == course.id }
        },
        expanded = courseExpanded,
        onExpandedChange = { courseExpanded = it },
      )

      if (selectedCourse.hasBranches && courseBranches.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        SimpleDropdownField(
          label = "Branch",
          selectedLabel = selectedBranch?.name ?: "Select Branch",
          options = courseBranches,
          optionLabel = { it.name },
          onSelect = { selectedBranch = it },
          expanded = branchExpanded,
          onExpandedChange = { branchExpanded = it },
        )
      }

      Spacer(Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        SimpleDropdownField(
          label = "Academic Year",
          selectedLabel = "Year $selectedYear",
          options = (1L..selectedCourse.durationYears).toList(),
          optionLabel = { "Year $it" },
          onSelect = { selectedYear = it },
          expanded = yearExpanded,
          onExpandedChange = { yearExpanded = it },
          modifier = Modifier.weight(1f),
        )
        SimpleDropdownField(
          label = "Semester",
          selectedLabel = "Sem $selectedSemester",
          options = (1L..selectedCourse.totalSemesters).toList(),
          optionLabel = { "Semester $it" },
          onSelect = { selectedSemester = it },
          expanded = semExpanded,
          onExpandedChange = { semExpanded = it },
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = photoUrl,
        onValueChange = { photoUrl = it },
        label = { Text("Profile Photo URL (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      if (!validationError.isNullOrBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
          text = validationError!!,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Spacer(Modifier.height(18.dp))
      Button(
        onClick = {
          val n = fullName.trim()
          val r = rollNumber.trim()
          val p = photoUrl.trim()
          when {
            n.length < 2 -> validationError = "Please enter your full name (at least 2 characters)."
            r.length < 2 -> validationError = "Please enter your Roll Number / Student ID."
            p.isNotEmpty() && !p.startsWith("http://") && !p.startsWith("https://") ->
              validationError = "Photo URL must start with http:// or https://"
            else -> {
              validationError = null
              onSaveProfile(
                n,
                r,
                selectedCourse,
                if (selectedCourse.hasBranches) selectedBranch else null,
                selectedYear,
                selectedSemester,
                p,
              )
            }
          }
        },
        enabled = !isBusy,
        modifier =
          Modifier.fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("save_profile_button"),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
          Spacer(Modifier.width(8.dp))
        }
        Text("Save Profile")
      }

      if (onCancelEdit != null) {
        Spacer(Modifier.height(8.dp))
        TextButton(
          onClick = onCancelEdit,
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
          Text("Cancel")
        }
      } else {
        Spacer(Modifier.height(8.dp))
        TextButton(
          onClick = onSignOut,
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
          Text("Sign Out")
        }
      }
    }
  }
}

@Composable
fun ProfileScreen(
  profile: StudentProfile?,
  isAdmin: Boolean,
  onEditProfile: () -> Unit,
  onOpenAdminDashboard: () -> Unit,
  onSignOut: () -> Unit,
) {
  val authUser = Firebase.auth.currentUser
  val context = LocalContext.current
  val displayPhoto = profile?.photoUrl?.takeIf { it.isNotBlank() } ?: authUser?.photoUrl?.toString()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CampusCard(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (!displayPhoto.isNullOrBlank()) {
          AsyncImage(
            model =
              ImageRequest.Builder(context)
                .data(displayPhoto)
                .crossfade(true)
                .size(160)
                .build(),
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(68.dp).clip(CircleShape),
          )
        } else {
          Box(
            modifier =
              Modifier.size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = "Profile avatar",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(34.dp),
            )
          }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = profile?.fullName?.ifBlank { authUser?.displayName ?: "ITG Student" }
              ?: authUser?.displayName ?: "ITG Student",
            style = MaterialTheme.typography.headlineSmall,
          )
          Spacer(Modifier.height(4.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(
              text = if (isAdmin) "ADMIN ACCESS" else "STUDENT",
              containerColor =
                if (isAdmin) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.primaryContainer,
              contentColor =
                if (isAdmin) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (profile != null) {
              StatusPill(
                text = "Year ${profile.academicYear} • Sem ${profile.semester}",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(18.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(Modifier.height(14.dp))

      ProfileInfoRow(
        icon = Icons.Outlined.Badge,
        label = "Roll Number / ID",
        value = profile?.rollNumber?.ifBlank { "Not set" } ?: "Not set",
      )
      Spacer(Modifier.height(10.dp))
      ProfileInfoRow(
        icon = Icons.Outlined.School,
        label = "Course & Branch",
        value =
          buildString {
            append(profile?.courseName?.ifBlank { "Not configured" } ?: "Not configured")
            if (!profile?.branchName.isNullOrBlank()) {
              append(" — ")
              append(profile.branchName)
            }
          },
      )
      Spacer(Modifier.height(10.dp))
      ProfileInfoRow(
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        label = "Academic Standing",
        value =
          if (profile != null) "Year ${profile.academicYear}, Semester ${profile.semester}"
          else "Complete profile setup",
      )
      Spacer(Modifier.height(10.dp))
      ProfileInfoRow(
        icon = Icons.Outlined.Email,
        label = "College / Google Email",
        value = profile?.email?.ifBlank { authUser?.email ?: "Unknown" }
          ?: authUser?.email ?: "Unknown",
      )

      Spacer(Modifier.height(18.dp))
      FilledTonalButton(
        onClick = onEditProfile,
        modifier =
          Modifier.fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("edit_profile_button"),
      ) {
        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Edit Academic Profile")
      }
    }

    if (isAdmin) {
      Spacer(Modifier.height(14.dp))
      CampusCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
      ) {
        Text(
          text = "Administrator Controls",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text =
            "Manage courses, branches, subjects, notices, syllabus, study notes, exam papers, and photo albums.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(
          onClick = onOpenAdminDashboard,
          modifier =
            Modifier.fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("profile_open_admin_button"),
        ) {
          Icon(
            Icons.Filled.AdminPanelSettings,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text("Open Admin Dashboard")
        }
      }
    }

    Spacer(Modifier.height(14.dp))
    CampusCard(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
      Text(
        text = "Govt. Institute of Technology (ITG) Gopeshwar",
        style = MaterialTheme.typography.titleSmall,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = "College Core • Academic & Campus Companion",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(14.dp))
      OutlinedButton(
        onClick = onSignOut,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier =
          Modifier.fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("sign_out_button"),
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("Sign Out")
      }
    }
  }
}

@Composable
private fun ProfileInfoRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
) {
  Row(verticalAlignment = Alignment.Top) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(20.dp).padding(top = 2.dp),
    )
    Spacer(Modifier.width(12.dp))
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}
