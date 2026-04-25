package com.calico.tutor.data.repository

import com.calico.tutor.data.datasource.remote.SubjectsApiService
import com.calico.tutor.data.dto.response.TutoringSessionsResponse
import com.calico.tutor.domain.repository.SessionsRepository
import com.calico.tutor.domain.utils.Result
import android.util.Log

/**
 * Implementación del Repository Pattern para Sessions
 * 
 * Responsabilidades:
 * ✅ Obtener Previous Sessions del DataSource
 * ✅ Obtener Upcoming Sessions del DataSource
 * ✅ Manejar errores y convertirlos a Result<T>
 * ✅ Abstraer al ViewModel de los detalles de HTTP
 * 
 * El ViewModel usa SessionsRepository, nunca SubjectsApiService directamente
 */
class SessionsRepositoryImpl(
    private val subjectsApiService: SubjectsApiService
) : SessionsRepository {
    
    private val TAG = "SessionsRepository"
    
    /**
     * Obtiene sesiones previas a través del DataSource
     */
    override suspend fun getPreviousSessions(tutorId: String): Result<TutoringSessionsResponse> {
        return try {
            Log.d(TAG, "Fetching previous sessions for tutor: $tutorId")
            
            // Step 1: Obtener datos del DataSource (API)
            val response = subjectsApiService.getTutoringSessionsForTutor(tutorId)
            
            // Step 2: Validar
            if (response.sessions.isNotEmpty()) {
                Log.d(TAG, "Previous sessions fetched: ${response.sessions.size}")
                Result.Success(response)
            } else {
                Log.d(TAG, "No previous sessions found")
                Result.Success(response) // Retornar lista vacía es válido
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching previous sessions: ${e.message}")
            Result.Error(
                exception = e,
                message = "Error fetching previous sessions: ${e.message}"
            )
        }
    }
    
    /**
     * Obtiene sesiones próximas a través del DataSource
     */
    override suspend fun getUpcomingSessions(tutorId: String): Result<TutoringSessionsResponse> {
        return try {
            Log.d(TAG, "Fetching upcoming sessions for tutor: $tutorId")
            
            // Step 1: Obtener datos del DataSource (API)
            val response = subjectsApiService.getTutoringSessionsForTutor(tutorId)
            
            // Step 2: Validar
            if (response.sessions.isNotEmpty()) {
                Log.d(TAG, "Upcoming sessions fetched: ${response.sessions.size}")
                Result.Success(response)
            } else {
                Log.d(TAG, "No upcoming sessions found")
                Result.Success(response) // Retornar lista vacía es válido
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching upcoming sessions: ${e.message}")
            Result.Error(
                exception = e,
                message = "Error fetching upcoming sessions: ${e.message}"
            )
        }
    }
}
