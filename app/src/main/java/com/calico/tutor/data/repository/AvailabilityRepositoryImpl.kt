package com.calico.tutor.data.repository

import android.util.Log
import com.calico.tutor.data.datasource.remote.AvailabilityApiService
import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.domain.repository.AvailabilityRepository
import com.calico.tutor.domain.utils.Result

private const val TAG = "AvailabilityRepo"

class AvailabilityRepositoryImpl(
    private val apiService: AvailabilityApiService
) : AvailabilityRepository {

    override suspend fun getAvailabilities(tutorId: String): Result<List<AvailabilityItem>> {
        return try {
            Log.d(TAG, "Cargando disponibilidades para tutor: $tutorId")
            val response = apiService.getAvailabilities(tutorId)
            val items = response.map { it.toModel() }
            Log.d(TAG, "Disponibilidades cargadas: ${items.size}")
            Result.Success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando disponibilidades: ${e.message}", e)
            Result.Error(e, e.localizedMessage ?: "Error cargando disponibilidades")
        }
    }

    override suspend fun createAvailability(request: CreateAvailabilityRequest): Result<AvailabilityItem> {
        return try {
            Log.d(TAG, "Creando disponibilidad: ${request.date} ${request.startTime}-${request.endTime}")
            val response = apiService.createAvailability(request)
            Log.d(TAG, "Disponibilidad creada con id: ${response.id}")
            Result.Success(response.toModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error creando disponibilidad: ${e.message}", e)
            Result.Error(e, e.localizedMessage ?: "Error creando disponibilidad")
        }
    }

    override suspend fun updateAvailability(id: Int, request: UpdateAvailabilityRequest): Result<AvailabilityItem> {
        return try {
            Log.d(TAG, "Actualizando disponibilidad id: $id")
            val response = apiService.updateAvailability(id, request)
            Log.d(TAG, "Disponibilidad actualizada")
            Result.Success(response.toModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando disponibilidad: ${e.message}", e)
            Result.Error(e, e.localizedMessage ?: "Error actualizando disponibilidad")
        }
    }

    override suspend fun deleteAvailability(id: Int): Result<Unit> {
        return try {
            Log.d(TAG, "Eliminando disponibilidad id: $id")
            apiService.deleteAvailability(id)
            Log.d(TAG, "Disponibilidad eliminada")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando disponibilidad: ${e.message}", e)
            Result.Error(e, e.localizedMessage ?: "Error eliminando disponibilidad")
        }
    }

    private fun com.calico.tutor.data.dto.response.AvailabilityResponse.toModel() = AvailabilityItem(
        id = id,
        title = title,
        date = date,
        startTime = startTime,
        endTime = endTime,
        location = location,
        description = description,
        course = course
    )
}
