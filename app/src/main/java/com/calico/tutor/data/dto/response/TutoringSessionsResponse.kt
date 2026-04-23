package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class TutoringSessionsResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("sessions")
    val sessions: List<TutoringSessionData> = emptyList()
)

data class TutoringSessionData(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("scheduledStart")
    val scheduledStart: String = "",
    @SerializedName("scheduledEnd")
    val scheduledEnd: String = "",
    @SerializedName("status")
    val status: String = "",
    @SerializedName("course")
    val course: String? = null,
    @SerializedName("courseId")
    val courseId: String? = null
)
