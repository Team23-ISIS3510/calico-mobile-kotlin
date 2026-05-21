package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.dto.response.UserProfileResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface UsersApiService {
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): UserProfileResponse
}