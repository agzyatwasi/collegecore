package com.example.data

import android.content.Context
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val BOOTSTRAPPED_ADMIN_EMAIL = "agzyatwasi@gmail.com"

class CollegeRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun isBootstrappedAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.isEmailVerified && user.email?.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) == true
  }

  private fun <T> Flow<T>.handleSnapshotErrors(path: String): Flow<T> = catch { error ->
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val toLog = firestoreError ?: (error as? Exception)
    if (toLog != null && auth.currentUser != null) {
      handleFirestoreError(toLog, OperationType.LIST, path)
    }
    throw error
  }

  private fun cleanMap(raw: Map<String, Any?>): Map<String, Any> {
    @Suppress("UNCHECKED_CAST")
    return raw.filterValues { it != null } as Map<String, Any>
  }

  // --- ADMIN CHECK & ACCESS ---

  fun observeIsAdmin(uid: String): Flow<Boolean> =
    if (uid.isBlank()) {
      flow { emit(false) }
    } else if (isBootstrappedAdmin()) {
      flow { emit(true) }
    } else {
      db.collection("admins").document(uid).snapshots()
        .map { it.exists() || isBootstrappedAdmin() }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          emit(isBootstrappedAdmin())
        }
    }

  suspend fun checkIsAdminOnce(uid: String): Boolean {
    if (uid.isBlank()) return false
    if (isBootstrappedAdmin()) return true
    return try {
      db.collection("admins").document(uid).get().await().exists()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  fun observeAdmins(): Flow<List<AdminAccount>> = flow {
    requireUserId()
    emitAll(
      db.collection("admins")
        .snapshots()
        .map { snap ->
          snap.toObjects(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .handleSnapshotErrors("admins")
    )
  }

  suspend fun addAdmin(targetUid: String, email: String, displayName: String): Result<Unit> {
    val cleanUid = targetUid.trim()
    val path = "admins/$cleanUid"
    return try {
      val adminUid = requireUserId()
      require(cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) { "UID must contain only letters, numbers, underscores, or hyphens." }
      require(email.trim().length in 3..120) { "Enter a valid email address." }
      require(displayName.trim().isNotEmpty()) { "Display name is required." }
      val payload =
        mapOf(
          "uid" to cleanUid,
          "email" to email.trim(),
          "displayName" to displayName.trim().take(80),
          "addedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      db.collection("admins").document(cleanUid).set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun removeAdmin(targetUid: String): Result<Unit> {
    val path = "admins/$targetUid"
    return try {
      val uid = requireUserId()
      require(targetUid != uid) { "You cannot remove your own admin access." }
      db.collection("admins").document(targetUid).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- STUDENT PROFILE & READ RECEIPTS ---

  fun observeStudentProfile(userId: String): Flow<StudentProfile?> = flow {
    requireUserId()
    val path = "users/$userId"
    emitAll(
      db.collection("users")
        .document(userId)
        .snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .handleSnapshotErrors(path)
    )
  }

  fun observeAllStudentsForAdmin(): Flow<List<StudentProfile>> = flow {
    requireUserId()
    emitAll(
      db.collection("users")
        .snapshots()
        .map { snap ->
          snap.toObjects(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .handleSnapshotErrors("users")
    )
  }

  suspend fun saveStudentProfile(
    fullName: String,
    rollNumber: String,
    course: AcademicCourse,
    branch: AcademicBranch?,
    academicYear: Long,
    semester: Long,
    photoUrl: String,
    isExistingProfile: Boolean,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: "unauthenticated"
    val path = "users/$uid"
    return try {
      val signedInUid = requireUserId()
      val email = auth.currentUser?.email?.takeIf { it.isNotBlank() } ?: "$signedInUid@itggopeshwar.ac.in"
      val trimmedName = fullName.trim()
      val trimmedRoll = rollNumber.trim()
      val trimmedPhoto = photoUrl.trim()
      require(trimmedName.length in 2..80) { "Name must be between 2 and 80 characters." }
      require(trimmedRoll.length in 2..40) { "Roll number / Student ID must be 2–40 characters." }
      require(academicYear in 1L..course.durationYears) { "Select a valid academic year." }
      require(semester in 1L..course.totalSemesters) { "Select a valid semester." }
      if (trimmedPhoto.isNotEmpty()) {
        require(trimmedPhoto.startsWith("http://") || trimmedPhoto.startsWith("https://")) {
          "Profile photo URL must start with http:// or https://"
        }
      }

      val docRef = db.collection("users").document(signedInUid)
      if (isExistingProfile) {
        val updatePayload =
          cleanMap(
            mapOf(
              "fullName" to trimmedName,
              "rollNumber" to trimmedRoll,
              "courseId" to course.id,
              "courseName" to course.name,
              "branchId" to (branch?.id ?: ""),
              "branchName" to (branch?.name ?: ""),
              "academicYear" to academicYear,
              "semester" to semester,
              "photoUrl" to trimmedPhoto.ifEmpty { null },
              "profileCompleted" to true,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        docRef.update(updatePayload).await()
      } else {
        val createPayload =
          cleanMap(
            mapOf(
              "userId" to signedInUid,
              "fullName" to trimmedName,
              "email" to email.take(120),
              "rollNumber" to trimmedRoll,
              "courseId" to course.id,
              "courseName" to course.name,
              "branchId" to (branch?.id ?: ""),
              "branchName" to (branch?.name ?: ""),
              "academicYear" to academicYear,
              "semester" to semester,
              "photoUrl" to trimmedPhoto.ifEmpty { null },
              "profileCompleted" to true,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        docRef.set(createPayload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isExistingProfile) OperationType.UPDATE else OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  fun observeReadNoticeIds(userId: String): Flow<Set<String>> = flow {
    requireUserId()
    val path = "users/$userId/read_notices"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("read_notices")
        .snapshots()
        .map { snap -> snap.documents.map { it.id }.toSet() }
        .handleSnapshotErrors(path)
    )
  }

  suspend fun markNoticeRead(userId: String, noticeId: String): Result<Unit> {
    val path = "users/$userId/read_notices/$noticeId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Cannot mark read receipt for another user." }
      require(noticeId.isNotBlank()) { "Notice ID cannot be blank." }
      db.collection("users")
        .document(uid)
        .collection("read_notices")
        .document(noticeId)
        .set(
          mapOf(
            "noticeId" to noticeId,
            "readAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  // --- ACADEMIC STRUCTURE OBSERVERS ---

  private inline fun <reified T> observeCollection(
    collectionName: String,
    includeDraftsForAdmin: Boolean,
  ): Flow<List<T>> = flow {
    val uid = requireUserId()
    val publishedFlow =
      db.collection(collectionName)
        .whereEqualTo("published", true)
        .snapshots()
        .map { it.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .handleSnapshotErrors(collectionName)

    if (!includeDraftsForAdmin) {
      emitAll(publishedFlow)
    } else {
      val ownDraftsFlow =
        db.collection(collectionName)
          .whereEqualTo("createdBy", uid)
          .snapshots()
          .map { it.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
          .handleSnapshotErrors(collectionName)

      emitAll(
        combine(publishedFlow, ownDraftsFlow) { pub, own ->
          (pub + own).distinctBy { it }
        }
      )
    }
  }

  fun observeCourses(isAdminView: Boolean = false): Flow<List<AcademicCourse>> =
    observeCollection<AcademicCourse>("academic_courses", isAdminView).map { list ->
      list.distinctBy { it.id }.sortedBy { it.name }
    }

  fun observeBranches(isAdminView: Boolean = false): Flow<List<AcademicBranch>> =
    observeCollection<AcademicBranch>("academic_branches", isAdminView).map { list ->
      list.distinctBy { it.id }.sortedWith(compareBy({ it.displayOrder }, { it.name }))
    }

  fun observeSubjects(isAdminView: Boolean = false): Flow<List<AcademicSubject>> =
    observeCollection<AcademicSubject>("academic_subjects", isAdminView).map { list ->
      list.distinctBy { it.id }.sortedWith(compareBy({ it.semester }, { it.name }))
    }

  fun observeExamCategories(isAdminView: Boolean = false): Flow<List<ExamCategory>> =
    observeCollection<ExamCategory>("exam_categories", isAdminView).map { list ->
      list.distinctBy { it.id }.sortedWith(compareBy({ it.displayOrder }, { it.name }))
    }

  fun observeNotices(isAdminView: Boolean = false): Flow<List<Notice>> =
    observeCollection<Notice>("notices", isAdminView).map { list ->
      list.distinctBy { it.id }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

  fun observeSyllabus(isAdminView: Boolean = false): Flow<List<SyllabusItem>> =
    observeCollection<SyllabusItem>("syllabus_items", isAdminView).map { list ->
      list.distinctBy { it.id }.sortedWith(compareBy({ it.semester }, { it.subjectName }))
    }

  fun observeStudyMaterials(isAdminView: Boolean = false): Flow<List<StudyMaterial>> =
    observeCollection<StudyMaterial>("study_materials", isAdminView).map { list ->
      list.distinctBy { it.id }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

  fun observeExamPapers(isAdminView: Boolean = false): Flow<List<ExamPaper>> =
    observeCollection<ExamPaper>("exam_papers", isAdminView).map { list ->
      list.distinctBy { it.id }.sortedByDescending { it.examYear }
    }

  fun observeGalleryAlbums(isAdminView: Boolean = false): Flow<List<GalleryAlbum>> =
    observeCollection<GalleryAlbum>("gallery_albums", isAdminView).map { list ->
      list.distinctBy { it.id }.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

  fun observeGalleryPhotos(albumId: String, isAdminView: Boolean = false): Flow<List<GalleryPhoto>> =
    flow {
      val uid = requireUserId()
      val pubFlow =
        db.collection("gallery_photos")
          .whereEqualTo("published", true)
          .snapshots()
          .map { snap ->
            snap
              .toObjects(GalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .filter { it.albumId == albumId }
          }
          .handleSnapshotErrors("gallery_photos")

      if (!isAdminView) {
        emitAll(pubFlow)
      } else {
        val ownFlow =
          db.collection("gallery_photos")
            .whereEqualTo("createdBy", uid)
            .snapshots()
            .map { snap ->
              snap
                .toObjects(GalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.albumId == albumId }
            }
            .handleSnapshotErrors("gallery_photos")
        emitAll(
          combine(pubFlow, ownFlow) { pub, own ->
            (pub + own).distinctBy { it.id }.sortedByDescending { it.createdAt?.seconds ?: 0L }
          }
        )
      }
    }

  // --- ADMIN CRUD: ACADEMIC STRUCTURE ---

  suspend fun saveCourse(
    existingId: String?,
    name: String,
    code: String,
    durationYears: Long,
    totalSemesters: Long,
    hasBranches: Boolean,
    published: Boolean,
  ): Result<Unit> {
    val docRef =
      if (existingId.isNullOrBlank()) db.collection("academic_courses").document()
      else db.collection("academic_courses").document(existingId)
    return try {
      val uid = requireUserId()
      require(name.trim().length in 2..60) { "Course name must be 2–60 characters." }
      require(code.trim().length in 2..20) { "Course code must be 2–20 characters." }
      require(durationYears in 1L..6L) { "Duration must be 1–6 years." }
      require(totalSemesters in 1L..12L) { "Total semesters must be 1–12." }
      if (existingId.isNullOrBlank()) {
        docRef
          .set(
            mapOf(
              "name" to name.trim(),
              "code" to code.trim().uppercase(),
              "durationYears" to durationYears,
              "totalSemesters" to totalSemesters,
              "hasBranches" to hasBranches,
              "published" to published,
              "createdBy" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .update(
            mapOf(
              "name" to name.trim(),
              "code" to code.trim().uppercase(),
              "durationYears" to durationYears,
              "totalSemesters" to totalSemesters,
              "hasBranches" to hasBranches,
              "published" to published,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveBranch(
    existingId: String?,
    courseId: String,
    name: String,
    code: String,
    displayOrder: Long,
    published: Boolean,
  ): Result<Unit> {
    val docRef =
      if (existingId.isNullOrBlank()) db.collection("academic_branches").document()
      else db.collection("academic_branches").document(existingId)
    return try {
      val uid = requireUserId()
      require(courseId.isNotBlank()) { "Select a parent course." }
      require(name.trim().length in 2..80) { "Branch name must be 2–80 characters." }
      require(code.trim().length in 1..20) { "Branch code must be 1–20 characters." }
      if (existingId.isNullOrBlank()) {
        docRef
          .set(
            mapOf(
              "courseId" to courseId,
              "name" to name.trim(),
              "code" to code.trim().uppercase(),
              "displayOrder" to displayOrder.coerceIn(0L, 50L),
              "published" to published,
              "createdBy" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .update(
            mapOf(
              "courseId" to courseId,
              "name" to name.trim(),
              "code" to code.trim().uppercase(),
              "displayOrder" to displayOrder.coerceIn(0L, 50L),
              "published" to published,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveSubject(
    existingId: String?,
    courseId: String,
    branchId: String,
    academicYear: Long,
    semester: Long,
    name: String,
    code: String,
    credits: Long,
    published: Boolean,
  ): Result<Unit> {
    val docRef =
      if (existingId.isNullOrBlank()) db.collection("academic_subjects").document()
      else db.collection("academic_subjects").document(existingId)
    return try {
      val uid = requireUserId()
      require(courseId.isNotBlank()) { "Course is required." }
      require(name.trim().length in 2..100) { "Subject name must be 2–100 characters." }
      require(code.trim().length in 2..24) { "Subject code must be 2–24 characters." }
      if (existingId.isNullOrBlank()) {
        docRef
          .set(
            mapOf(
              "courseId" to courseId,
              "branchId" to branchId,
              "academicYear" to academicYear.coerceIn(1L, 6L),
              "semester" to semester.coerceIn(1L, 12L),
              "name" to name.trim(),
              "code" to code.trim().uppercase(),
              "credits" to credits.coerceIn(0L, 20L),
              "published" to published,
              "createdBy" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .update(
            mapOf(
              "courseId" to courseId,
              "branchId" to branchId,
              "academicYear" to academicYear.coerceIn(1L, 6L),
              "semester" to semester.coerceIn(1L, 12L),
              "name" to name.trim(),
              "code" to code.trim().uppercase(),
              "credits" to credits.coerceIn(0L, 20L),
              "published" to published,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveExamCategory(
    existingId: String?,
    name: String,
    code: String,
    displayOrder: Long,
    published: Boolean,
  ): Result<Unit> {
    val docRef =
      if (existingId.isNullOrBlank()) db.collection("exam_categories").document()
      else db.collection("exam_categories").document(existingId)
    return try {
      val uid = requireUserId()
      require(name.trim().length in 2..60) { "Category name must be 2–60 characters." }
      require(code.trim().length in 2..24) { "Category code must be 2–24 characters." }
      if (existingId.isNullOrBlank()) {
        docRef
          .set(
            mapOf(
              "name" to name.trim(),
              "code" to code.trim().uppercase(),
              "displayOrder" to displayOrder.coerceIn(0L, 50L),
              "published" to published,
              "createdBy" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .update(
            mapOf(
              "name" to name.trim(),
              "code" to code.trim().uppercase(),
              "displayOrder" to displayOrder.coerceIn(0L, 50L),
              "published" to published,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  // --- ADMIN CRUD: NOTICES, SYLLABUS, STUDY MATERIAL, EXAM PAPERS, GALLERY ---

  suspend fun saveNotice(
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
  ): Result<Unit> {
    val docRef =
      if (existingId.isNullOrBlank()) db.collection("notices").document()
      else db.collection("notices").document(existingId)
    return try {
      val uid = requireUserId()
      val cleanTitle = title.trim()
      val cleanMsg = message.trim()
      val cleanUrl = attachmentUrl.trim()
      val cleanAuthor = authorName.trim().ifEmpty { "ITG Administration" }
      require(cleanTitle.length in 3..140) { "Notice title must be 3–140 characters." }
      require(cleanMsg.length in 3..4000) { "Notice message must be 3–4000 characters." }
      if (cleanUrl.isNotEmpty()) {
        require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
          "Attachment URL must start with http:// or https://"
        }
      }
      val fields =
        mutableMapOf<String, Any?>(
          "title" to cleanTitle,
          "message" to cleanMsg,
          "category" to category,
          "attachmentUrl" to cleanUrl.ifEmpty { null },
          "attachmentName" to attachmentName.trim().take(120).ifEmpty { null },
          "targetCourseId" to targetCourseId.ifBlank { "ALL" },
          "targetBranchId" to targetBranchId.ifBlank { "ALL" },
          "targetYear" to targetYear.coerceIn(0L, 6L),
          "targetSemester" to targetSemester.coerceIn(0L, 12L),
          "published" to published,
          "important" to important,
          "authorName" to cleanAuthor.take(80),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (existingId.isNullOrBlank()) {
        fields["createdBy"] = uid
        fields["createdAt"] = FieldValue.serverTimestamp()
        docRef.set(cleanMap(fields)).await()
      } else {
        docRef.update(cleanMap(fields)).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveSyllabus(
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
  ): Result<Unit> {
    val docRef =
      if (existingId.isNullOrBlank()) db.collection("syllabus_items").document()
      else db.collection("syllabus_items").document(existingId)
    return try {
      val uid = requireUserId()
      val cleanUrl = pdfUrl.trim()
      require(courseId.isNotBlank()) { "Select a course." }
      require(subjectName.trim().length in 2..120) { "Subject name is required." }
      require(subjectCode.trim().length in 2..24) { "Subject code is required." }
      require(description.trim().length in 3..3000) { "Syllabus description must be 3–3000 characters." }
      if (cleanUrl.isNotEmpty()) {
        require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
          "Syllabus PDF link must start with http:// or https://"
        }
      }
      val fields =
        mutableMapOf<String, Any?>(
          "courseId" to courseId,
          "branchId" to branchId,
          "academicYear" to academicYear.coerceIn(1L, 6L),
          "semester" to semester.coerceIn(1L, 12L),
          "subjectId" to subjectId.ifBlank { subjectCode.trim().uppercase() },
          "subjectName" to subjectName.trim(),
          "subjectCode" to subjectCode.trim().uppercase(),
          "description" to description.trim(),
          "pdfUrl" to cleanUrl.ifEmpty { null },
          "fileSizeLabel" to fileSizeLabel.trim().take(40).ifEmpty { null },
          "published" to published,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (existingId.isNullOrBlank()) {
        fields["createdBy"] = uid
        fields["createdAt"] = FieldValue.serverTimestamp()
        docRef.set(cleanMap(fields)).await()
      } else {
        docRef.update(cleanMap(fields)).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveStudyMaterial(
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
  ): Result<Unit> {
    val docRef =
      if (existingId.isNullOrBlank()) db.collection("study_materials").document()
      else db.collection("study_materials").document(existingId)
    return try {
      val uid = requireUserId()
      val cleanUrl = fileUrl.trim()
      require(title.trim().length in 3..140) { "Title must be 3–140 characters." }
      require(courseId.isNotBlank()) { "Course is required." }
      require(subjectName.trim().length in 2..120) { "Subject name is required." }
      require(unit.trim().length in 1..60) { "Unit / topic is required." }
      require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
        "Document/image URL must start with http:// or https://"
      }
      val fields =
        mutableMapOf<String, Any?>(
          "title" to title.trim(),
          "description" to description.trim().take(2000),
          "courseId" to courseId,
          "branchId" to branchId,
          "academicYear" to academicYear.coerceIn(1L, 6L),
          "semester" to semester.coerceIn(1L, 12L),
          "subjectId" to subjectId.ifBlank { "GENERAL" },
          "subjectName" to subjectName.trim(),
          "unit" to unit.trim(),
          "resourceType" to resourceType,
          "fileUrl" to cleanUrl,
          "uploadedByName" to uploadedByName.trim().ifEmpty { "Faculty" }.take(80),
          "published" to published,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (existingId.isNullOrBlank()) {
        fields["createdBy"] = uid
        fields["createdAt"] = FieldValue.serverTimestamp()
        docRef.set(cleanMap(fields)).await()
      } else {
        docRef.update(cleanMap(fields)).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveExamPaper(
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
  ): Result<Unit> {
    val docRef =
      if (existingId.isNullOrBlank()) db.collection("exam_papers").document()
      else db.collection("exam_papers").document(existingId)
    return try {
      val uid = requireUserId()
      val cleanUrl = pdfUrl.trim()
      require(title.trim().length in 3..140) { "Paper title must be 3–140 characters." }
      require(courseId.isNotBlank()) { "Course is required." }
      require(subjectName.trim().length in 2..120) { "Subject name is required." }
      require(examCategoryName.trim().length in 2..60) { "Exam category is required." }
      require(examYear in 2000L..2100L) { "Enter a valid exam year (2000–2100)." }
      require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
        "Paper PDF URL must start with http:// or https://"
      }
      val fields =
        mutableMapOf<String, Any?>(
          "title" to title.trim(),
          "courseId" to courseId,
          "branchId" to branchId,
          "academicYear" to academicYear.coerceIn(1L, 6L),
          "semester" to semester.coerceIn(1L, 12L),
          "subjectId" to subjectId.ifBlank { "GENERAL" },
          "subjectName" to subjectName.trim(),
          "examCategoryId" to examCategoryId.ifBlank { "EXT" },
          "examCategoryName" to examCategoryName.trim(),
          "examYear" to examYear,
          "pdfUrl" to cleanUrl,
          "published" to published,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (existingId.isNullOrBlank()) {
        fields["createdBy"] = uid
        fields["createdAt"] = FieldValue.serverTimestamp()
        docRef.set(cleanMap(fields)).await()
      } else {
        docRef.update(cleanMap(fields)).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveGalleryAlbum(
    existingId: String?,
    title: String,
    description: String,
    eventCategory: String,
    eventDateText: String,
    coverImageUrl: String,
    photoCount: Long,
    published: Boolean,
  ): Result<String> {
    val docRef =
      if (existingId.isNullOrBlank()) db.collection("gallery_albums").document()
      else db.collection("gallery_albums").document(existingId)
    return try {
      val uid = requireUserId()
      val cleanCover = coverImageUrl.trim()
      require(title.trim().length in 3..120) { "Album title must be 3–120 characters." }
      require(eventDateText.trim().length in 3..40) { "Event date is required." }
      if (cleanCover.isNotEmpty()) {
        require(cleanCover.startsWith("http://") || cleanCover.startsWith("https://")) {
          "Cover image URL must start with http:// or https://"
        }
      }
      val fields =
        mutableMapOf<String, Any?>(
          "title" to title.trim(),
          "description" to description.trim().take(1500),
          "eventCategory" to eventCategory,
          "eventDateText" to eventDateText.trim(),
          "coverImageUrl" to cleanCover.ifEmpty { null },
          "photoCount" to photoCount.coerceIn(0L, 500L),
          "published" to published,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (existingId.isNullOrBlank()) {
        fields["createdBy"] = uid
        fields["createdAt"] = FieldValue.serverTimestamp()
        docRef.set(cleanMap(fields)).await()
      } else {
        docRef.update(cleanMap(fields)).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addPhotoToAlbum(
    albumId: String,
    caption: String,
    imageUrl: String,
    thumbnailUrl: String,
    published: Boolean,
  ): Result<Unit> {
    val docRef = db.collection("gallery_photos").document()
    return try {
      val uid = requireUserId()
      val cleanUrl = imageUrl.trim()
      val cleanThumb = thumbnailUrl.trim()
      require(albumId.isNotBlank()) { "Select an album first." }
      require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
        "Image URL must start with http:// or https://"
      }
      if (cleanThumb.isNotEmpty()) {
        require(cleanThumb.startsWith("http://") || cleanThumb.startsWith("https://")) {
          "Thumbnail URL must start with http:// or https://"
        }
      }
      val payload =
        cleanMap(
          mapOf(
            "albumId" to albumId,
            "caption" to caption.trim().take(240),
            "imageUrl" to cleanUrl,
            "thumbnailUrl" to cleanThumb.ifEmpty { null },
            "published" to published,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      docRef.set(payload).await()
      db.collection("gallery_albums")
        .document(albumId)
        .update(
          mapOf(
            "photoCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteDocument(collection: String, documentId: String): Result<Unit> {
    val path = "$collection/$documentId"
    return try {
      requireUserId()
      require(documentId.isNotBlank()) { "Document ID cannot be blank." }
      db.collection(collection).document(documentId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  /**
   * Seeds clearly labeled [DEMO] data for ITG Gopeshwar using a batched write.
   * Note: Does NOT invent B.Tech branch names—creates 5 configurable branch slots
   * labeled "[Demo] Branch Slot 1..5 (Rename in Admin)" per specification.
   */
  suspend fun seedDemoCollegeData(): Result<Unit> {
    return try {
      val uid = requireUserId()
      val batch = db.batch()
      val now = FieldValue.serverTimestamp()

      // 1. Courses: B.Tech (4 yrs, 8 sems, 5 configurable branches) & BCA (3 yrs, 6 sems)
      val btechRef = db.collection("academic_courses").document("demo_btech")
      batch.set(
        btechRef,
        mapOf(
          "name" to "B.Tech",
          "code" to "BTECH",
          "durationYears" to 4L,
          "totalSemesters" to 8L,
          "hasBranches" to true,
          "published" to true,
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )
      val bcaRef = db.collection("academic_courses").document("demo_bca")
      batch.set(
        bcaRef,
        mapOf(
          "name" to "BCA",
          "code" to "BCA",
          "durationYears" to 3L,
          "totalSemesters" to 6L,
          "hasBranches" to false,
          "published" to true,
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )

      // 2. Five configurable B.Tech branch slots (without inventing branch names)
      (1L..5L).forEach { idx ->
        val brRef = db.collection("academic_branches").document("demo_btech_branch_$idx")
        batch.set(
          brRef,
          mapOf(
            "courseId" to "demo_btech",
            "name" to "[Demo] B.Tech Branch $idx (Configure in Admin)",
            "code" to "BR$idx",
            "displayOrder" to idx,
            "published" to true,
            "createdBy" to uid,
            "createdAt" to now,
            "updatedAt" to now,
          ),
        )
      }

      // 3. Configurable Exam Categories: 2 Internal + 1 External
      val examCats =
        listOf(
          Triple("demo_cat_int1", "Internal Exam 1", "INT1" to 1L),
          Triple("demo_cat_int2", "Internal Exam 2", "INT2" to 2L),
          Triple("demo_cat_ext", "External Examination", "EXT" to 3L),
        )
      examCats.forEach { (id, name, codeOrder) ->
        batch.set(
          db.collection("exam_categories").document(id),
          mapOf(
            "name" to name,
            "code" to codeOrder.first,
            "displayOrder" to codeOrder.second,
            "published" to true,
            "createdBy" to uid,
            "createdAt" to now,
            "updatedAt" to now,
          ),
        )
      }

      // 4. Sample Subjects
      batch.set(
        db.collection("academic_subjects").document("demo_sub_101"),
        mapOf(
          "courseId" to "demo_btech",
          "branchId" to "demo_btech_branch_1",
          "academicYear" to 1L,
          "semester" to 1L,
          "name" to "[Demo] Engineering Mathematics I",
          "code" to "BAS101",
          "credits" to 4L,
          "published" to true,
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )
      batch.set(
        db.collection("academic_subjects").document("demo_sub_bca101"),
        mapOf(
          "courseId" to "demo_bca",
          "branchId" to "",
          "academicYear" to 1L,
          "semester" to 1L,
          "name" to "[Demo] Fundamentals of Computing",
          "code" to "BCA101",
          "credits" to 4L,
          "published" to true,
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )

      // 5. Sample Notices
      batch.set(
        db.collection("notices").document("demo_notice_welcome"),
        mapOf(
          "title" to "[Demo] Welcome to College Core — ITG Gopeshwar",
          "message" to
            "This is a sample general notice for all students at Govt. Institute of Technology (ITG) Gopeshwar. Administrators can edit or delete demo notices and publish official campus updates from the Admin Dashboard.",
          "category" to "General",
          "targetCourseId" to "ALL",
          "targetBranchId" to "ALL",
          "targetYear" to 0L,
          "targetSemester" to 0L,
          "published" to true,
          "important" to true,
          "authorName" to "ITG Administration (Demo)",
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )
      batch.set(
        db.collection("notices").document("demo_notice_exam"),
        mapOf(
          "title" to "[Demo] Internal Examination 1 Schedule Notice",
          "message" to
            "Sample academic notice: Internal Examination 1 timetable and reporting instructions for B.Tech Year 1 students. Check the Academics tab for syllabus and previous papers.",
          "category" to "Exam",
          "targetCourseId" to "demo_btech",
          "targetBranchId" to "ALL",
          "targetYear" to 1L,
          "targetSemester" to 1L,
          "published" to true,
          "important" to false,
          "authorName" to "Examination Cell (Demo)",
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )

      // 6. Sample Syllabus
      batch.set(
        db.collection("syllabus_items").document("demo_syl_101"),
        mapOf(
          "courseId" to "demo_btech",
          "branchId" to "demo_btech_branch_1",
          "academicYear" to 1L,
          "semester" to 1L,
          "subjectId" to "demo_sub_101",
          "subjectName" to "[Demo] Engineering Mathematics I",
          "subjectCode" to "BAS101",
          "description" to
            "Unit 1: Matrices & Linear Algebra. Unit 2: Differential Calculus. Unit 3: Multivariable Calculus. Unit 4: Vector Calculus. (Demo syllabus entry — replace with official university syllabus PDF link.)",
          "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
          "fileSizeLabel" to "Demo PDF",
          "published" to true,
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )

      // 7. Sample Study Material
      batch.set(
        db.collection("study_materials").document("demo_note_101"),
        mapOf(
          "title" to "[Demo] Unit 1 Matrices & Eigenvalues Lecture Notes",
          "description" to
            "Sample study notes demonstrating unit organization, uploader info, and PDF preview.",
          "courseId" to "demo_btech",
          "branchId" to "demo_btech_branch_1",
          "academicYear" to 1L,
          "semester" to 1L,
          "subjectId" to "demo_sub_101",
          "subjectName" to "[Demo] Engineering Mathematics I",
          "unit" to "Unit 1",
          "resourceType" to "PDF",
          "fileUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
          "uploadedByName" to "Faculty (Demo)",
          "published" to true,
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )

      // 8. Sample Exam Paper
      batch.set(
        db.collection("exam_papers").document("demo_paper_101"),
        mapOf(
          "title" to "[Demo] BAS101 Internal Exam 1 Question Paper (2025)",
          "courseId" to "demo_btech",
          "branchId" to "demo_btech_branch_1",
          "academicYear" to 1L,
          "semester" to 1L,
          "subjectId" to "demo_sub_101",
          "subjectName" to "[Demo] Engineering Mathematics I",
          "examCategoryId" to "demo_cat_int1",
          "examCategoryName" to "Internal Exam 1",
          "examYear" to 2025L,
          "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
          "published" to true,
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )

      // 9. Sample Gallery Album + Photo
      batch.set(
        db.collection("gallery_albums").document("demo_album_campus"),
        mapOf(
          "title" to "[Demo] ITG Gopeshwar Campus & Tech Fest",
          "description" to
            "Sample campus event album. Admins can add real event albums and optimized photo links.",
          "eventCategory" to "Technical",
          "eventDateText" to "Nov 2026",
          "coverImageUrl" to "https://images.unsplash.com/photo-1562774053-701939374585?w=600",
          "photoCount" to 1L,
          "published" to true,
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )
      batch.set(
        db.collection("gallery_photos").document("demo_photo_1"),
        mapOf(
          "albumId" to "demo_album_campus",
          "caption" to "[Demo] Academic Block & Innovation Showcase",
          "imageUrl" to "https://images.unsplash.com/photo-1562774053-701939374585?w=1080",
          "thumbnailUrl" to "https://images.unsplash.com/photo-1562774053-701939374585?w=480",
          "published" to true,
          "createdBy" to uid,
          "createdAt" to now,
          "updatedAt" to now,
        ),
      )

      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "batch_seed_demo")
      Result.failure(e)
    }
  }
}
