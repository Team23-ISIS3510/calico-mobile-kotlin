package com.calico.tutor.domain.repository

import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.domain.utils.Result

interface AvailabilityRepository {
    suspend fun getAvailabilities(tutorId: String): Result<List<AvailabilityItem>>
    suspend fun createAvailability(request: CreateAvailabilityRequest): Result<AvailabilityItem>
    suspend fun updateAvailability(id: Int, request: UpdateAvailabilityRequest): Result<AvailabilityItem>
    suspend fun deleteAvailability(id: Int): Result<Unit>
}
