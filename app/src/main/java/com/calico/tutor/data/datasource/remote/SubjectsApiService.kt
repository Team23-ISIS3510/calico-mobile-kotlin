package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.dto.response.SubjectsHistoryResponse
import com.calico.tutor.data.dto.response.TutorResponse
import com.calico.tutor.data.dto.response.TutoringSessionsResponse
import com.calico.tutor.data.dto.response.TutorOccupancyResponse
import com.calico.tutor.data.dto.response.CourseResponse
import com.calico.tutor.domain.model.SessionHistory
import com.calico.tutor.data.dto.AvailabilityResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SubjectsApiService {
    @GET("subjects/history")
    suspend fun getSubjectsHistory(): SubjectsHistoryResponse

    @GET("subjects/history/tutor/{tutorId}")
    suspend fun getTutorSessionHistory(@Path("tutorId") tutorId: String): SessionHistory

    @GET("subjects/history/range")
    suspend fun getSessionsByDateRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): SessionHistory

    @GET("tutors/{tutorId}")
    suspend fun getTutorProfile(@Path("tutorId") tutorId: String): TutorResponse

    @GET("tutoring-sessions/tutor/{tutorId}")
    suspend fun getTutoringSessionsForTutor(@Path("tutorId") tutorId: String): TutoringSessionsResponse

    @GET("tutoring-sessions/tutor/{tutorId}/previous")
    suspend fun getPreviousSessions(@Path("tutorId") tutorId: String): TutoringSessionsResponse

    @GET("tutoring-sessions/tutor/{tutorId}/upcoming")
    suspend fun getUpcomingSessions(@Path("tutorId") tutorId: String): TutoringSessionsResponse

    @GET("analytics/tutor-occupancy/{tutorId}")
    suspend fun getTutorOccupancy(@Path("tutorId") tutorId: String): TutorOccupancyResponse

    @GET("courses/{courseId}")
    suspend fun getCourseById(@Path("courseId") courseId: String): CourseResponse
}
