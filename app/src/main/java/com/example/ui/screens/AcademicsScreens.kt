package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
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
import com.example.model.*
import com.example.ui.components.CampusCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SimpleDropdownField
import com.example.ui.components.StatusPill
import com.example.ui.components.formatTimestamp
import com.example.ui.components.openExternalUrl

@Composable
fun AcademicsScreen(
  initialTab: Int,
  profile: StudentProfile?,
  isAdmin: Boolean,
  courses: List<AcademicCourse>,
  branches: List<AcademicBranch>,
  examCategories: List<ExamCategory>,
  syllabusList: List<SyllabusItem>,
  studyMaterials: List<StudyMaterial>,
  examPapers: List<ExamPaper>,
) {
  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
  var searchQuery by remember { mutableStateOf("") }

  var selectedCourseId by remember(profile?.courseId) {
    mutableStateOf(profile?.courseId?.ifBlank { "ALL" } ?: "ALL")
  }
  var selectedBranchId by remember(profile?.branchId) {
    mutableStateOf(profile?.branchId?.ifBlank { "ALL" } ?: "ALL")
  }
  var selectedSemester by remember(profile?.semester) {
    mutableLongStateOf(profile?.semester ?: 0L)
  }
  var selectedExamCategoryId by remember { mutableStateOf("ALL") }

  var courseMenuOpen by remember { mutableStateOf(false) }
  var branchMenuOpen by remember { mutableStateOf(false) }
  var semMenuOpen by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val selectedCourse = courses.firstOrNull { it.id == selectedCourseId }
  val availableBranches =
    remember(branches, selectedCourseId) {
      if (selectedCourseId == "ALL") branches
      else branches.filter { it.courseId == selectedCourseId }
    }
  val maxSemesters = selectedCourse?.totalSemesters ?: 8L

  Column(modifier = Modifier.fillMaxSize()) {
    TabRow(selectedTabIndex = selectedTab) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Syllabus") },
        modifier = Modifier.testTag("academics_tab_syllabus"),
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Study Notes") },
        modifier = Modifier.testTag("academics_tab_notes"),
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("Exam Papers") },
        modifier = Modifier.testTag("academics_tab_papers"),
      )
    }

    // Search + Hierarchy Filter Header
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search subject, title, code, or unit...") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Outlined.Close, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("academics_search_input"),
      )
      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val courseOptions = listOf(AcademicCourse(id = "ALL", name = "All Courses")) + courses
        SimpleDropdownField(
          label = "Course",
          selectedLabel = courseOptions.firstOrNull { it.id == selectedCourseId }?.name ?: "All Courses",
          options = courseOptions,
          optionLabel = { it.name },
          onSelect = { c ->
            selectedCourseId = c.id
            selectedBranchId = "ALL"
            selectedSemester = 0L
          },
          expanded = courseMenuOpen,
          onExpandedChange = { courseMenuOpen = it },
          modifier = Modifier.weight(1f),
        )

        val semOptions = listOf(0L) + (1L..maxSemesters).toList()
        SimpleDropdownField(
          label = "Semester",
          selectedLabel = if (selectedSemester == 0L) "All Sems" else "Sem $selectedSemester",
          options = semOptions,
          optionLabel = { if (it == 0L) "All Semesters" else "Semester $it" },
          onSelect = { selectedSemester = it },
          expanded = semMenuOpen,
          onExpandedChange = { semMenuOpen = it },
          modifier = Modifier.weight(1f),
        )
      }

      if (availableBranches.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        val branchOptions = listOf(AcademicBranch(id = "ALL", name = "All Branches")) + availableBranches
        SimpleDropdownField(
          label = "Branch",
          selectedLabel = branchOptions.firstOrNull { it.id == selectedBranchId }?.name ?: "All Branches",
          options = branchOptions,
          optionLabel = { it.name },
          onSelect = { selectedBranchId = it.id },
          expanded = branchMenuOpen,
          onExpandedChange = { branchMenuOpen = it },
          modifier = Modifier.fillMaxWidth(),
        )
      }

      if (selectedTab == 2 && examCategories.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = selectedExamCategoryId == "ALL",
              onClick = { selectedExamCategoryId = "ALL" },
              label = { Text("All Exams") },
            )
          }
          items(examCategories, key = { it.id }) { cat ->
            FilterChip(
              selected = selectedExamCategoryId == cat.id,
              onClick = { selectedExamCategoryId = cat.id },
              label = { Text(cat.name) },
            )
          }
        }
      }
    }

    val query = searchQuery.trim().lowercase()

    when (selectedTab) {
      0 -> {
        val filtered =
          syllabusList
            .filter { isAdmin || it.published }
            .filter { selectedCourseId == "ALL" || it.courseId == selectedCourseId }
            .filter { selectedBranchId == "ALL" || it.branchId.isBlank() || it.branchId == selectedBranchId }
            .filter { selectedSemester == 0L || it.semester == selectedSemester }
            .filter {
              query.isEmpty() ||
                it.subjectName.lowercase().contains(query) ||
                it.subjectCode.lowercase().contains(query) ||
                it.description.lowercase().contains(query)
            }

        if (filtered.isEmpty()) {
          EmptyStateView(
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            title = "No Syllabus Entries",
            subtitle = "Try clearing filters or ask your admin to add syllabus files for this semester.",
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(filtered, key = { it.id }) { item ->
              CampusCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  StatusPill(text = item.subjectCode)
                  StatusPill(
                    text = "Year ${item.academicYear} • Sem ${item.semester}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
                Spacer(Modifier.height(8.dp))
                Text(item.subjectName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                  text = item.description,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.pdfUrl.isNotBlank()) {
                  Spacer(Modifier.height(12.dp))
                  FilledTonalButton(
                    onClick = { openExternalUrl(context, item.pdfUrl) },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                  ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                      if (item.fileSizeLabel.isNotBlank()) "Open Syllabus PDF (${item.fileSizeLabel})"
                      else "Open / Download Syllabus PDF"
                    )
                  }
                }
              }
            }
          }
        }
      }

      1 -> {
        val filtered =
          studyMaterials
            .filter { isAdmin || it.published }
            .filter { selectedCourseId == "ALL" || it.courseId == selectedCourseId }
            .filter { selectedBranchId == "ALL" || it.branchId.isBlank() || it.branchId == selectedBranchId }
            .filter { selectedSemester == 0L || it.semester == selectedSemester }
            .filter {
              query.isEmpty() ||
                it.title.lowercase().contains(query) ||
                it.subjectName.lowercase().contains(query) ||
                it.unit.lowercase().contains(query)
            }

        if (filtered.isEmpty()) {
          EmptyStateView(
            icon = Icons.Outlined.Description,
            title = "No Study Notes Found",
            subtitle = "No notes or materials match your selected course, semester, or search query.",
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(filtered, key = { it.id }) { mat ->
              CampusCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  StatusPill(text = mat.resourceType)
                  StatusPill(
                    text = mat.unit,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                  Spacer(Modifier.weight(1f))
                  Text(
                    text = formatTimestamp(mat.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(Modifier.height(8.dp))
                Text(mat.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                  text = "${mat.subjectName} • Sem ${mat.semester}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                if (mat.description.isNotBlank()) {
                  Spacer(Modifier.height(6.dp))
                  Text(
                    text = mat.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (mat.resourceType == "IMAGE" && mat.fileUrl.isNotBlank()) {
                  Spacer(Modifier.height(10.dp))
                  AsyncImage(
                    model =
                      ImageRequest.Builder(context)
                        .data(mat.fileUrl)
                        .crossfade(true)
                        .size(720)
                        .build(),
                    contentDescription = mat.title,
                    contentScale = ContentScale.Crop,
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.small),
                  )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "Uploaded by ${mat.uploadedByName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  FilledTonalButton(
                    onClick = { openExternalUrl(context, mat.fileUrl) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                  ) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (mat.resourceType == "PDF") "Open PDF" else "Open")
                  }
                }
              }
            }
          }
        }
      }

      else -> {
        val filtered =
          examPapers
            .filter { isAdmin || it.published }
            .filter { selectedCourseId == "ALL" || it.courseId == selectedCourseId }
            .filter { selectedBranchId == "ALL" || it.branchId.isBlank() || it.branchId == selectedBranchId }
            .filter { selectedSemester == 0L || it.semester == selectedSemester }
            .filter { selectedExamCategoryId == "ALL" || it.examCategoryId == selectedExamCategoryId }
            .filter {
              query.isEmpty() ||
                it.title.lowercase().contains(query) ||
                it.subjectName.lowercase().contains(query) ||
                it.examCategoryName.lowercase().contains(query) ||
                it.examYear.toString().contains(query)
            }

        if (filtered.isEmpty()) {
          EmptyStateView(
            icon = Icons.Outlined.Quiz,
            title = "No Exam Papers Found",
            subtitle = "Previous year internal and external exam papers will appear here.",
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(filtered, key = { it.id }) { paper ->
              CampusCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  StatusPill(
                    text = paper.examCategoryName,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                  StatusPill(text = "${paper.examYear}")
                  Spacer(Modifier.weight(1f))
                  Text(
                    text = "Sem ${paper.semester}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
                Spacer(Modifier.height(8.dp))
                Text(paper.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                  text = paper.subjectName,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                  onClick = { openExternalUrl(context, paper.pdfUrl) },
                  modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                ) {
                  Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("View / Download Paper PDF")
                }
              }
            }
          }
        }
      }
    }
  }
}
