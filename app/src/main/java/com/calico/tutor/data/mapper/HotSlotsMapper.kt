package com.calico.tutor.data.mapper

import com.calico.tutor.data.dto.response.HotSlotDto
import com.calico.tutor.data.dto.response.HotSlotsAnalysisResponseDto
import com.calico.tutor.domain.model.HotSlot
import com.calico.tutor.domain.model.HotSlotsAnalysis

fun HotSlotDto.toDomain(): HotSlot = HotSlot(
    slotStart = slotStart,
    slotEnd = slotEnd,
    bookingCount = bookingCount,
    tutorAvailability = tutorAvailability,
    availabilityStart = availabilityStart,
    availabilityEnd = availabilityEnd
)

fun HotSlotsAnalysisResponseDto.toDomain(): HotSlotsAnalysis = HotSlotsAnalysis(
    tutorId = tutorId,
    analysisStartDate = analysisStartDate,
    analysisEndDate = analysisEndDate,
    totalSessionsLastWeek = totalSessionsLastWeek,
    hotSlots = hotSlots.map { it.toDomain() }
)
