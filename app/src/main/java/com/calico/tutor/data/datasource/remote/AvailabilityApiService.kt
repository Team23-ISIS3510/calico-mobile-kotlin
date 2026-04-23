package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.data.dto.response.AvailabilityResponse
import retrofit2.Response
import retrofit2.http.*

interface AvailabilityApiService {
    @GET("availability/tutor/{tutorId}")
    suspend fun getAvailabilities(@Path("tutorId") tutorId: String): List<AvailabilityResponse>

    @POST("availability/create")
    suspend fun createAvailability(@Body request: CreateAvailabilityRequest): AvailabilityResponse

    @PUT("availability/{availabilityId}")
    suspend fun updateAvailability(
        @Path("availabilityId") id: Int,
        @Body request: UpdateAvailabilityRequest
    ): AvailabilityResponse

    @DELETE("availability/{availabilityId}")
    suspend fun deleteAvailability(@Path("availabilityId") id: Int): Response<Unit>
}
