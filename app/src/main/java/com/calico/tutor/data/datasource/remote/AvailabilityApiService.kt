package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.data.dto.response.AvailabilityListResponse
import com.calico.tutor.data.dto.response.AvailabilityResponse
import retrofit2.Response
import retrofit2.http.*

interface AvailabilityApiService {
    // GET /availability?tutorId=xxx
    @GET("availability")
    suspend fun getAvailabilities(@Query("tutorId") tutorId: String): AvailabilityListResponse

    // POST /availability/create
    @POST("availability/create")
    suspend fun createAvailability(@Body request: CreateAvailabilityRequest): AvailabilityResponse

    // PUT /availability/:availabilityId
    @PUT("availability/{availabilityId}")
    suspend fun updateAvailability(
        @Path("availabilityId") id: String,
        @Body request: UpdateAvailabilityRequest
    ): AvailabilityResponse

    // DELETE /availability/delete?eventId=ID
    @DELETE("availability/delete")
    suspend fun deleteAvailability(@Query("eventId") id: String): Response<Unit>
}
