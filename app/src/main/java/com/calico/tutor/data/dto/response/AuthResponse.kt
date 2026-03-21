package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("idToken")
    val idToken: String? = null,
    @SerializedName("refreshToken")
    val refreshToken: String? = null,
    @SerializedName("expiresIn")
    val expiresIn: Long = 3600
)
