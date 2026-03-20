package com.calico.tutor.domain.model

/**
 * Métricas de demanda por período (alto/normal)
 */
data class DemandMetrics(
    val sessionsPerHour: Double = 0.0,
    val occupancyRate: Double = 0.0  // Porcentaje 0-100
)

/**
 * Clasificación de ocupación del tutor
 */
enum class OccupancyLevel {
    OVERLOADED,      // 🔴 Sobrecargado (>80%)
    MODERATE,        // 🟡 Moderado (40-80%)
    AVAILABLE        // 🟢 Disponible (<40%)
}

/**
 * Métrica de analítica por tutor y materia
 */
data class TutorSubjectAnalytics(
    val tutorId: String,
    val tutorName: String,
    val subject: String,
    val subjectCode: String,
    
    // Métricas generales
    val totalSessions: Int,
    val totalAvailableHours: Double,
    
    // Desglose por demanda
    val highDemandMetrics: DemandMetrics,
    val normalDemandMetrics: DemandMetrics,
    
    // Métricas combinadas
    val overallSessionsPerHour: Double,
    val overallOccupancyRate: Double,
    val occupancyLevel: OccupancyLevel,
    
    // Metadata
    val periodStartDate: String,
    val periodEndDate: String,
    val lastUpdated: String
)

/**
 * Respuesta del endpoint de analítica
 */
data class TutorAnalyticsResponse(
    val analytics: List<TutorSubjectAnalytics>,
    val totalTutors: Int,
    val analysisStartDate: String,
    val analysisEndDate: String,
    val timestamp: String
)
