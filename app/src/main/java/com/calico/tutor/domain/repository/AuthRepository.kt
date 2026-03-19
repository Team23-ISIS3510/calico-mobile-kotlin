package com.calico.tutor.domain.repository

import com.calico.tutor.domain.model.AuthToken
import com.calico.tutor.domain.utils.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthToken>
    suspend fun register(
        email: String,
        password: String,
        name: String,
        phone: String,
        isTutor: Boolean,
        courses: List<String>? = null
    ): Result<AuthToken>

    suspend fun getStoredToken(): Result<AuthToken?>
    suspend fun clearToken(): Result<Unit>
    suspend fun isTokenValid(): Boolean
}
