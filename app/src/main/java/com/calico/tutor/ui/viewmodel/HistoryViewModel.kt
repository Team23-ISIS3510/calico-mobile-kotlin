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
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.FileManager
import com.calico.tutor.data.local.UserPreferencesDataStore
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.Session
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "HistoryViewModel"

sealed class HistoryState {
    data object Idle : HistoryState()
    data object Loading : HistoryState()
    data class Success(val sessions: List<Session>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}

class HistoryViewModel(private val context: Context) : ViewModel() {
    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Idle)
    val historyState: StateFlow<HistoryState> = _historyState.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val cacheDb = ServiceLocator.cacheDatabase(context)
    private val userPrefs = ServiceLocator.userPreferences(context)
    private val memoryCache = ServiceLocator.inMemoryCache()
    // Estrategia 3 de local storage: FileManager para logs auditables (mismo patrón que Home y Disponibilidad)
    private val fileManager = ServiceLocator.fileManager(context)
    private val telemetryRepo = ServiceLocator.telemetryRepository(context)
    private val gson = Gson()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var monitoredTutorId: String = ""
    private var monitoredStudentId: String = ""

    init {
        checkCurrentConnectivity()
    }

    fun reportHistoryViewOpened(tutorId: String) {
        telemetryRepo.reportHistoryViewOpened(tutorId)
    }

    fun loadTutorHistory(tutorId: String) {
        monitoredTutorId = tutorId

        // Corrutina padre: administrada por viewModelScope (Main Thread)
        // Se cancela automáticamente si el usuario sale de la pantalla → sin memory leaks
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading

            // ── PASO 1: cache hit ────────────────────────────────────────────────────
            // Lee L1 (InMemoryCache) → L2 (SQLite) sin tocar la red.
            // Dispatchers.IO: lectura de SQLite bloquea; nunca correr en Main Thread.
            val cachedResult = withContext(Dispatchers.IO) {
                HistoryCacheLoader.readCachedTutorHistory(
                    context = context,
                    cacheDb = cacheDb,
                    memoryCache = memoryCache,
                    userPrefs = userPrefs,
                    fileManager = fileManager,
                    gson = gson,
                    tutorId = tutorId
                )
            }

            // Mostrar datos locales inmediatamente si existen (UX: sin esperar la red)
            // Si no hay caché, la UI permanece en Loading hasta que la red responda
            if (cachedResult != null) {
                _historyState.value = cachedResult
            }

            // ── PASO 2: corrutina hija 1 — refresh de red en background ─────────────
            // Patrón stale-while-revalidate: la UI ya muestra el caché, la red actualiza en paralelo.
            // No usamos coroutineScope{} → el padre NO espera a esta hija para continuar.
            // Dispatchers.IO: llamada Retrofit bloquea; SQLite también.
            launch(Dispatchers.IO) {
                runCatching {
                    HistoryCacheLoader.fetchAndCacheTutorHistory(
                        context = context,
                        cacheDb = cacheDb,
                        memoryCache = memoryCache,
                        fileManager = fileManager,
                        gson = gson,
                        userPrefs = userPrefs,
                        tutorId = tutorId
                    )
                }.onSuccess { freshResult ->
                    // Volver a Main Thread para actualizar la UI con datos frescos de la red
                    withContext(Dispatchers.Main) {
                        _historyState.value = freshResult
                        userPrefs.updateLastSyncTime()
                    }
                }.onFailure { e ->
                    // Red falló: si ya había caché visible, el usuario no nota nada
                    // Solo mostrar error si el cache miss fue total (sin datos locales)
                    fileManager.appendLog("History background refresh falló para $tutorId: ${e.message}")
                    Log.w(TAG, "[$tutorId] Refresh de red falló — manteniendo caché visible")
                    if (cachedResult == null) {
                        withContext(Dispatchers.Main) {
                            _historyState.value = HistoryState.Error("Failed to load history.")
                        }
                    }
                }
            }

            // ── PASO 3: corrutina hija 2 — timestamp de acceso en SQLite ────────────
            // Fire-and-forget: registrar cuándo se visitó el historial por última vez.
            launch(Dispatchers.IO) {
                cacheDb.saveCache(
                    "history_last_access_$tutorId",
                    System.currentTimeMillis().toString()
                )
            }

            // ── PASO 4: corrutina hija 3 — pre-calentar caché de materias ────────────
            // Calienta caché secundaria en background sin impactar la UI principal.
            launch(Dispatchers.IO) {
                runCatching {
                    val subjectHistory = ServiceLocator.subjectsApiService(context)
                        .getTutorSessionHistory(tutorId)
                    cacheDb.saveCache("history_subjects_$tutorId", gson.toJson(subjectHistory))
                }.onFailure { Log.w(TAG, "[$tutorId] Subject warmup falló (no crítico)") }
            }
        }
    }

    fun loadStudentHistory(
        studentId: String,
        startDate: String? = null,
        endDate: String? = null,
        course: String? = null,
        limit: Int? = null
    ) {
        monitoredStudentId = studentId

        // Corrutina padre: administrada por viewModelScope (Main Thread)
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading

            // ── PASO 1: cache hit ────────────────────────────────────────────────────
            // Lee L1 (InMemoryCache) → L2 (SQLite) sin tocar la red.
            val cachedResult = withContext(Dispatchers.IO) {
                HistoryCacheLoader.readCachedStudentHistory(
                    context = context,
                    cacheDb = cacheDb,
                    memoryCache = memoryCache,
                    userPrefs = userPrefs,
                    fileManager = fileManager,
                    gson = gson,
                    studentId = studentId,
                    startDate = startDate,
                    endDate = endDate,
                    course = course,
                    limit = limit
                )
            }

            if (cachedResult != null) {
                _historyState.value = cachedResult
            }

            // ── PASO 2: corrutina hija 1 — refresh de red en background ─────────────
            launch(Dispatchers.IO) {
                runCatching {
                    HistoryCacheLoader.fetchAndCacheStudentHistory(
                        context = context,
                        cacheDb = cacheDb,
                        memoryCache = memoryCache,
                        fileManager = fileManager,
                        gson = gson,
                        userPrefs = userPrefs,
                        studentId = studentId,
                        startDate = startDate,
                        endDate = endDate,
                        course = course,
                        limit = limit
                    )
                }.onSuccess { freshResult ->
                    withContext(Dispatchers.Main) {
                        _historyState.value = freshResult
                        userPrefs.updateLastSyncTime()
                    }
                }.onFailure { e ->
                    fileManager.appendLog("History background refresh falló para student $studentId: ${e.message}")
                    if (cachedResult == null) {
                        withContext(Dispatchers.Main) {
                            _historyState.value = HistoryState.Error("Failed to load history.")
                        }
                    }
                }
            }

            // ── PASO 3: corrutina hija 2 — timestamp de acceso en SQLite ────────────
            launch(Dispatchers.IO) {
                cacheDb.saveCache(
                    "history_last_access_student_$studentId",
                    System.currentTimeMillis().toString()
                )
            }

            // ── PASO 4: corrutina hija 3 — persistir contador de sesiones ────────────
            launch(Dispatchers.IO) {
                if (cachedResult is HistoryState.Success) {
                    cacheDb.saveCache(
                        "history_session_count_$studentId",
                        cachedResult.sessions.size.toString()
                    )
                }
            }
        }
    }

    fun startTutorConnectivityMonitoring(tutorId: String) {
        if (networkCallback != null) return
        monitoredTutorId = tutorId
        checkCurrentConnectivity()
        registerConnectivityCallback {
            if (monitoredTutorId.isNotBlank()) {
                loadTutorHistory(monitoredTutorId)
            }
        }
        Log.d(TAG, "Tutor connectivity monitoring started for $tutorId")
    }

    fun startStudentConnectivityMonitoring(studentId: String) {
        if (networkCallback != null) return
        monitoredStudentId = studentId
        checkCurrentConnectivity()
        registerConnectivityCallback {
            if (monitoredStudentId.isNotBlank()) {
                loadStudentHistory(monitoredStudentId)
            }
        }
        Log.d(TAG, "Student connectivity monitoring started for $studentId")
    }

    private fun registerConnectivityCallback(onReconnect: () -> Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                _isOnline.value = true
                val shouldReload = _historyState.value is HistoryState.Error || _historyState.value is HistoryState.Idle
                if (shouldReload) {
                    viewModelScope.launch(Dispatchers.Main) {
                        onReconnect()
                    }
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                _isOnline.value = false
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    private fun checkCurrentConnectivity() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOnline.value = hasInternet
        Log.d(TAG, "Current connectivity status: $hasInternet")
    }

    override fun onCleared() {
        super.onCleared()
        networkCallback?.let { callback ->
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
    }
}

class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}