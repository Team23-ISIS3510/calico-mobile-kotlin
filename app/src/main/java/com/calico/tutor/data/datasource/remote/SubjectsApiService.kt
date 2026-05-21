package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.dto.response.SubjectsHistoryResponse
import com.calico.tutor.data.dto.response.TutorResponse
import com.calico.tutor.data.dto.response.TutoringSessionsResponse
import com.calico.tutor.data.dto.response.TutorOccupancyResponse
import com.calico.tutor.data.dto.response.CourseResponse
import com.calico.tutor.data.dto.response.TutorCoursesResponse
import com.calico.tutor.data.dto.response.TutorCourseData
import com.calico.tutor.data.dto.response.CourseApplicationResponse
import com.calico.tutor.data.dto.response.AvailableCourseResponse
import com.calico.tutor.data.dto.response.AllCoursesResponse
import com.calico.tutor.data.dto.response.UserProfileResponse
import com.calico.tutor.domain.model.SessionHistory
import com.calico.tutor.data.dto.AvailabilityResponseDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

interface SubjectsApiService {
    @GET("subjects/history")
    
    suspend fun getSubjectsHistory(): SubjectsHistoryResponse

    @GET("tutoring-sessions/tutor/{tutorId}/previous")
    suspend fun getPreviousTutoringSessionsForTutor(
        @Path("tutorId") tutorId: String
    ): TutoringSessionsResponse

    @GET("tutoring-sessions/student/{studentId}/history")
    suspend fun getStudentTutoringSessionsHistory(
        @Path("studentId") studentId: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("course") course: String? = null,
        @Query("limit") limit: Int? = null
    ): TutoringSessionsResponse

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): UserProfileResponse

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

    @GET("analytics/tutor-occupancy/{tutorId}")
    suspend fun getTutorOccupancy(@Path("tutorId") tutorId: String): TutorOccupancyResponse

    @GET("courses/{courseId}")
    suspend fun getCourseById(@Path("courseId") courseId: String): CourseResponse

    @GET("tutors/{tutorId}/courses")
    suspend fun getTutorCourses(@Path("tutorId") tutorId: String): List<TutorCourseData>

    @GET("tutors/{tutorId}/applications")
    suspend fun getTutorApplications(@Path("tutorId") tutorId: String): List<CourseApplicationResponse>

    @GET("courses")
    suspend fun getAllAvailableCourses(): AllCoursesResponse

    @POST("tutors/apply")
    suspend fun applyForCourse(@Body request: ApplyCourseRequest): Map<String, Any>
}

data class ApplyCourseRequest(
    val tutorId: String,
    val courseId: String,
    val notes: String? = null
)
