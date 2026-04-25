package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class TutorCoursesResponse(
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("tutorId")
    val tutorId: String = "",
    @SerializedName("courses")
    val courses: List<TutorCourseData> = emptyList()
)

data class TutorCourseData(
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
    @SerializedName("enrollmentCount")
    val enrollmentCount: Int = 0,
    @SerializedName("maxCapacity")
    val maxCapacity: Int = 0
)
