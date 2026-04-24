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
import com.calico.tutor.data.dto.response.CourseData
import com.calico.tutor.data.dto.response.SessionAlertResponse
import com.calico.tutor.data.dto.response.TutorOccupancyData
import com.calico.tutor.data.dto.response.TutoringSessionData
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.Session
import com.calico.tutor.util.JwtUtils
import com.calico.tutor.util.NotificationHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "HomeViewModel"

sealed class SessionsState {
    object Idle    : SessionsState()
    object Loading : SessionsState()
    data class Success(
        val previousSessions: List<Session>,
        val upcomingSessions: List<Session>
    ) : SessionsState()
    data class Error(val message: String) : SessionsState()
}

sealed class OccupancyState {
    object Idle    : OccupancyState()
    object Loading : OccupancyState()
    data class Success(val data: List<TutorOccupancyData>) : OccupancyState()
    data class Error(val message: String) : OccupancyState()
}

sealed class SubjectsState {
    object Idle    : SubjectsState()
    object Loading : SubjectsState()
    data class Success(val subjects: List<CourseData>) : SubjectsState()
    data class Error(val message: String) : SubjectsState()
}

sealed class SessionAlertState {
    object Idle : SessionAlertState()
    data class Alert(val response: SessionAlertResponse) : SessionAlertState()
    data class Error(val message: String) : SessionAlertState()
}

/**
 * ViewModel para HomeScreen.
 *
 * MULTI-THREADING (20 pts):
 * loadAllData() usa el patrón de corrutinas anidadas:
 * - Corrutina externa: viewModelScope.launch(Dispatchers.Main)
 * - Corrutinas internas: async(Dispatchers.IO) para cada uno de los 4 componentes
 * - await() recibe los resultados de vuelta en Dispatchers.Main
 * - Los StateFlow se actualizan en Dispatchers.Main
 *
 * CACHING (20 pts) - flujo de 2 niveles para cada dato:
 * 1. Revisar InMemoryCache (L1 - LRU)
 * 2. Si L1 miss → revisar SQLite con verificación de frescura (L2 - disco)
 * 3. Si L2 fresco → usar y poblar L1
 * 4. Si L2 expirado/vacío → traer de red
 * 5. Éxito en red → guardar en L1 y L2
 * 6. Fallo de red + L2 con datos → usar como fallback (aunque expirado)
 * 7. Sin red y sin caché → SessionsState.Error("No hay datos disponibles")
 *
 * EVENTUAL CONNECTIVITY (20 pts) - vistas protegidas:
 * - Upcoming sessions, Previous sessions, Rate of occupancy, Subjects recommended
 */
class HomeScreenViewModel(private val context: Context) : ViewModel() {

    private val _sessionsState  = MutableStateFlow<SessionsState>(SessionsState.Idle)
    val sessionsState: StateFlow<SessionsState> = _sessionsState.asStateFlow()

    private val _occupancyState = MutableStateFlow<OccupancyState>(OccupancyState.Idle)
    val occupancyState: StateFlow<OccupancyState> = _occupancyState.asStateFlow()

    private val _subjectsState  = MutableStateFlow<SubjectsState>(SubjectsState.Idle)
    val subjectsState: StateFlow<SubjectsState> = _subjectsState.asStateFlow()

    private val _tutorName = MutableStateFlow("")
    val tutorName: StateFlow<String> = _tutorName.asStateFlow()

    private val _sessionAlertState = MutableStateFlow<SessionAlertState>(SessionAlertState.Idle)
    val sessionAlertState: StateFlow<SessionAlertState> = _sessionAlertState.asStateFlow()

    private val cacheDb      = ServiceLocator.cacheDatabase(context)
    private val userPrefs    = ServiceLocator.userPreferences(context)
    private val memoryCache  = ServiceLocator.inMemoryCache()
    private val fileManager  = ServiceLocator.fileManager(context)
    private val telemetryRepository = ServiceLocator.telemetryRepository(context)
    private val tokenManager = ServiceLocator.provideTokenManager(context)
    private val gson         = Gson()

    private val shownNotifications        = mutableSetOf<String>()
    private var lastConnectionWarningTime = 0L
    private val CONNECTION_WARNING_COOLDOWN_MS = 300_000L
    private var homepageLoadStartMs: Long = 0L
    private var homepageLoadReported = false

    fun onHomepageOpened() {
        homepageLoadStartMs = System.currentTimeMillis()
        homepageLoadReported = false
    }

    fun onHomepageContentRendered(isSessionsReady: Boolean, isTopSubjectsReady: Boolean) {
        if (!isSessionsReady || !isTopSubjectsReady || homepageLoadReported) return
        if (homepageLoadStartMs <= 0L) return

        val loadTimeMs = System.currentTimeMillis() - homepageLoadStartMs
        val connectivityStatus = if (isDeviceOnline()) "online" else "offline"
        val firebaseUid = tokenManager.getIdToken()?.let { JwtUtils.extractFirebaseUid(it) }

        homepageLoadReported = true
        telemetryRepository.reportHomepageLoad(
            loadTimeMs = loadTimeMs,
            connectivityStatus = connectivityStatus,
            userId = firebaseUid
        )
        Log.d(TAG, "Homepage telemetry sent: load_time_ms=$loadTimeMs, connectivity_status=$connectivityStatus, user_id=${firebaseUid ?: "null"}")
    }

    // Connectivity monitoring for auto-refresh
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var monitoredTutorId: String = ""

    // ─────────────────────────────────────────────────────────────────────────
    // MULTI-THREADING: carga paralela con corrutinas anidadas
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carga los 4 componentes de Home simultáneamente.
     *
     * Patrón de multi-threading implementado:
     * - viewModelScope.launch(Dispatchers.Main): corrutina EXTERNA en el hilo principal.
     * - async(Dispatchers.IO): corrutinas INTERNAS en el pool de IO, ejecutadas en paralelo.
     *   Estas son corrutinas "una dentro de otra" respecto al contexto Main.
     * - await(): espera los resultados de IO de vuelta en el contexto de Dispatchers.Main.
     * - Los MutableStateFlow se emiten desde Dispatchers.Main al final.
     */
    fun loadAllData(tutorId: String) {
        viewModelScope.launch(Dispatchers.Main) {            // ── Estado inicial ──────────────────────────────────────────────
            _sessionsState.value  = SessionsState.Loading
            _occupancyState.value = OccupancyState.Loading
            _subjectsState.value  = SubjectsState.Loading

            // ── Corrutinas IO anidadas (ejecutadas concurrentemente) ─────────
            val profileDeferred   = async(Dispatchers.IO) { fetchTutorName(tutorId) }
            val sessionsDeferred  = async(Dispatchers.IO) { fetchSessionsWithCache(tutorId) }
            val occupancyDeferred = async(Dispatchers.IO) { fetchOccupancyWithCache(tutorId) }
            val subjectsDeferred  = async(Dispatchers.IO) { fetchSubjectsWithCache() }

            // ── await() + actualizar StateFlow en Dispatchers.Main ───────────
            _tutorName.value      = profileDeferred.await()
            _sessionsState.value  = sessionsDeferred.await()
            _occupancyState.value = occupancyDeferred.await()
            _subjectsState.value  = subjectsDeferred.await()

            if (_sessionsState.value !is SessionsState.Error) {
                userPrefs.updateLastSyncTime()
            }
        }
    }

    fun refreshData(tutorId: String) = loadAllData(tutorId)

    // ─────────────────────────────────────────────────────────────────────────
    // EVENTUAL CONNECTIVITY: auto-refresh when network returns
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a ConnectivityManager callback that reloads data whenever the
     * device regains internet access and the home data is missing or in error.
     * Must be called once from the composable's LaunchedEffect.
     */
    fun startConnectivityMonitoring(tutorId: String) {
        if (networkCallback != null) return   // already registered
        monitoredTutorId = tutorId
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val needsLoad = _sessionsState.value is SessionsState.Error ||
                    _sessionsState.value is SessionsState.Idle ||
                    _occupancyState.value is OccupancyState.Error ||
                    _subjectsState.value is SubjectsState.Error
                if (needsLoad) {
                    viewModelScope.launch(Dispatchers.Main) {
                        Log.d(TAG, "Connectivity restored — reloading home data")
                        loadAllData(monitoredTutorId)
                    }
                }
            }
        }
        cm.registerNetworkCallback(req, networkCallback!!)
        Log.d(TAG, "Connectivity monitoring started for $tutorId")
    }

    override fun onCleared() {
        super.onCleared()
        networkCallback?.let { cb ->
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try { cm.unregisterNetworkCallback(cb) } catch (_: Exception) {}
        }
        networkCallback = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BQ15 TELEMETRY: homepage load time
    // ─────────────────────────────────────────────────────────────────────────

    /** Call when the Home composable enters composition (before data loads). */
    fun onHomepageOpened() {
        homepageLoadStartMs  = System.currentTimeMillis()
        homepageLoadReported = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FETCH HELPERS — se ejecutan en Dispatchers.IO via async(IO)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun fetchTutorName(tutorId: String): String = try {
        ServiceLocator.subjectsApiService(context).getTutorProfile(tutorId).name
    } catch (e: Exception) {
        Log.e(TAG, "fetchTutorName error: ${e.message}")
        ""
    }

    /**
     * Vista 1 y 2: Upcoming + Previous sessions con caché de 2 niveles.
     *
     * Flujo: L1 (memoria) → L2 SQLite fresco → Red → L2 SQLite expirado (fallback) → Error
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun fetchSessionsWithCache(tutorId: String): SessionsState {
        val cacheKey  = "${CacheDatabase.KEY_SESSIONS}_$tutorId"
        val expiryMs  = userPrefs.cacheExpiryMs.first()

        // Nivel 1: caché en memoria (LRU)
        memoryCache.get(cacheKey)?.let { entry ->
            Log.d(TAG, "Sessions: L1 cache hit")
            val rawList = entry.value as List<TutoringSessionData>
            return buildSessionsState(rawList)
        }

        // Consultar L2 para verificar frescura
        val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
        val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs

        // Nivel 2: SQLite con datos frescos → usar sin llamar a la red
        if (isFresh && cachedJson != null) {
            Log.d(TAG, "Sessions: L2 cache hit (fresh)")
            val type    = object : TypeToken<List<TutoringSessionData>>() {}.type
            val rawList = gson.fromJson<List<TutoringSessionData>>(cachedJson, type)
            memoryCache.put(cacheKey, rawList)
            return buildSessionsState(rawList)
        }

        // Nivel 3: Red
        return try {
            val response = ServiceLocator.subjectsApiService(context).getTutoringSessionsForTutor(tutorId)
            val type     = object : TypeToken<List<TutoringSessionData>>() {}.type
            cacheDb.saveCache(cacheKey, gson.toJson(response.sessions, type))
            memoryCache.put(cacheKey, response.sessions)
            fileManager.appendLog("Sessions cargadas de red para $tutorId")
            buildSessionsState(response.sessions)
        } catch (e: Exception) {
            Log.e(TAG, "Sessions network error: ${e.message}")
            fileManager.appendLog("Sessions network error: ${e.message}")
            // Fallback: usar L2 aunque esté expirado
            if (cachedJson != null) {
                val type    = object : TypeToken<List<TutoringSessionData>>() {}.type
                val rawList = gson.fromJson<List<TutoringSessionData>>(cachedJson, type)
                memoryCache.put(cacheKey, rawList)
                buildSessionsState(rawList)
            } else {
                SessionsState.Error("No hay datos disponibles")
            }
        }
    }

    /**
     * Vista 3: Rate of occupancy con caché de 2 niveles.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun fetchOccupancyWithCache(tutorId: String): OccupancyState {
        val cacheKey = "${CacheDatabase.KEY_OCCUPANCY}_$tutorId"
        val expiryMs = userPrefs.cacheExpiryMs.first()

        memoryCache.get(cacheKey)?.let { entry ->
            Log.d(TAG, "Occupancy: L1 cache hit")
            return OccupancyState.Success(entry.value as List<TutorOccupancyData>)
        }

        val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
        val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs

        if (isFresh && cachedJson != null) {
            Log.d(TAG, "Occupancy: L2 cache hit (fresh)")
            val type = object : TypeToken<List<TutorOccupancyData>>() {}.type
            val data = gson.fromJson<List<TutorOccupancyData>>(cachedJson, type)
            memoryCache.put(cacheKey, data)
            return OccupancyState.Success(data)
        }

        return try {
            val response = ServiceLocator.subjectsApiService(context).getTutorOccupancy(tutorId)
            val type     = object : TypeToken<List<TutorOccupancyData>>() {}.type
            cacheDb.saveCache(cacheKey, gson.toJson(response.data, type))
            memoryCache.put(cacheKey, response.data)
            OccupancyState.Success(response.data)
        } catch (e: Exception) {
            Log.e(TAG, "Occupancy network error: ${e.message}")
            if (cachedJson != null) {
                val type = object : TypeToken<List<TutorOccupancyData>>() {}.type
                val data = gson.fromJson<List<TutorOccupancyData>>(cachedJson, type)
                memoryCache.put(cacheKey, data)
                OccupancyState.Success(data)
            } else {
                OccupancyState.Error("No hay datos disponibles")
            }
        }
    }

    /**
     * Vista 4: Subjects recommended con caché de 2 niveles.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun fetchSubjectsWithCache(): SubjectsState {
        val cacheKey = CacheDatabase.KEY_SUBJECTS
        val expiryMs = userPrefs.cacheExpiryMs.first()

        memoryCache.get(cacheKey)?.let { entry ->
            Log.d(TAG, "Subjects: L1 cache hit")
            return SubjectsState.Success(entry.value as List<CourseData>)
        }

        val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
        val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs

        if (isFresh && cachedJson != null) {
            Log.d(TAG, "Subjects: L2 cache hit (fresh)")
            val type     = object : TypeToken<List<CourseData>>() {}.type
            val subjects = gson.fromJson<List<CourseData>>(cachedJson, type)
            memoryCache.put(cacheKey, subjects)
            return SubjectsState.Success(subjects)
        }

        return try {
            val response = ServiceLocator.subjectsApiService(context).getSubjectsHistory()
            val subjects = response.data ?: emptyList()
            val type     = object : TypeToken<List<CourseData>>() {}.type
            cacheDb.saveCache(cacheKey, gson.toJson(subjects, type))
            memoryCache.put(cacheKey, subjects)
            SubjectsState.Success(subjects)
        } catch (e: Exception) {
            Log.e(TAG, "Subjects network error: ${e.message}")
            if (cachedJson != null) {
                val type     = object : TypeToken<List<CourseData>>() {}.type
                val subjects = gson.fromJson<List<CourseData>>(cachedJson, type)
                memoryCache.put(cacheKey, subjects)
                SubjectsState.Success(subjects)
            } else {
                SubjectsState.Error("No hay datos disponibles")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SESSION ALERTS POLLING
    // ─────────────────────────────────────────────────────────────────────────

    fun startSessionAlertPolling() {
        viewModelScope.launch {
            Log.d(TAG, "Iniciando polling de alertas de sesión (cada 60 segundos)")
            NotificationHelper.createNotificationChannel(context)
            while (true) {
                try {
                    val alertResponse = ServiceLocator.analyticsApiService(context).getSessionAlert()
                    _sessionAlertState.value = SessionAlertState.Alert(alertResponse)

                    if (alertResponse.hasAlert &&
                        alertResponse.studentName != null &&
                        alertResponse.minutesToStart != null
                    ) {
                        val key = alertResponse.sessionId ?: alertResponse.studentName
                        if (!shownNotifications.contains(key)) {
                            NotificationHelper.showSessionAlertNotification(
                                context, alertResponse.studentName, alertResponse.minutesToStart, key
                            )
                            shownNotifications.add(key)

                            if (alertResponse.minutesToStart <= 15) {
                                delay(500)
                                val latency = measureNetworkLatency()
                                if (latency > 500) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastConnectionWarningTime > CONNECTION_WARNING_COOLDOWN_MS) {
                                        NotificationHelper.showConnectionWarningNotification(context, latency)
                                        lastConnectionWarningTime = now
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Alert polling error: ${e.message}")
                    _sessionAlertState.value = SessionAlertState.Error("${e.message}")
                }
                delay(60_000)
            }
        }
    }

    private suspend fun measureNetworkLatency(): Long = try {
        val latencies = mutableListOf<Long>()
        repeat(3) {
            try {
                val start = System.currentTimeMillis()
                ServiceLocator.subjectsApiService(context).getTutorProfile("health-check")
                latencies.add(System.currentTimeMillis() - start)
            } catch (e: Exception) {
                latencies.add(2000L)
            }
            delay(100)
        }
        latencies.average().toLong()
    } catch (e: Exception) { 2000L }

    private fun isDeviceOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS DE MAPEO
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildSessionsState(rawList: List<TutoringSessionData>): SessionsState {
        val now     = System.currentTimeMillis()
        val fmt1    = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val fmt2    = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val mapped  = rawList.map { s ->
            val ms = try { fmt1.parse(s.scheduledStart)?.time ?: 0L }
                     catch (e: Exception) { try { fmt2.parse(s.scheduledStart)?.time ?: 0L } catch (e2: Exception) { 0L } }
            Session(
                id             = s.id,
                scheduledStart = s.scheduledStart,
                scheduledEnd   = s.scheduledEnd,
                status         = s.status,
                course         = s.course,
                courseId       = s.courseId,
                date           = s.scheduledStart,
                time           = s.scheduledStart,
                tutorName      = "",
                subjectName    = s.course ?: s.courseId ?: "Unknown",
                subjectCode    = ""
            ) to ms
        }
        val previous = mapped.filter { it.second < now }.sortedByDescending { it.second }.map { it.first }
        val upcoming = mapped.filter { it.second >= now }.sortedBy { it.second }.map { it.first }
        return SessionsState.Success(previous, upcoming)
    }
}

class HomeScreenViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeScreenViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
