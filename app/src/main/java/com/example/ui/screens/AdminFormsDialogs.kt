package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.CampusCard
import com.example.ui.components.SimpleDropdownField
import com.example.ui.components.StatusPill

@Composable
fun CourseFormDialog(
  initial: AcademicCourse?,
  onDismiss: () -> Unit,
  onSave: (name: String, code: String, years: Long, sems: Long, hasBranches: Boolean, published: Boolean) -> Unit,
) {
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var code by remember { mutableStateOf(initial?.code ?: "") }
  var yearsText by remember { mutableStateOf((initial?.durationYears ?: 4L).toString()) }
  var semsText by remember { mutableStateOf((initial?.totalSemesters ?: 8L).toString()) }
  var hasBranches by remember { mutableStateOf(initial?.hasBranches ?: true) }
  var published by remember { mutableStateOf(initial?.published ?: true) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Course" else "Edit Course") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name (e.g., B.Tech)") }, singleLine = true)
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code (e.g., BTECH)") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = yearsText, onValueChange = { yearsText = it }, label = { Text("Years") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = semsText, onValueChange = { semsText = it }, label = { Text("Semesters") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = hasBranches, onCheckedChange = { hasBranches = it })
          Text("Course has branches")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = published, onCheckedChange = { published = it })
          Spacer(Modifier.width(8.dp))
          Text("Published to students")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        val yrs = yearsText.toLongOrNull() ?: 0L
        val sems = semsText.toLongOrNull() ?: 0L
        when {
          name.trim().length < 2 -> error = "Enter a valid course name."
          code.trim().length < 2 -> error = "Enter a course code."
          yrs !in 1L..6L -> error = "Duration must be 1–6 years."
          sems !in 1L..12L -> error = "Semesters must be 1–12."
          else -> onSave(name.trim(), code.trim(), yrs, sems, hasBranches, published)
        }
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun BranchFormDialog(
  initial: AcademicBranch?,
  courses: List<AcademicCourse>,
  onDismiss: () -> Unit,
  onSave: (courseId: String, name: String, code: String, order: Long, published: Boolean) -> Unit,
) {
  var selectedCourse by remember {
    mutableStateOf(courses.firstOrNull { it.id == initial?.courseId } ?: courses.firstOrNull())
  }
  var courseMenu by remember { mutableStateOf(false) }
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var code by remember { mutableStateOf(initial?.code ?: "") }
  var orderText by remember { mutableStateOf((initial?.displayOrder ?: 1L).toString()) }
  var published by remember { mutableStateOf(initial?.published ?: true) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Branch" else "Edit Branch") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (courses.isNotEmpty()) {
          SimpleDropdownField(
            label = "Parent Course",
            selectedLabel = selectedCourse?.name ?: "Select Course",
            options = courses,
            optionLabel = { it.name },
            onSelect = { selectedCourse = it },
            expanded = courseMenu,
            onExpandedChange = { courseMenu = it },
          )
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Branch Name") }, singleLine = true)
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Branch Code") }, singleLine = true)
        OutlinedTextField(value = orderText, onValueChange = { orderText = it }, label = { Text("Display Order") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = published, onCheckedChange = { published = it })
          Spacer(Modifier.width(8.dp))
          Text("Published")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        val cId = selectedCourse?.id.orEmpty()
        val ord = orderText.toLongOrNull() ?: 0L
        when {
          cId.isBlank() -> error = "Create or select a course first."
          name.trim().length < 2 -> error = "Enter a valid branch name."
          code.trim().isEmpty() -> error = "Enter a branch code."
          else -> onSave(cId, name.trim(), code.trim(), ord, published)
        }
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun SubjectFormDialog(
  initial: AcademicSubject?,
  courses: List<AcademicCourse>,
  branches: List<AcademicBranch>,
  onDismiss: () -> Unit,
  onSave: (courseId: String, branchId: String, year: Long, sem: Long, name: String, code: String, credits: Long, published: Boolean) -> Unit,
) {
  var selectedCourse by remember {
    mutableStateOf(courses.firstOrNull { it.id == initial?.courseId } ?: courses.firstOrNull())
  }
  val courseBranches = remember(selectedCourse, branches) { branches.filter { it.courseId == selectedCourse?.id } }
  var selectedBranch by remember(courseBranches) {
    mutableStateOf(courseBranches.firstOrNull { it.id == initial?.branchId } ?: courseBranches.firstOrNull())
  }
  var courseMenu by remember { mutableStateOf(false) }
  var branchMenu by remember { mutableStateOf(false) }
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var code by remember { mutableStateOf(initial?.code ?: "") }
  var yearText by remember { mutableStateOf((initial?.academicYear ?: 1L).toString()) }
  var semText by remember { mutableStateOf((initial?.semester ?: 1L).toString()) }
  var creditsText by remember { mutableStateOf((initial?.credits ?: 4L).toString()) }
  var published by remember { mutableStateOf(initial?.published ?: true) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Subject" else "Edit Subject") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (courses.isNotEmpty()) {
          SimpleDropdownField(
            label = "Course",
            selectedLabel = selectedCourse?.name ?: "Select Course",
            options = courses,
            optionLabel = { it.name },
            onSelect = { selectedCourse = it },
            expanded = courseMenu,
            onExpandedChange = { courseMenu = it },
          )
        }
        if (courseBranches.isNotEmpty()) {
          SimpleDropdownField(
            label = "Branch",
            selectedLabel = selectedBranch?.name ?: "All / General",
            options = courseBranches,
            optionLabel = { it.name },
            onSelect = { selectedBranch = it },
            expanded = branchMenu,
            onExpandedChange = { branchMenu = it },
          )
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name") }, singleLine = true)
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Subject Code") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = yearText, onValueChange = { yearText = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = semText, onValueChange = { semText = it }, label = { Text("Sem") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = creditsText, onValueChange = { creditsText = it }, label = { Text("Credits") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = published, onCheckedChange = { published = it })
          Spacer(Modifier.width(8.dp))
          Text("Published")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        val cId = selectedCourse?.id.orEmpty()
        val y = yearText.toLongOrNull() ?: 1L
        val s = semText.toLongOrNull() ?: 1L
        val cr = creditsText.toLongOrNull() ?: 4L
        when {
          cId.isBlank() -> error = "Create or select a course first."
          name.trim().length < 2 -> error = "Enter subject name."
          code.trim().length < 2 -> error = "Enter subject code."
          else -> onSave(cId, selectedBranch?.id.orEmpty(), y, s, name.trim(), code.trim(), cr, published)
        }
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun ExamCategoryFormDialog(
  initial: ExamCategory?,
  onDismiss: () -> Unit,
  onSave: (name: String, code: String, order: Long, published: Boolean) -> Unit,
) {
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var code by remember { mutableStateOf(initial?.code ?: "") }
  var orderText by remember { mutableStateOf((initial?.displayOrder ?: 1L).toString()) }
  var published by remember { mutableStateOf(initial?.published ?: true) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Exam Category" else "Edit Exam Category") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (e.g., Internal Exam 1)") }, singleLine = true)
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code (e.g., INT1)") }, singleLine = true)
        OutlinedTextField(value = orderText, onValueChange = { orderText = it }, label = { Text("Display Order") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = published, onCheckedChange = { published = it })
          Spacer(Modifier.width(8.dp))
          Text("Published")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        when {
          name.trim().length < 2 -> error = "Enter category name."
          code.trim().length < 2 -> error = "Enter category code."
          else -> onSave(name.trim(), code.trim(), orderText.toLongOrNull() ?: 1L, published)
        }
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun NoticeFormDialog(
  initial: Notice?,
  courses: List<AcademicCourse>,
  branches: List<AcademicBranch>,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    message: String,
    category: String,
    attachmentUrl: String,
    attachmentName: String,
    targetCourseId: String,
    targetBranchId: String,
    targetYear: Long,
    targetSemester: Long,
    published: Boolean,
    important: Boolean,
    authorName: String,
  ) -> Unit,
) {
  var title by remember { mutableStateOf(initial?.title ?: "") }
  var message by remember { mutableStateOf(initial?.message ?: "") }
  var category by remember { mutableStateOf(initial?.category ?: "General") }
  var attachmentUrl by remember { mutableStateOf(initial?.attachmentUrl ?: "") }
  var attachmentName by remember { mutableStateOf(initial?.attachmentName ?: "") }
  var targetCourseId by remember { mutableStateOf(initial?.targetCourseId ?: "ALL") }
  var targetBranchId by remember { mutableStateOf(initial?.targetBranchId ?: "ALL") }
  var targetYear by remember { mutableLongStateOf(initial?.targetYear ?: 0L) }
  var targetSemester by remember { mutableLongStateOf(initial?.targetSemester ?: 0L) }
  var published by remember { mutableStateOf(initial?.published ?: true) }
  var important by remember { mutableStateOf(initial?.important ?: false) }
  var authorName by remember { mutableStateOf(initial?.authorName ?: "ITG Administration") }
  var showPreview by remember { mutableStateOf(false) }
  var catMenu by remember { mutableStateOf(false) }
  var courseMenu by remember { mutableStateOf(false) }
  var branchMenu by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  val categoryOptions = listOf("General", "Academic", "Exam", "Event", "Urgent")
  val courseOptions = listOf(AcademicCourse(id = "ALL", name = "All Students")) + courses
  val branchOptions =
    listOf(AcademicBranch(id = "ALL", name = "All Branches")) +
      branches.filter { targetCourseId == "ALL" || it.courseId == targetCourseId }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (showPreview) "Preview Before Publishing" else if (initial == null) "Create Notice" else "Edit Notice") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (showPreview) {
          CampusCard(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              StatusPill(text = category)
              if (important) StatusPill(text = "IMPORTANT", containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
              StatusPill(text = if (published) "PUBLIC" else "DRAFT")
            }
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("By $authorName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notice Title") }, singleLine = true)
          OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, minLines = 3, maxLines = 6)
          SimpleDropdownField(
            label = "Category",
            selectedLabel = category,
            options = categoryOptions,
            optionLabel = { it },
            onSelect = { category = it },
            expanded = catMenu,
            onExpandedChange = { catMenu = it },
          )
          SimpleDropdownField(
            label = "Target Course",
            selectedLabel = courseOptions.firstOrNull { it.id == targetCourseId }?.name ?: "All Students",
            options = courseOptions,
            optionLabel = { it.name },
            onSelect = {
              targetCourseId = it.id
              targetBranchId = "ALL"
            },
            expanded = courseMenu,
            onExpandedChange = { courseMenu = it },
          )
          SimpleDropdownField(
            label = "Target Branch",
            selectedLabel = branchOptions.firstOrNull { it.id == targetBranchId }?.name ?: "All Branches",
            options = branchOptions,
            optionLabel = { it.name },
            onSelect = { targetBranchId = it.id },
            expanded = branchMenu,
            onExpandedChange = { branchMenu = it },
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = targetYear.toString(),
              onValueChange = { targetYear = it.toLongOrNull()?.coerceIn(0L, 6L) ?: 0L },
              label = { Text("Year (0=All)") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = targetSemester.toString(),
              onValueChange = { targetSemester = it.toLongOrNull()?.coerceIn(0L, 12L) ?: 0L },
              label = { Text("Sem (0=All)") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          OutlinedTextField(value = attachmentUrl, onValueChange = { attachmentUrl = it }, label = { Text("Attachment URL (Optional)") }, singleLine = true)
          OutlinedTextField(value = attachmentName, onValueChange = { attachmentName = it }, label = { Text("Attachment Label (Optional)") }, singleLine = true)
          OutlinedTextField(value = authorName, onValueChange = { authorName = it }, label = { Text("Issued By") }, singleLine = true)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = important, onCheckedChange = { important = it })
            Text("Mark as Important")
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = published, onCheckedChange = { published = it })
            Spacer(Modifier.width(8.dp))
            Text("Publish immediately")
          }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { showPreview = !showPreview }) {
          Text(if (showPreview) "Edit" else "Preview")
        }
        Button(onClick = {
          val u = attachmentUrl.trim()
          when {
            title.trim().length < 3 -> error = "Title must be at least 3 characters."
            message.trim().length < 3 -> error = "Message must be at least 3 characters."
            u.isNotEmpty() && !u.startsWith("http://") && !u.startsWith("https://") ->
              error = "Attachment URL must start with http:// or https://"
            else ->
              onSave(
                title.trim(),
                message.trim(),
                category,
                u,
                attachmentName.trim(),
                targetCourseId,
                targetBranchId,
                targetYear,
                targetSemester,
                published,
                important,
                authorName.trim(),
              )
          }
        }) { Text(if (published) "Publish" else "Save Draft") }
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun SyllabusFormDialog(
  initial: SyllabusItem?,
  courses: List<AcademicCourse>,
  branches: List<AcademicBranch>,
  onDismiss: () -> Unit,
  onSave: (
    courseId: String,
    branchId: String,
    year: Long,
    sem: Long,
    subjectName: String,
    subjectCode: String,
    description: String,
    pdfUrl: String,
    fileSizeLabel: String,
    published: Boolean,
  ) -> Unit,
) {
  var selectedCourse by remember {
    mutableStateOf(courses.firstOrNull { it.id == initial?.courseId } ?: courses.firstOrNull())
  }
  val courseBranches = remember(selectedCourse, branches) { branches.filter { it.courseId == selectedCourse?.id } }
  var selectedBranch by remember(courseBranches) {
    mutableStateOf(courseBranches.firstOrNull { it.id == initial?.branchId } ?: courseBranches.firstOrNull())
  }
  var courseMenu by remember { mutableStateOf(false) }
  var branchMenu by remember { mutableStateOf(false) }
  var subjectName by remember { mutableStateOf(initial?.subjectName ?: "") }
  var subjectCode by remember { mutableStateOf(initial?.subjectCode ?: "") }
  var yearText by remember { mutableStateOf((initial?.academicYear ?: 1L).toString()) }
  var semText by remember { mutableStateOf((initial?.semester ?: 1L).toString()) }
  var description by remember { mutableStateOf(initial?.description ?: "") }
  var pdfUrl by remember { mutableStateOf(initial?.pdfUrl ?: "") }
  var sizeLabel by remember { mutableStateOf(initial?.fileSizeLabel ?: "") }
  var published by remember { mutableStateOf(initial?.published ?: true) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Syllabus" else "Edit Syllabus") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (courses.isNotEmpty()) {
          SimpleDropdownField(
            label = "Course",
            selectedLabel = selectedCourse?.name ?: "Select Course",
            options = courses,
            optionLabel = { it.name },
            onSelect = { selectedCourse = it },
            expanded = courseMenu,
            onExpandedChange = { courseMenu = it },
          )
        }
        if (courseBranches.isNotEmpty()) {
          SimpleDropdownField(
            label = "Branch",
            selectedLabel = selectedBranch?.name ?: "Select Branch",
            options = courseBranches,
            optionLabel = { it.name },
            onSelect = { selectedBranch = it },
            expanded = branchMenu,
            onExpandedChange = { branchMenu = it },
          )
        }
        OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject Name") }, singleLine = true)
        OutlinedTextField(value = subjectCode, onValueChange = { subjectCode = it }, label = { Text("Subject Code") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = yearText, onValueChange = { yearText = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = semText, onValueChange = { semText = it }, label = { Text("Semester") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Topics / Description") }, minLines = 2, maxLines = 5)
        OutlinedTextField(value = pdfUrl, onValueChange = { pdfUrl = it }, label = { Text("Syllabus PDF URL (https://...)") }, singleLine = true)
        OutlinedTextField(value = sizeLabel, onValueChange = { sizeLabel = it }, label = { Text("File Size Label (e.g., 1.2 MB)") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = published, onCheckedChange = { published = it })
          Spacer(Modifier.width(8.dp))
          Text("Published")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        val cId = selectedCourse?.id.orEmpty()
        val u = pdfUrl.trim()
        when {
          cId.isBlank() -> error = "Create or select a course first."
          subjectName.trim().length < 2 -> error = "Subject name is required."
          subjectCode.trim().length < 2 -> error = "Subject code is required."
          description.trim().length < 3 -> error = "Description must be at least 3 characters."
          u.isNotEmpty() && !u.startsWith("http://") && !u.startsWith("https://") ->
            error = "PDF URL must start with http:// or https://"
          else ->
            onSave(
              cId,
              selectedBranch?.id.orEmpty(),
              yearText.toLongOrNull() ?: 1L,
              semText.toLongOrNull() ?: 1L,
              subjectName.trim(),
              subjectCode.trim(),
              description.trim(),
              u,
              sizeLabel.trim(),
              published,
            )
        }
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun StudyMaterialFormDialog(
  initial: StudyMaterial?,
  courses: List<AcademicCourse>,
  branches: List<AcademicBranch>,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    description: String,
    courseId: String,
    branchId: String,
    year: Long,
    sem: Long,
    subjectName: String,
    unit: String,
    resourceType: String,
    fileUrl: String,
    uploadedByName: String,
    published: Boolean,
  ) -> Unit,
) {
  var selectedCourse by remember {
    mutableStateOf(courses.firstOrNull { it.id == initial?.courseId } ?: courses.firstOrNull())
  }
  val courseBranches = remember(selectedCourse, branches) { branches.filter { it.courseId == selectedCourse?.id } }
  var selectedBranch by remember(courseBranches) {
    mutableStateOf(courseBranches.firstOrNull { it.id == initial?.branchId } ?: courseBranches.firstOrNull())
  }
  var courseMenu by remember { mutableStateOf(false) }
  var branchMenu by remember { mutableStateOf(false) }
  var typeMenu by remember { mutableStateOf(false) }
  var title by remember { mutableStateOf(initial?.title ?: "") }
  var description by remember { mutableStateOf(initial?.description ?: "") }
  var subjectName by remember { mutableStateOf(initial?.subjectName ?: "") }
  var unit by remember { mutableStateOf(initial?.unit ?: "Unit 1") }
  var resourceType by remember { mutableStateOf(initial?.resourceType ?: "PDF") }
  var yearText by remember { mutableStateOf((initial?.academicYear ?: 1L).toString()) }
  var semText by remember { mutableStateOf((initial?.semester ?: 1L).toString()) }
  var fileUrl by remember { mutableStateOf(initial?.fileUrl ?: "") }
  var uploadedBy by remember { mutableStateOf(initial?.uploadedByName ?: "Faculty") }
  var published by remember { mutableStateOf(initial?.published ?: true) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Study Material" else "Edit Study Material") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (courses.isNotEmpty()) {
          SimpleDropdownField(
            label = "Course",
            selectedLabel = selectedCourse?.name ?: "Select Course",
            options = courses,
            optionLabel = { it.name },
            onSelect = { selectedCourse = it },
            expanded = courseMenu,
            onExpandedChange = { courseMenu = it },
          )
        }
        if (courseBranches.isNotEmpty()) {
          SimpleDropdownField(
            label = "Branch",
            selectedLabel = selectedBranch?.name ?: "Select Branch",
            options = courseBranches,
            optionLabel = { it.name },
            onSelect = { selectedBranch = it },
            expanded = branchMenu,
            onExpandedChange = { branchMenu = it },
          )
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notes Title") }, singleLine = true)
        OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject Name") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f))
          SimpleDropdownField(
            label = "Type",
            selectedLabel = resourceType,
            options = listOf("PDF", "IMAGE", "LINK"),
            optionLabel = { it },
            onSelect = { resourceType = it },
            expanded = typeMenu,
            onExpandedChange = { typeMenu = it },
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = yearText, onValueChange = { yearText = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = semText, onValueChange = { semText = it }, label = { Text("Semester") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2, maxLines = 4)
        OutlinedTextField(value = fileUrl, onValueChange = { fileUrl = it }, label = { Text("Document / Image URL (https://...)") }, singleLine = true)
        OutlinedTextField(value = uploadedBy, onValueChange = { uploadedBy = it }, label = { Text("Uploaded By") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = published, onCheckedChange = { published = it })
          Spacer(Modifier.width(8.dp))
          Text("Published")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        val cId = selectedCourse?.id.orEmpty()
        val u = fileUrl.trim()
        when {
          cId.isBlank() -> error = "Create or select a course first."
          title.trim().length < 3 -> error = "Title must be at least 3 characters."
          subjectName.trim().length < 2 -> error = "Subject name is required."
          !u.startsWith("http://") && !u.startsWith("https://") ->
            error = "Document/image URL must start with http:// or https://"
          else ->
            onSave(
              title.trim(),
              description.trim(),
              cId,
              selectedBranch?.id.orEmpty(),
              yearText.toLongOrNull() ?: 1L,
              semText.toLongOrNull() ?: 1L,
              subjectName.trim(),
              unit.trim(),
              resourceType,
              u,
              uploadedBy.trim(),
              published,
            )
        }
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun ExamPaperFormDialog(
  initial: ExamPaper?,
  courses: List<AcademicCourse>,
  branches: List<AcademicBranch>,
  examCategories: List<ExamCategory>,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    courseId: String,
    branchId: String,
    year: Long,
    sem: Long,
    subjectName: String,
    examCategoryId: String,
    examCategoryName: String,
    examYear: Long,
    pdfUrl: String,
    published: Boolean,
  ) -> Unit,
) {
  var selectedCourse by remember {
    mutableStateOf(courses.firstOrNull { it.id == initial?.courseId } ?: courses.firstOrNull())
  }
  val courseBranches = remember(selectedCourse, branches) { branches.filter { it.courseId == selectedCourse?.id } }
  var selectedBranch by remember(courseBranches) {
    mutableStateOf(courseBranches.firstOrNull { it.id == initial?.branchId } ?: courseBranches.firstOrNull())
  }
  var selectedCat by remember {
    mutableStateOf(examCategories.firstOrNull { it.id == initial?.examCategoryId } ?: examCategories.firstOrNull())
  }
  var courseMenu by remember { mutableStateOf(false) }
  var branchMenu by remember { mutableStateOf(false) }
  var catMenu by remember { mutableStateOf(false) }

  var title by remember { mutableStateOf(initial?.title ?: "") }
  var subjectName by remember { mutableStateOf(initial?.subjectName ?: "") }
  var yearText by remember { mutableStateOf((initial?.academicYear ?: 1L).toString()) }
  var semText by remember { mutableStateOf((initial?.semester ?: 1L).toString()) }
  var examYearText by remember { mutableStateOf((initial?.examYear ?: 2025L).toString()) }
  var pdfUrl by remember { mutableStateOf(initial?.pdfUrl ?: "") }
  var published by remember { mutableStateOf(initial?.published ?: true) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Exam Paper" else "Edit Exam Paper") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (courses.isNotEmpty()) {
          SimpleDropdownField(
            label = "Course",
            selectedLabel = selectedCourse?.name ?: "Select Course",
            options = courses,
            optionLabel = { it.name },
            onSelect = { selectedCourse = it },
            expanded = courseMenu,
            onExpandedChange = { courseMenu = it },
          )
        }
        if (courseBranches.isNotEmpty()) {
          SimpleDropdownField(
            label = "Branch",
            selectedLabel = selectedBranch?.name ?: "Select Branch",
            options = courseBranches,
            optionLabel = { it.name },
            onSelect = { selectedBranch = it },
            expanded = branchMenu,
            onExpandedChange = { branchMenu = it },
          )
        }
        if (examCategories.isNotEmpty()) {
          SimpleDropdownField(
            label = "Exam Category",
            selectedLabel = selectedCat?.name ?: "Select Category",
            options = examCategories,
            optionLabel = { it.name },
            onSelect = { selectedCat = it },
            expanded = catMenu,
            onExpandedChange = { catMenu = it },
          )
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Paper Title") }, singleLine = true)
        OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject Name") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = yearText, onValueChange = { yearText = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = semText, onValueChange = { semText = it }, label = { Text("Sem") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = examYearText, onValueChange = { examYearText = it }, label = { Text("Exam Yr") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = pdfUrl, onValueChange = { pdfUrl = it }, label = { Text("Question Paper PDF URL (https://...)") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = published, onCheckedChange = { published = it })
          Spacer(Modifier.width(8.dp))
          Text("Published")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        val cId = selectedCourse?.id.orEmpty()
        val u = pdfUrl.trim()
        val exYr = examYearText.toLongOrNull() ?: 2025L
        when {
          cId.isBlank() -> error = "Create or select a course first."
          title.trim().length < 3 -> error = "Title must be at least 3 characters."
          subjectName.trim().length < 2 -> error = "Subject name is required."
          exYr !in 2000L..2100L -> error = "Enter valid exam year (2000–2100)."
          !u.startsWith("http://") && !u.startsWith("https://") ->
            error = "Paper PDF URL must start with http:// or https://"
          else ->
            onSave(
              title.trim(),
              cId,
              selectedBranch?.id.orEmpty(),
              yearText.toLongOrNull() ?: 1L,
              semText.toLongOrNull() ?: 1L,
              subjectName.trim(),
              selectedCat?.id ?: "EXT",
              selectedCat?.name ?: "External Examination",
              exYr,
              u,
              published,
            )
        }
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun GalleryAlbumFormDialog(
  initial: GalleryAlbum?,
  onDismiss: () -> Unit,
  onSave: (title: String, desc: String, category: String, dateText: String, coverUrl: String, published: Boolean) -> Unit,
) {
  var title by remember { mutableStateOf(initial?.title ?: "") }
  var description by remember { mutableStateOf(initial?.description ?: "") }
  var category by remember { mutableStateOf(initial?.eventCategory ?: "Campus") }
  var dateText by remember { mutableStateOf(initial?.eventDateText ?: "") }
  var coverUrl by remember { mutableStateOf(initial?.coverImageUrl ?: "") }
  var published by remember { mutableStateOf(initial?.published ?: true) }
  var catMenu by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Create Gallery Album" else "Edit Gallery Album") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event / Album Title") }, singleLine = true)
        SimpleDropdownField(
          label = "Event Category",
          selectedLabel = category,
          options = listOf("Cultural", "Technical", "Sports", "Campus", "Other"),
          optionLabel = { it },
          onSelect = { category = it },
          expanded = catMenu,
          onExpandedChange = { catMenu = it },
        )
        OutlinedTextField(value = dateText, onValueChange = { dateText = it }, label = { Text("Event Date (e.g., Nov 2026)") }, singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2, maxLines = 4)
        OutlinedTextField(value = coverUrl, onValueChange = { coverUrl = it }, label = { Text("Cover Image URL (Optional)") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = published, onCheckedChange = { published = it })
          Spacer(Modifier.width(8.dp))
          Text("Published")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        val c = coverUrl.trim()
        when {
          title.trim().length < 3 -> error = "Album title must be at least 3 characters."
          dateText.trim().length < 3 -> error = "Enter an event date."
          c.isNotEmpty() && !c.startsWith("http://") && !c.startsWith("https://") ->
            error = "Cover URL must start with http:// or https://"
          else -> onSave(title.trim(), description.trim(), category, dateText.trim(), c, published)
        }
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun AddPhotoToAlbumDialog(
  albums: List<GalleryAlbum>,
  onDismiss: () -> Unit,
  onSave: (albumId: String, caption: String, imageUrl: String, thumbnailUrl: String, published: Boolean) -> Unit,
) {
  var selectedAlbum by remember { mutableStateOf(albums.firstOrNull()) }
  var albumMenu by remember { mutableStateOf(false) }
  var caption by remember { mutableStateOf("") }
  var imageUrl by remember { mutableStateOf("") }
  var thumbUrl by remember { mutableStateOf("") }
  var published by remember { mutableStateOf(true) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Photo to Album") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (albums.isNotEmpty()) {
          SimpleDropdownField(
            label = "Album",
            selectedLabel = selectedAlbum?.title ?: "Select Album",
            options = albums,
            optionLabel = { it.title },
            onSelect = { selectedAlbum = it },
            expanded = albumMenu,
            onExpandedChange = { albumMenu = it },
          )
        }
        OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Photo Caption") }, singleLine = true)
        OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Full Image URL (https://...)") }, singleLine = true)
        OutlinedTextField(value = thumbUrl, onValueChange = { thumbUrl = it }, label = { Text("Thumbnail URL (Optional)") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(checked = published, onCheckedChange = { published = it })
          Spacer(Modifier.width(8.dp))
          Text("Published")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        val aId = selectedAlbum?.id.orEmpty()
        val u = imageUrl.trim()
        val t = thumbUrl.trim()
        when {
          aId.isBlank() -> error = "Create or select an album first."
          !u.startsWith("http://") && !u.startsWith("https://") ->
            error = "Image URL must start with http:// or https://"
          t.isNotEmpty() && !t.startsWith("http://") && !t.startsWith("https://") ->
            error = "Thumbnail URL must start with http:// or https://"
          else -> onSave(aId, caption.trim(), u, t, published)
        }
      }) { Text("Add Photo") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun AddAdminDialog(
  onDismiss: () -> Unit,
  onSave: (uid: String, email: String, displayName: String) -> Unit,
) {
  var uid by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var displayName by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Authorize Administrator") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Admins must sign in once via Google so their Firebase Auth UID is generated. Public admin sign-up is disabled.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("Firebase Auth UID") }, singleLine = true)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Google Email") }, singleLine = true)
        OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Admin Name") }, singleLine = true)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    },
    confirmButton = {
      Button(onClick = {
        when {
          !uid.trim().matches(Regex("^[a-zA-Z0-9_\\-]+$")) -> error = "Enter a valid alphanumeric UID."
          !email.contains("@") -> error = "Enter a valid email."
          displayName.trim().isEmpty() -> error = "Enter display name."
          else -> onSave(uid.trim(), email.trim(), displayName.trim())
        }
      }) { Text("Grant Admin") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
