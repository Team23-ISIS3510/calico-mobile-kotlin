package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.data.cache.InMemoryCache
import com.calico.tutor.data.datasource.remote.SubjectsApiService
import com.calico.tutor.data.dto.response.TutorCourseData
import com.calico.tutor.data.dto.response.TutoringSessionData
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.UserPreferencesDataStore
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.CourseDetail
import com.calico.tutor.domain.model.Session
import com.calico.tutor.domain.repository.CourseDetailRepository
import com.calico.tutor.domain.utils.Result
import com.calico.tutor.ui.screen.DatabaseHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

private const val COURSE_DETAIL_TAG = "CourseDetailVM"

data class CourseNoteUiState(
    val text: String = "",
    val isPendingSync: Boolean = false,
    val updatedAt: Long = 0L
)

sealed class CourseDetailState {
    object Idle : CourseDetailState()
    object Loading : CourseDetailState()
    data class Success(
        val course: CourseDetail,
        val recentSessions: List<Session>,
        val occupancy: List<com.calico.tutor.data.dto.response.TutorOccupancyData>,
        val noteState: CourseNoteUiState,
        val isOffline: Boolean
    ) : CourseDetailState()
    data class Error(val message: String) : CourseDetailState()
}

class CourseDetailViewModel(
    private val context: Context,
    private val repository: CourseDetailRepository,
    private val subjectsApiService: SubjectsApiService,
    private val cacheDatabase: CacheDatabase,
    private val dbHelper: DatabaseHelper,
    private val memoryCache: InMemoryCache,
    private val userPreferences: UserPreferencesDataStore,
    private val gson: Gson = Gson()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourseDetailState>(CourseDetailState.Idle)
    val uiState: StateFlow<CourseDetailState> = _uiState.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private var currentCourseId: String = ""
    private var currentTutorId: String = ""
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var monitoringStarted = false

    fun load(courseId: String, tutorId: String) {
        currentCourseId = courseId
        currentTutorId = tutorId
        startConnectivityMonitoring()

        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value = CourseDetailState.Loading

            val detailDeferred = async(Dispatchers.IO) { repository.getCourseDetail(courseId) }
            val sessionsDeferred = async(Dispatchers.IO) { loadRecentSessions(tutorId, courseId) }
            val occupancyDeferred = async(Dispatchers.IO) { loadOccupancy(tutorId) }
            val noteDeferred = async(Dispatchers.IO) { loadCourseNote(courseId, tutorId) }

            val detailResult = detailDeferred.await()
            val sessions = sessionsDeferred.await()
            val occupancy = occupancyDeferred.await()
            val noteState = noteDeferred.await()

            _isOnline.value = isConnected()

            when (detailResult) {
                is Result.Success -> {
                    _uiState.value = CourseDetailState.Success(
                        course = detailResult.data,
                        recentSessions = sessions,
                        occupancy = occupancy,
                        noteState = noteState.copy(isPendingSync = noteState.isPendingSync || hasPendingNote(courseId)),
                        isOffline = !_isOnline.value
                    )
                    if (_isOnline.value) {
                        syncPendingNotes()
                    }
                }
                is Result.Error -> {
                    val message = if (!isConnected()) {
                        "No cached data available. Connect to the internet to load this course."
                    } else {
                        "Unable to load course detail. Please try again."
                    }
                    _uiState.value = CourseDetailState.Error(
                        message
                    )
                }
                Result.Loading -> {
                    _uiState.value = CourseDetailState.Error("Unable to load course detail")
                }
            }
        }
    }

    fun saveNote(noteText: String) {
        val courseId = currentCourseId
        val tutorId = currentTutorId
        if (courseId.isBlank() || tutorId.isBlank()) return

        val previous = getCurrentNoteState()
        val updated = previous.copy(
            text = noteText,
            updatedAt = System.currentTimeMillis(),
            isPendingSync = !isConnected()
        )

        viewModelScope.launch(Dispatchers.Main) {
            _isSaving.value = true
            try {
                withContext(Dispatchers.IO) {
                    dbUpsertNote(courseId, updated)
                    dbHelperDeletePendingNote(courseId)
                }
                if (isConnected()) {
                    val result = repository.updateCourseNote(tutorId, courseId, noteText)
                    when (result) {
                        is Result.Success -> {
                            withContext(Dispatchers.IO) {
                                dbHelperDeletePendingNote(courseId)
                            }
                            _snackbarMessages.tryEmit("Saved")
                        }
                        is Result.Error -> withContext(Dispatchers.IO) {
                            dbHelperEnqueuePendingNote(courseId, noteText, updated.updatedAt)
                        }
                        Result.Loading -> Unit
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        dbHelperEnqueuePendingNote(courseId, noteText, updated.updatedAt)
                    }
                    _snackbarMessages.tryEmit("Saved offline. Will sync when online.")
                }
                refreshUiAfterLocalChange(courseId, tutorId)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun syncPendingNotes() {
        val tutorId = currentTutorId
        if (tutorId.isBlank() || !isConnected()) return

        viewModelScope.launch(Dispatchers.Main) {
            val pendingNotes = withContext(Dispatchers.IO) {
                runCatching { dbHelper.getPendingCourseNotes() }
                    .getOrElse {
                        Log.e(COURSE_DETAIL_TAG, "Failed to load pending notes: ${it.message}")
                        emptyList()
                    }
            }

            if (pendingNotes.isEmpty()) return@launch

            for (pending in pendingNotes) {
                try {
                    val result = repository.updateCourseNote(
                        tutorId = tutorId,
                        courseId = pending.courseId,
                        note = pending.note.orEmpty()
                    )
                    if (result is Result.Success) {
                        withContext(Dispatchers.IO) {
                            dbHelper.deletePendingCourseNote(pending.id)
                            val local = dbHelper.getCourseNote(pending.courseId)
                            if (local != null) {
                                dbHelper.upsertCourseNote(local)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(COURSE_DETAIL_TAG, "Failed to sync note ${pending.courseId}: ${e.message}")
                }
            }

            if (currentCourseId.isNotBlank()) {
                refreshUiAfterLocalChange(currentCourseId, tutorId)
            }
        }
    }

    fun startConnectivityMonitoring() {
        if (monitoringStarted || currentTutorId.isBlank()) return
        monitoringStarted = true

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                viewModelScope.launch {
                    if (currentCourseId.isNotBlank() && currentTutorId.isNotBlank()) {
                        load(currentCourseId, currentTutorId)
                    }
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                _isOnline.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        }

        cm.registerNetworkCallback(request, networkCallback!!)
        _isOnline.value = isConnected()
    }

    override fun onCleared() {
        super.onCleared()
        networkCallback?.let { callback ->
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
    }

    private fun isConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private suspend fun loadRecentSessions(tutorId: String, courseId: String): List<Session> {
        val cacheKey = "${CacheDatabase.KEY_SESSIONS}_$tutorId"
        val expiryMs = userPreferences.cacheExpiryMs.first()

        memoryCache.get(cacheKey)?.let { entry ->
            (entry.value as? List<*>)?.let { raw ->
                val mapped = raw.filterIsInstance<TutoringSessionData>()
                return mapped.toSessions(courseId)
            }
        }

        val (cachedJson, cachedTs) = cacheDatabase.getCache(cacheKey)
        val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs
        val type = object : TypeToken<List<TutoringSessionData>>() {}.type

        if (isFresh && cachedJson != null) {
            val cached = gson.fromJson<List<TutoringSessionData>>(cachedJson, type)
            memoryCache.put(cacheKey, cached)
            return cached.toSessions(courseId)
        }

        return try {
            val response = subjectsApiService.getTutoringSessionsForTutor(tutorId)
            cacheDatabase.saveCache(cacheKey, gson.toJson(response.sessions, type))
            memoryCache.put(cacheKey, response.sessions)
            response.sessions.toSessions(courseId)
        } catch (e: Exception) {
            Log.e(COURSE_DETAIL_TAG, "Sessions fetch error: ${e.message}")
            if (cachedJson != null) {
                val cached = gson.fromJson<List<TutoringSessionData>>(cachedJson, type)
                memoryCache.put(cacheKey, cached)
                cached.toSessions(courseId)
            } else {
                emptyList()
            }
        }
    }

    private suspend fun loadOccupancy(tutorId: String): List<com.calico.tutor.data.dto.response.TutorOccupancyData> {
        val cacheKey = "${CacheDatabase.KEY_OCCUPANCY}_$tutorId"
        val expiryMs = userPreferences.cacheExpiryMs.first()

        memoryCache.get(cacheKey)?.let { entry ->
            (entry.value as? List<*>)?.let { raw ->
                return raw.filterIsInstance<com.calico.tutor.data.dto.response.TutorOccupancyData>()
            }
        }

        val (cachedJson, cachedTs) = cacheDatabase.getCache(cacheKey)
        val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs
        val type = object : TypeToken<List<com.calico.tutor.data.dto.response.TutorOccupancyData>>() {}.type

        if (isFresh && cachedJson != null) {
            val cached = gson.fromJson<List<com.calico.tutor.data.dto.response.TutorOccupancyData>>(cachedJson, type)
            memoryCache.put(cacheKey, cached)
            return cached
        }

        return try {
            val response = subjectsApiService.getTutorOccupancy(tutorId)
            cacheDatabase.saveCache(cacheKey, gson.toJson(response.data, type))
            memoryCache.put(cacheKey, response.data)
            response.data
        } catch (e: Exception) {
            Log.e(COURSE_DETAIL_TAG, "Occupancy fetch error: ${e.message}")
            if (cachedJson != null) {
                val cached = gson.fromJson<List<com.calico.tutor.data.dto.response.TutorOccupancyData>>(cachedJson, type)
                memoryCache.put(cacheKey, cached)
                cached
            } else {
                emptyList()
            }
        }
    }

    private suspend fun loadCourseNote(courseId: String, tutorId: String): CourseNoteUiState {
        val local = runCatching {
            withContext(Dispatchers.IO) { dbHelper.getCourseNote(courseId) }
        }.getOrNull()
        val pending = hasPendingNote(courseId)
        val remote = when (val result = repository.getTutorCourses(tutorId)) {
            is Result.Success -> result.data.firstOrNull { it.id == courseId }?.note.orEmpty()
            is Result.Error -> ""
            Result.Loading -> ""
        }

        val text = when {
            local != null -> local.note.orEmpty()
            remote.isNotBlank() -> remote
            else -> ""
        }

        return CourseNoteUiState(
            text = text,
            isPendingSync = pending,
            updatedAt = local?.updatedAt ?: 0L
        )
    }

    private suspend fun hasPendingNote(courseId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { dbHelper.getPendingCourseNotes().any { it.courseId == courseId } }
            .getOrDefault(false)
    }

    private fun getCurrentNoteState(): CourseNoteUiState {
        val state = _uiState.value
        return when (state) {
            is CourseDetailState.Success -> state.noteState
            else -> CourseNoteUiState()
        }
    }

    private suspend fun dbUpsertNote(courseId: String, noteState: CourseNoteUiState) {
        withContext(Dispatchers.IO) {
            dbHelper.upsertCourseNote(
                DatabaseHelper.CourseNote(
                    courseId = courseId,
                    note = noteState.text,
                    updatedAt = noteState.updatedAt
                )
            )
        }
    }

    private suspend fun dbHelperEnqueuePendingNote(courseId: String, note: String?, updatedAt: Long) {
        runCatching { dbHelper.deletePendingCourseNotesForCourse(courseId) }
        runCatching { dbHelper.enqueuePendingCourseNote(courseId, note, updatedAt) }
    }

    private suspend fun dbHelperDeletePendingNote(courseId: String) {
        runCatching { dbHelper.deletePendingCourseNotesForCourse(courseId) }
    }

    private suspend fun refreshUiAfterLocalChange(courseId: String, tutorId: String) {
        val current = _uiState.value
        if (current is CourseDetailState.Success) {
            _uiState.value = current.copy(
                noteState = loadCourseNote(courseId, tutorId).copy(isPendingSync = hasPendingNote(courseId)),
                isOffline = !isConnected()
            )
        } else {
            load(courseId, tutorId)
        }
    }

    private fun List<TutoringSessionData>.toSessions(courseId: String): List<Session> {
        val mapped = map { session -> session.toDomainSession() }

        return mapped
            .filter { it.courseId == courseId || it.course == courseId }
            .sortedByDescending { parseStartTimestamp(it.scheduledStart) }
            .take(5)
    }

    private fun TutoringSessionData.toDomainSession(): Session {
        return Session(
            id = id,
            studentId = studentId,
            scheduledStart = scheduledStart,
            scheduledEnd = scheduledEnd,
            status = status,
            course = course,
            courseId = courseId,
            date = scheduledStart,
            time = scheduledStart,
            tutorName = "",
            subjectName = course ?: courseId ?: "Unknown",
            subjectCode = "",
            studentName = studentName,
            studentAvatarUrl = studentAvatarUrl,
            price = price
        )
    }

    private fun parseStartTimestamp(value: String): Long {
        val fmt1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val fmt2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        return try {
            fmt1.parse(value)?.time ?: 0L
        } catch (_: Exception) {
            try {
                fmt2.parse(value)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }

}

class CourseDetailViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourseDetailViewModel(
                context = context,
                repository = ServiceLocator.courseDetailRepository(context),
                subjectsApiService = ServiceLocator.subjectsApiService(context),
                cacheDatabase = ServiceLocator.cacheDatabase(context),
                dbHelper = ServiceLocator.provideDatabaseHelper(context),
                memoryCache = ServiceLocator.inMemoryCache(),
                userPreferences = ServiceLocator.userPreferences(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
