package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.FileManager
import com.calico.tutor.data.local.UserPreferencesDataStore
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.domain.repository.AvailabilityRepository
import com.calico.tutor.domain.utils.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "AvailabilityVM"

sealed class AvailabilityListState {
    object Idle    : AvailabilityListState()
    object Loading : AvailabilityListState()
    data class Success(val items: List<AvailabilityItem>) : AvailabilityListState()
    data class Error(val message: String) : AvailabilityListState()
}

sealed class AvailabilityActionState {
    object Idle    : AvailabilityActionState()
    object Loading : AvailabilityActionState()
    object Done    : AvailabilityActionState()
    data class Error(val message: String) : AvailabilityActionState()
    /** Emitted when an action was queued offline; message is shown to the user. */
    data class OfflineSaved(val message: String) : AvailabilityActionState()
}

/**
 * ViewModel para AvailabilityScreen.
 *
 * EVENTUAL CONNECTIVITY — offline-first para todas las mutaciones:
 * 1. Guardar en pending_availabilities (SQLite) SIEMPRE antes de llamar a la red.
 * 2. Si no hay conexión → emitir OfflineSaved inmediatamente (sin esperar timeout de red).
 * 3. Si hay conexión → intentar red; en éxito eliminar de cola; en fallo mantener en cola.
 * 4. WorkManager reintenta periódicamente cuando hay red.
 *
 * pendingCount: StateFlow visible en UI para mostrar badge de sincronizaciones pendientes.
 */
class AvailabilityViewModel(
    private val repository: AvailabilityRepository,
    private val tutorId: String,
    private val context: Context
) : ViewModel() {

    private val _listState   = MutableStateFlow<AvailabilityListState>(AvailabilityListState.Idle)
    val listState: StateFlow<AvailabilityListState> = _listState.asStateFlow()

    private val _actionState = MutableStateFlow<AvailabilityActionState>(AvailabilityActionState.Idle)
    val actionState: StateFlow<AvailabilityActionState> = _actionState.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val cacheDb     = ServiceLocator.cacheDatabase(context)
    private val userPrefs   = ServiceLocator.userPreferences(context)
    private val memoryCache = ServiceLocator.inMemoryCache()
    private val fileManager = ServiceLocator.fileManager(context)
    private val gson        = Gson()

    init {
        load()
        refreshPendingCount()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Connectivity helper
    // ─────────────────────────────────────────────────────────────────────────

    private fun isConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vista 5: Ver availabilities con caché + offline fallback
    // ─────────────────────────────────────────────────────────────────────────

    fun load() {
        viewModelScope.launch {
            _listState.value = AvailabilityListState.Loading
            val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
            val expiryMs = userPrefs.cacheExpiryMs.first()

            // Nivel 1: caché en memoria
            @Suppress("UNCHECKED_CAST")
            memoryCache.get(cacheKey)?.let { entry ->
                Log.d(TAG, "Availabilities: L1 hit")
                _listState.value = AvailabilityListState.Success(entry.value as List<AvailabilityItem>)
                return@launch
            }

            // Consultar L2 para verificar frescura
            val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
            val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs

            // Nivel 2: SQLite fresco
            if (isFresh && cachedJson != null) {
                Log.d(TAG, "Availabilities: L2 hit (fresh)")
                val type  = object : TypeToken<List<AvailabilityItem>>() {}.type
                val items = gson.fromJson<List<AvailabilityItem>>(cachedJson, type)
                memoryCache.put(cacheKey, items)
                _listState.value = AvailabilityListState.Success(items)
                return@launch
            }

            // Nivel 3: Red
            when (val result = repository.getAvailabilities(tutorId)) {
                is Result.Success -> {
                    val type = object : TypeToken<List<AvailabilityItem>>() {}.type
                    cacheDb.saveCache(cacheKey, gson.toJson(result.data, type))
                    memoryCache.put(cacheKey, result.data)
                    _listState.value = AvailabilityListState.Success(result.data)
                }
                is Result.Error -> {
                    Log.e(TAG, "Network error: ${result.message}")
                    if (cachedJson != null) {
                        val type  = object : TypeToken<List<AvailabilityItem>>() {}.type
                        val items = gson.fromJson<List<AvailabilityItem>>(cachedJson, type)
                        memoryCache.put(cacheKey, items)
                        _listState.value = AvailabilityListState.Success(items)
                    } else {
                        _listState.value = AvailabilityListState.Error("No hay datos disponibles")
                    }
                }
                is Result.Loading -> _listState.value = AvailabilityListState.Loading
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Crear disponibilidad: offline-first (single)
    // ─────────────────────────────────────────────────────────────────────────

    fun create(request: CreateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading

            val pendingId = cacheDb.savePending(
                gson.toJson(request),
                CacheDatabase.ACTION_CREATE
            )
            fileManager.appendLog("CREATE queued id=$pendingId tutorId=$tutorId")
            refreshPendingCount()

            if (!isConnected()) {
                fileManager.appendLog("Offline — CREATE id=$pendingId kept in queue")
                _actionState.value = AvailabilityActionState.OfflineSaved(
                    "No internet connection. This will be saved later."
                )
                return@launch
            }

            when (val result = repository.createAvailability(request)) {
                is Result.Success -> {
                    cacheDb.deletePending(pendingId)
                    refreshPendingCount()
                    invalidateAndReload()
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Error -> {
                    Log.w(TAG, "Network failed for CREATE id=$pendingId: ${result.message}")
                    fileManager.appendLog("Network failed CREATE id=$pendingId: ${result.message}")
                    _actionState.value = AvailabilityActionState.OfflineSaved(
                        "No internet connection. This will be saved later."
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actualizar disponibilidad: offline-first
    // ─────────────────────────────────────────────────────────────────────────

    fun update(id: String, request: UpdateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading

            val pendingId = cacheDb.savePending(
                gson.toJson(request),
                CacheDatabase.ACTION_UPDATE,
                id
            )
            fileManager.appendLog("UPDATE queued id=$pendingId availabilityId=$id")
            refreshPendingCount()

            if (!isConnected()) {
                fileManager.appendLog("Offline — UPDATE id=$pendingId kept in queue")
                _actionState.value = AvailabilityActionState.OfflineSaved(
                    "No internet connection. Changes will be saved later."
                )
                return@launch
            }

            when (val result = repository.updateAvailability(id, request)) {
                is Result.Success -> {
                    cacheDb.deletePending(pendingId)
                    refreshPendingCount()
                    invalidateAndReload()
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Error -> {
                    Log.w(TAG, "Network failed for UPDATE id=$pendingId: ${result.message}")
                    fileManager.appendLog("Network failed UPDATE id=$pendingId: ${result.message}")
                    _actionState.value = AvailabilityActionState.OfflineSaved(
                        "No internet connection. Changes will be saved later."
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Eliminar disponibilidades del tutor: offline-first
    // ─────────────────────────────────────────────────────────────────────────

    fun deleteByTutor() {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading

            val pendingId = cacheDb.savePending(
                "{}",
                CacheDatabase.ACTION_DELETE,
                tutorId
            )
            fileManager.appendLog("DELETE queued id=$pendingId tutorId=$tutorId")
            refreshPendingCount()

            if (!isConnected()) {
                fileManager.appendLog("Offline — DELETE id=$pendingId kept in queue")
                _actionState.value = AvailabilityActionState.OfflineSaved(
                    "No internet connection. This will be deleted later."
                )
                return@launch
            }

            when (val result = repository.deleteAvailabilitiesByTutor(tutorId)) {
                is Result.Success -> {
                    cacheDb.deletePending(pendingId)
                    refreshPendingCount()
                    invalidateAndReload()
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Error -> {
                    Log.w(TAG, "Network failed for DELETE id=$pendingId: ${result.message}")
                    fileManager.appendLog("Network failed DELETE id=$pendingId: ${result.message}")
                    _actionState.value = AvailabilityActionState.OfflineSaved(
                        "No internet connection. This will be deleted later."
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Crear múltiples disponibilidades (repetición): offline-first
    // ─────────────────────────────────────────────────────────────────────────

    fun createBatch(requests: List<CreateAvailabilityRequest>) {
        if (requests.isEmpty()) return
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading

            // Queue all items first, before any network attempt
            val pendingIds = requests.map { req ->
                cacheDb.savePending(gson.toJson(req), CacheDatabase.ACTION_CREATE).also {
                    fileManager.appendLog("Batch CREATE queued id=$it tutorId=$tutorId")
                }
            }
            refreshPendingCount()

            if (!isConnected()) {
                fileManager.appendLog("Offline — batch of ${requests.size} CREATE(s) kept in queue")
                _actionState.value = AvailabilityActionState.OfflineSaved(
                    "No internet connection. This will be saved later."
                )
                return@launch
            }

            var anyFailed = false
            for ((index, request) in requests.withIndex()) {
                when (val result = repository.createAvailability(request)) {
                    is Result.Success -> {
                        cacheDb.deletePending(pendingIds[index])
                        refreshPendingCount()
                    }
                    is Result.Error -> {
                        anyFailed = true
                        Log.w(TAG, "Batch network failed id=${pendingIds[index]}: ${result.message}")
                        fileManager.appendLog("Batch network failed id=${pendingIds[index]}: ${result.message}")
                    }
                    else -> {}
                }
            }

            invalidateAndReload()

            _actionState.value = if (anyFailed) {
                AvailabilityActionState.OfflineSaved(
                    "No internet connection. This will be saved later."
                )
            } else {
                AvailabilityActionState.Done
            }
        }
    }

    fun resetActionState() {
        _actionState.value = AvailabilityActionState.Idle
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers internos
    // ─────────────────────────────────────────────────────────────────────────

    private fun refreshPendingCount() {
        viewModelScope.launch {
            _pendingCount.value = cacheDb.getPendingCount()
        }
    }

    private suspend fun invalidateAndReload() {
        val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
        // Overwrite L1 slot to force next load() to fetch fresh data from network
        memoryCache.put(cacheKey, emptyList<AvailabilityItem>())
        when (val sync = repository.getAvailabilities(tutorId)) {
            is Result.Success -> {
                val type = object : TypeToken<List<AvailabilityItem>>() {}.type
                cacheDb.saveCache(cacheKey, gson.toJson(sync.data, type))
                memoryCache.put(cacheKey, sync.data)
                _listState.value = AvailabilityListState.Success(sync.data)
            }
            else -> {}
        }
    }
}

class AvailabilityViewModelFactory(
    private val context: Context,
    private val tutorId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AvailabilityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AvailabilityViewModel(
                repository = ServiceLocator.availabilityRepository(context),
                tutorId    = tutorId,
                context    = context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
