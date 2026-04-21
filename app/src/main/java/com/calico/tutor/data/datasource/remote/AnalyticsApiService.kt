package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.dto.response.SessionAlertResponse
import retrofit2.http.GET

/**
 * API service for analytics and alerting endpoints
 */
interface AnalyticsApiService {
    @GET("analytics/session-alert")
    suspend fun getSessionAlert(): SessionAlertResponse
}
