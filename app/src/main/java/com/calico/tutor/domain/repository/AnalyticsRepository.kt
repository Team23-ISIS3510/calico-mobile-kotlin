package com.calico.tutor.domain.repository

import com.calico.tutor.data.dto.response.SessionAlertResponse
import com.calico.tutor.domain.model.TutorAnalyticsResponse

/**
 * Repository para manejar operaciones de analítica
 */
interface AnalyticsRepository {
    
    /**
     * Obtiene analítica de tutorías (últimos 2 años)
     * @return Respuesta con analítica de todos los tutores
     */
    suspend fun getTutoringDemandAnalytics(): TutorAnalyticsResponse
    
    /**
     * Obtiene analítica para un tutor específico
     * @param tutorId ID del tutor
     * @return Respuesta con analítica del tutor
     */
    suspend fun getTutorDemandAnalytics(tutorId: String): TutorAnalyticsResponse

    /**
     * Gets session alert information from the server
     * @return SessionAlertResponse containing alert status and session details
     */
    suspend fun getSessionAlert(): SessionAlertResponse
}

