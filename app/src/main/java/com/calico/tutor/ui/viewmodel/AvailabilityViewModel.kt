package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.AvailabilityItem
import com.calico.tutor.domain.repository.AvailabilityRepository
import com.calico.tutor.domain.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AvailabilityVM"

sealed class AvailabilityListState {
    object Idle : AvailabilityListState()
    object Loading : AvailabilityListState()
    data class Success(val items: List<AvailabilityItem>) : AvailabilityListState()
    data class Error(val message: String) : AvailabilityListState()
}

sealed class AvailabilityActionState {
    object Idle : AvailabilityActionState()
    object Loading : AvailabilityActionState()
    object Done : AvailabilityActionState()
    data class Error(val message: String) : AvailabilityActionState()
}

class AvailabilityViewModel(
    private val repository: AvailabilityRepository,
    private val tutorId: String
) : ViewModel() {

    private val _listState = MutableStateFlow<AvailabilityListState>(AvailabilityListState.Idle)
    val listState: StateFlow<AvailabilityListState> = _listState.asStateFlow()

    private val _actionState = MutableStateFlow<AvailabilityActionState>(AvailabilityActionState.Idle)
    val actionState: StateFlow<AvailabilityActionState> = _actionState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _listState.value = AvailabilityListState.Loading
            Log.d(TAG, "Cargando disponibilidades para tutorId: $tutorId")
            when (val result = repository.getAvailabilities(tutorId)) {
                is Result.Success -> {
                    Log.d(TAG, "Cargadas ${result.data.size} disponibilidades")
                    _listState.value = AvailabilityListState.Success(result.data)
                }
                is Result.Error -> {
                    Log.e(TAG, "Error cargando: ${result.message}")
                    _listState.value = AvailabilityListState.Error(result.message ?: "Error cargando disponibilidades")
                }
                is Result.Loading -> _listState.value = AvailabilityListState.Loading
            }
        }
    }

    fun create(request: CreateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            when (val result = repository.createAvailability(request)) {
                is Result.Success -> {
                    Log.d(TAG, "Creada disponibilidad: ${result.data.id}")
                    _actionState.value = AvailabilityActionState.Done
                    load()
                }
                is Result.Error -> {
                    Log.e(TAG, "Error creando: ${result.message}")
                    _actionState.value = AvailabilityActionState.Error(result.message ?: "Error creando disponibilidad")
                }
                is Result.Loading -> _actionState.value = AvailabilityActionState.Loading
            }
        }
    }

    fun update(id: String, request: UpdateAvailabilityRequest) {
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            when (val result = repository.updateAvailability(id, request)) {
                is Result.Success -> {
                    Log.d(TAG, "Actualizada disponibilidad: $id")
                    _actionState.value = AvailabilityActionState.Done
                    load()
                }
                is Result.Error -> {
                    Log.e(TAG, "Error actualizando: ${result.message}")
                    _actionState.value = AvailabilityActionState.Error(result.message ?: "Error actualizando disponibilidad")
                }
                is Result.Loading -> _actionState.value = AvailabilityActionState.Loading
            }
        }
    }

    fun delete(id: String) {
        // Optimistic removal so the UI reacts instantly
        val currentItems = (_listState.value as? AvailabilityListState.Success)?.items ?: emptyList()
        _listState.value = AvailabilityListState.Success(currentItems.filter { it.id != id })

        viewModelScope.launch {
            when (val result = repository.deleteAvailability(id)) {
                is Result.Success -> {
                    Log.d(TAG, "Eliminada disponibilidad: $id")
                    load() // sync list from server
                }
                is Result.Error -> {
                    Log.e(TAG, "Error eliminando: ${result.message}")
                    // Restore list and notify error
                    _listState.value = AvailabilityListState.Success(currentItems)
                    _actionState.value = AvailabilityActionState.Error(result.message ?: "Error deleting availability")
                }
                is Result.Loading -> {}
            }
        }
    }

    fun createBatch(requests: List<CreateAvailabilityRequest>) {
        if (requests.isEmpty()) return
        viewModelScope.launch {
            _actionState.value = AvailabilityActionState.Loading
            for (request in requests) {
                when (val result = repository.createAvailability(request)) {
                    is Result.Error -> {
                        Log.e(TAG, "Error en batch: ${result.message}")
                        _actionState.value = AvailabilityActionState.Error(result.message ?: "Error creando disponibilidad")
                        return@launch
                    }
                    else -> {}
                }
            }
            _actionState.value = AvailabilityActionState.Done
            load()
        }
    }

    fun resetActionState() {
        _actionState.value = AvailabilityActionState.Idle
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
                tutorId = tutorId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
