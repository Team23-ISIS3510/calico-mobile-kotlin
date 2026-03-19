package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("idToken")
    val idToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String,
    @SerializedName("expiresIn")
    val expiresIn: Long
)
