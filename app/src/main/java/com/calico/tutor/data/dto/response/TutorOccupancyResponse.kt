package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class TutorOccupancyResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("tutorId")
    val tutorId: String = "",
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("data")
    val data: List<TutorOccupancyData> = emptyList()
)

data class TutorOccupancyData(
    @SerializedName("tutorId")
    val tutorId: String = "",
    @SerializedName("subject")
    val subject: String = "",
    @SerializedName("totalSessions")
    val totalSessions: Int = 0,
    @SerializedName("totalAvailableHours")
    val totalAvailableHours: Double = 0.0,
    @SerializedName("sessionsPerHour")
    val sessionsPerHour: Double = 0.0,
    @SerializedName("occupancyRate")
    val occupancyRate: Double = 0.0,
    @SerializedName("highDemand")
    val highDemand: DemandMetrics? = null,
    @SerializedName("normalDemand")
    val normalDemand: DemandMetrics? = null
)

data class DemandMetrics(
    @SerializedName("sessionsPerHour")
    val sessionsPerHour: Double = 0.0,
    @SerializedName("occupancyRate")
    val occupancyRate: Double = 0.0,
    @SerializedName("totalSessions")
    val totalSessions: Int? = null,
    @SerializedName("totalHoursOccupied")
    val totalHoursOccupied: Double? = null
)
