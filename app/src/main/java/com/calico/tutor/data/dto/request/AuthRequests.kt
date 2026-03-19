package com.calico.tutor.data.dto.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val isTutor: Boolean,
    @SerializedName("courses")
    val courses: List<String>? = null
)
