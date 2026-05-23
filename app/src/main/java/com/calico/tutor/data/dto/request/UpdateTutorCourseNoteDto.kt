package com.calico.tutor.data.dto.request

import com.google.gson.annotations.SerializedName

data class UpdateTutorCourseNoteDto(
    @SerializedName("note")
    val note: String
)
