package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.dto.request.LoginRequest
import com.calico.tutor.data.dto.request.RegisterRequest
import com.calico.tutor.data.dto.request.GoogleLoginRequest
import com.calico.tutor.data.dto.response.AuthResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/google-login")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): AuthResponse
}
