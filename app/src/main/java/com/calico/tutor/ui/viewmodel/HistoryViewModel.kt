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
import kotlinx.coroutines.coroutineScope
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

    private val cacheDb     = ServiceLocator.cacheDatabase(context)
    private val userPrefs   = ServiceLocator.userPreferences(context)
    private val memoryCache = ServiceLocator.inMemoryCache()
    // Estrategia 3 de local storage: FileManager para logs auditables (mismo patrón que Home y Disponibilidad)
    private val fileManager = ServiceLocator.fileManager(context)
    private val gson        = Gson()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var monitoredTutorId: String = ""
    private var monitoredStudentId: String = ""

    init {
        checkCurrentConnectivity()
    }

    fun loadTutorHistory(tutorId: String) {
        monitoredTutorId = tutorId

        // Corrutina padre: ligada al ciclo de vida del ViewModel, ejecuta en Main Thread
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading

            // Cambio a Dispatchers.IO para operación pesada: red + SQLite + resolución de perfiles
            // Flujo idéntico a Home: L1 (InMemoryCache) → L2 (SQLite fresco) → Red → L2 expirado (fallback)
            val result = withContext(Dispatchers.IO) {
                HistoryCacheLoader.loadTutorHistory(
                    context = context,
                    cacheDb = cacheDb,
                    memoryCache = memoryCache,
                    userPrefs = userPrefs,
                    fileManager = fileManager,
                    gson = gson,
                    tutorId = tutorId
                )
            }

            // Structured concurrency: corrutinas hijas en paralelo dentro de la corrutina padre
            coroutineScope {
                // Corrutina hija 1: persistir timestamp de último acceso en BD local (Dispatchers.IO)
                launch(Dispatchers.IO) {
                    cacheDb.saveCache(
                        "history_last_access_$tutorId",
                        System.currentTimeMillis().toString()
                    )
                    Log.d(TAG, "[$tutorId] Timestamp de último acceso persistido en SQLite")
                }

                // Corrutina hija 2: pre-calentar caché de materias del tutor en segundo plano (Dispatchers.IO)
                launch(Dispatchers.IO) {
                    runCatching {
                        val subjectHistory = ServiceLocator.subjectsApiService(context)
                            .getTutorSessionHistory(tutorId)
                        cacheDb.saveCache(
                            "history_subjects_$tutorId",
                            gson.toJson(subjectHistory)
                        )
                        Log.d(TAG, "[$tutorId] Caché de materias pre-calentado correctamente")
                    }.onFailure { e ->
                        Log.w(TAG, "[$tutorId] Warmup de materias falló (no crítico): ${e.message}")
                    }
                }
            }

            // Sincronizar timestamp de última carga exitosa con DataStore (mismo patrón que Home)
            if (result is HistoryState.Success) {
                userPrefs.updateLastSyncTime()
            }
            _historyState.value = result
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

        // Corrutina padre: ligada al ciclo de vida del ViewModel, ejecuta en Main Thread
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading

            // Cambio a Dispatchers.IO para operación pesada: red + SQLite + resolución de perfiles
            // Flujo idéntico a Home: L1 (InMemoryCache) → L2 (SQLite fresco) → Red → L2 expirado (fallback)
            val result = withContext(Dispatchers.IO) {
                HistoryCacheLoader.loadStudentHistory(
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

            // Structured concurrency: corrutinas hijas en paralelo dentro de la corrutina padre
            coroutineScope {
                // Corrutina hija 1: persistir timestamp de último acceso en BD local (Dispatchers.IO)
                launch(Dispatchers.IO) {
                    cacheDb.saveCache(
                        "history_last_access_student_$studentId",
                        System.currentTimeMillis().toString()
                    )
                    Log.d(TAG, "[$studentId] Timestamp de último acceso persistido en SQLite")
                }

                // Corrutina hija 2: persistir contador de sesiones para métricas locales (Dispatchers.IO)
                launch(Dispatchers.IO) {
                    if (result is HistoryState.Success) {
                        cacheDb.saveCache(
                            "history_session_count_$studentId",
                            result.sessions.size.toString()
                        )
                        Log.d(TAG, "[$studentId] Contador de sesiones cacheado: ${result.sessions.size}")
                    }
                }
            }

            // Sincronizar timestamp de última carga exitosa con DataStore (mismo patrón que Home)
            if (result is HistoryState.Success) {
                userPrefs.updateLastSyncTime()
            }
            _historyState.value = result
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