package com.calico.tutor.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.Session
import com.calico.tutor.data.dto.response.TutorOccupancyData
import com.calico.tutor.data.dto.response.SessionAlertResponse
import com.calico.tutor.domain.utils.Result
import com.calico.tutor.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log

/**
 * Estados UI para sesiones
 */
sealed class SessionsState {
    object Idle : SessionsState()
    object Loading : SessionsState()
    data class Success(
        val previousSessions: List<Session>,
        val upcomingSessions: List<Session>
    ) : SessionsState()
    data class Error(val message: String) : SessionsState()
}

/**
 * Estados UI para ocupancy
 */
sealed class OccupancyState {
    object Idle : OccupancyState()
    object Loading : OccupancyState()
    data class Success(val data: List<TutorOccupancyData>) : OccupancyState()
    data class Error(val message: String) : OccupancyState()
}

/**
 * Estados UI para session alert
 */
sealed class SessionAlertState {
    object Idle : SessionAlertState()
    data class Alert(val response: SessionAlertResponse) : SessionAlertState()
    data class Error(val message: String) : SessionAlertState()
}

/**
 * ViewModel para HomeScreen
 * Maneja la lógica de carga de sesiones previas, próximas, datos de ocupancy y polling de alertas
 */
class HomeScreenViewModel(
    private val context: Context
) : ViewModel() {

    // Estado de sesiones
    private val _sessionsState = MutableStateFlow<SessionsState>(SessionsState.Idle)
    val sessionsState: StateFlow<SessionsState> = _sessionsState.asStateFlow()

    // Estado de ocupancy
    private val _occupancyState = MutableStateFlow<OccupancyState>(OccupancyState.Idle)
    val occupancyState: StateFlow<OccupancyState> = _occupancyState.asStateFlow()

    // Nombre del tutor
    private val _tutorName = MutableStateFlow<String>("")
    val tutorName: StateFlow<String> = _tutorName.asStateFlow()

    // Estado de session alert
    private val _sessionAlertState = MutableStateFlow<SessionAlertState>(SessionAlertState.Idle)
    val sessionAlertState: StateFlow<SessionAlertState> = _sessionAlertState.asStateFlow()

    // Track shown notifications to prevent duplicates
    private val shownNotifications = mutableSetOf<String>()
    
    // Track if connection warning has been shown recently (to avoid spamming)
    private var lastConnectionWarningTime: Long = 0
    private val CONNECTION_WARNING_COOLDOWN_MS = 300000L // 5 minutes

    /**
     * Carga datos de sesiones previas y próximas
     */
    fun loadSessions(tutorId: String) {
        viewModelScope.launch {
            _sessionsState.value = SessionsState.Loading

            try {
                val subjectsApiService = ServiceLocator.subjectsApiService(context)

                // Cargar nombre del tutor
                try {
                    val tutorResponse = subjectsApiService.getTutorProfile(tutorId)
                    _tutorName.value = tutorResponse.name
                } catch (e: Exception) {
                    Log.e("HomeScreenViewModel", "Error loading tutor profile: ${e.message}")
                }

                // Cargar todas las sesiones y filtrar por tiempo
                val allSessionsResponse = subjectsApiService.getTutoringSessionsForTutor(tutorId)
                val now = System.currentTimeMillis()
                val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                val dateFmtShort = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())

                val allMapped = allSessionsResponse.sessions.map { sessionData ->
                    val startMs = try { dateFmt.parse(sessionData.scheduledStart)?.time ?: 0L }
                        catch (e: Exception) { try { dateFmtShort.parse(sessionData.scheduledStart)?.time ?: 0L } catch (e2: Exception) { 0L } }
                    Session(
                        id = sessionData.id,
                        scheduledStart = sessionData.scheduledStart,
                        scheduledEnd = sessionData.scheduledEnd,
                        status = sessionData.status,
                        course = sessionData.course,
                        courseId = sessionData.courseId,
                        date = "",
                        time = "",
                        tutorName = "",
                        subjectName = "",
                        subjectCode = ""
                    ) to startMs
                }

                val previousSessions = allMapped.filter { it.second < now }.sortedByDescending { it.second }.map { it.first }
                val upcomingSessions = allMapped.filter { it.second > now }.sortedBy { it.second }.map { it.first }

                Log.d("HomeScreenViewModel", "Previous sessions loaded: ${previousSessions.size}")
                Log.d("HomeScreenViewModel", "Upcoming sessions loaded: ${upcomingSessions.size}")

                _sessionsState.value = SessionsState.Success(previousSessions, upcomingSessions)
            } catch (e: Exception) {
                Log.e("HomeScreenViewModel", "Error loading sessions: ${e.message}")
                _sessionsState.value = SessionsState.Error("Error loading sessions: ${e.message}")
            }
        }
    }

    /**
     * Carga datos de ocupancy del tutor
     */
    fun loadOccupancy(tutorId: String) {
        viewModelScope.launch {
            _occupancyState.value = OccupancyState.Loading

            try {
                val subjectsApiService = ServiceLocator.subjectsApiService(context)
                val occupancyResponse = subjectsApiService.getTutorOccupancy(tutorId)
                
                Log.d("HomeScreenViewModel", "Occupancy loaded: ${occupancyResponse.data.size} subjects")
                _occupancyState.value = OccupancyState.Success(occupancyResponse.data)
            } catch (e: Exception) {
                Log.e("HomeScreenViewModel", "Error loading occupancy: ${e.message}")
                _occupancyState.value = OccupancyState.Error("Error loading occupancy: ${e.message}")
            }
        }
    }

    /**
     * Starts polling for session alerts every 60 seconds
     * Automatically shows notifications when alerts are available
     */
    fun startSessionAlertPolling() {
        viewModelScope.launch {
            Log.d("SessionAlertPolling", "Starting session alert polling (every 60 seconds)")
            NotificationHelper.createNotificationChannel(context)
            
            while (true) {
                try {
                    Log.d("SessionAlertPolling", "Fetching session alert from /analytics/session-alert...")
                    val analyticsApiService = ServiceLocator.analyticsApiService(context)
                    val alertResponse = analyticsApiService.getSessionAlert()
                    
                    Log.d("SessionAlertPolling", "Alert response received: hasAlert=${alertResponse.hasAlert}, studentName=${alertResponse.studentName}, minutesToStart=${alertResponse.minutesToStart}, sessionId=${alertResponse.sessionId}")
                    
                    _sessionAlertState.value = SessionAlertState.Alert(alertResponse)
                    
                    // Show notification if alert is true and hasn't been shown yet
                    if (alertResponse.hasAlert && 
                        alertResponse.studentName != null && 
                        alertResponse.minutesToStart != null) {
                        
                        // Use sessionId from response, or generate one from student name if missing
                        val sessionId = alertResponse.sessionId ?: alertResponse.studentName
                        val notificationKey = sessionId
                        
                        if (!shownNotifications.contains(notificationKey)) {
                            Log.d("SessionAlertPolling", "Showing notification for ${alertResponse.studentName}")
                            NotificationHelper.showSessionAlertNotification(
                                context = context,
                                studentName = alertResponse.studentName,
                                minutesToStart = alertResponse.minutesToStart,
                                sessionId = sessionId
                            )
                            shownNotifications.add(notificationKey)
                            Log.d("SessionAlertPolling", "Session alert notification shown for ${alertResponse.studentName}")
                            
                            // If session is within 15 minutes, check network latency
                            if (alertResponse.minutesToStart <= 15) {
                                Log.d("SessionAlertPolling", "Session within 15 minutes, measuring network latency...")
                                delay(500) // Small delay before latency check
                                
                                val latency = measureNetworkLatency()
                                
                                // Show connection warning if latency > 500ms and cooldown has passed
                                if (latency > 500) {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastConnectionWarningTime > CONNECTION_WARNING_COOLDOWN_MS) {
                                        Log.d("SessionAlertPolling", "High latency detected (${latency}ms > 500ms), showing connection warning")
                                        NotificationHelper.showConnectionWarningNotification(context, latency)
                                        lastConnectionWarningTime = currentTime
                                    } else {
                                        Log.d("SessionAlertPolling", "High latency detected (${latency}ms) but connection warning cooldown active")
                                    }
                                } else {
                                    Log.d("SessionAlertPolling", "Network latency is good (${latency}ms <= 500ms)")
                                }
                            }
                        } else {
                            Log.d("SessionAlertPolling", "Notification already shown for session $sessionId, skipping duplicate")
                        }
                    } else {
                        Log.d("SessionAlertPolling", "No alert to show (hasAlert=false or missing fields)")
                    }
                    
                    // Poll every 60 seconds
                    Log.d("SessionAlertPolling", "Waiting 60 seconds before next poll...")
                    delay(60000)
                } catch (e: Exception) {
                    Log.e("SessionAlertPolling", "Error polling session alerts: ${e.message}", e)
                    _sessionAlertState.value = SessionAlertState.Error("Error checking for alerts: ${e.message}")
                    // Retry after delay even on error
                    delay(60000)
                }
            }
        }
    }

    /**
     * Recarga todos los datos
     */
    fun refreshData(tutorId: String) {
        loadSessions(tutorId)
        loadOccupancy(tutorId)
    }

    /**
     * Measures network latency by pinging the backend health endpoint
     * Returns the average latency in milliseconds
     */
    private suspend fun measureNetworkLatency(): Long {
        return try {
            val latencies = mutableListOf<Long>()
            
            // Take 3 latency measurements
            repeat(3) { attempt ->
                try {
                    val startTime = System.currentTimeMillis()
                    val subjectsApiService = ServiceLocator.subjectsApiService(context)
                    // Simple health check by calling getTutorProfile with a dummy ID
                    // This measures actual backend connectivity
                    subjectsApiService.getTutorProfile("health-check")
                    val latency = System.currentTimeMillis() - startTime
                    latencies.add(latency)
                    Log.d("LatencyMeasurement", "Ping attempt ${attempt + 1}: ${latency}ms")
                } catch (e: Exception) {
                    // On error, record as higher latency penalty
                    latencies.add(2000L)
                    Log.w("LatencyMeasurement", "Ping attempt ${attempt + 1} failed: ${e.message}")
                }
                delay(100) // Small delay between attempts
            }
            
            val averageLatency = latencies.average().toLong()
            Log.d("LatencyMeasurement", "Average latency: ${averageLatency}ms (samples: $latencies)")
            averageLatency
        } catch (e: Exception) {
            Log.e("LatencyMeasurement", "Error measuring latency: ${e.message}")
            2000 // Assume worst case on measurement error
        }
    }
}

/**
 * Factory para crear instancias de HomeScreenViewModel
 */
class HomeScreenViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeScreenViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
