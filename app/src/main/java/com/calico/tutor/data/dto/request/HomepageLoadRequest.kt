package com.calico.tutor.data.dto.request

import com.google.gson.annotations.SerializedName

data class HomepageLoadRequest(
    @SerializedName("load_time_ms")        val loadTimeMs: Long,
    @SerializedName("connectivity_status") val connectivityStatus: String,
    @SerializedName("user_id")             val userId: String?
)
