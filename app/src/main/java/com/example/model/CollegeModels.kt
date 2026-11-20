package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class AdminAccount(
  @DocumentId val id: String = "",
  val uid: String = "",
  val email: String = "",
  val displayName: String = "",
  val addedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StudentProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val email: String = "",
  val rollNumber: String = "",
  val courseId: String = "",
  val courseName: String = "",
  val branchId: String = "",
  val branchName: String = "",
  val academicYear: Long = 1L,
  val semester: Long = 1L,
  val photoUrl: String = "",
  val profileCompleted: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ReadNotice(
  @DocumentId val id: String = "",
  val noticeId: String = "",
  @ServerTimestamp val readAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AcademicCourse(
  @DocumentId val id: String = "",
  val name: String = "",
  val code: String = "",
  val durationYears: Long = 4L,
  val totalSemesters: Long = 8L,
  val hasBranches: Boolean = true,
  val published: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AcademicBranch(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val name: String = "",
  val code: String = "",
  val displayOrder: Long = 0L,
  val published: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AcademicSubject(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val branchId: String = "",
  val academicYear: Long = 1L,
  val semester: Long = 1L,
  val name: String = "",
  val code: String = "",
  val credits: Long = 4L,
  val published: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ExamCategory(
  @DocumentId val id: String = "",
  val name: String = "",
  val code: String = "",
  val displayOrder: Long = 0L,
  val published: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Notice(
  @DocumentId val id: String = "",
  val title: String = "",
  val message: String = "",
  val category: String = "General",
  val attachmentUrl: String = "",
  val attachmentName: String = "",
  val targetCourseId: String = "ALL",
  val targetBranchId: String = "ALL",
  val targetYear: Long = 0L,
  val targetSemester: Long = 0L,
  val published: Boolean = true,
  val important: Boolean = false,
  val authorName: String = "",
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun matchesStudent(profile: StudentProfile?): Boolean {
    if (profile == null) return true
    val courseMatch = targetCourseId == "ALL" || targetCourseId == profile.courseId
    val branchMatch = targetBranchId == "ALL" || targetBranchId.isBlank() || targetBranchId == profile.branchId
    val yearMatch = targetYear == 0L || targetYear == profile.academicYear
    val semMatch = targetSemester == 0L || targetSemester == profile.semester
    return courseMatch && branchMatch && yearMatch && semMatch
  }
}

@Keep
@IgnoreExtraProperties
data class SyllabusItem(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val branchId: String = "",
  val academicYear: Long = 1L,
  val semester: Long = 1L,
  val subjectId: String = "",
  val subjectName: String = "",
  val subjectCode: String = "",
  val description: String = "",
  val pdfUrl: String = "",
  val fileSizeLabel: String = "",
  val published: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StudyMaterial(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val courseId: String = "",
  val branchId: String = "",
  val academicYear: Long = 1L,
  val semester: Long = 1L,
  val subjectId: String = "",
  val subjectName: String = "",
  val unit: String = "",
  val resourceType: String = "PDF",
  val fileUrl: String = "",
  val uploadedByName: String = "",
  val published: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ExamPaper(
  @DocumentId val id: String = "",
  val title: String = "",
  val courseId: String = "",
  val branchId: String = "",
  val academicYear: Long = 1L,
  val semester: Long = 1L,
  val subjectId: String = "",
  val subjectName: String = "",
  val examCategoryId: String = "",
  val examCategoryName: String = "",
  val examYear: Long = 2025L,
  val pdfUrl: String = "",
  val published: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class GalleryAlbum(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val eventCategory: String = "Campus",
  val eventDateText: String = "",
  val coverImageUrl: String = "",
  val photoCount: Long = 0L,
  val published: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class GalleryPhoto(
  @DocumentId val id: String = "",
  val albumId: String = "",
  val caption: String = "",
  val imageUrl: String = "",
  val thumbnailUrl: String = "",
  val published: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}
