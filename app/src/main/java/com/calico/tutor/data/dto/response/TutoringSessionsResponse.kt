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
    @SerializedName(value = "studentId", alternate = ["uid", "student_uid", "userId"])
    val studentId: String = "",
    @SerializedName("scheduledStart")
    val scheduledStart: String = "",
    @SerializedName("scheduledEnd")
    val scheduledEnd: String = "",
    @SerializedName("status")
    val status: String = "",
    @SerializedName("course")
    val course: String? = null,
    @SerializedName("courseId")
    val courseId: String? = null,
    @SerializedName(value = "studentName", alternate = ["student", "studentFullName", "name"])
    val studentName: String = "",
    @SerializedName(value = "studentAvatarUrl", alternate = ["avatarUrl", "profilePictureUrl", "profileImage"])
    val studentAvatarUrl: String = "",
    @SerializedName(value = "price", alternate = ["amount", "cost", "sessionPrice", "value"])
    val price: String = ""
)
