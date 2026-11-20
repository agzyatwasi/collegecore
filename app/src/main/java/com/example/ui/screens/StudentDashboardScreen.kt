package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Notice
import com.example.model.StudentProfile
import com.example.model.StudyMaterial
import com.example.ui.components.CampusCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.formatTimestamp

@Composable
fun StudentDashboardScreen(
  profile: StudentProfile?,
  isAdmin: Boolean,
  notices: List<Notice>,
  readNoticeIds: Set<String>,
  studyMaterials: List<StudyMaterial>,
  onNavigateNotices: () -> Unit,
  onNavigateAcademicsTab: (tabIndex: Int) -> Unit,
  onNavigateGallery: () -> Unit,
  onNavigateProfile: () -> Unit,
  onOpenNoticeDetail: (Notice) -> Unit,
  onOpenAdminDashboard: () -> Unit,
  onSeedDemoData: () -> Unit,
  isBusy: Boolean,
) {
  val relevantNotices =
    if (isAdmin) notices else notices.filter { it.published && it.matchesStudent(profile) }
  val unreadCount = relevantNotices.count { it.id !in readNoticeIds }

  val relevantNotes =
    if (isAdmin) studyMaterials
    else {
      studyMaterials.filter { mat ->
        mat.published &&
          (profile == null ||
            (mat.courseId == profile.courseId &&
              (mat.branchId.isBlank() || mat.branchId == profile.branchId)))
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Welcome & Profile Summary Banner
    item {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
              Brush.linearGradient(
                listOf(Color(0xFF152B52), Color(0xFF284E91))
              )
            )
            .padding(20.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            StatusPill(
              text = "ITG GOPESHWAR",
              containerColor = Color(0xFFF59E0B),
              contentColor = Color(0xFF1E1B18),
            )
            if (unreadCount > 0) {
              StatusPill(
                text = "$unreadCount Unread Notice${if (unreadCount > 1) "s" else ""}",
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White,
              )
            }
          }
          Spacer(Modifier.height(12.dp))
          Text(
            text = "Welcome, ${profile?.fullName?.ifBlank { "Student" } ?: "Campus Member"}",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text =
              if (profile != null) {
                buildString {
                  append(profile.courseName)
                  if (profile.branchName.isNotBlank()) append(" • ${profile.branchName}")
                  append(" • Year ${profile.academicYear}, Sem ${profile.semester}")
                }
              } else {
                "Govt. Institute of Technology, Gopeshwar"
              },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
          )
          if (profile != null && profile.rollNumber.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
              text = "ID: ${profile.rollNumber}",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.78f),
            )
          }
        }
      }
    }

    // Admin Quick Bar if Admin
    if (isAdmin) {
      item {
        CampusCard(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
          borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Administrator Mode Active",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "Publish notices, manage courses, branches, syllabus, notes & gallery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(Modifier.width(12.dp))
            Button(
              onClick = onOpenAdminDashboard,
              modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("home_open_admin_button"),
            ) {
              Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Admin")
            }
          }
          if (notices.isEmpty() && studyMaterials.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(
              onClick = onSeedDemoData,
              enabled = !isBusy,
              modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            ) {
              Text("Seed [DEMO] ITG Gopeshwar College Data")
            }
          }
        }
      }
    }

    // 2. Quick Access Grid (Notices, Syllabus, Notes, Previous Papers, Gallery, Profile)
    item {
      SectionHeader(title = "Quick Access")
      Spacer(Modifier.height(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickAccessTile(
            title = "Notices",
            subtitle = if (unreadCount > 0) "$unreadCount new" else "${relevantNotices.size} updates",
            icon = Icons.Outlined.Campaign,
            accent = Color(0xFFDC2626),
            onClick = onNavigateNotices,
            modifier = Modifier.weight(1f).testTag("quick_notices"),
          )
          QuickAccessTile(
            title = "Syllabus",
            subtitle = "Course & Sem",
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            accent = Color(0xFF1D4ED8),
            onClick = { onNavigateAcademicsTab(0) },
            modifier = Modifier.weight(1f).testTag("quick_syllabus"),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickAccessTile(
            title = "Study Notes",
            subtitle = "${relevantNotes.size} resources",
            icon = Icons.Outlined.Description,
            accent = Color(0xFF0F766E),
            onClick = { onNavigateAcademicsTab(1) },
            modifier = Modifier.weight(1f).testTag("quick_notes"),
          )
          QuickAccessTile(
            title = "Exam Papers",
            subtitle = "Internal & Ext",
            icon = Icons.Outlined.Quiz,
            accent = Color(0xFFB45309),
            onClick = { onNavigateAcademicsTab(2) },
            modifier = Modifier.weight(1f).testTag("quick_papers"),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickAccessTile(
            title = "Photo Gallery",
            subtitle = "Campus events",
            icon = Icons.Outlined.PhotoLibrary,
            accent = Color(0xFF7C3AED),
            onClick = onNavigateGallery,
            modifier = Modifier.weight(1f).testTag("quick_gallery"),
          )
          QuickAccessTile(
            title = "My Profile",
            subtitle = profile?.rollNumber?.ifBlank { "Account" } ?: "Account",
            icon = Icons.Outlined.Person,
            accent = Color(0xFF334155),
            onClick = onNavigateProfile,
            modifier = Modifier.weight(1f).testTag("quick_profile"),
          )
        }
      }
    }

    // 3. Latest Notices
    item {
      SectionHeader(
        title = "Latest Notices",
        actionLabel = "View All",
        onAction = onNavigateNotices,
      )
    }

    if (relevantNotices.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Outlined.Campaign,
          title = "No Notices Yet",
          subtitle = "Official notices for your course and semester will appear here.",
        )
      }
    } else {
      items(relevantNotices.take(3), key = { it.id }) { notice ->
        val isUnread = notice.id !in readNoticeIds
        CampusCard(
          onClick = { onOpenNoticeDetail(notice) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (isUnread) {
              Box(
                modifier =
                  Modifier.size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
              )
              Spacer(Modifier.width(8.dp))
            }
            StatusPill(
              text = notice.category,
              containerColor =
                if (notice.important) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
              contentColor =
                if (notice.important) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.weight(1f))
            Text(
              text = formatTimestamp(notice.createdAt),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.height(8.dp))
          Text(
            text = notice.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = notice.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    // 4. Recently Added Study Material
    item {
      SectionHeader(
        title = "Recent Study Material",
        actionLabel = "Browse Notes",
        onAction = { onNavigateAcademicsTab(1) },
      )
    }

    if (relevantNotes.isEmpty()) {
      item {
        EmptyStateView(
          icon = Icons.Outlined.Description,
          title = "No Study Material Yet",
          subtitle = "Lecture notes and unit PDFs added by faculty will show here.",
        )
      }
    } else {
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(relevantNotes.take(5), key = { it.id }) { material ->
            CampusCard(
              onClick = { onNavigateAcademicsTab(1) },
              modifier = Modifier.width(240.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                StatusPill(text = material.resourceType)
                Text(
                  text = material.unit,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(Modifier.height(8.dp))
              Text(
                text = material.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(Modifier.height(4.dp))
              Text(
                text = material.subjectName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(Modifier.height(6.dp))
              Text(
                text = "By ${material.uploadedByName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickAccessTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CampusCard(
    onClick = onClick,
    modifier = modifier.defaultMinSize(minHeight = 96.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier.size(40.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accent,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
