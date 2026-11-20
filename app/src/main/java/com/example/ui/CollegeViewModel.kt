package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CollegeRepository
import com.example.data.unwrapFirestoreMessage
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActionFeedback(
  val message: String,
  val isError: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CollegeViewModel(
  private val repository: CollegeRepository,
  val currentUserId: String,
) : ViewModel() {

  private fun <T> Flow<T>.asUiState(tag: String, fallbackError: String): StateFlow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(tag, fallbackError, rootError)
        emit(UiState.Error(unwrapFirestoreMessage(rootError, fallbackError)))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val isAdmin: StateFlow<Boolean> =
    repository.observeIsAdmin(currentUserId)
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(false)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = false,
      )

  val studentProfileState: StateFlow<UiState<StudentProfile?>> =
    repository
      .observeStudentProfile(currentUserId)
      .asUiState("ProfileVM", "Failed to load student profile")

  val readNoticeIds: StateFlow<Set<String>> =
    repository
      .observeReadNoticeIds(currentUserId)
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(emptySet())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptySet(),
      )

  val coursesState: StateFlow<UiState<List<AcademicCourse>>> =
    isAdmin
      .flatMapLatest { admin -> repository.observeCourses(isAdminView = admin) }
      .asUiState("CoursesVM", "Failed to load courses")

  val branchesState: StateFlow<UiState<List<AcademicBranch>>> =
    isAdmin
      .flatMapLatest { admin -> repository.observeBranches(isAdminView = admin) }
      .asUiState("BranchesVM", "Failed to load branches")

  val subjectsState: StateFlow<UiState<List<AcademicSubject>>> =
    isAdmin
      .flatMapLatest { admin -> repository.observeSubjects(isAdminView = admin) }
      .asUiState("SubjectsVM", "Failed to load subjects")

  val examCategoriesState: StateFlow<UiState<List<ExamCategory>>> =
    isAdmin
      .flatMapLatest { admin -> repository.observeExamCategories(isAdminView = admin) }
      .asUiState("ExamCatVM", "Failed to load exam categories")

  val noticesState: StateFlow<UiState<List<Notice>>> =
    isAdmin
      .flatMapLatest { admin -> repository.observeNotices(isAdminView = admin) }
      .asUiState("NoticesVM", "Failed to load notices")

  val syllabusState: StateFlow<UiState<List<SyllabusItem>>> =
    isAdmin
      .flatMapLatest { admin -> repository.observeSyllabus(isAdminView = admin) }
      .asUiState("SyllabusVM", "Failed to load syllabus")

  val studyMaterialsState: StateFlow<UiState<List<StudyMaterial>>> =
    isAdmin
      .flatMapLatest { admin -> repository.observeStudyMaterials(isAdminView = admin) }
      .asUiState("MaterialsVM", "Failed to load study material")

  val examPapersState: StateFlow<UiState<List<ExamPaper>>> =
    isAdmin
      .flatMapLatest { admin -> repository.observeExamPapers(isAdminView = admin) }
      .asUiState("PapersVM", "Failed to load exam papers")

  val galleryAlbumsState: StateFlow<UiState<List<GalleryAlbum>>> =
    isAdmin
      .flatMapLatest { admin -> repository.observeGalleryAlbums(isAdminView = admin) }
      .asUiState("GalleryVM", "Failed to load gallery albums")

  private val selectedAlbumId = MutableStateFlow<String?>(null)

  val albumPhotosState: StateFlow<UiState<List<GalleryPhoto>>> =
    combine(selectedAlbumId, isAdmin) { albumId, admin -> albumId to admin }
      .flatMapLatest { (albumId, admin) ->
        if (albumId.isNullOrBlank()) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeGalleryPhotos(albumId, isAdminView = admin)
            .map<List<GalleryPhoto>, UiState<List<GalleryPhoto>>> { UiState.Success(it) }
            .catch { error ->
              val firestoreError =
                generateSequence(error) { it.cause }
                  .filterIsInstance<FirebaseFirestoreException>()
                  .firstOrNull()
              if (firestoreError == null && error is CancellationException) throw error
              emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load album photos")))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  // Admin-only collections: gated behind isAdmin == true so students never trigger PERMISSION_DENIED
  val adminStudentsState: StateFlow<UiState<List<StudentProfile>>> =
    isAdmin
      .flatMapLatest { admin ->
        if (!admin) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeAllStudentsForAdmin()
            .map<List<StudentProfile>, UiState<List<StudentProfile>>> { UiState.Success(it) }
            .catch { error ->
              val firestoreError =
                generateSequence(error) { it.cause }
                  .filterIsInstance<FirebaseFirestoreException>()
                  .firstOrNull()
              if (firestoreError == null && error is CancellationException) throw error
              emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load students")))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminsListState: StateFlow<UiState<List<AdminAccount>>> =
    isAdmin
      .flatMapLatest { admin ->
        if (!admin) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeAdmins()
            .map<List<AdminAccount>, UiState<List<AdminAccount>>> { UiState.Success(it) }
            .catch { error ->
              val firestoreError =
                generateSequence(error) { it.cause }
                  .filterIsInstance<FirebaseFirestoreException>()
                  .firstOrNull()
              if (firestoreError == null && error is CancellationException) throw error
              emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load admins")))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _feedback = MutableStateFlow<ActionFeedback?>(null)
  val feedback: StateFlow<ActionFeedback?> = _feedback.asStateFlow()

  fun clearFeedback() {
    _feedback.value = null
  }

  fun selectAlbum(albumId: String) {
    selectedAlbumId.value = albumId
  }

  private fun runAction(
    successMessage: String?,
    onSuccess: () -> Unit = {},
    block: suspend () -> Result<Unit>,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val result = block()
      _isBusy.value = false
      result
        .onSuccess {
          if (successMessage != null) {
            _feedback.value = ActionFeedback(successMessage, isError = false)
          }
          onSuccess()
        }
        .onFailure { error ->
          _feedback.value =
            ActionFeedback(error.localizedMessage ?: "Operation failed", isError = true)
        }
    }
  }

  fun saveStudentProfile(
    fullName: String,
    rollNumber: String,
    course: AcademicCourse,
    branch: AcademicBranch?,
    academicYear: Long,
    semester: Long,
    photoUrl: String,
    isExistingProfile: Boolean,
    onDone: () -> Unit = {},
  ) =
    runAction("Profile saved successfully", onSuccess = onDone) {
      repository.saveStudentProfile(
        fullName = fullName,
        rollNumber = rollNumber,
        course = course,
        branch = branch,
        academicYear = academicYear,
        semester = semester,
        photoUrl = photoUrl,
        isExistingProfile = isExistingProfile,
      )
    }

  fun markNoticeAsRead(noticeId: String) {
    viewModelScope.launch { repository.markNoticeRead(currentUserId, noticeId) }
  }

  fun seedDemoData() =
    runAction("[DEMO] ITG Gopeshwar sample data loaded") { repository.seedDemoCollegeData() }

  fun saveCourse(
    existingId: String?,
    name: String,
    code: String,
    durationYears: Long,
    totalSemesters: Long,
    hasBranches: Boolean,
    published: Boolean,
    onDone: () -> Unit = {},
  ) =
    runAction("Course saved", onSuccess = onDone) {
      repository.saveCourse(existingId, name, code, durationYears, totalSemesters, hasBranches, published)
    }

  fun saveBranch(
    existingId: String?,
    courseId: String,
    name: String,
    code: String,
    displayOrder: Long,
    published: Boolean,
    onDone: () -> Unit = {},
  ) =
    runAction("Branch saved", onSuccess = onDone) {
      repository.saveBranch(existingId, courseId, name, code, displayOrder, published)
    }

  fun saveSubject(
    existingId: String?,
    courseId: String,
    branchId: String,
    academicYear: Long,
    semester: Long,
    name: String,
    code: String,
    credits: Long,
    published: Boolean,
    onDone: () -> Unit = {},
  ) =
    runAction("Subject saved", onSuccess = onDone) {
      repository.saveSubject(
        existingId,
        courseId,
        branchId,
        academicYear,
        semester,
        name,
        code,
        credits,
        published,
      )
    }

  fun saveExamCategory(
    existingId: String?,
    name: String,
    code: String,
    displayOrder: Long,
    published: Boolean,
    onDone: () -> Unit = {},
  ) =
    runAction("Exam category saved", onSuccess = onDone) {
      repository.saveExamCategory(existingId, name, code, displayOrder, published)
    }

  fun saveNotice(
    existingId: String?,
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
    onDone: () -> Unit = {},
  ) =
    runAction(if (published) "Notice published" else "Notice saved as draft", onSuccess = onDone) {
      repository.saveNotice(
        existingId,
        title,
        message,
        category,
        attachmentUrl,
        attachmentName,
        targetCourseId,
        targetBranchId,
        targetYear,
        targetSemester,
        published,
        important,
        authorName,
      )
    }

  fun saveSyllabus(
    existingId: String?,
    courseId: String,
    branchId: String,
    academicYear: Long,
    semester: Long,
    subjectId: String,
    subjectName: String,
    subjectCode: String,
    description: String,
    pdfUrl: String,
    fileSizeLabel: String,
    published: Boolean,
    onDone: () -> Unit = {},
  ) =
    runAction("Syllabus entry saved", onSuccess = onDone) {
      repository.saveSyllabus(
        existingId,
        courseId,
        branchId,
        academicYear,
        semester,
        subjectId,
        subjectName,
        subjectCode,
        description,
        pdfUrl,
        fileSizeLabel,
        published,
      )
    }

  fun saveStudyMaterial(
    existingId: String?,
    title: String,
    description: String,
    courseId: String,
    branchId: String,
    academicYear: Long,
    semester: Long,
    subjectId: String,
    subjectName: String,
    unit: String,
    resourceType: String,
    fileUrl: String,
    uploadedByName: String,
    published: Boolean,
    onDone: () -> Unit = {},
  ) =
    runAction("Study material saved", onSuccess = onDone) {
      repository.saveStudyMaterial(
        existingId,
        title,
        description,
        courseId,
        branchId,
        academicYear,
        semester,
        subjectId,
        subjectName,
        unit,
        resourceType,
        fileUrl,
        uploadedByName,
        published,
      )
    }

  fun saveExamPaper(
    existingId: String?,
    title: String,
    courseId: String,
    branchId: String,
    academicYear: Long,
    semester: Long,
    subjectId: String,
    subjectName: String,
    examCategoryId: String,
    examCategoryName: String,
    examYear: Long,
    pdfUrl: String,
    published: Boolean,
    onDone: () -> Unit = {},
  ) =
    runAction("Exam paper saved", onSuccess = onDone) {
      repository.saveExamPaper(
        existingId,
        title,
        courseId,
        branchId,
        academicYear,
        semester,
        subjectId,
        subjectName,
        examCategoryId,
        examCategoryName,
        examYear,
        pdfUrl,
        published,
      )
    }

  fun saveGalleryAlbum(
    existingId: String?,
    title: String,
    description: String,
    eventCategory: String,
    eventDateText: String,
    coverImageUrl: String,
    photoCount: Long,
    published: Boolean,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res =
        repository.saveGalleryAlbum(
          existingId,
          title,
          description,
          eventCategory,
          eventDateText,
          coverImageUrl,
          photoCount,
          published,
        )
      _isBusy.value = false
      res
        .onSuccess {
          _feedback.value = ActionFeedback("Gallery album saved", isError = false)
          onDone()
        }
        .onFailure { err ->
          _feedback.value = ActionFeedback(err.localizedMessage ?: "Failed to save album", isError = true)
        }
    }
  }

  fun addPhotoToAlbum(
    albumId: String,
    caption: String,
    imageUrl: String,
    thumbnailUrl: String,
    published: Boolean,
    onDone: () -> Unit = {},
  ) =
    runAction("Photo added to album", onSuccess = onDone) {
      repository.addPhotoToAlbum(albumId, caption, imageUrl, thumbnailUrl, published)
    }

  fun deleteDocument(collection: String, documentId: String, label: String) =
    runAction("$label deleted") { repository.deleteDocument(collection, documentId) }

  fun addAdmin(targetUid: String, email: String, displayName: String, onDone: () -> Unit = {}) =
    runAction("Administrator added", onSuccess = onDone) {
      repository.addAdmin(targetUid, email, displayName)
    }

  fun removeAdmin(targetUid: String) =
    runAction("Administrator removed") { repository.removeAdmin(targetUid) }

  private companion object {
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
