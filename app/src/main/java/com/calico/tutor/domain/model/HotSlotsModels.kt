package com.calico.tutor.domain.model

data class HotSlot(
    val slotStart: String,
    val slotEnd: String,
    val bookingCount: Int,
    val tutorAvailability: String,
    val availabilityStart: String? = null,
    val availabilityEnd: String? = null
)

data class HotSlotsAnalysis(
    val tutorId: String,
    val analysisStartDate: String,
    val analysisEndDate: String,
    val totalSessionsLastWeek: Int,
    val hotSlots: List<HotSlot>
)
