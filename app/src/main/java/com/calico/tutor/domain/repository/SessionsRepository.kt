package com.calico.tutor.domain.repository

import com.calico.tutor.data.dto.response.TutoringSessionsResponse
import com.calico.tutor.domain.utils.Result

/**
 * Repository Pattern para Sessions Feature (Previous & Upcoming)
 * 
 * Abstracción que define el contrato para obtener sesiones de tutorías
 * SIN conocer detalles de HTTP, JSON, endpoint específico, etc.
 */
interface SessionsRepository {
    
    /**
     * Obtiene sesiones previas de un tutor
     * 
     * @param tutorId ID del tutor
     * @return Resultado con lista de sesiones previas
     */
    suspend fun getPreviousSessions(tutorId: String): Result<TutoringSessionsResponse>
    
    /**
     * Obtiene sesiones próximas de un tutor
     * 
     * @param tutorId ID del tutor
     * @return Resultado con lista de sesiones próximas
     */
    suspend fun getUpcomingSessions(tutorId: String): Result<TutoringSessionsResponse>
}
