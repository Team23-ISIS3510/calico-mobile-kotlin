package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class TutorCourseNoteResponseDto(
    @SerializedName("tutorId")
    val tutorId: String = "",
    @SerializedName("courseId")
    val courseId: String = "",
    @SerializedName("note")
    val note: String? = null
)
