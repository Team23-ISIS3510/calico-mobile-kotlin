package com.calico.tutor.data.repository

import android.util.Log
import com.calico.tutor.data.datasource.local.TokenManager
import com.calico.tutor.data.datasource.remote.AuthApiService
import com.calico.tutor.data.dto.request.LoginRequest
import com.calico.tutor.data.dto.request.RegisterRequest
import com.calico.tutor.data.dto.request.GoogleLoginRequest
import com.calico.tutor.data.mapper.AuthMapper
import com.calico.tutor.domain.model.AuthToken
import com.calico.tutor.domain.repository.AuthRepository
import com.calico.tutor.domain.utils.Result
import org.json.JSONObject
import retrofit2.HttpException

private const val TAG = "AuthRepository"

class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthToken> {
        return try {
            Log.d(TAG, "Iniciando login con email: ${email.take(5)}...")
            val request = LoginRequest(email = email, password = password)
            val response = authApiService.login(request)
            val authToken = AuthMapper.toAuthToken(response)
            tokenManager.saveToken(authToken.idToken, authToken.refreshToken, authToken.expiresIn)
            tokenManager.saveEmail(email)
            Log.d(TAG, "Login exitoso")
            Result.Success(authToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error en login: ${e.message}", e)
            val backendMessage = if (e is HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string().orEmpty()
                    if (errorBody.isNotBlank()) {
                        JSONObject(errorBody).optString("error", errorBody)
                    } else {
                        "HTTP ${e.code()}"
                    }
                } catch (_: Exception) {
                    "HTTP ${e.code()}"
                }
            } else {
                com.calico.tutor.data.utils.ErrorMessageMapper.getErrorMessage(e)
            }
            Result.Error(e, backendMessage)
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
            Log.d(TAG, "Iniciando registro con email: ${email.take(5)}...")
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
            Log.d(TAG, "Registro exitoso")
            Result.Success(authToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error en registro: ${e.message}", e)
            val backendMessage = if (e is HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string().orEmpty()
                    if (errorBody.isNotBlank()) {
                        JSONObject(errorBody).optString("error", errorBody)
                    } else {
                        "HTTP ${e.code()}"
                    }
                } catch (_: Exception) {
                    "HTTP ${e.code()}"
                }
            } else {
                com.calico.tutor.data.utils.ErrorMessageMapper.getErrorMessage(e)
            }
            Result.Error(e, backendMessage)
        }
    }

    override suspend fun loginWithGoogle(idToken: String, email: String?): Result<AuthToken> {
        return try {
            Log.d(TAG, "Enviando idToken de Google sin modificar al backend (primeros 20 chars): ${idToken.take(20)}...")
            
            // ✅ IMPORTANTE: Enviar el idToken de Google SIN transformar
            // El backend es responsable de validar con Google
            val request = GoogleLoginRequest(idToken = idToken, userId = null)
            val response = authApiService.loginWithGoogle(request)
            Log.d(TAG, "Respuesta del backend recibida. idToken presente: ${response.idToken != null}, refreshToken presente: ${response.refreshToken != null}")
            val authToken = AuthMapper.toAuthToken(response)
            tokenManager.saveToken(authToken.idToken, authToken.refreshToken, authToken.expiresIn)
            if (!email.isNullOrBlank()) {
                tokenManager.saveEmail(email)
                Log.d(TAG, "Email guardado: ${email.take(5)}...")
            }
            Log.d(TAG, "Google login exitoso")
            Result.Success(authToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error en Google login: ${e::class.simpleName} - ${e.message}", e)
            val backendMessage = if (e is HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string().orEmpty()
                    if (errorBody.isNotBlank()) {
                        JSONObject(errorBody).optString("error", errorBody)
                    } else {
                        "HTTP ${e.code()}"
                    }
                } catch (_: Exception) {
                    "HTTP ${e.code()}"
                }
            } else {
                // Use ErrorMessageMapper for network and other errors
                com.calico.tutor.data.utils.ErrorMessageMapper.getErrorMessage(e)
            }
            Result.Error(e, backendMessage)
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
