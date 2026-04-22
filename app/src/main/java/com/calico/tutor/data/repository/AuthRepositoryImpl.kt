package com.calico.tutor.data.repository

import com.calico.tutor.data.datasource.local.TokenManager
import com.calico.tutor.data.datasource.remote.AuthApiService
import com.calico.tutor.data.dto.request.LoginRequest
import com.calico.tutor.data.dto.request.RegisterRequest
import com.calico.tutor.data.dto.request.GoogleLoginRequest
import com.calico.tutor.data.mapper.AuthMapper
import com.calico.tutor.domain.model.AuthToken
import com.calico.tutor.domain.repository.AuthRepository
import com.calico.tutor.domain.utils.Result

class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthToken> {
        return try {
            val request = LoginRequest(email = email, password = password)
            val response = authApiService.login(request)
            val authToken = AuthMapper.toAuthToken(response)
            tokenManager.saveToken(authToken.idToken, authToken.refreshToken, authToken.expiresIn)
            tokenManager.saveEmail(email)
            Result.Success(authToken)
        } catch (e: Exception) {
            Result.Error(e, e.localizedMessage ?: "Login failed")
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        name: String,
        phone: String,
        isTutor: Boolean,
        courses: List<String>?
    ): Result<AuthToken> {
        return try {
            val request = RegisterRequest(
                email = email,
                password = password,
                name = name,
                phone = phone,
                isTutor = isTutor,
                courses = courses
            )
            val response = authApiService.register(request)
            val authToken = AuthMapper.toAuthToken(response)
            tokenManager.saveToken(authToken.idToken, authToken.refreshToken, authToken.expiresIn)
            tokenManager.saveEmail(email)
            Result.Success(authToken)
        } catch (e: Exception) {
            Result.Error(e, e.localizedMessage ?: "Registration failed")
        }
    }

    override suspend fun loginWithGoogle(idToken: String, email: String?): Result<AuthToken> {
        return try {
            val request = GoogleLoginRequest(idToken = idToken)
            val response = authApiService.loginWithGoogle(request)
            val authToken = AuthMapper.toAuthToken(response)
            tokenManager.saveToken(authToken.idToken, authToken.refreshToken, authToken.expiresIn)
            if (!email.isNullOrBlank()) {
                tokenManager.saveEmail(email)
            }
            Result.Success(authToken)
        } catch (e: Exception) {
            Result.Error(e, e.localizedMessage ?: "Google login failed")
        }
    }

    override suspend fun getStoredToken(): Result<AuthToken?> {
        return try {
            val idToken = tokenManager.getIdToken()
            val refreshToken = tokenManager.getRefreshToken()
            val expiresIn = tokenManager.getExpiresIn()

            if (idToken != null && refreshToken != null) {
                Result.Success(AuthToken(idToken, refreshToken, expiresIn))
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to retrieve stored token")
        }
    }

    override suspend fun clearToken(): Result<Unit> {
        return try {
            tokenManager.clearToken()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to clear token")
        }
    }

    override suspend fun isTokenValid(): Boolean {
        val idToken = tokenManager.getIdToken()
        return !idToken.isNullOrEmpty()
    }
}
