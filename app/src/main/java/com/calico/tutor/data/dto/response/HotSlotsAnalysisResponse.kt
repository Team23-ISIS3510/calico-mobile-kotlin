package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class HotSlotDto(
    @SerializedName("slotStart")
    val slotStart: String,

    @SerializedName("slotEnd")
    val slotEnd: String,

    @SerializedName("bookingCount")
    val bookingCount: Int,

    @SerializedName("tutorAvailability")
    val tutorAvailability: String,

    @SerializedName("availabilityStart")
    val availabilityStart: String? = null,

    @SerializedName("availabilityEnd")
    val availabilityEnd: String? = null
)

data class HotSlotsAnalysisResponseDto(
    @SerializedName("tutorId")
    val tutorId: String,

    @SerializedName("analysisStartDate")
    val analysisStartDate: String,

    @SerializedName("analysisEndDate")
    val analysisEndDate: String,

    @SerializedName("totalSessionsLastWeek")
    val totalSessionsLastWeek: Int,

    @SerializedName("hotSlots")
    val hotSlots: List<HotSlotDto> = emptyList()
)
