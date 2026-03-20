package com.calico.tutor.di

import android.content.Context
import com.calico.tutor.data.datasource.local.TokenManager
import com.calico.tutor.data.datasource.remote.AuthApiService
import com.calico.tutor.data.datasource.remote.SubjectsApiService
import com.calico.tutor.data.datasource.remote.RetrofitClient
import com.calico.tutor.data.repository.AuthRepositoryImpl
import com.calico.tutor.data.repository.AnalyticsRepositoryImpl
import com.calico.tutor.domain.repository.AuthRepository
import com.calico.tutor.domain.repository.AnalyticsRepository
import com.calico.tutor.domain.usecase.GetAuthTokenUseCase
import com.calico.tutor.domain.usecase.LoginUseCase
import com.calico.tutor.domain.usecase.RegisterUseCase

object ServiceLocator {
    @Volatile
    private var tokenManager: TokenManager? = null
    @Volatile
    private var authApiService: AuthApiService? = null
    @Volatile
    private var subjectsApiService: SubjectsApiService? = null
    @Volatile
    private var authRepository: AuthRepository? = null
    @Volatile
    private var analyticsRepository: AnalyticsRepository? = null

    private fun tokenManager(context: Context): TokenManager {
        return tokenManager ?: synchronized(this) {
            tokenManager ?: TokenManager(context.applicationContext).also { tokenManager = it }
        }
    }

    private fun authApiService(context: Context): AuthApiService {
        return authApiService ?: synchronized(this) {
            authApiService ?: RetrofitClient.createAuthApiService(
                RetrofitClient.createRetrofit(
                    RetrofitClient.createHttpClientWithTokenManager(tokenManager(context))
                )
            ).also { authApiService = it }
        }
    }

    private fun authRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            authRepository ?: AuthRepositoryImpl(
                authApiService = authApiService(context),
                tokenManager = tokenManager(context)
            ).also { authRepository = it }
        }
    }

    fun subjectsApiService(context: Context): SubjectsApiService {
        return subjectsApiService ?: synchronized(this) {
            subjectsApiService ?: RetrofitClient.createSubjectsApiService(
                RetrofitClient.createRetrofit(
                    RetrofitClient.createHttpClientWithTokenManager(tokenManager(context))
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

    fun loginUseCase(context: Context): LoginUseCase = LoginUseCase(authRepository(context))

    fun registerUseCase(context: Context): RegisterUseCase = RegisterUseCase(authRepository(context))

    fun getAuthTokenUseCase(context: Context): GetAuthTokenUseCase =
        GetAuthTokenUseCase(authRepository(context))
}
