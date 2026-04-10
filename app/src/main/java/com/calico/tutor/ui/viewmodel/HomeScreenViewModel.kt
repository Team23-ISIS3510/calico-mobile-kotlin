package com.calico.tutor.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.Session
import com.calico.tutor.data.dto.response.TutorOccupancyData
import com.calico.tutor.domain.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
 * ViewModel para HomeScreen
 * Maneja la lógica de carga de sesiones previas, próximas y datos de ocupancy
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
