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
private const val SYNC_DONE_BANNER_MS    = 4_000L
private const val GLOBAL_OFFLINE_MESSAGE = "No internet connection. Changes will be synchronized later."
private const val CREATE_OFFLINE_MESSAGE = "No internet connection. This will be saved later."
private const val UPDATE_OFFLINE_MESSAGE = "No internet connection. Changes will be applied when connection is restored."
private const val DELETE_OFFLINE_MESSAGE = "No internet connection. This will be deleted later."

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
 * - Hidden:          no pending actions / fully synced
 * - Offline(message): offline or no connectivity; shows operation-specific context
 * - SyncDone:        reconnected and synced; auto-hides after 4 s
 */
sealed class OfflineBannerState {
    object Hidden : OfflineBannerState()
    data class Offline(val message: String) : OfflineBannerState()
    object SyncDone : OfflineBannerState()
}

/**
 * ViewModel para AvailabilityScreen.
 *
 * EVENTUAL CONNECTIVITY — offline-first para todas las mutaciones:
 * 1. Aplica cambios locales (cache + lista) SIEMPRE de forma inmediata.
 * 2. Guarda en pending_availabilities (SQLite) antes de llamar a la red.
 * 3. Si no hay conexión → emitir Done + mostrar banner Offline específico.
 * 4. Si hay conexión → fire-and-forget; Done se emite antes de la llamada de red.
 * 5. Al reconectar → syncPendingActions() sincroniza todo con banner SyncDone.
 * 6. WorkManager reintenta periódicamente como respaldo.
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

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val cacheDb     = ServiceLocator.cacheDatabase(context)
    private val userPrefs   = ServiceLocator.userPreferences(context)
    private val memoryCache = ServiceLocator.inMemoryCache()
    private val fileManager = ServiceLocator.fileManager(context)
    private val gson        = Gson()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var syncJob: Job? = null
    private var pendingActionMessage: String? = null

    init {
        load()
        viewModelScope.launch {
            val count = cacheDb.getPendingCount()
            _pendingCount.value = count
            when {
                count > 0 && isConnected() -> triggerPendingSync(showDoneBanner = false)
                !isConnected() -> _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Connectivity
    // ─────────────────────────────────────────────────────────────────────────

    private fun isConnected(): Boolean {
        val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net  = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Refreshes data and syncs pending actions. Call from ON_RESUME lifecycle event.
     */
    fun onScreenVisible() {
        viewModelScope.launch {
            val count = cacheDb.getPendingCount()
            _pendingCount.value = count
            if (isConnected()) {
                if (count > 0) triggerPendingSync(showDoneBanner = false)
                load()
            } else {
                _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
                load()
            }
        }
    }

    /**
     * Registers ConnectivityManager callback. Call once from the composable.
     * Guarded against re-registration.
     */
    fun startConnectivityMonitoring() {
        if (networkCallback != null) return
        val cm  = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isConnected()) {
                    _bannerState.value = OfflineBannerState.Hidden
                    triggerPendingSync(showDoneBanner = true)
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    _bannerState.value = OfflineBannerState.Hidden
                    triggerPendingSync(showDoneBanner = true)
                }
            }

            override fun onLost(network: Network) {
                viewModelScope.launch {
                    _pendingCount.value = cacheDb.getPendingCount()
                    _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
                    Log.d(TAG, "Connectivity lost — ${_pendingCount.value} action(s) pending")
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
    // Sync on reconnect
    // ─────────────────────────────────────────────────────────────────────────

    private fun triggerPendingSync(showDoneBanner: Boolean) {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            Log.d(TAG, "Triggering pending sync (showDone=$showDoneBanner)")
            syncPendingActions(showDoneBanner)
        }
    }

    private suspend fun syncPendingActions(showDoneBanner: Boolean) {
        if (!isConnected()) {
            val count = cacheDb.getPendingCount()
            _pendingCount.value = count
            _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
            return
        }

        val pending = cacheDb.getAllPending()
        if (pending.isEmpty()) {
            _bannerState.value = OfflineBannerState.Hidden
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
                            val id = item.availabilityId
                            if (id == null) {
                                cacheDb.deletePending(item.id); syncedCount++; return@withTimeoutOrNull true
                            }
                            repository.deleteAvailability(id) is Result.Success
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
            if (showDoneBanner) {
                _bannerState.value = OfflineBannerState.SyncDone
                fileManager.appendLog("Synced $syncedCount action(s) on reconnect")
                viewModelScope.launch {
                    delay(SYNC_DONE_BANNER_MS)
                    if (_bannerState.value is OfflineBannerState.SyncDone) {
                        _bannerState.value = OfflineBannerState.Hidden
                    }
                }
            } else {
                _bannerState.value = OfflineBannerState.Hidden
            }
        } else {
            _bannerState.value = OfflineBannerState.Hidden
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load with 2-level cache
    // ─────────────────────────────────────────────────────────────────────────

    fun load() {
        viewModelScope.launch {
            _listState.value = AvailabilityListState.Loading
            val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
            val expiryMs = userPrefs.cacheExpiryMs.first()

            @Suppress("UNCHECKED_CAST")
            memoryCache.get(cacheKey)?.let { entry ->
                Log.d(TAG, "Availabilities: L1 hit")
                val cachedItems = entry.value as List<AvailabilityItem>
                _listState.value = AvailabilityListState.Success(applyPendingOverlay(cachedItems))
                return@launch
            }

            val (cachedJson, cachedTs) = cacheDb.getCache(cacheKey)
            val isFresh = cachedJson != null && (System.currentTimeMillis() - cachedTs) < expiryMs

            if (isFresh) {
                Log.d(TAG, "Availabilities: L2 hit (fresh)")
                val type  = object : TypeToken<List<AvailabilityItem>>() {}.type
                val items = gson.fromJson<List<AvailabilityItem>>(cachedJson, type)
                val mergedItems = applyPendingOverlay(items)
                memoryCache.put(cacheKey, mergedItems)
                _listState.value = AvailabilityListState.Success(mergedItems)
                return@launch
            }

            when (val result = repository.getAvailabilities(tutorId)) {
                is Result.Success -> {
                    val type = object : TypeToken<List<AvailabilityItem>>() {}.type
                    cacheDb.saveCache(cacheKey, gson.toJson(result.data, type))
                    val mergedItems = applyPendingOverlay(result.data)
                    memoryCache.put(cacheKey, mergedItems)
                    _listState.value = AvailabilityListState.Success(mergedItems)
                }
                is Result.Error -> {
                    if (cachedJson != null) {
                        val type  = object : TypeToken<List<AvailabilityItem>>() {}.type
                        val items = gson.fromJson<List<AvailabilityItem>>(cachedJson, type)
                        val mergedItems = applyPendingOverlay(items)
                        memoryCache.put(cacheKey, mergedItems)
                        _listState.value = AvailabilityListState.Success(mergedItems)
                    } else {
                        _listState.value = AvailabilityListState.Error("No hay datos disponibles")
                    }
                }
                is Result.Loading -> _listState.value = AvailabilityListState.Loading
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mutations: offline-first, fire-and-forget for online path
    // ─────────────────────────────────────────────────────────────────────────

    fun create(request: CreateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            val online = isConnected()

            // Prevent overlapping availabilities on the frontend
            val current = (_listState.value as? AvailabilityListState.Success)?.items ?: emptyList()
            val hasOverlap = current.any { existing ->
                existing.date == request.date && timeOverlap(existing.startTime, existing.endTime, request.startTime, request.endTime)
            }
            if (hasOverlap) {
                _actionState.value = AvailabilityActionState.Error("Ya existe una disponibilidad en ese horario")
                return@launch
            }

            val pendingId = cacheDb.savePending(gson.toJson(request), CacheDatabase.ACTION_CREATE)
            fileManager.appendLog("CREATE queued id=$pendingId tutorId=$tutorId")
            applyLocalCreate(request)

            _pendingCount.value = cacheDb.getPendingCount()
            if (!online) {
                pendingActionMessage = CREATE_OFFLINE_MESSAGE
                _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
            }
            _actionState.value = AvailabilityActionState.Done

            if (online) {
                when (val result = repository.createAvailability(request)) {
                    is Result.Success -> {
                        cacheDb.deletePending(pendingId)
                        _pendingCount.value = cacheDb.getPendingCount()
                        invalidateAndReload()
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Network failed CREATE id=$pendingId: ${result.message}")
                        _pendingCount.value = cacheDb.getPendingCount()
                        pendingActionMessage = CREATE_OFFLINE_MESSAGE
                        _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    private fun timeOverlap(startA: String, endA: String, startB: String, endB: String): Boolean {
        // Assumes format HH:mm (24h). Convert to minutes since midnight.
        fun toMinutes(t: String): Int {
            return try {
                val parts = t.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                h * 60 + m
            } catch (e: Exception) { 0 }
        }

        val aStart = toMinutes(startA)
        val aEnd = toMinutes(endA)
        val bStart = toMinutes(startB)
        val bEnd = toMinutes(endB)

        if (aEnd <= aStart || bEnd <= bStart) return false
        return aStart < bEnd && bStart < aEnd
    }

    fun update(id: String, request: UpdateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            val online = isConnected()

            val pendingId = cacheDb.savePending(gson.toJson(request), CacheDatabase.ACTION_UPDATE, id)
            fileManager.appendLog("UPDATE queued id=$pendingId availabilityId=$id")
            applyLocalUpdate(id, request)

            _pendingCount.value = cacheDb.getPendingCount()
            if (!online) {
                pendingActionMessage = UPDATE_OFFLINE_MESSAGE
                _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
            }
            _actionState.value = AvailabilityActionState.Done

            if (online) {
                when (val result = repository.updateAvailability(id, request)) {
                    is Result.Success -> {
                        cacheDb.deletePending(pendingId)
                        _pendingCount.value = cacheDb.getPendingCount()
                        invalidateAndReload()
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Network failed UPDATE id=$pendingId: ${result.message}")
                        _pendingCount.value = cacheDb.getPendingCount()
                        pendingActionMessage = UPDATE_OFFLINE_MESSAGE
                        _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    /** Deletes a single availability. Optimistic: removes item immediately from the list. */
    fun deleteAvailability(availabilityId: String) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            val online = isConnected()

            applyLocalDelete(availabilityId)
            val pendingId = cacheDb.savePending("{}", CacheDatabase.ACTION_DELETE, availabilityId)
            fileManager.appendLog("DELETE queued id=$pendingId availabilityId=$availabilityId")

            _pendingCount.value = cacheDb.getPendingCount()
            if (!online) {
                pendingActionMessage = DELETE_OFFLINE_MESSAGE
                _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
                _actionState.value = AvailabilityActionState.Done
                return@launch
            }

            when (val result = repository.deleteAvailability(availabilityId)) {
                is Result.Success -> {
                    cacheDb.deletePending(pendingId)
                    _pendingCount.value = cacheDb.getPendingCount()
                    invalidateAndReload()
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Error -> {
                    Log.w(TAG, "Network failed DELETE id=$pendingId: ${result.message}")
                    _pendingCount.value = cacheDb.getPendingCount()
                    pendingActionMessage = DELETE_OFFLINE_MESSAGE
                    _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Loading -> _actionState.value = AvailabilityActionState.Done
            }
        }
    }

    fun createBatch(requests: List<CreateAvailabilityRequest>) {
        if (requests.isEmpty()) return
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            val online = isConnected()

            val pendingIds = requests.map { req ->
                val id = cacheDb.savePending(gson.toJson(req), CacheDatabase.ACTION_CREATE)
                applyLocalCreate(req)
                id
            }

            _pendingCount.value = cacheDb.getPendingCount()
            if (!online) {
                pendingActionMessage = CREATE_OFFLINE_MESSAGE
                _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
                fileManager.appendLog("Offline — batch of ${requests.size} CREATE(s) queued")
            }
            _actionState.value = AvailabilityActionState.Done

            if (online) {
                var anyFailed = false
                for ((index, request) in requests.withIndex()) {
                    when (val result = repository.createAvailability(request)) {
                        is Result.Success -> cacheDb.deletePending(pendingIds[index])
                        is Result.Error -> {
                            anyFailed = true
                            Log.w(TAG, "Batch network failed id=${pendingIds[index]}: ${result.message}")
                        }
                        else -> {}
                    }
                }
                _pendingCount.value = cacheDb.getPendingCount()
                invalidateAndReload()
                if (anyFailed) {
                    pendingActionMessage = CREATE_OFFLINE_MESSAGE
                    _bannerState.value = OfflineBannerState.Offline(GLOBAL_OFFLINE_MESSAGE)
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = AvailabilityActionState.Idle
    }

    fun consumePendingActionMessage(): String? {
        val message = pendingActionMessage
        pendingActionMessage = null
        return message
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Optimistic local cache helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun applyLocalCreate(request: CreateAvailabilityRequest) {
        val current = (_listState.value as? AvailabilityListState.Success)?.items ?: emptyList()
        val optimistic = AvailabilityItem(
            id          = "local-${System.currentTimeMillis()}",
            title       = request.title,
            date        = request.date,
            startTime   = request.startTime,
            endTime     = request.endTime,
            location    = request.location,
            description = request.description,
            course      = request.course
        )
        val updated = (current + optimistic)
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
        updateLocalCache(updated)
    }

    private suspend fun applyLocalUpdate(id: String, request: UpdateAvailabilityRequest) {
        val current = (_listState.value as? AvailabilityListState.Success)?.items ?: return
        val updated = current.map { item ->
            if (item.id == id) item.copy(
                title       = request.title       ?: item.title,
                date        = request.date        ?: item.date,
                startTime   = request.startTime   ?: item.startTime,
                endTime     = request.endTime     ?: item.endTime,
                location    = request.location    ?: item.location,
                description = request.description ?: item.description,
                course      = request.course      ?: item.course
            ) else item
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
        updateLocalCache(updated)
    }

    private suspend fun applyLocalDelete(availabilityId: String) {
        val current = (_listState.value as? AvailabilityListState.Success)?.items ?: emptyList()
        updateLocalCache(current.filterNot { it.id == availabilityId })
    }

    private suspend fun updateLocalCache(items: List<AvailabilityItem>) {
        val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
        val type = object : TypeToken<List<AvailabilityItem>>() {}.type
        cacheDb.saveCache(cacheKey, gson.toJson(items, type))
        memoryCache.put(cacheKey, items)
        _listState.value = AvailabilityListState.Success(items)
    }

    private suspend fun applyPendingOverlay(baseItems: List<AvailabilityItem>): List<AvailabilityItem> {
        val pending = cacheDb.getAllPending()
        if (pending.isEmpty()) return baseItems.sortedWith(compareBy({ it.date }, { it.startTime }))

        var items = baseItems.toMutableList()
        pending.forEach { pendingItem ->
            when (pendingItem.actionType) {
                CacheDatabase.ACTION_DELETE -> {
                    val id = pendingItem.availabilityId ?: return@forEach
                    items = items.filterNot { it.id == id }.toMutableList()
                }
                CacheDatabase.ACTION_UPDATE -> {
                    val id = pendingItem.availabilityId ?: return@forEach
                    val req = runCatching {
                        gson.fromJson(pendingItem.json, UpdateAvailabilityRequest::class.java)
                    }.getOrNull() ?: return@forEach

                    items = items.map { item ->
                        if (item.id == id) {
                            item.copy(
                                title = req.title ?: item.title,
                                date = req.date ?: item.date,
                                startTime = req.startTime ?: item.startTime,
                                endTime = req.endTime ?: item.endTime,
                                location = req.location ?: item.location,
                                description = req.description ?: item.description,
                                course = req.course ?: item.course
                            )
                        } else {
                            item
                        }
                    }.toMutableList()
                }
                CacheDatabase.ACTION_CREATE -> {
                    val req = runCatching {
                        gson.fromJson(pendingItem.json, CreateAvailabilityRequest::class.java)
                    }.getOrNull() ?: return@forEach

                    val alreadyExists = items.any {
                        it.title == req.title &&
                            it.date == req.date &&
                            it.startTime == req.startTime &&
                            it.endTime == req.endTime
                    }
                    if (!alreadyExists) {
                        items.add(
                            AvailabilityItem(
                                id = "pending-create-${pendingItem.id}",
                                title = req.title,
                                date = req.date,
                                startTime = req.startTime,
                                endTime = req.endTime,
                                location = req.location,
                                description = req.description,
                                course = req.course
                            )
                        )
                    }
                }
            }
        }

        return items
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    private suspend fun invalidateAndReload() {
        val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
        memoryCache.put(cacheKey, emptyList<AvailabilityItem>())
        when (val sync = repository.getAvailabilities(tutorId)) {
            is Result.Success -> {
                val type = object : TypeToken<List<AvailabilityItem>>() {}.type
                cacheDb.saveCache(cacheKey, gson.toJson(sync.data, type))
                val mergedItems = applyPendingOverlay(sync.data)
                memoryCache.put(cacheKey, mergedItems)
                _listState.value = AvailabilityListState.Success(mergedItems)
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
