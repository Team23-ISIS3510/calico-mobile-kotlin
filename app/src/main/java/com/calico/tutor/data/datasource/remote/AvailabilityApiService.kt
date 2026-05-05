package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.data.dto.response.AvailabilityListResponse
import com.calico.tutor.data.dto.response.AvailabilityMutationResponse
import com.calico.tutor.data.dto.response.AvailabilityResponse
import com.calico.tutor.data.dto.response.HotSlotsAnalysisResponseDto
import retrofit2.Response
import retrofit2.http.*

interface AvailabilityApiService {
    // GET /availability?tutorId=xxx&course=...&startDate=...&endDate=...&limit=...
    @GET("availability")
    suspend fun getAvailabilities(
        @Query("tutorId") tutorId: String,
        @Query("course") course: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("limit") limit: Int? = null
    ): AvailabilityListResponse

    // POST /availability/create
    @POST("availability/create")
    suspend fun createAvailability(@Body request: CreateAvailabilityRequest): Response<AvailabilityMutationResponse>

    // PUT /availability/:availabilityId
    @PUT("availability/{availabilityId}")
    suspend fun updateAvailability(
        @Path("availabilityId") id: String,
        @Body request: UpdateAvailabilityRequest
    ): Response<AvailabilityMutationResponse>

    // DELETE /availability/:availabilityId
    @DELETE("availability/{availabilityId}")
    suspend fun deleteAvailability(@Path("availabilityId") availabilityId: String): Response<Unit>

    // GET /tutors/:tutorId/hot-slots
    @GET("tutors/{tutorId}/hot-slots")
    suspend fun getHotSlotsAnalysis(@Path("tutorId") tutorId: String): HotSlotsAnalysisResponseDto
}
