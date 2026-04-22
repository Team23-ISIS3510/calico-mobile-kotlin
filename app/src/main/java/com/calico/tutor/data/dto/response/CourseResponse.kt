package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class CourseResponse(
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("course")
    val course: CourseDetail? = null
)

data class CourseDetail(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "Unknown Course",
    @SerializedName("code")
    val code: String = "",
    @SerializedName("credits")
    val credits: Int = 0,
    @SerializedName("faculty")
    val faculty: String = "",
    @SerializedName("description")
    val description: String = "",
    @SerializedName("prerequisites")
    val prerequisites: List<String> = emptyList(),
    @SerializedName("difficulty")
    val difficulty: String = "",
    @SerializedName("semester")
    val semester: String = ""
)
