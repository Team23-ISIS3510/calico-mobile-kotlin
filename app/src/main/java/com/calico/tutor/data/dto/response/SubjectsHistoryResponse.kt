package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class SubjectsHistoryResponse(
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("data")
    val data: List<CourseData>? = emptyList()
)

data class CourseData(
    @SerializedName("courseId")
    val courseId: String = "",
    @SerializedName("course")
    val course: String? = null
)
