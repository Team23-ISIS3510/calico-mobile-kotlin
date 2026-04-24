package com.calico.tutor.data.datasource.remote

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.50.0.253:3000/"

    fun createRetrofit(
        httpClient: OkHttpClient = createHttpClient(null, null)
    ): Retrofit {
        val gson = GsonBuilder()
            .serializeNulls()
            .create()
        
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun createAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    fun createSubjectsApiService(retrofit: Retrofit): SubjectsApiService {
        return retrofit.create(SubjectsApiService::class.java)
    }

    fun createTelemetryApiService(retrofit: Retrofit): TelemetryApiService {
        return retrofit.create(TelemetryApiService::class.java)
    }

    fun createAnalyticsApiService(retrofit: Retrofit): AnalyticsApiService {
        return retrofit.create(AnalyticsApiService::class.java)
    }

    fun createHttpClientWithTokenManager(tokenManager: com.calico.tutor.data.datasource.local.TokenManager): OkHttpClient {
        return createHttpClient(tokenManager, null)
    }

    fun createHttpClientWithTokenManagerAndLatency(
        tokenManager: com.calico.tutor.data.datasource.local.TokenManager,
        onLatencyMeasured: (endpoint: String, method: String, durationMs: Long, statusCode: Int) -> Unit
    ): OkHttpClient {
        return createHttpClient(tokenManager, onLatencyMeasured)
    }

    private fun createHttpClient(
        tokenManager: com.calico.tutor.data.datasource.local.TokenManager?,
        onLatencyMeasured: ((endpoint: String, method: String, durationMs: Long, statusCode: Int) -> Unit)?
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (onLatencyMeasured != null) {
            builder.addInterceptor(LatencyTelemetryInterceptor(onLatencyMeasured))
        }

        if (tokenManager != null) {
            builder.addInterceptor(TokenInterceptor(tokenManager))
            builder.authenticator(TokenAuthenticator(tokenManager))
        }

        return builder.build()
    }
}
