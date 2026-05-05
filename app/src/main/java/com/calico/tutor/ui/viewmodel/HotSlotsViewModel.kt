package com.calico.tutor.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calico.tutor.domain.model.HotSlotsAnalysis
import com.calico.tutor.domain.usecase.GetHotSlotsAnalysisUseCase
import com.calico.tutor.domain.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HotSlotsState {
    object Idle : HotSlotsState()
    object Loading : HotSlotsState()
    data class Success(val analysis: HotSlotsAnalysis) : HotSlotsState()
    data class Error(val message: String) : HotSlotsState()
}

class HotSlotsViewModel(
    private val getHotSlotsAnalysisUseCase: GetHotSlotsAnalysisUseCase
) : ViewModel() {

    private val _hotSlotsState = MutableStateFlow<HotSlotsState>(HotSlotsState.Idle)
    val hotSlotsState: StateFlow<HotSlotsState> = _hotSlotsState.asStateFlow()

    fun loadHotSlots(tutorId: String) {
        viewModelScope.launch {
            _hotSlotsState.value = HotSlotsState.Loading
            
            val result = getHotSlotsAnalysisUseCase(tutorId)
            
            when (result) {
                is Result.Success -> {
                    Log.d("HotSlotsViewModel", "Hot slots cargados exitosamente")
                    _hotSlotsState.value = HotSlotsState.Success(result.data)
                }
                is Result.Error -> {
                    val errorMessage = result.message ?: "Error cargando hot slots"
                    Log.e("HotSlotsViewModel", "Error: $errorMessage")
                    _hotSlotsState.value = HotSlotsState.Error(errorMessage)
                }
                Result.Loading -> {
                    // Already in loading state, no action needed
                }
            }
        }
    }

    fun resetState() {
        _hotSlotsState.value = HotSlotsState.Idle
    }
}
