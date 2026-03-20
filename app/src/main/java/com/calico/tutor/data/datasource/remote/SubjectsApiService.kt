package com.calico.tutor.data.datasource.remote

import com.calico.tutor.domain.model.SubjectsHistory
import com.calico.tutor.domain.model.SessionHistory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SubjectsApiService {
    @GET("subjects/history")
    suspend fun getSubjectsHistory(): SubjectsHistory

    @GET("subjects/history/tutor/{tutorId}")
    suspend fun getTutorSessionHistory(@Path("tutorId") tutorId: String): SessionHistory

    @GET("subjects/history/range")
    suspend fun getSessionsByDateRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): SessionHistory
}
