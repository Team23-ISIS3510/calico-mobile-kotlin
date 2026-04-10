package com.calico.tutor.domain.repository

import com.calico.tutor.data.dto.response.TutorOccupancyResponse
import com.calico.tutor.domain.utils.Result

/**
 * Repository Pattern para Occupancy Feature
 * 
 * Abstracción que define el contrato para obtener datos de ocupancy
 * SIN conocer detalles de implementación (HTTP, JSON, etc)
 */
interface OccupancyRepository {
    
    /**
     * Obtiene data de ocupancy para un tutor específico
     * 
     * @param tutorId ID del tutor
     * @return TutorOccupancyResponse con datos de ocupancy
     * 
     * NOTA: la implementación puede obtener datos de:
     * - API REST
     * - Base de datos local
     * - Cache
     * - Mock (para tests)
     */
    suspend fun getTutorOccupancy(tutorId: String): Result<TutorOccupancyResponse>
}
