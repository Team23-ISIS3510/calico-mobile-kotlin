package com.calico.tutor.data.repository

import com.calico.tutor.data.datasource.remote.SubjectsApiService
import com.calico.tutor.data.dto.response.TutorOccupancyResponse
import com.calico.tutor.domain.repository.OccupancyRepository
import com.calico.tutor.domain.utils.Result

/**
 * Implementación del Repository Pattern para Occupancy
 * 
 * Responsabilidades:
 * ✅ Obtener datos del DataSource (SubjectsApiService)
 * ✅ Transformar DTOs a Domain Models si es necesario
 * ✅ Manejar errores y convertirlos a Result<T>
 * ✅ Abstraer al ViewModel de los detalles de obtención de datos
 * 
 * El ViewModel NO conoce que existe:
 * ❌ HTTP
 * ❌ JSON
 * ❌ Retrofit
 * ❌ APIs endpoints
 */
class OccupancyRepositoryImpl(
    private val subjectsApiService: SubjectsApiService
) : OccupancyRepository {
    
    /**
     * Obtiene ocupancy del backend a través del DataSource
     * 
     * Flujo:
     * 1. Llama al DataSource (API)
     * 2. Mapea DTO → Domain Model si es necesario
     * 3. Retorna Result<TutorOccupancyResponse>
     */
    override suspend fun getTutorOccupancy(tutorId: String): Result<TutorOccupancyResponse> {
        return try {
            // Step 1: Obtener datos del DataSource (API)
            val response = subjectsApiService.getTutorOccupancy(tutorId)
            
            // Step 2: Validar respuesta
            if (response.success) {
                // Step 3: Retornar Success
                Result.Success(response)
            } else {
                Result.Error(
                    exception = Exception("API returned false for occupancy"),
                    message = "Failed to fetch occupancy data"
                )
            }
        } catch (e: Exception) {
            // Step 4: Manejar errores
            Result.Error(
                exception = e,
                message = "Error fetching occupancy: ${e.message}"
            )
        }
    }
}
