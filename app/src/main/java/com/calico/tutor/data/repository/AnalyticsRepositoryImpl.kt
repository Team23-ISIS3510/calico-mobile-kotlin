package com.calico.tutor.data.repository

import com.calico.tutor.data.datasource.remote.SubjectsApiService
import com.calico.tutor.data.service.TutoringDemandCalculatorService
import com.calico.tutor.domain.model.TutorAnalyticsResponse
import com.calico.tutor.domain.repository.AnalyticsRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * Implementación del repositorio de analítica
 * Obtiene datos de los endpoints existentes de SubjectsApiService y calcula analítica localmente
 */
class AnalyticsRepositoryImpl(
    private val subjectsApiService: SubjectsApiService
) : AnalyticsRepository {
    
    private val calculator = TutoringDemandCalculatorService()
    
    override suspend fun getTutoringDemandAnalytics(): TutorAnalyticsResponse {
        // Obtener rango de 2 años
        val (startDateStr, endDateStr) = calculator.getTwoYearsRange()
        
        // Obtener sesiones del rango
        val sessionsResponse = subjectsApiService.getSessionsByDateRange(startDateStr, endDateStr)
        val sessions = sessionsResponse.sessions
        
        if (sessions.isEmpty()) {
            return TutorAnalyticsResponse(
                analytics = emptyList(),
                totalTutors = 0,
                analysisStartDate = startDateStr,
                analysisEndDate = endDateStr,
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .format(Calendar.getInstance().time)
            )
        }
        
        // Obtener todos los tutores únicos
        val tutorIds: List<String> = sessions.map { it.tutorId }.distinct()
        
        // Obtener disponibilidad de cada tutor
        val allAvailabilities: MutableList<com.calico.tutor.data.dto.AvailabilityDto> = mutableListOf()
        for (tutorId: String in tutorIds) {
            try {
                val availabilityResponse = subjectsApiService.getAvailability(tutorId)
                val tutorAvails = availabilityResponse.getAll()
                allAvailabilities.addAll(tutorAvails)
            } catch (e: Exception) {
                // Si falla disponibilidad de un tutor, continuar con otros
            }
        }
        
        // Calcular analítica
        return calculator.calculateAnalytics(sessions, allAvailabilities)
    }
    
    override suspend fun getTutorDemandAnalytics(tutorId: String): TutorAnalyticsResponse {
        // Obtener rango de 2 años
        val (startDateStr, endDateStr) = calculator.getTwoYearsRange()
        
        // Obtener sesiones del rango
        val sessionsResponse = subjectsApiService.getSessionsByDateRange(startDateStr, endDateStr)
        val sessions: List<com.calico.tutor.domain.model.Session> = sessionsResponse.sessions.filter { session ->
            session.tutorId == tutorId
        }
        
        if (sessions.isEmpty()) {
            return TutorAnalyticsResponse(
                analytics = emptyList(),
                totalTutors = 0,
                analysisStartDate = startDateStr,
                analysisEndDate = endDateStr,
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .format(Calendar.getInstance().time)
            )
        }
        
        // Obtener disponibilidad del tutor
        val availabilityResponse = subjectsApiService.getAvailability(tutorId)
        val availabilities = availabilityResponse.getAll()
        
        // Calcular analítica
        return calculator.calculateAnalytics(sessions, availabilities)
    }
}

