package com.calico.tutor.domain.model

data class AuthToken(
    val idToken: String = "user",
    val refreshToken: String = "",
    val expiresIn: Long = 3600
)

data class User(
    val email: String,
    val name: String,
    val phone: String,
    val isTutor: Boolean,
    val courses: List<String>? = null
)
