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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "AvailabilityVM"
private const val ACTION_SYNC_TIMEOUT_MS = 6_000L
private const val SYNC_DONE_BANNER_MS = 4_000L

sealed class AvailabilityListState {
    object Idle    : AvailabilityListState()
    object Loading : AvailabilityListState()
    data class Success(val items: List<AvailabilityItem>) : AvailabilityListState()
    data class Error(val message: String) : AvailabilityListState()
}

sealed class AvailabilityActionState {
    object Idle    : AvailabilityActionState()
    object Loading : AvailabilityActionState()
    data class Done(val message: String? = null) : AvailabilityActionState()
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
    private var syncJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            refreshPendingAndBanner()
            if (isConnected()) triggerPendingSync(showDoneBanner = false)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Connectivity helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun isConnected(): Boolean {
        val cm    = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net   = cm.activeNetwork ?: return false
        val caps  = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun onScreenVisible() {
        viewModelScope.launch {
            refreshPendingAndBanner()
            if (isConnected()) triggerPendingSync(showDoneBanner = false)
            load()
        }
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
                if (isConnected()) triggerPendingSync(showDoneBanner = true)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    triggerPendingSync(showDoneBanner = true)
                }
            }

            override fun onLost(network: Network) {
                viewModelScope.launch {
                    refreshPendingAndBanner()
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

    private fun triggerPendingSync(showDoneBanner: Boolean) {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            Log.d(TAG, "Connectivity available — syncing pending actions")
            syncPendingActions(showDoneBanner)
        }
    }

    private suspend fun syncPendingActions(showDoneBanner: Boolean) {
        if (!isConnected()) {
            refreshPendingAndBanner()
            return
        }

        val pending = cacheDb.getAllPending()
        if (pending.isEmpty()) {
            _bannerState.value = if (isConnected()) OfflineBannerState.Hidden else OfflineBannerState.PendingSync(0)
            return
        }

        var syncedCount = 0
        for (item in pending) {
            try {
                val success = withTimeoutOrNull(ACTION_SYNC_TIMEOUT_MS) {
                    when (item.actionType) {
                        CacheDatabase.ACTION_CREATE -> {
                            val req = gson.fromJson(item.json, CreateAvailabilityRequest::class.java)
                            repository.createAvailability(req) is Result.Success
                        }
                        CacheDatabase.ACTION_UPDATE -> {
                            val id = item.availabilityId
                            if (id == null) {
                                cacheDb.deletePending(item.id); syncedCount++; return@withTimeoutOrNull true
                            }
                            val req = gson.fromJson(item.json, UpdateAvailabilityRequest::class.java)
                            repository.updateAvailability(id, req) is Result.Success
                        }
                        CacheDatabase.ACTION_DELETE -> {
                            val availabilityId = item.availabilityId
                            if (availabilityId == null) {
                                cacheDb.deletePending(item.id); syncedCount++; return@withTimeoutOrNull true
                            }
                            repository.deleteAvailability(availabilityId) is Result.Success
                        }
                        else -> { cacheDb.deletePending(item.id); syncedCount++; true }
                    }
                } ?: false

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
            _bannerState.value = if (showDoneBanner) OfflineBannerState.SyncDone(syncedCount)
                                 else OfflineBannerState.Hidden
            fileManager.appendLog("Synced $syncedCount action(s) on reconnect")
            if (showDoneBanner) {
                viewModelScope.launch {
                    delay(SYNC_DONE_BANNER_MS)
                    if (_bannerState.value is OfflineBannerState.SyncDone) {
                        _bannerState.value = OfflineBannerState.Hidden
                    }
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
            val connectedAtAction = isConnected()

            val pendingId = cacheDb.savePending(gson.toJson(request), CacheDatabase.ACTION_CREATE)
            fileManager.appendLog("CREATE queued id=$pendingId tutorId=$tutorId")

            applyLocalCreate(request)

            refreshPendingAndBanner()
            val pendingAfter = _pendingCount.value
            _actionState.value = AvailabilityActionState.Done(
                if (!connectedAtAction || pendingAfter > 0) "No internet connection. This will be saved later." else null
            )
            if (connectedAtAction) triggerPendingSync(showDoneBanner = false)
        }
    }

    fun update(id: String, request: UpdateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            val connectedAtAction = isConnected()

            val pendingId = cacheDb.savePending(gson.toJson(request), CacheDatabase.ACTION_UPDATE, id)
            fileManager.appendLog("UPDATE queued id=$pendingId availabilityId=$id")

            applyLocalUpdate(id, request)

            refreshPendingAndBanner()
            val pendingAfter = _pendingCount.value
            _actionState.value = AvailabilityActionState.Done(
                if (!connectedAtAction || pendingAfter > 0) "No internet connection. Changes will be applied when connection is restored." else null
            )
            if (connectedAtAction) triggerPendingSync(showDoneBanner = false)
        }
    }

    fun deleteAvailability(availabilityId: String) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            val connectedAtAction = isConnected()

            val pendingId = cacheDb.savePending("{}", CacheDatabase.ACTION_DELETE, availabilityId)
            fileManager.appendLog("DELETE queued id=$pendingId availabilityId=$availabilityId")

            applyLocalDelete(availabilityId)

            refreshPendingAndBanner()
            val pendingAfter = _pendingCount.value
            _actionState.value = AvailabilityActionState.Done(
                if (!connectedAtAction || pendingAfter > 0) "No internet connection. This will be deleted later." else null
            )
            if (connectedAtAction) triggerPendingSync(showDoneBanner = false)
        }
    }

    fun createBatch(requests: List<CreateAvailabilityRequest>) {
        if (requests.isEmpty()) return
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            val connectedAtAction = isConnected()

            requests.forEach { req ->
                cacheDb.savePending(gson.toJson(req), CacheDatabase.ACTION_CREATE)
                applyLocalCreate(req)
            }

            refreshPendingAndBanner()
            val pendingAfter = _pendingCount.value
            _actionState.value = AvailabilityActionState.Done(
                if (!connectedAtAction || pendingAfter > 0) "No internet connection. This will be saved later." else null
            )
            if (connectedAtAction) triggerPendingSync(showDoneBanner = false)
        }
    }

    fun resetActionState() {
        _actionState.value = AvailabilityActionState.Idle
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun refreshPendingAndBanner() {
        val count = cacheDb.getPendingCount()
        _pendingCount.value = count
        _bannerState.value = if (!isConnected()) {
            OfflineBannerState.PendingSync(count)
        } else {
            OfflineBannerState.Hidden
        }
    }

    private suspend fun applyLocalCreate(request: CreateAvailabilityRequest) {
        val current = (_listState.value as? AvailabilityListState.Success)?.items ?: emptyList()
        val optimistic = AvailabilityItem(
            id = "local-${System.currentTimeMillis()}",
            title = request.title,
            date = request.date,
            startTime = request.startTime,
            endTime = request.endTime,
            location = request.location,
            description = request.description,
            course = request.course
        )
        val updated = (current + optimistic)
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
        updateLocalAvailabilityCache(updated)
    }

    private suspend fun applyLocalUpdate(id: String, request: UpdateAvailabilityRequest) {
        val current = (_listState.value as? AvailabilityListState.Success)?.items ?: return
        val updated = current.map { item ->
            if (item.id == id) {
                item.copy(
                    title = request.title ?: item.title,
                    date = request.date ?: item.date,
                    startTime = request.startTime ?: item.startTime,
                    endTime = request.endTime ?: item.endTime,
                    location = request.location ?: item.location,
                    description = request.description ?: item.description,
                    course = request.course ?: item.course
                )
            } else item
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
        updateLocalAvailabilityCache(updated)
    }

    private suspend fun applyLocalDelete(availabilityId: String) {
        val current = (_listState.value as? AvailabilityListState.Success)?.items ?: emptyList()
        val updated = current.filterNot { it.id == availabilityId }
        updateLocalAvailabilityCache(updated)
    }

    private suspend fun updateLocalAvailabilityCache(items: List<AvailabilityItem>) {
        val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
        val type = object : TypeToken<List<AvailabilityItem>>() {}.type
        cacheDb.saveCache(cacheKey, gson.toJson(items, type))
        memoryCache.put(cacheKey, items)
        _listState.value = AvailabilityListState.Success(items)
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
