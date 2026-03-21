package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class TutorResponse(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("isTutor")
    val isTutor: Boolean = false,
    @SerializedName("rating")
    val rating: Double? = null,
    @SerializedName("hourlyRate")
    val hourlyRate: Double? = null,
    @SerializedName("bio")
    val bio: String? = null,
    @SerializedName("courses")
    val courses: List<String>? = null,
    @SerializedName("profileImage")
    val profileImage: String? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("totalAvailabilities")
    val totalAvailabilities: Int? = null,
    @SerializedName("hasAvailability")
    val hasAvailability: Boolean = false,
    @SerializedName("upcomingSessions")
    val upcomingSessions: Int? = null
)
