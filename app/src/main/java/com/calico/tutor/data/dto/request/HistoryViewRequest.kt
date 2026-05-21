package com.calico.tutor.data.dto.request

import com.google.gson.annotations.SerializedName

data class HistoryViewRequest(
    @SerializedName("tutorId")    val tutorId: String,
    @SerializedName("eventType")  val eventType: String,
    @SerializedName("timestamp")  val timestamp: String
)
