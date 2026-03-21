package com.calico.tutor.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs para Analytics API
 */

data class AvailabilityDto(
    @SerializedName("tutorId")
    val tutorId: String,
    
    @SerializedName("startTime")
    val startTime: String,
    
    @SerializedName("endTime")
    val endTime: String,
    
    @SerializedName("date")
    val date: String? = null,
    
    @SerializedName("dayOfWeek")
    val dayOfWeek: Int? = null
)

data class AvailabilityResponseDto(
    @SerializedName("availabilities")
    val availabilities: List<AvailabilityDto> = emptyList(),
    
    @SerializedName("data")
    val data: List<AvailabilityDto>? = null // Para ambos posibles formatos
) {
    fun getAll(): List<AvailabilityDto> = availabilities.ifEmpty { data ?: emptyList() }
}

data class DemandMetricsDto(
    @SerializedName("sessionsPerHour")
    val sessionsPerHour: Double = 0.0,
    
    @SerializedName("occupancyRate")
    val occupancyRate: Double = 0.0
)

data class TutorSubjectAnalyticsDto(
    @SerializedName("tutorId")
    val tutorId: String,
    
    @SerializedName("tutorName")
    val tutorName: String,
    
    @SerializedName("subject")
    val subject: String,
    
    @SerializedName("subjectCode")
    val subjectCode: String,
    
    @SerializedName("totalSessions")
    val totalSessions: Int,
    
    @SerializedName("totalAvailableHours")
    val totalAvailableHours: Double,
    
    @SerializedName("highDemandMetrics")
    val highDemandMetrics: DemandMetricsDto,
    
    @SerializedName("normalDemandMetrics")
    val normalDemandMetrics: DemandMetricsDto,
    
    @SerializedName("overallSessionsPerHour")
    val overallSessionsPerHour: Double,
    
    @SerializedName("overallOccupancyRate")
    val overallOccupancyRate: Double,
    
    @SerializedName("occupancyLevel")
    val occupancyLevel: String,
    
    @SerializedName("periodStartDate")
    val periodStartDate: String,
    
    @SerializedName("periodEndDate")
    val periodEndDate: String,
    
    @SerializedName("lastUpdated")
    val lastUpdated: String
)

data class TutorAnalyticsResponseDto(
    @SerializedName("analytics")
    val analytics: List<TutorSubjectAnalyticsDto>,
    
    @SerializedName("totalTutors")
    val totalTutors: Int,
    
    @SerializedName("analysisStartDate")
    val analysisStartDate: String,
    
    @SerializedName("analysisEndDate")
    val analysisEndDate: String,
    
    @SerializedName("timestamp")
    val timestamp: String
)
