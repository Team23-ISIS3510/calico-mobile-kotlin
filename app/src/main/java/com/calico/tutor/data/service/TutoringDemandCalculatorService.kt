package com.calico.tutor.data.service

import com.calico.tutor.domain.model.OccupancyLevel
import com.calico.tutor.domain.model.DemandMetrics
import com.calico.tutor.domain.model.TutorSubjectAnalytics
import com.calico.tutor.domain.model.TutorAnalyticsResponse
import com.calico.tutor.data.dto.AvailabilityDto
import com.calico.tutor.domain.model.Session
import java.text.SimpleDateFormat
import java.util.*

/**
 * Servicio que calcula la analítica de demanda de tutorías
 * Realiza todos los cálculos localmente en el frontend
 */
class TutoringDemandCalculatorService {

    /**
     * Períodos de alta demanda durante el año
     */
    private val highDemandPeriods = listOf(
        // Marzo 1-15
        Triple(3, 1, 15),
        // Mayo 17-31
        Triple(5, 17, 31),
        // Septiembre 13-27
        Triple(9, 13, 27),
        // Noviembre 29-30
        Triple(11, 29, 30),
        // Diciembre 1-6
        Triple(12, 1, 6)
    )

    /**
     * Verifica si una fecha está en período de alta demanda
     */
    fun isHighDemandPeriod(dateString: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString) ?: return false
            val calendar = Calendar.getInstance().apply { time = date }
            
            val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH es 0-indexed
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            highDemandPeriods.any { (demandMonth, startDay, endDay) ->
                month == demandMonth && day >= startDay && day <= endDay
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calcula la duración en horas entre dos horarios
     */
    fun calculateDurationHours(startTime: String, endTime: String): Double {
        return try {
            val (startH, startM) = startTime.split(":").map { it.toInt() }
            val (endH, endM) = endTime.split(":").map { it.toInt() }
            
            val startMinutes = startH * 60 + startM
            val endMinutes = endH * 60 + endM
            
            val durationMinutes = if (endMinutes >= startMinutes) {
                endMinutes - startMinutes
            } else {
                // Si es hora de noche (ej: 22:00 a 06:00 del día siguiente)
                (24 * 60 - startMinutes) + endMinutes
            }
            
            (durationMinutes / 60.0)
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Obtiene rango de 2 años atrás desde hoy
     */
    fun getTwoYearsRange(): Pair<String, String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val endDate = Calendar.getInstance()
        val startDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, -2)
        }
        
        return Pair(
            dateFormat.format(startDate.time),
            dateFormat.format(endDate.time)
        )
    }

    /**
     * Calcula las métricas para un conjunto de sesiones
     */
    private fun calculateDemandMetrics(
        sessions: List<Session>,
        totalAvailableHours: Double
    ): DemandMetrics {
        if (totalAvailableHours == 0.0) {
            return DemandMetrics(sessionsPerHour = 0.0, occupancyRate = 0.0)
        }

        val sessionsPerHour = sessions.size / totalAvailableHours
        // Normalizar ocupación como porcentaje
        val occupancyRate = Math.min(100.0, sessionsPerHour * 50.0)

        return DemandMetrics(
            sessionsPerHour = Math.round(sessionsPerHour * 100.0) / 100.0,
            occupancyRate = Math.round(occupancyRate * 100.0) / 100.0
        )
    }

    /**
     * Determina el nivel de ocupación basado en la tasa
     */
    private fun getOccupancyLevel(occupancyRate: Double): OccupancyLevel {
        return when {
            occupancyRate > 80.0 -> OccupancyLevel.OVERLOADED
            occupancyRate >= 40.0 -> OccupancyLevel.MODERATE
            else -> OccupancyLevel.AVAILABLE
        }
    }

    /**
     * Calcula analítica completa a partir de sesiones y disponibilidad
     */
    fun calculateAnalytics(
        sessions: List<Session>,
        availabilities: List<AvailabilityDto>
    ): TutorAnalyticsResponse {
        val (startDateStr, endDateStr) = getTwoYearsRange()
        
        // Agrupar sesiones por tutor y materia
        val filteredSessions: List<com.calico.tutor.domain.model.Session> = sessions
            .filter { session -> session.status != "cancelled" } // Excluir canceladas
        
        val groupedByTutorSubject: Map<String, List<com.calico.tutor.domain.model.Session>> = filteredSessions
            .groupBy { session -> "${session.tutorId}:${session.subjectName}" }

        val analytics = mutableListOf<TutorSubjectAnalytics>()
        val processedTutors = mutableSetOf<String>()

        groupedByTutorSubject.forEach { (key, tutorSubjectSessions) ->
            if (tutorSubjectSessions.isEmpty()) return@forEach

            val firstSession: com.calico.tutor.domain.model.Session = tutorSubjectSessions.first()
            val tutorId: String = firstSession.tutorId

            // Obtener disponibilidad del tutor
            val tutorAvailabilities: List<com.calico.tutor.data.dto.AvailabilityDto> = availabilities.filter { avail -> 
                avail.tutorId == tutorId 
            }
            
            // Calcular total de horas disponibles
            val totalAvailableHours = tutorAvailabilities.sumOf { avail ->
                calculateDurationHours(avail.startTime, avail.endTime)
            }

            processedTutors.add(tutorId)

            // Separar sesiones por demanda
            val highDemandSessions = tutorSubjectSessions.filter { 
                isHighDemandPeriod(it.date)
            }
            val normalDemandSessions = tutorSubjectSessions.filter { 
                !isHighDemandPeriod(it.date)
            }

            // Calcular métricas
            val highDemandMetrics = calculateDemandMetrics(highDemandSessions, totalAvailableHours)
            val normalDemandMetrics = calculateDemandMetrics(normalDemandSessions, totalAvailableHours)

            // Métricas combinadas
            val overallSessionsPerHour = if (totalAvailableHours > 0) {
                tutorSubjectSessions.size / totalAvailableHours
            } else {
                0.0
            }
            
            val overallOccupancyRate = Math.max(
                highDemandMetrics.occupancyRate,
                normalDemandMetrics.occupancyRate
            )

            analytics.add(
                TutorSubjectAnalytics(
                    tutorId = tutorId,
                    tutorName = firstSession.tutorName,
                    subject = firstSession.subjectName,
                    subjectCode = firstSession.subjectCode,
                    totalSessions = tutorSubjectSessions.size,
                    totalAvailableHours = Math.round(totalAvailableHours * 100.0) / 100.0,
                    highDemandMetrics = highDemandMetrics,
                    normalDemandMetrics = normalDemandMetrics,
                    overallSessionsPerHour = Math.round(overallSessionsPerHour * 100.0) / 100.0,
                    overallOccupancyRate = Math.round(overallOccupancyRate * 100.0) / 100.0,
                    occupancyLevel = getOccupancyLevel(overallOccupancyRate),
                    periodStartDate = startDateStr,
                    periodEndDate = endDateStr,
                    lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .format(Calendar.getInstance().time)
                )
            )
        }

        // Ordenar por ocupación (descendente)
        analytics.sortByDescending { it.overallOccupancyRate }

        return TutorAnalyticsResponse(
            analytics = analytics,
            totalTutors = processedTutors.size,
            analysisStartDate = startDateStr,
            analysisEndDate = endDateStr,
            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(Calendar.getInstance().time)
        )
    }
}
