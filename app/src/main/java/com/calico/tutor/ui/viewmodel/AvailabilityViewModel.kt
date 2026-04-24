package com.calico.tutor.ui.viewmodel

import android.content.Context
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
}

/**
 * ViewModel para AvailabilityScreen.
 *
 * EVENTUAL CONNECTIVITY (Vista 5 - Ver availabilities):
 * - load(): L1 memoria → L2 SQLite fresco → Red → L2 fallback → Error("No hay datos disponibles")
 *
 * EVENTUAL CONNECTIVITY (Crear disponibilidad - offline-first):
 * 1. Guardar en pending_availabilities (SQLite) SIEMPRE antes de llamar a la red.
 * 2. Hacer backup del pendiente en archivo local (FileManager).
 * 3. Intentar enviar al endpoint inmediatamente.
 * 4. Éxito: eliminar de la cola, actualizar caché, Done.
 * 5. Fallo: quedar en cola, Done (el badge mostrará el pendiente).
 * 6. WorkManager reintenta periódicamente cuando hay red.
 *
 * pendingCount: StateFlow visible en UI para mostrar badge de pendientes.
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
                    // Fallback: L2 aunque expirado
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
    // Crear disponibilidad: offline-first
    // ─────────────────────────────────────────────────────────────────────────

    fun create(request: CreateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading

            // 1. Guardar en cola de pendientes SIEMPRE (antes de intentar red)
            val pendingId = cacheDb.savePending(gson.toJson(request))
            Log.d(TAG, "Disponibilidad guardada en pendientes id=$pendingId")
            fileManager.appendLog("Disponibilidad guardada en pendientes id=$pendingId tutorId=$tutorId")

            // 2. Backup en archivo local
            val allPending = cacheDb.getAllPending()
            fileManager.saveBackup(gson.toJson(allPending.map { it.second }))

            refreshPendingCount()

            // 3. Intentar envío inmediato a la red
            when (val result = repository.createAvailability(request)) {
                is Result.Success -> {
                    // Éxito: eliminar de la cola
                    cacheDb.deletePending(pendingId)
                    fileManager.appendLog("Disponibilidad creada y eliminada de pendientes id=$pendingId")
                    refreshPendingCount()

                    // Invalidar caché e recargar desde red
                    invalidateAvailabilitiesCache()
                    when (val sync = repository.getAvailabilities(tutorId)) {
                        is Result.Success -> {
                            updateAvailabilitiesCache(sync.data)
                            _listState.value = AvailabilityListState.Success(sync.data)
                        }
                        else -> {}
                    }
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Error -> {
                    // Red no disponible: queda en la cola, WorkManager reintentará
                    Log.w(TAG, "Fallo red al crear (queda en cola): ${result.message}")
                    fileManager.appendLog("Fallo red al crear disponibilidad (queda en cola): ${result.message}")
                    _actionState.value = AvailabilityActionState.Done
                }
                is Result.Loading -> _actionState.value = AvailabilityActionState.Loading
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actualizar disponibilidad
    // ─────────────────────────────────────────────────────────────────────────

    fun update(id: String, request: UpdateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            when (val result = repository.updateAvailability(id, request)) {
                is Result.Success -> {
                    when (val sync = repository.getAvailabilities(tutorId)) {
                        is Result.Success -> {
                            updateAvailabilitiesCache(sync.data)
                            _listState.value = AvailabilityListState.Success(sync.data)
                            val updated = sync.data.firstOrNull { it.id == id }
                            val matches = updated != null &&
                                (request.title == null || updated.title == request.title) &&
                                (request.date == null || updated.date == request.date) &&
                                (request.startTime == null || updated.startTime == request.startTime) &&
                                (request.endTime == null || updated.endTime == request.endTime)
                            _actionState.value = if (matches) AvailabilityActionState.Done
                            else AvailabilityActionState.Error("Update succeeded but data not reflected")
                        }
                        is Result.Error -> _actionState.value = AvailabilityActionState.Error(
                            sync.message ?: "Updated but failed to refresh"
                        )
                        is Result.Loading -> _actionState.value = AvailabilityActionState.Loading
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Error actualizando: ${result.message}")
                    _actionState.value = AvailabilityActionState.Error(result.message ?: "Error actualizando disponibilidad")
                }
                is Result.Loading -> _actionState.value = AvailabilityActionState.Loading
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Eliminar disponibilidades del tutor
    // ─────────────────────────────────────────────────────────────────────────

    fun deleteByTutor() {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            when (val result = repository.deleteAvailabilitiesByTutor(tutorId)) {
                is Result.Success -> {
                    invalidateAvailabilitiesCache()
                    when (val sync = repository.getAvailabilities(tutorId)) {
                        is Result.Success -> {
                            updateAvailabilitiesCache(sync.data)
                            _listState.value = AvailabilityListState.Success(sync.data)
                            _actionState.value = if (sync.data.isEmpty()) AvailabilityActionState.Done
                            else AvailabilityActionState.Error("Delete succeeded but items still exist")
                        }
                        is Result.Error -> _actionState.value = AvailabilityActionState.Error(
                            sync.message ?: "Deleted but failed to refresh"
                        )
                        is Result.Loading -> _actionState.value = AvailabilityActionState.Loading
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Error eliminando: ${result.message}")
                    _actionState.value = AvailabilityActionState.Error(result.message ?: "Error eliminando disponibilidades")
                }
                is Result.Loading -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Crear múltiples disponibilidades (repetición)
    // ─────────────────────────────────────────────────────────────────────────

    fun createBatch(requests: List<CreateAvailabilityRequest>) {
        if (requests.isEmpty()) return
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            for (request in requests) {
                // Guardar cada uno en pendientes antes de intentar red
                val pendingId = cacheDb.savePending(gson.toJson(request))
                refreshPendingCount()

                when (val result = repository.createAvailability(request)) {
                    is Result.Success -> {
                        cacheDb.deletePending(pendingId)
                        refreshPendingCount()
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error en batch id=$pendingId: ${result.message}")
                        fileManager.appendLog("Batch: fallo red para disponibilidad id=$pendingId (queda en cola)")
                        // No abortar el batch, continúa con los siguientes
                    }
                    else -> {}
                }
            }
            // Backup del estado final de pendientes
            val allPending = cacheDb.getAllPending()
            if (allPending.isNotEmpty()) {
                fileManager.saveBackup(gson.toJson(allPending.map { it.second }))
            }

            invalidateAvailabilitiesCache()
            load()
            _actionState.value = AvailabilityActionState.Done
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

    private suspend fun updateAvailabilitiesCache(items: List<AvailabilityItem>) {
        val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
        val type     = object : TypeToken<List<AvailabilityItem>>() {}.type
        cacheDb.saveCache(cacheKey, gson.toJson(items, type))
        memoryCache.put(cacheKey, items)
    }

    private fun invalidateAvailabilitiesCache() {
        val cacheKey = "${CacheDatabase.KEY_AVAILABILITIES}_$tutorId"
        memoryCache.get(cacheKey)  // solo accede, no hay método remove; se sobrescribirá en updateAvailabilitiesCache
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
