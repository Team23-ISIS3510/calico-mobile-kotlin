package com.calico.tutor.data.datasource.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.108:3000/api/"

    fun createRetrofit(
        httpClient: OkHttpClient = createHttpClient(null)
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    fun createHttpClientWithTokenManager(tokenManager: com.calico.tutor.data.datasource.local.TokenManager): OkHttpClient {
        return createHttpClient(tokenManager)
    }

    private fun createHttpClient(tokenManager: com.calico.tutor.data.datasource.local.TokenManager?): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)

        if (tokenManager != null) {
            builder.addInterceptor(TokenInterceptor(tokenManager))
            builder.authenticator(TokenAuthenticator(tokenManager))
        }

        return builder.build()
    }
}
