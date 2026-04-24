package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.dto.HomepageLoadRequest
import com.calico.tutor.data.dto.request.BugReportRequest
import com.calico.tutor.data.dto.request.HomepageLoadRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TelemetryApiService {
    @POST("analytics/bug")
    suspend fun reportBug(@Body request: BugReportRequest): Response<Unit>

    @POST("analytics/homepage-load")
    suspend fun reportHomepageLoad(@Body request: HomepageLoadRequest): Response<Unit>
}
