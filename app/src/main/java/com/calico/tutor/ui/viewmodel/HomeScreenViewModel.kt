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

                // Cargar sesiones previas
                val previousResponse = subjectsApiService.getPreviousSessions(tutorId)
                val previousSessions = previousResponse.sessions.map { sessionData ->
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
                    )
                }

                Log.d("HomeScreenViewModel", "Previous sessions loaded: ${previousSessions.size}")

                // Cargar sesiones próximas
                val upcomingResponse = subjectsApiService.getUpcomingSessions(tutorId)
                val upcomingSessions = upcomingResponse.sessions.map { sessionData ->
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
                    )
                }

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
                    val analyticsRepository = ServiceLocator.analyticsRepository(context)
                    val alertResponse = analyticsRepository.getSessionAlert()
                    
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
