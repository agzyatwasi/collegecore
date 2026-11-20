package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.CollegeViewModel
import com.example.ui.components.CampusCard
import com.example.ui.components.DestructiveConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  viewModel: CollegeViewModel,
  students: List<StudentProfile>,
  admins: List<AdminAccount>,
  courses: List<AcademicCourse>,
  branches: List<AcademicBranch>,
  subjects: List<AcademicSubject>,
  examCategories: List<ExamCategory>,
  notices: List<Notice>,
  syllabusList: List<SyllabusItem>,
  studyMaterials: List<StudyMaterial>,
  examPapers: List<ExamPaper>,
  galleryAlbums: List<GalleryAlbum>,
  isBusy: Boolean,
  onBack: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var contentSection by remember { mutableStateOf("Notices") }

  // Form dialog states
  var showCourseDialog by remember { mutableStateOf<Pair<Boolean, AcademicCourse?>>(false to null) }
  var showBranchDialog by remember { mutableStateOf<Pair<Boolean, AcademicBranch?>>(false to null) }
  var showSubjectDialog by remember { mutableStateOf<Pair<Boolean, AcademicSubject?>>(false to null) }
  var showExamCatDialog by remember { mutableStateOf<Pair<Boolean, ExamCategory?>>(false to null) }
  var showNoticeDialog by remember { mutableStateOf<Pair<Boolean, Notice?>>(false to null) }
  var showSyllabusDialog by remember { mutableStateOf<Pair<Boolean, SyllabusItem?>>(false to null) }
  var showMaterialDialog by remember { mutableStateOf<Pair<Boolean, StudyMaterial?>>(false to null) }
  var showPaperDialog by remember { mutableStateOf<Pair<Boolean, ExamPaper?>>(false to null) }
  var showAlbumDialog by remember { mutableStateOf<Pair<Boolean, GalleryAlbum?>>(false to null) }
  var showAddPhotoDialog by remember { mutableStateOf(false) }
  var showAddAdminDialog by remember { mutableStateOf(false) }

  // Delete confirmation state (collection, docId, label)
  var pendingDelete by remember { mutableStateOf<Triple<String, String, String>?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Admin Dashboard") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to student view")
          }
        },
        actions = {
          TextButton(onClick = { viewModel.seedDemoData() }, enabled = !isBusy) {
            Text("Seed Demo")
          }
        },
      )
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Structure") })
        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Content") })
      }

      when (selectedTab) {
        // OVERVIEW TAB: Summary counts, Demo Seeder, Quick Publish, Admin Access
        0 -> {
          LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              SectionHeader(title = "Campus Summary")
              Spacer(Modifier.height(8.dp))
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  AdminMetricCard("Students", students.size.toString(), Modifier.weight(1f))
                  AdminMetricCard("Notices", notices.size.toString(), Modifier.weight(1f))
                  AdminMetricCard("Syllabus", syllabusList.size.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  AdminMetricCard("Notes", studyMaterials.size.toString(), Modifier.weight(1f))
                  AdminMetricCard("Papers", examPapers.size.toString(), Modifier.weight(1f))
                  AdminMetricCard("Albums", galleryAlbums.size.toString(), Modifier.weight(1f))
                }
              }
            }

            item {
              CampusCard {
                Text("Demo & Quick Setup", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                  text =
                    "Populate [DEMO] B.Tech (5 configurable branch slots), BCA, exam categories (2 Internal + 1 External), notices, syllabus, notes, papers, and gallery.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                  onClick = { viewModel.seedDemoData() },
                  enabled = !isBusy,
                  modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("seed_demo_button"),
                ) {
                  Text("Seed [DEMO] ITG Gopeshwar Data")
                }
              }
            }

            item {
              SectionHeader(
                title = "Authorized Admins (${admins.size})",
                actionLabel = "+ Add Admin",
                onAction = { showAddAdminDialog = true },
              )
              Text(
                text = "Your User UID: ${viewModel.currentUserId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            if (admins.isEmpty()) {
              item {
                CampusCard {
                  Text(
                    text = "Primary bootstrapped admin access is active. You can authorize additional admins by their Firebase Auth UID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            } else {
              items(admins, key = { it.id }) { admin ->
                CampusCard {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(admin.displayName, style = MaterialTheme.typography.titleSmall)
                      Text(admin.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Text("UID: ${admin.uid}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (admin.uid != viewModel.currentUserId) {
                      IconButton(onClick = {
                        pendingDelete = Triple("admins", admin.id, "Admin ${admin.displayName}")
                      }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove admin", tint = MaterialTheme.colorScheme.error)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // STRUCTURE TAB: Configurable Courses, Branches (5 B.Tech configurable!), Subjects, Exam Categories
        1 -> {
          LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              SectionHeader(
                title = "Courses (${courses.size})",
                actionLabel = "+ Add Course",
                onAction = { showCourseDialog = true to null },
              )
            }
            if (courses.isEmpty()) {
              item { EmptyStateView(title = "No Courses", subtitle = "Add B.Tech, BCA, or other degree programs.") }
            } else {
              items(courses, key = { it.id }) { course ->
                AdminRowCard(
                  title = "${course.name} (${course.code})",
                  subtitle = "${course.durationYears} Years • ${course.totalSemesters} Semesters • ${if (course.hasBranches) "Has Branches" else "No Branches"}",
                  published = course.published,
                  onEdit = { showCourseDialog = true to course },
                  onDelete = { pendingDelete = Triple("academic_courses", course.id, course.name) },
                )
              }
            }

            item {
              Spacer(Modifier.height(6.dp))
              SectionHeader(
                title = "Configurable Branches (${branches.size})",
                actionLabel = "+ Add Branch",
                onAction = { showBranchDialog = true to null },
              )
            }
            if (branches.isEmpty()) {
              item { EmptyStateView(title = "No Branches", subtitle = "Configure your 5 B.Tech branches or course streams here.") }
            } else {
              items(branches, key = { it.id }) { branch ->
                val parent = courses.firstOrNull { it.id == branch.courseId }?.name ?: branch.courseId
                AdminRowCard(
                  title = "${branch.name} (${branch.code})",
                  subtitle = "Course: $parent • Order: ${branch.displayOrder}",
                  published = branch.published,
                  onEdit = { showBranchDialog = true to branch },
                  onDelete = { pendingDelete = Triple("academic_branches", branch.id, branch.name) },
                )
              }
            }

            item {
              Spacer(Modifier.height(6.dp))
              SectionHeader(
                title = "Subjects (${subjects.size})",
                actionLabel = "+ Add Subject",
                onAction = { showSubjectDialog = true to null },
              )
            }
            items(subjects, key = { it.id }) { subject ->
              AdminRowCard(
                title = "${subject.code} — ${subject.name}",
                subtitle = "Year ${subject.academicYear} • Sem ${subject.semester} • ${subject.credits} Credits",
                published = subject.published,
                onEdit = { showSubjectDialog = true to subject },
                onDelete = { pendingDelete = Triple("academic_subjects", subject.id, subject.name) },
              )
            }

            item {
              Spacer(Modifier.height(6.dp))
              SectionHeader(
                title = "Exam Categories (${examCategories.size})",
                actionLabel = "+ Add Category",
                onAction = { showExamCatDialog = true to null },
              )
            }
            items(examCategories, key = { it.id }) { cat ->
              AdminRowCard(
                title = "${cat.name} (${cat.code})",
                subtitle = "Order: ${cat.displayOrder}",
                published = cat.published,
                onEdit = { showExamCatDialog = true to cat },
                onDelete = { pendingDelete = Triple("exam_categories", cat.id, cat.name) },
              )
            }
          }
        }

        // CONTENT TAB: Notices, Syllabus, Notes, Exam Papers, Gallery Albums & Photos
        else -> {
          val sections = listOf("Notices", "Syllabus", "Notes", "Papers", "Gallery")
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(sections) { sec ->
              FilterChip(
                selected = contentSection == sec,
                onClick = { contentSection = sec },
                label = { Text(sec) },
              )
            }
          }

          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            when (contentSection) {
              "Notices" -> {
                item {
                  SectionHeader(
                    title = "Notices (${notices.size})",
                    actionLabel = "+ Publish Notice",
                    onAction = { showNoticeDialog = true to null },
                  )
                }
                items(notices, key = { it.id }) { notice ->
                  AdminRowCard(
                    title = notice.title,
                    subtitle = "${notice.category} • Target: ${notice.targetCourseId} • Sem ${if (notice.targetSemester == 0L) "All" else notice.targetSemester}",
                    published = notice.published,
                    onEdit = { showNoticeDialog = true to notice },
                    onDelete = { pendingDelete = Triple("notices", notice.id, notice.title) },
                  )
                }
              }

              "Syllabus" -> {
                item {
                  SectionHeader(
                    title = "Syllabus (${syllabusList.size})",
                    actionLabel = "+ Add Syllabus",
                    onAction = { showSyllabusDialog = true to null },
                  )
                }
                items(syllabusList, key = { it.id }) { item ->
                  AdminRowCard(
                    title = "${item.subjectCode} — ${item.subjectName}",
                    subtitle = "Year ${item.academicYear} • Sem ${item.semester}",
                    published = item.published,
                    onEdit = { showSyllabusDialog = true to item },
                    onDelete = { pendingDelete = Triple("syllabus_items", item.id, item.subjectName) },
                  )
                }
              }

              "Notes" -> {
                item {
                  SectionHeader(
                    title = "Study Notes (${studyMaterials.size})",
                    actionLabel = "+ Upload Notes",
                    onAction = { showMaterialDialog = true to null },
                  )
                }
                items(studyMaterials, key = { it.id }) { mat ->
                  AdminRowCard(
                    title = mat.title,
                    subtitle = "${mat.subjectName} • ${mat.unit} • ${mat.resourceType}",
                    published = mat.published,
                    onEdit = { showMaterialDialog = true to mat },
                    onDelete = { pendingDelete = Triple("study_materials", mat.id, mat.title) },
                  )
                }
              }

              "Papers" -> {
                item {
                  SectionHeader(
                    title = "Exam Papers (${examPapers.size})",
                    actionLabel = "+ Add Paper",
                    onAction = { showPaperDialog = true to null },
                  )
                }
                items(examPapers, key = { it.id }) { paper ->
                  AdminRowCard(
                    title = paper.title,
                    subtitle = "${paper.subjectName} • ${paper.examCategoryName} (${paper.examYear})",
                    published = paper.published,
                    onEdit = { showPaperDialog = true to paper },
                    onDelete = { pendingDelete = Triple("exam_papers", paper.id, paper.title) },
                  )
                }
              }

              else -> {
                item {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Button(
                      onClick = { showAlbumDialog = true to null },
                      modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    ) {
                      Text("+ New Album")
                    }
                    FilledTonalButton(
                      onClick = { showAddPhotoDialog = true },
                      enabled = galleryAlbums.isNotEmpty(),
                      modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    ) {
                      Text("+ Add Photo")
                    }
                  }
                }
                items(galleryAlbums, key = { it.id }) { album ->
                  AdminRowCard(
                    title = album.title,
                    subtitle = "${album.eventCategory} • ${album.eventDateText} • ${album.photoCount} photos",
                    published = album.published,
                    onEdit = { showAlbumDialog = true to album },
                    onDelete = { pendingDelete = Triple("gallery_albums", album.id, album.title) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Dialogs
  if (showCourseDialog.first) {
    CourseFormDialog(
      initial = showCourseDialog.second,
      onDismiss = { showCourseDialog = false to null },
      onSave = { name, code, yrs, sems, hasBr, pub ->
        viewModel.saveCourse(showCourseDialog.second?.id, name, code, yrs, sems, hasBr, pub) {
          showCourseDialog = false to null
        }
      },
    )
  }

  if (showBranchDialog.first) {
    BranchFormDialog(
      initial = showBranchDialog.second,
      courses = courses,
      onDismiss = { showBranchDialog = false to null },
      onSave = { cId, name, code, ord, pub ->
        viewModel.saveBranch(showBranchDialog.second?.id, cId, name, code, ord, pub) {
          showBranchDialog = false to null
        }
      },
    )
  }

  if (showSubjectDialog.first) {
    SubjectFormDialog(
      initial = showSubjectDialog.second,
      courses = courses,
      branches = branches,
      onDismiss = { showSubjectDialog = false to null },
      onSave = { cId, bId, y, s, name, code, cr, pub ->
        viewModel.saveSubject(showSubjectDialog.second?.id, cId, bId, y, s, name, code, cr, pub) {
          showSubjectDialog = false to null
        }
      },
    )
  }

  if (showExamCatDialog.first) {
    ExamCategoryFormDialog(
      initial = showExamCatDialog.second,
      onDismiss = { showExamCatDialog = false to null },
      onSave = { name, code, ord, pub ->
        viewModel.saveExamCategory(showExamCatDialog.second?.id, name, code, ord, pub) {
          showExamCatDialog = false to null
        }
      },
    )
  }

  if (showNoticeDialog.first) {
    NoticeFormDialog(
      initial = showNoticeDialog.second,
      courses = courses,
      branches = branches,
      onDismiss = { showNoticeDialog = false to null },
      onSave = { t, m, cat, aUrl, aName, tC, tB, tY, tS, pub, imp, auth ->
        viewModel.saveNotice(
          showNoticeDialog.second?.id,
          t,
          m,
          cat,
          aUrl,
          aName,
          tC,
          tB,
          tY,
          tS,
          pub,
          imp,
          auth,
        ) {
          showNoticeDialog = false to null
        }
      },
    )
  }

  if (showSyllabusDialog.first) {
    SyllabusFormDialog(
      initial = showSyllabusDialog.second,
      courses = courses,
      branches = branches,
      onDismiss = { showSyllabusDialog = false to null },
      onSave = { cId, bId, y, s, sName, sCode, desc, pdf, sizeLbl, pub ->
        viewModel.saveSyllabus(
          showSyllabusDialog.second?.id,
          cId,
          bId,
          y,
          s,
          sCode,
          sName,
          sCode,
          desc,
          pdf,
          sizeLbl,
          pub,
        ) {
          showSyllabusDialog = false to null
        }
      },
    )
  }

  if (showMaterialDialog.first) {
    StudyMaterialFormDialog(
      initial = showMaterialDialog.second,
      courses = courses,
      branches = branches,
      onDismiss = { showMaterialDialog = false to null },
      onSave = { t, desc, cId, bId, y, s, sName, unit, rType, fUrl, upBy, pub ->
        viewModel.saveStudyMaterial(
          showMaterialDialog.second?.id,
          t,
          desc,
          cId,
          bId,
          y,
          s,
          "GEN",
          sName,
          unit,
          rType,
          fUrl,
          upBy,
          pub,
        ) {
          showMaterialDialog = false to null
        }
      },
    )
  }

  if (showPaperDialog.first) {
    ExamPaperFormDialog(
      initial = showPaperDialog.second,
      courses = courses,
      branches = branches,
      examCategories = examCategories,
      onDismiss = { showPaperDialog = false to null },
      onSave = { t, cId, bId, y, s, sName, catId, catName, exYr, pdf, pub ->
        viewModel.saveExamPaper(
          showPaperDialog.second?.id,
          t,
          cId,
          bId,
          y,
          s,
          "GEN",
          sName,
          catId,
          catName,
          exYr,
          pdf,
          pub,
        ) {
          showPaperDialog = false to null
        }
      },
    )
  }

  if (showAlbumDialog.first) {
    GalleryAlbumFormDialog(
      initial = showAlbumDialog.second,
      onDismiss = { showAlbumDialog = false to null },
      onSave = { t, d, cat, dt, cov, pub ->
        viewModel.saveGalleryAlbum(
          showAlbumDialog.second?.id,
          t,
          d,
          cat,
          dt,
          cov,
          showAlbumDialog.second?.photoCount ?: 0L,
          pub,
        ) {
          showAlbumDialog = false to null
        }
      },
    )
  }

  if (showAddPhotoDialog) {
    AddPhotoToAlbumDialog(
      albums = galleryAlbums,
      onDismiss = { showAddPhotoDialog = false },
      onSave = { aId, cap, img, th, pub ->
        viewModel.addPhotoToAlbum(aId, cap, img, th, pub) {
          showAddPhotoDialog = false
        }
      },
    )
  }

  if (showAddAdminDialog) {
    AddAdminDialog(
      onDismiss = { showAddAdminDialog = false },
      onSave = { uid, email, name ->
        viewModel.addAdmin(uid, email, name) { showAddAdminDialog = false }
      },
    )
  }

  if (pendingDelete != null) {
    val (col, docId, label) = pendingDelete!!
    DestructiveConfirmDialog(
      title = "Delete $label?",
      message = "This action permanently removes \"$label\" from Cloud Firestore.",
      onConfirm = {
        viewModel.deleteDocument(col, docId, label)
        pendingDelete = null
      },
      onDismiss = { pendingDelete = null },
    )
  }
}

@Composable
private fun AdminMetricCard(
  label: String,
  count: String,
  modifier: Modifier = Modifier,
) {
  CampusCard(modifier = modifier) {
    Text(
      text = count,
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun AdminRowCard(
  title: String,
  subtitle: String,
  published: Boolean,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  CampusCard(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          StatusPill(
            text = if (published) "PUBLISHED" else "DRAFT",
            containerColor =
              if (published) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.tertiaryContainer,
            contentColor =
              if (published) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
        Spacer(Modifier.height(6.dp))
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      IconButton(onClick = onEdit, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)) {
        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
      }
      IconButton(onClick = onDelete, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)) {
        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
      }
    }
  }
}
