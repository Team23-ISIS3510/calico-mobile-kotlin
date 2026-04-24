package com.calico.tutor.data.datasource.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://157.253.83.194:3000/"

    fun createRetrofit(
        httpClient: OkHttpClient = createHttpClient(null)
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
            .build()
    }

    fun createAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    fun createSubjectsApiService(retrofit: Retrofit): SubjectsApiService {
        return retrofit.create(SubjectsApiService::class.java)
    }

    fun createAvailabilityApiService(retrofit: Retrofit): AvailabilityApiService {
        return retrofit.create(AvailabilityApiService::class.java)
    }

    fun createAnalyticsApiService(retrofit: Retrofit): AnalyticsApiService {
        return retrofit.create(AnalyticsApiService::class.java)
    }

    fun createTelemetryApiService(retrofit: Retrofit): TelemetryApiService {
        return retrofit.create(TelemetryApiService::class.java)
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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (tokenManager != null) {
            builder.addInterceptor(TokenInterceptor(tokenManager))
            builder.authenticator(TokenAuthenticator(tokenManager))
        }

        return builder.build()
    }
}