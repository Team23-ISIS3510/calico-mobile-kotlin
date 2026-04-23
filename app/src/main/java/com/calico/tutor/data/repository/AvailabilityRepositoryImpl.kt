package com.calico.tutor.data.repository

import android.util.Log
import com.calico.tutor.data.datasource.remote.AvailabilityApiService
import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.domain.repository.AvailabilityRepository
import com.calico.tutor.domain.utils.Result
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "AvailabilityRepo"

class AvailabilityRepositoryImpl(
    private val apiService: AvailabilityApiService
) : AvailabilityRepository {

    override suspend fun getAvailabilities(tutorId: String): Result<List<AvailabilityItem>> {
        return try {
            Log.d(TAG, "Cargando disponibilidades para tutor: $tutorId")
            val response = apiService.getAvailabilities(tutorId)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val items = response.getList()
                .map { it.toModel() }
                .filter { it.date > today }
            Log.d(TAG, "Disponibilidades futuras: ${items.size}")
            Result.Success(items)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Log.d(TAG, "No hay disponibilidades registradas para: $tutorId")
                Result.Success(emptyList())
            } else {
                Log.e(TAG, "HTTP ${e.code()} cargando disponibilidades: ${e.message()}", e)
                Result.Error(e, "Error del servidor (${e.code()})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando disponibilidades: ${e.message}", e)
            Result.Error(e, e.localizedMessage ?: "Error cargando disponibilidades")
        }
    }

    override suspend fun createAvailability(request: CreateAvailabilityRequest): Result<AvailabilityItem> {
        return try {
            Log.d(TAG, "POST /availability/create → ${request.date} ${request.startTime}-${request.endTime}")
            val response = apiService.createAvailability(request)
            Log.d(TAG, "Creada con id: ${response.id}")
            Result.Success(response.toModel())
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP ${e.code()} al crear disponibilidad: ${e.message()}")
            Result.Error(e, "Server error (${e.code()}). Please try again later")
        } catch (e: Exception) {
            Log.e(TAG, "Error creando disponibilidad: ${e.message}", e)
            Result.Error(e, "Server error. Please try again later")
        }
    }

    override suspend fun updateAvailability(id: String, request: UpdateAvailabilityRequest): Result<AvailabilityItem> {
        return try {
            Log.d(TAG, "PUT /availability/$id")
            val response = apiService.updateAvailability(id, request)
            Log.d(TAG, "Actualizada disponibilidad: $id")
            Result.Success(response.toModel())
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP ${e.code()} al actualizar disponibilidad: ${e.message()}")
            Result.Error(e, "Server error (${e.code()}). Please try again later")
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando disponibilidad: ${e.message}", e)
            Result.Error(e, "Server error. Please try again later")
        }
    }

    override suspend fun deleteAvailability(id: String): Result<Unit> {
        return try {
            Log.d(TAG, "DELETE /availability/$id")
            val response = apiService.deleteAvailability(id)
            when {
                response.isSuccessful -> {
                    Log.d(TAG, "Eliminada disponibilidad: $id (${response.code()})")
                    Result.Success(Unit)
                }
                response.code() == 404 -> Result.Error(Exception("404"), "Availability not found")
                else -> {
                    Log.e(TAG, "HTTP ${response.code()} al eliminar disponibilidad")
                    Result.Error(Exception("HTTP ${response.code()}"), "Server error. Please try again later")
                }
            }
        } catch (e: HttpException) {
            if (e.code() == 404) Result.Error(e, "Availability not found")
            else Result.Error(e, "Server error. Please try again later")
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando disponibilidad: ${e.message}", e)
            Result.Error(e, "Server error. Please try again later")
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
