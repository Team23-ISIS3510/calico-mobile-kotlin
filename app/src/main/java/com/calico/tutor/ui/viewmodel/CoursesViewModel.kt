package com.calico.tutor.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calico.tutor.data.dto.response.AvailableCourseResponse
import com.calico.tutor.data.dto.response.CourseApplicationResponse
import com.calico.tutor.data.dto.response.TutorCourseData
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.ui.screen.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import android.util.Log

sealed class CoursesState {
    object Idle : CoursesState()
    object Loading : CoursesState()
    data class Success(
        val approvedCourses: List<TutorCourseData>,
        val availableCourses: List<AvailableCourseResponse>,
        val applications: List<CourseApplicationResponse>
    ) : CoursesState()
    data class Error(val message: String) : CoursesState()
}

class CoursesViewModel(
    private val context: Context,
    private val dbHelper: DatabaseHelper
) : ViewModel() {

    private val _coursesState = MutableStateFlow<CoursesState>(CoursesState.Idle)
    val coursesState: StateFlow<CoursesState> = _coursesState.asStateFlow()

    private val subjectsApiService by lazy { ServiceLocator.subjectsApiService(context) }

    fun loadData(tutorId: String) {
        viewModelScope.launch {
            _coursesState.value = CoursesState.Loading

            try {
                Log.d("CoursesViewModel", "Fetching from API for tutor: $tutorId")

                val approvedCourses = withContext(Dispatchers.IO) {
                    subjectsApiService.getTutorCourses(tutorId)
                }
                val applications = withContext(Dispatchers.IO) {
                    subjectsApiService.getTutorApplications(tutorId)
                }
                val allCoursesResponse = withContext(Dispatchers.IO) {
                    subjectsApiService.getAllAvailableCourses()
                }

                val allAvailableCourses = allCoursesResponse.courses

                val markedCourses = allAvailableCourses.map { course ->
                    val hasApplication = applications.any { it.courseId == course.id }
                    course.copy(hasApplied = hasApplication)
                }

                val availableCourses = markedCourses.filter { available ->
                    approvedCourses.none { approved -> approved.id == available.id }
                }

                // Only save available courses (exclude approved)
                val coursesToCache = availableCourses.map { course ->
                    DatabaseHelper.Course(
                        title = course.name,
                        description = course.code,
                        category = course.faculty
                    )
                }
                val approvedToCache = approvedCourses.map { course ->
                    DatabaseHelper.Course(
                        title = course.name,
                        description = course.code,
                        category = course.credits.toString()
                    )
                }
                val applicationsToCache = applications.map { app ->
                    DatabaseHelper.Application(
                        courseId = app.courseId,
                        courseName = app.courseName,
                        courseCode = app.courseCode,
                        status = app.status,
                        rejectionReason = app.rejectionReason
                    )
                }
                withContext(Dispatchers.IO) {
                    dbHelper.saveCourses(coursesToCache)
                    dbHelper.saveApprovedCourses(approvedToCache)
                    dbHelper.saveApplications(applicationsToCache)
                }

                _coursesState.value = CoursesState.Success(
                    approvedCourses = approvedCourses,
                    availableCourses = availableCourses,
                    applications = applications
                )

                Log.d("CoursesViewModel", "Loaded: ${approvedCourses.size} approved, ${availableCourses.size} available")

            } catch (e: ConnectException) {
                Log.w("CoursesViewModel", "Connection failed, falling back to local cache")

                val localCourses = withContext(Dispatchers.IO) {
                    dbHelper.getCourses()
                }
                val localApproved = withContext(Dispatchers.IO) {
                    dbHelper.getApprovedCourses()
                }
                val localApplications = try {
                    withContext(Dispatchers.IO) {
                        dbHelper.getApplications()
                    }
                } catch (e: Exception) {
                    Log.w("CoursesViewModel", "No applications table: ${e.message}")
                    emptyList()
                }

                Log.d("CoursesViewModel", "Cache: ${localCourses.size} available, ${localApproved.size} approved")

                // If no cached data at all, show error
                if (localCourses.isEmpty() && localApproved.isEmpty()) {
                    _coursesState.value = CoursesState.Error("No cached data. Connect to internet.")
                    return@launch
                }

                val localApprovedCourses = localApproved.map { course ->
                    TutorCourseData(
                        id = course.id.toString(),
                        name = course.title,
                        code = course.description ?: "",
                        credits = course.category?.toIntOrNull() ?: 0
                    )
                }

                val approvedIds = localApproved.map { it.title }.toSet()
                val filteredAvailable = localCourses.filter { course -> course.title !in approvedIds }

                val localAvailableCourses = filteredAvailable.map { course ->
                    val hasApp = localApplications.any { it.courseName == course.title && it.status == "pending" }
                    AvailableCourseResponse(
                        id = course.id.toString(),
                        name = course.title,
                        code = course.description ?: "",
                        credits = 0,
                        faculty = course.category,
                        hasApplied = hasApp
                    )
                }

                val localApplicationsList = localApplications.map { app ->
                    CourseApplicationResponse(
                        id = app.id.toString(),
                        courseId = app.courseId,
                        courseName = app.courseName,
                        courseCode = app.courseCode,
                        status = app.status,
                        rejectionReason = app.rejectionReason
                    )
                }

                _coursesState.value = CoursesState.Success(
                    approvedCourses = localApprovedCourses,
                    availableCourses = localAvailableCourses,
                    applications = localApplicationsList
                )

                Log.d("CoursesViewModel", "Loaded from cache: ${filteredAvailable.size} available, ${localApproved.size} approved, ${localApplications.size} apps")

            } catch (e: Exception) {
                Log.e("CoursesViewModel", "Error loading data: ${e.message}", e)

                val localCourses = withContext(Dispatchers.IO) {
                    dbHelper.getCourses()
                }
                val localApproved = withContext(Dispatchers.IO) {
                    dbHelper.getApprovedCourses()
                }

                Log.d("CoursesViewModel", "Cache fallback: ${localCourses.size} available, ${localApproved.size} approved")

                if (localCourses.isEmpty() && localApproved.isEmpty()) {
                    _coursesState.value = CoursesState.Error("Error: ${e.message}")
                    return@launch
                }

                val localApprovedCourses = localApproved.map { course ->
                    TutorCourseData(
                        id = course.id.toString(),
                        name = course.title,
                        code = course.description ?: "",
                        credits = course.category?.toIntOrNull() ?: 0
                    )
                }

                val approvedIds = localApproved.map { it.title }.toSet()
                val filteredAvailable = localCourses.filter { course -> course.title !in approvedIds }

                val localAvailableCourses = filteredAvailable.map { course ->
                    AvailableCourseResponse(
                        id = course.id.toString(),
                        name = course.title,
                        code = course.description ?: "",
                        credits = 0,
                        faculty = course.category,
                        hasApplied = false
                    )
                }

                _coursesState.value = CoursesState.Success(
                    approvedCourses = localApprovedCourses,
                    availableCourses = localAvailableCourses,
                    applications = emptyList()
                )
            }
        }
    }
}