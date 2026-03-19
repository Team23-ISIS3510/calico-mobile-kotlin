package com.calico.tutor.domain.model

data class AuthToken(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

data class User(
    val email: String,
    val name: String,
    val phone: String,
    val isTutor: Boolean,
    val courses: List<String>? = null
)
