package com.calico.tutor.domain.usecase

import com.calico.tutor.domain.model.HotSlotsAnalysis
import com.calico.tutor.domain.repository.AvailabilityRepository
import com.calico.tutor.domain.utils.Result

class GetHotSlotsAnalysisUseCase(private val repository: AvailabilityRepository) {
    suspend operator fun invoke(tutorId: String): Result<HotSlotsAnalysis> {
        return repository.getHotSlotsAnalysis(tutorId)
    }
}
