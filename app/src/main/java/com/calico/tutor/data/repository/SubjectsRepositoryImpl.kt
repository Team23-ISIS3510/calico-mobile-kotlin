package com.calico.tutor.data.repository

import com.calico.tutor.data.datasource.remote.SubjectsApiService
import com.calico.tutor.data.dto.response.SubjectsHistoryResponse
import com.calico.tutor.domain.repository.SubjectsRepository
import com.calico.tutor.domain.utils.Result
import android.util.Log

/**
 * Implementación del Repository Pattern para Subjects (Recommended Subjects)
 * 
 * El ViewModel usa SubjectsRepository sin conocer DetallesImpl
 */
class SubjectsRepositoryImpl(
    private val subjectsApiService: SubjectsApiService
) : SubjectsRepository {
    
    private val TAG = "SubjectsRepository"

    override suspend fun getSubjectsHistory(): Result<SubjectsHistoryResponse> {
        return try {
            Log.d(TAG, "Fetching subjects history...")
            
            // Step 1: Obtener datos del DataSource (API)
            val response = subjectsApiService.getSubjectsHistory()
            
            // Step 2: Validar
            if (response.data != null && response.data.isNotEmpty()) {
                Log.d(TAG, "Subjects history fetched: ${response.data.size} subjects")
                
                // Step 3 (Opcional): Aplicar lógica de negocio aquí si es necesaria
                // Ejemplo: filtrar, ordenar, cachear, etc.
                
                Result.Success(response)
            } else {
                Log.d(TAG, "No subjects history available")
                Result.Success(response) // Retornar respuesta vacía es válido
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching subjects history: ${e.message}")
            Result.Error(
                exception = e,
                message = "Error fetching subjects history: ${e.message}"
            )
        }
    }
}
