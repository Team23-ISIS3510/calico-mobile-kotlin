package com.calico.tutor.di

import android.content.Context
import com.calico.tutor.data.datasource.local.TokenManager
import com.calico.tutor.data.datasource.remote.AuthApiService
import com.calico.tutor.data.datasource.remote.SubjectsApiService
import com.calico.tutor.data.datasource.remote.TelemetryApiService
import com.calico.tutor.data.datasource.remote.RetrofitClient
import com.calico.tutor.data.repository.AuthRepositoryImpl
import com.calico.tutor.data.repository.AnalyticsRepositoryImpl
import com.calico.tutor.data.repository.TelemetryRepository
import com.calico.tutor.domain.repository.AuthRepository
import com.calico.tutor.domain.repository.AnalyticsRepository
import com.calico.tutor.domain.usecase.GetAuthTokenUseCase
import com.calico.tutor.domain.usecase.LoginUseCase
import com.calico.tutor.domain.usecase.RegisterUseCase

object ServiceLocator {
    @Volatile
    private var _tokenManager: TokenManager? = null
    @Volatile
    private var authApiService: AuthApiService? = null
    @Volatile
    private var subjectsApiService: SubjectsApiService? = null
    @Volatile
    private var telemetryApiService: TelemetryApiService? = null
    @Volatile
    private var authRepository: AuthRepository? = null
    @Volatile
    private var analyticsRepository: AnalyticsRepository? = null
    @Volatile
    private var telemetryRepository: TelemetryRepository? = null

    private fun getTokenManager(context: Context): TokenManager {
        return _tokenManager ?: synchronized(this) {
            _tokenManager ?: TokenManager(context.applicationContext).also { _tokenManager = it }
        }
    }

    private fun authApiService(context: Context): AuthApiService {
        return authApiService ?: synchronized(this) {
            authApiService ?: RetrofitClient.createAuthApiService(
                RetrofitClient.createRetrofit(
                    RetrofitClient.createHttpClientWithTokenManager(getTokenManager(context))
                )
            ).also { authApiService = it }
        }
    }

    private fun authRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            authRepository ?: AuthRepositoryImpl(
                authApiService = authApiService(context),
                tokenManager = getTokenManager(context)
            ).also { authRepository = it }
        }
    }

    fun subjectsApiService(context: Context): SubjectsApiService {
        return subjectsApiService ?: synchronized(this) {
            subjectsApiService ?: RetrofitClient.createSubjectsApiService(
                RetrofitClient.createRetrofit(
                    RetrofitClient.createHttpClientWithTokenManager(getTokenManager(context))
                )
            ).also { subjectsApiService = it }
        }
    }

    fun analyticsRepository(context: Context): AnalyticsRepository {
        return analyticsRepository ?: synchronized(this) {
            analyticsRepository ?: AnalyticsRepositoryImpl(
                subjectsApiService = subjectsApiService(context)
            ).also { analyticsRepository = it }
        }
    }

    private fun telemetryApiService(context: Context): TelemetryApiService {
        return telemetryApiService ?: synchronized(this) {
            telemetryApiService ?: RetrofitClient.createTelemetryApiService(
                RetrofitClient.createRetrofit()
            ).also { telemetryApiService = it }
        }
    }

    fun telemetryRepository(context: Context): TelemetryRepository {
        return telemetryRepository ?: synchronized(this) {
            telemetryRepository ?: TelemetryRepository(
                apiService = telemetryApiService(context)
            ).also { telemetryRepository = it }
        }
    }

    fun loginUseCase(context: Context): LoginUseCase = LoginUseCase(authRepository(context))

    fun registerUseCase(context: Context): RegisterUseCase = RegisterUseCase(authRepository(context))

    fun getAuthTokenUseCase(context: Context): GetAuthTokenUseCase =
        GetAuthTokenUseCase(authRepository(context))

    // Expose TokenManager publicly
    fun provideTokenManager(context: Context): TokenManager =
        getTokenManager(context)
}
