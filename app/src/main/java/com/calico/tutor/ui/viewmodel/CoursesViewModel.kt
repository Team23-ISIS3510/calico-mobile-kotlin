package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calico.tutor.data.dto.response.AllCoursesResponse
import com.calico.tutor.data.dto.response.AvailableCourseResponse
import com.calico.tutor.data.dto.response.CourseApplicationResponse
import com.calico.tutor.data.dto.response.TutorCourseData
import com.calico.tutor.data.datasource.remote.ApplyCourseRequest
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.ui.screen.DatabaseHelper
import com.calico.tutor.util.ApiResponseCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

sealed class CoursesState {
    object Idle : CoursesState()
    object Loading : CoursesState()
    data class Success(
        val approvedCourses: List<TutorCourseData>,
        val availableCourses: List<AvailableCourseResponse>,
        val applications: List<CourseApplicationResponse>,
        val pendingApplications: List<DatabaseHelper.PendingApplication> = emptyList(),
        val isOffline: Boolean = false,
        val applicationQueued: Boolean = false
    ) : CoursesState()
    data class Error(val message: String) : CoursesState()
}

class CoursesViewModel(
    private val context: Context,
    private val dbHelper: DatabaseHelper
) : ViewModel() {

    private val _coursesState = MutableStateFlow<CoursesState>(CoursesState.Idle)
    val coursesState: StateFlow<CoursesState> = _coursesState.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    private val _applicationQueued = MutableStateFlow(false)
    val applicationQueued: StateFlow<Boolean> = _applicationQueued.asStateFlow()
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    private val subjectsApiService by lazy { ServiceLocator.subjectsApiService(context) }

    private val approvedCoursesCache = ApiResponseCache.approvedCourses()
    private val applicationsCache = ApiResponseCache.applications()
    private val allCoursesCache = ApiResponseCache.allCourses()

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun loadData(tutorId: String, preservePending: Boolean = false) {
        viewModelScope.launch {
            // If not preserving state, show loading
            if (!preservePending) {
                _coursesState.value = CoursesState.Loading
            }

            val hasNetwork = isNetworkAvailable()
            Log.d("CoursesViewModel", "Network available: $hasNetwork, preservePending: $preservePending")

            // If no network, go directly to offline mode
            if (!hasNetwork) {
                loadFromCache(tutorId, isOffline = true, preservePending = preservePending)
                return@launch
            }

            // Always try to sync pending applications when coming online
            val pendingAppsFromDb = try {
                withContext(Dispatchers.Main) {
                    withContext(Dispatchers.IO) {
                        dbHelper.getPendingApplications()
                    }
                }
            } catch (e: Exception) {
                Log.w("CoursesViewModel", "Pending apps table error: ${e.message}")
                emptyList()
            }
            
            if (pendingAppsFromDb.isNotEmpty()) {
                Log.d("CoursesViewModel", "Found ${pendingAppsFromDb.size} pending apps to sync")
                syncPendingApplications(tutorId, pendingAppsFromDb)
                // After sync, loadData will be called again from syncPendingApplications
                return@launch
            }

            try {
                Log.d("CoursesViewModel", "Fetching from API for tutor: $tutorId")

                // ALWAYS try API first - no cache fallback for main data
                var approvedCourses: List<TutorCourseData> = emptyList()
                var applications: List<CourseApplicationResponse> = emptyList()
                var allCoursesResponse: AllCoursesResponse = AllCoursesResponse()

                try {
                    approvedCourses = withContext(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            subjectsApiService.getTutorCourses(tutorId)
                        }
                    }
                    // Only cache after successful API call
                    approvedCoursesCache.put("approved_$tutorId", approvedCourses)
                    Log.d("CoursesViewModel", "✅ API: approved courses loaded (${approvedCourses.size})")
                } catch (e: Exception) {
                    Log.e("CoursesViewModel", "❌ API failed for approved: ${e.message}", e)
                    // Try LRU cache
                    val cached = approvedCoursesCache.get("approved_$tutorId")
                    approvedCourses = (cached as? List<TutorCourseData>) ?: emptyList()
                    // If still empty, try DB
                    if (approvedCourses.isEmpty()) {
                        approvedCourses = withContext(Dispatchers.Main) {
                            withContext(Dispatchers.IO) {
                                dbHelper.getApprovedCourses().map { course ->
                                    TutorCourseData(
                                        id = course.id.toString(),
                                        name = course.title,
                                        code = course.description ?: "",
                                        credits = course.category?.toIntOrNull() ?: 0
                                    )
                                }
                            }
                        }
                    }
                }

                try {
                    applications = withContext(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            subjectsApiService.getTutorApplications(tutorId)
                        }
                    }
                    applicationsCache.put("apps_$tutorId", applications)
                    Log.d("CoursesViewModel", "✅ API: applications loaded (${applications.size})")
                } catch (e: Exception) {
                    Log.w("CoursesViewModel", "❌ API failed for applications: ${e.message}")
                    val cached = applicationsCache.get("apps_$tutorId")
                    applications = (cached as? List<CourseApplicationResponse>) ?: emptyList()
                    if (applications.isEmpty()) {
                        applications = withContext(Dispatchers.Main) {
                            withContext(Dispatchers.IO) {
                                try {
                                    dbHelper.getApplications().map { app ->
                                        CourseApplicationResponse(
                                            id = app.id.toString(),
                                            courseId = app.courseId,
                                            courseName = app.courseName,
                                            courseCode = app.courseCode,
                                            status = app.status,
                                            rejectionReason = app.rejectionReason
                                        )
                                    }
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                        }
                    }
                }

                try {
                    allCoursesResponse = withContext(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            subjectsApiService.getAllAvailableCourses()
                        }
                    }
                    allCoursesCache.put("all", allCoursesResponse)
                    Log.d("CoursesViewModel", "✅ API: all courses loaded (${allCoursesResponse.courses.size})")
                } catch (e: Exception) {
                    Log.w("CoursesViewModel", "❌ API failed for all courses: ${e.message}")
                    val cached = allCoursesCache.get("all")
                    allCoursesResponse = cached as? AllCoursesResponse ?: AllCoursesResponse()
                    if (allCoursesResponse.courses.isEmpty()) {
                        allCoursesResponse = withContext(Dispatchers.Main) {
                            withContext(Dispatchers.IO) {
                                val localCourses = dbHelper.getCourses()
                                val courseList = localCourses.map { course ->
                                    AvailableCourseResponse(
                                        id = course.id.toString(),
                                        name = course.title,
                                        code = course.description ?: "",
                                        credits = 0,
                                        faculty = course.category,
                                        hasApplied = false
                                    )
                                }
                                AllCoursesResponse(courses = courseList)
                            }
                        }
                    }
                }

                val allAvailableCourses = allCoursesResponse.courses

                val markedCourses = allAvailableCourses.map { course ->
                    val hasApplication = applications.any { it.courseId == course.id }
                    course.copy(hasApplied = hasApplication)
                }

                val availableCourses = markedCourses.filter { available ->
                    approvedCourses.none { approved -> approved.id == available.id }
                }

                // Cache to SQLite for offline
                val coursesToCache = availableCourses.map { course ->
                    DatabaseHelper.Course(
                        apiId = course.id,
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

                // Load pending applications (wrapped in try-catch for backwards compatibility)
                val pendingApps = try {
                    withContext(Dispatchers.IO) {
                        dbHelper.getPendingApplications()
                    }
                } catch (e: Exception) {
                    Log.w("CoursesViewModel", "Pending applications table not available: ${e.message}")
                    emptyList()
                }

                // Try to sync pending applications if online
                if (hasNetwork && pendingApps.isNotEmpty()) {
                    syncPendingApplications(tutorId, pendingApps)
                }

                // Preserve existing pending apps if doing background refresh
                val finalPendingApps = if (preservePending && _coursesState.value is CoursesState.Success) {
                    val existingPending = (_coursesState.value as CoursesState.Success).pendingApplications
                    val newPendingIds = pendingApps.map { it.courseId }.toSet()
                    existingPending + pendingApps.filter { it.courseId !in newPendingIds }
                } else {
                    pendingApps
                }

                _coursesState.value = CoursesState.Success(
                    approvedCourses = approvedCourses,
                    availableCourses = availableCourses,
                    applications = applications,
                    pendingApplications = finalPendingApps,
                    isOffline = false
                )

                Log.d("CoursesViewModel", "Loaded: ${approvedCourses.size} approved, ${availableCourses.size} available")

            } catch (e: Exception) {
                Log.e("CoursesViewModel", "Error loading data: ${e.message}", e)

                // Check if we have network - if not, try offline
                if (!isNetworkAvailable()) {
                    loadFromCache(tutorId, isOffline = true)
                    return@launch
                }

                loadFromCache(tutorId, isOffline = true)
            }
        }
    }

    private suspend fun loadFromCache(tutorId: String, isOffline: Boolean, preservePending: Boolean = false) {
        val localCourses = withContext(Dispatchers.IO) { dbHelper.getCourses() }
        val localApproved = withContext(Dispatchers.IO) { dbHelper.getApprovedCourses() }
        val localApplications = try {
            withContext(Dispatchers.IO) { dbHelper.getApplications() }
        } catch (e: Exception) {
            Log.w("CoursesViewModel", "No applications table: ${e.message}")
            emptyList()
        }

        Log.d("CoursesViewModel", "Cache: ${localCourses.size} available, ${localApproved.size} approved")

        if (localCourses.isEmpty() && localApproved.isEmpty()) {
            _coursesState.value = CoursesState.Error("No data available. Connect to internet.")
            return
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
                id = course.apiId ?: course.id.toString(),
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

        // Load pending applications (wrapped in try-catch for backwards compatibility)
        val pendingApps = try {
            withContext(Dispatchers.IO) {
                dbHelper.getPendingApplications()
            }
        } catch (e: Exception) {
            Log.w("CoursesViewModel", "Pending applications table not available: ${e.message}")
            emptyList()
        }

        // Preserve existing pending apps if doing background refresh
        val finalPendingApps = if (preservePending) {
            val existingPending = (_coursesState.value as? CoursesState.Success)?.pendingApplications ?: emptyList()
            val newPendingIds = pendingApps.map { it.courseId }.toSet()
            existingPending + pendingApps.filter { it.courseId !in newPendingIds }
        } else {
            pendingApps
        }

        _coursesState.value = CoursesState.Success(
            approvedCourses = localApprovedCourses,
            availableCourses = localAvailableCourses,
            applications = localApplicationsList,
            pendingApplications = finalPendingApps,
            isOffline = isOffline
        )

        Log.d("CoursesViewModel", "Loaded from cache: ${filteredAvailable.size} available, ${localApproved.size} approved, ${finalPendingApps.size} pending")
    }

    private suspend fun syncPendingApplications(tutorId: String, pendingApps: List<DatabaseHelper.PendingApplication>) {
        val successfulIds = mutableListOf<Long>()
        
        for (app in pendingApps) {
            try {
                val request = ApplyCourseRequest(
                    tutorId = tutorId,
                    courseId = app.courseId,
                    notes = app.notes
                )
                withContext(Dispatchers.IO) {
                    subjectsApiService.applyForCourse(request)
                }
                // Remove from DB on success
                withContext(Dispatchers.IO) {
                    dbHelper.deletePendingApplication(app.id)
                }
                successfulIds.add(app.id)
                Log.d("CoursesViewModel", "✅ Synced pending application: ${app.courseName}")
            } catch (e: Exception) {
                Log.w("CoursesViewModel", "❌ Failed to sync application: ${app.courseName} - ${e.message}")
            }
        }
        
        if (successfulIds.isNotEmpty()) {
            Log.d("CoursesViewModel", "Synced ${successfulIds.size} pending applications, re-fetching API data")
            // Clear caches and fetch fresh data from API (don't preserve pending)
            applicationsCache.remove("apps_$tutorId")
            approvedCoursesCache.remove("approved_$tutorId")
            allCoursesCache.remove("all")
            
            // Fetch fresh data - this will NOT trigger sync again since DB is now empty
            fetchAndUpdateData(tutorId)
        }
    }
    
    private suspend fun fetchAndUpdateData(tutorId: String) {
        try {
            var approvedCourses: List<TutorCourseData> = emptyList()
            var applications: List<CourseApplicationResponse> = emptyList()
            var allCoursesResponse: AllCoursesResponse = AllCoursesResponse()

            try {
                approvedCourses = withContext(Dispatchers.IO) {
                    subjectsApiService.getTutorCourses(tutorId)
                }
                approvedCoursesCache.put("approved_$tutorId", approvedCourses)
            } catch (e: Exception) {
                Log.e("CoursesViewModel", "Error fetching approved courses: ${e.message}")
            }

            try {
                applications = withContext(Dispatchers.IO) {
                    subjectsApiService.getTutorApplications(tutorId)
                }
                applicationsCache.put("apps_$tutorId", applications)
            } catch (e: Exception) {
                Log.e("CoursesViewModel", "Error fetching applications: ${e.message}")
            }

            try {
                allCoursesResponse = withContext(Dispatchers.IO) {
                    subjectsApiService.getAllAvailableCourses()
                }
                allCoursesCache.put("all", allCoursesResponse)
            } catch (e: Exception) {
                Log.e("CoursesViewModel", "Error fetching all courses: ${e.message}")
            }

            // Filter out courses that already have pending or approved applications
            val appliedCourseIds = applications.map { it.courseId }.toSet()
            val availableCourses = allCoursesResponse.courses.filter { course ->
                val isApproved = approvedCourses.any { it.id == course.id }
                val hasApplication = appliedCourseIds.contains(course.id)
                !isApproved && !hasApplication
            }.map { course ->
                course.copy(hasApplied = false)
            }

            _coursesState.value = CoursesState.Success(
                approvedCourses = approvedCourses,
                availableCourses = availableCourses,
                applications = applications,
                pendingApplications = emptyList(), // DB is now empty after sync
                isOffline = false
            )
            Log.d("CoursesViewModel", "✅ Fresh data loaded after sync: ${approvedCourses.size} approved, ${availableCourses.size} available")
        } catch (e: Exception) {
            Log.e("CoursesViewModel", "Error fetching fresh data: ${e.message}")
        }
    }

    fun applyForCourse(tutorId: String, courseId: String, courseName: String, courseCode: String, notes: String?) {
        viewModelScope.launch {
            _isApplying.value = true
            
            val hasNetwork = isNetworkAvailable()
            Log.d("CoursesViewModel", "Network available for application: $hasNetwork")
            
            if (!hasNetwork) {
                // Queue the application for later
                try {
                    val pendingApp = DatabaseHelper.PendingApplication(
                        courseId = courseId,
                        courseName = courseName,
                        courseCode = courseCode,
                        notes = notes,
                        createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
                    )
                    withContext(Dispatchers.IO) {
                        dbHelper.savePendingApplication(pendingApp)
                    }
                    Log.d("CoursesViewModel", "Application queued for later: $courseName")
                    
                    // Update UI immediately without waiting for loadData
                    val currentState = _coursesState.value
                    if (currentState is CoursesState.Success) {
                        val updatedPendingApps = currentState.pendingApplications + pendingApp.copy(id = System.currentTimeMillis())
                        _coursesState.value = currentState.copy(pendingApplications = updatedPendingApps)
                    }
                    
                    // Reload in background to refresh from DB
                    loadData(tutorId, preservePending = true)
                    
                    // Set flag to close dialog
                    _applicationQueued.value = true
                } catch (e: Exception) {
                    Log.e("CoursesViewModel", "Failed to queue application: ${e.message}")
                } finally {
                    _isApplying.value = false
                }
                return@launch
            }

            // Online - submit directly
            try {
                val request = ApplyCourseRequest(
                    tutorId = tutorId,
                    courseId = courseId,
                    notes = notes
                )
                val result = withContext(Dispatchers.IO) {
                    subjectsApiService.applyForCourse(request)
                }
                Log.d("CoursesViewModel", "Application submitted: $result")

                // Clear caches to force fresh data from API
                applicationsCache.remove("apps_$tutorId")
                approvedCoursesCache.remove("approved_$tutorId")
                allCoursesCache.remove("all")

                // Reload data to reflect the new application
                loadData(tutorId)
                
                // Set flag to close dialog
                _applicationQueued.value = true

            } catch (e: Exception) {
                Log.e("CoursesViewModel", "Error applying for course: ${e.message}", e)
            } finally {
                _isApplying.value = false
            }
        }
    }
    
    fun resetApplicationQueued() {
        _applicationQueued.value = false
    }
}