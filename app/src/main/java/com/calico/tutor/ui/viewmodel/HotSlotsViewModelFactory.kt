package com.calico.tutor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.calico.tutor.domain.usecase.GetHotSlotsAnalysisUseCase

class HotSlotsViewModelFactory(
    private val getHotSlotsAnalysisUseCase: GetHotSlotsAnalysisUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HotSlotsViewModel::class.java)) {
            return HotSlotsViewModel(getHotSlotsAnalysisUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
