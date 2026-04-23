package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class CourseApplicationResponse(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("tutorId")
    val tutorId: String = "",
    @SerializedName("courseId")
    val courseId: String = "",
    @SerializedName("courseName")
    val courseName: String = "",
    @SerializedName("courseCode")
    val courseCode: String = "",
    @SerializedName("status")
    val status: String = "", // pending, approved, rejected
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("rejectionReason")
    val rejectionReason: String? = null,
    @SerializedName("appliedAt")
    val appliedAt: Any? = null, // Can be String or Firestore Timestamp object
    @SerializedName("reviewedAt")
    val reviewedAt: Any? = null // Can be String or Firestore Timestamp object
)

data class AvailableCourseResponse(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("code")
    val code: String = "",
    @SerializedName("credits")
    val credits: Int = 0,
    @SerializedName("difficulty")
    val difficulty: String = "",
    @SerializedName("faculty")
    val faculty: String? = null,
    @SerializedName("enrollmentCount")
    val enrollmentCount: Int? = null,
    @SerializedName("maxCapacity")
    val maxCapacity: Int? = null,
    @SerializedName("hasApplied")
    val hasApplied: Boolean = false
)

data class AllCoursesResponse(
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("courses")
    val courses: List<AvailableCourseResponse> = emptyList(),
    @SerializedName("count")
    val count: Int = 0
)
