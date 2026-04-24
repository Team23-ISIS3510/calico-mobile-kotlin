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
import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.domain.repository.AvailabilityRepository
import com.calico.tutor.domain.utils.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
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
}

/**
 * Drives the offline banner shown in AvailabilityScreen.
 *
 * - Hidden:      no pending actions / fully synced
 * - PendingSync: no network, N actions queued
 * - SyncDone:    just reconnected and synced N actions (auto-hides after 4 s)
 */
sealed class OfflineBannerState {
    object Hidden : OfflineBannerState()
    data class PendingSync(val count: Int) : OfflineBannerState()
    data class SyncDone(val syncedCount: Int) : OfflineBannerState()
}

/**
 * ViewModel para AvailabilityScreen.
 *
 * EVENTUAL CONNECTIVITY — offline-first para todas las mutaciones:
 * 1. Guardar en pending_availabilities (SQLite) SIEMPRE antes de llamar a la red.
 * 2. Si no hay conexión → emitir Done + actualizar banner PendingSync (sin bloquear UI).
 * 3. Si hay conexión → intentar red; en éxito eliminar de cola; en fallo mantener en cola.
 * 4. Al reconectar → syncPendingActions() sincroniza todo inmediatamente.
 * 5. WorkManager reintenta periódicamente como respaldo.
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

    private val _bannerState = MutableStateFlow<OfflineBannerState>(OfflineBannerState.Hidden)
    val bannerState: StateFlow<OfflineBannerState> = _bannerState.asStateFlow()

    // kept for badge count used elsewhere (worker, etc.)
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val cacheDb     = ServiceLocator.cacheDatabase(context)
    private val userPrefs   = ServiceLocator.userPreferences(context)
    private val memoryCache = ServiceLocator.inMemoryCache()
    private val fileManager = ServiceLocator.fileManager(context)
    private val gson        = Gson()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        load()
        viewModelScope.launch {
            val count = cacheDb.getPendingCount()
            _pendingCount.value = count
            if (count > 0 && !isConnected()) {
                _bannerState.value = OfflineBannerState.PendingSync(count)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Connectivity helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun isConnected(): Boolean {
        val cm    = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net   = cm.activeNetwork ?: return false
        val caps  = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Registers a ConnectivityManager callback.
     * - onAvailable → immediately sync all pending actions
     * - onLost      → show PendingSync banner if there are queued items
     * Call once from the composable's LaunchedEffect(Unit).
     */
    fun startConnectivityMonitoring() {
        if (networkCallback != null) return
        val cm  = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModelScope.launch {
                    Log.d(TAG, "Connectivity restored — syncing pending actions")
                    syncPendingActions()
                }
            }
            override fun onLost(network: Network) {
                viewModelScope.launch {
                    val count = cacheDb.getPendingCount()
                    if (count > 0) {
                        _bannerState.value = OfflineBannerState.PendingSync(count)
                        Log.d(TAG, "Connectivity lost — $count action(s) pending")
                    }
                }
            }
        }
        cm.registerNetworkCallback(req, networkCallback!!)
        Log.d(TAG, "Connectivity monitoring started")
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
    // Immediate sync on reconnect
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun syncPendingActions() {
        val pending = cacheDb.getAllPending()
        if (pending.isEmpty()) {
            _bannerState.value = OfflineBannerState.Hidden
            return
        }

        var syncedCount = 0
        for (item in pending) {
            try {
                val success = when (item.actionType) {
                    CacheDatabase.ACTION_CREATE -> {
                        val req = gson.fromJson(item.json, CreateAvailabilityRequest::class.java)
                        repository.createAvailability(req) is Result.Success
                    }
                    CacheDatabase.ACTION_UPDATE -> {
                        val id = item.availabilityId
                        if (id == null) {
                            cacheDb.deletePending(item.id); syncedCount++; continue
                        }
                        val req = gson.fromJson(item.json, UpdateAvailabilityRequest::class.java)
                        repository.updateAvailability(id, req) is Result.Success
                    }
                    CacheDatabase.ACTION_DELETE -> {
                        val tid = item.availabilityId
                        if (tid == null) {
                            cacheDb.deletePending(item.id); syncedCount++; continue
                        }
                        repository.deleteAvailabilitiesByTutor(tid) is Result.Success
                    }
                    else -> { cacheDb.deletePending(item.id); syncedCount++; continue }
                }
                if (success) {
                    cacheDb.deletePending(item.id)
                    syncedCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync exception id=${item.id}: ${e.message}")
            }
        }

        _pendingCount.value = cacheDb.getPendingCount()

        if (syncedCount > 0) {
            invalidateAndReload()
            _bannerState.value = OfflineBannerState.SyncDone(syncedCount)
            fileManager.appendLog("Synced $syncedCount action(s) on reconnect")
            viewModelScope.launch {
                delay(4_000)
                if (_bannerState.value is OfflineBannerState.SyncDone) {
                    _bannerState.value = OfflineBannerState.Hidden
                }
            }
        } else {
            val remaining = _pendingCount.value
            _bannerState.value = if (remaining > 0) OfflineBannerState.PendingSync(remaining)
                                 else OfflineBannerState.Hidden
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vista 5: Ver availabilities con caché + offline fallback
    // ─────────────────────────────────────────────────────────────────────────

    fun load() {
        viewModelScope.launch {
            _listState.value = AvailabilityListState.Loading
            val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
            val expiryMs = userPrefs.cacheExpiryMs.first()

            @Suppress("UNCHECKED_CAST")
            memoryCache.get(cacheKey)?.let { entry ->
                Log.d(TAG, "Availabilities: L1 hit")
                _listState.value = AvailabilityListState.Success(entry.value as List<AvailabilityItem>)
                return@launch
            }

            val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
            val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs

            if (isFresh && cachedJson != null) {
                Log.d(TAG, "Availabilities: L2 hit (fresh)")
                val type  = object : TypeToken<List<AvailabilityItem>>() {}.type
                val items = gson.fromJson<List<AvailabilityItem>>(cachedJson, type)
                memoryCache.put(cacheKey, items)
                _listState.value = AvailabilityListState.Success(items)
                return@launch
            }

            when (val result = repository.getAvailabilities(tutorId)) {
                is Result.Success -> {
                    val type = object : TypeToken<List<AvailabilityItem>>() {}.type
                    cacheDb.saveCache(cacheKey, gson.toJson(result.data, type))
                    memoryCache.put(cacheKey, result.data)
                    _listState.value = AvailabilityListState.Success(result.data)
                }
                is Result.Error -> {
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
    // Mutaciones: offline-first
    // ─────────────────────────────────────────────────────────────────────────

    fun create(request: CreateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading

            val pendingId = cacheDb.savePending(gson.toJson(request), CacheDatabase.ACTION_CREATE)
            fileManager.appendLog("CREATE queued id=$pendingId tutorId=$tutorId")

            if (!isConnected()) {
                val count = cacheDb.getPendingCount()
                _pendingCount.value = count
                _bannerState.value  = OfflineBannerState.PendingSync(count)
                _actionState.value  = AvailabilityActionState.Done
                return@launch
            }

            when (val result = repository.createAvailability(request)) {
                is Result.Success -> {
                    cacheDb.deletePending(pendingId)
                    refreshPendingCount()
                    invalidateAndReload()
                    updateBannerAfterOnlineAction()
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Error -> {
                    Log.w(TAG, "Network failed CREATE id=$pendingId: ${result.message}")
                    val count = cacheDb.getPendingCount()
                    _pendingCount.value = count
                    _bannerState.value  = OfflineBannerState.PendingSync(count)
                    _actionState.value  = AvailabilityActionState.Done
                }
                is Result.Loading -> {}
            }
        }
    }

    fun update(id: String, request: UpdateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading

            val pendingId = cacheDb.savePending(gson.toJson(request), CacheDatabase.ACTION_UPDATE, id)
            fileManager.appendLog("UPDATE queued id=$pendingId availabilityId=$id")

            if (!isConnected()) {
                val count = cacheDb.getPendingCount()
                _pendingCount.value = count
                _bannerState.value  = OfflineBannerState.PendingSync(count)
                _actionState.value  = AvailabilityActionState.Done
                return@launch
            }

            when (val result = repository.updateAvailability(id, request)) {
                is Result.Success -> {
                    cacheDb.deletePending(pendingId)
                    refreshPendingCount()
                    invalidateAndReload()
                    updateBannerAfterOnlineAction()
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Error -> {
                    Log.w(TAG, "Network failed UPDATE id=$pendingId: ${result.message}")
                    val count = cacheDb.getPendingCount()
                    _pendingCount.value = count
                    _bannerState.value  = OfflineBannerState.PendingSync(count)
                    _actionState.value  = AvailabilityActionState.Done
                }
                is Result.Loading -> {}
            }
        }
    }

    fun deleteByTutor() {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading

            val pendingId = cacheDb.savePending("{}", CacheDatabase.ACTION_DELETE, tutorId)
            fileManager.appendLog("DELETE queued id=$pendingId tutorId=$tutorId")

            if (!isConnected()) {
                val count = cacheDb.getPendingCount()
                _pendingCount.value = count
                _bannerState.value  = OfflineBannerState.PendingSync(count)
                _actionState.value  = AvailabilityActionState.Done
                return@launch
            }

            when (val result = repository.deleteAvailabilitiesByTutor(tutorId)) {
                is Result.Success -> {
                    cacheDb.deletePending(pendingId)
                    refreshPendingCount()
                    invalidateAndReload()
                    updateBannerAfterOnlineAction()
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Error -> {
                    Log.w(TAG, "Network failed DELETE id=$pendingId: ${result.message}")
                    val count = cacheDb.getPendingCount()
                    _pendingCount.value = count
                    _bannerState.value  = OfflineBannerState.PendingSync(count)
                    _actionState.value  = AvailabilityActionState.Done
                }
                is Result.Loading -> {}
            }
        }
    }

    fun createBatch(requests: List<CreateAvailabilityRequest>) {
        if (requests.isEmpty()) return
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading

            val pendingIds = requests.map { req ->
                cacheDb.savePending(gson.toJson(req), CacheDatabase.ACTION_CREATE)
            }

            if (!isConnected()) {
                val count = cacheDb.getPendingCount()
                _pendingCount.value = count
                _bannerState.value  = OfflineBannerState.PendingSync(count)
                _actionState.value  = AvailabilityActionState.Done
                fileManager.appendLog("Offline — batch of ${requests.size} CREATE(s) queued")
                return@launch
            }

            var anyFailed = false
            for ((index, request) in requests.withIndex()) {
                when (val result = repository.createAvailability(request)) {
                    is Result.Success -> {
                        cacheDb.deletePending(pendingIds[index])
                    }
                    is Result.Error -> {
                        anyFailed = true
                        Log.w(TAG, "Batch network failed id=${pendingIds[index]}: ${result.message}")
                    }
                    else -> {}
                }
            }

            refreshPendingCount()
            invalidateAndReload()

            if (anyFailed) {
                val count = _pendingCount.value
                _bannerState.value = OfflineBannerState.PendingSync(count)
            } else {
                updateBannerAfterOnlineAction()
            }
            _actionState.value = AvailabilityActionState.Done
        }
    }

    fun resetActionState() {
        _actionState.value = AvailabilityActionState.Idle
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun refreshPendingCount() {
        viewModelScope.launch {
            _pendingCount.value = cacheDb.getPendingCount()
        }
    }

    private suspend fun updateBannerAfterOnlineAction() {
        val count = cacheDb.getPendingCount()
        _pendingCount.value = count
        _bannerState.value = if (count > 0) OfflineBannerState.PendingSync(count)
                             else OfflineBannerState.Hidden
    }

    private suspend fun invalidateAndReload() {
        val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
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
