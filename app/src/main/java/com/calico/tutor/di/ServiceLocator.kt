package com.calico.tutor.di

import android.content.Context
import com.calico.tutor.data.cache.InMemoryCache
import com.calico.tutor.data.datasource.local.TokenManager
import com.calico.tutor.data.datasource.remote.AnalyticsApiService
import com.calico.tutor.data.datasource.remote.AuthApiService
import com.calico.tutor.data.datasource.remote.SubjectsApiService
import com.calico.tutor.data.datasource.remote.AvailabilityApiService
import com.calico.tutor.data.datasource.remote.RetrofitClient
import com.calico.tutor.data.datasource.remote.TelemetryApiService
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.FileManager
import com.calico.tutor.data.local.UserPreferencesDataStore
import com.calico.tutor.data.repository.TelemetryRepository
import com.calico.tutor.data.repository.AuthRepositoryImpl
import com.calico.tutor.data.repository.AnalyticsRepositoryImpl
import com.calico.tutor.data.repository.AvailabilityRepositoryImpl
import com.calico.tutor.domain.repository.AuthRepository
import com.calico.tutor.domain.repository.AnalyticsRepository
import com.calico.tutor.domain.repository.AvailabilityRepository
import com.calico.tutor.domain.usecase.GetAuthTokenUseCase
import com.calico.tutor.domain.usecase.LoginUseCase
import com.calico.tutor.domain.usecase.RegisterUseCase
import com.calico.tutor.domain.usecase.GoogleLoginUseCase
import com.calico.tutor.domain.usecase.ClearTokenUseCase

object ServiceLocator {
    // ── Almacenamiento local y caché ─────────────────────────────────────────
    @Volatile private var _cacheDatabase: CacheDatabase? = null
    @Volatile private var _userPreferences: UserPreferencesDataStore? = null
    @Volatile private var _inMemoryCache: InMemoryCache? = null
    @Volatile private var _fileManager: FileManager? = null

    fun cacheDatabase(context: Context): CacheDatabase =
        _cacheDatabase ?: synchronized(this) {
            _cacheDatabase ?: CacheDatabase(context.applicationContext).also { _cacheDatabase = it }
        }

    fun userPreferences(context: Context): UserPreferencesDataStore =
        _userPreferences ?: synchronized(this) {
            _userPreferences ?: UserPreferencesDataStore(context.applicationContext).also { _userPreferences = it }
        }

    fun inMemoryCache(): InMemoryCache =
        _inMemoryCache ?: synchronized(this) {
            _inMemoryCache ?: InMemoryCache(maxSize = 20).also { _inMemoryCache = it }
        }

    fun fileManager(context: Context): FileManager =
        _fileManager ?: synchronized(this) {
            _fileManager ?: FileManager(context.applicationContext).also { _fileManager = it }
        }

    // ── Autenticación ─────────────────────────────────────────────────────────
    @Volatile
    private var _tokenManager: TokenManager? = null
    @Volatile
    private var authApiService: AuthApiService? = null
    @Volatile
    private var subjectsApiService: SubjectsApiService? = null
    @Volatile
    private var availabilityApiService: AvailabilityApiService? = null
    @Volatile
    private var _analyticsApiService: AnalyticsApiService? = null
    @Volatile
    private var _telemetryApiService: TelemetryApiService? = null
    @Volatile
    private var authRepository: AuthRepository? = null
    @Volatile
    private var analyticsRepository: AnalyticsRepository? = null
    @Volatile
    private var availabilityRepository: AvailabilityRepository? = null
    @Volatile
    private var _telemetryRepository: TelemetryRepository? = null

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

    fun availabilityApiService(context: Context): AvailabilityApiService {
        return availabilityApiService ?: synchronized(this) {
            availabilityApiService ?: RetrofitClient.createAvailabilityApiService(
                RetrofitClient.createRetrofit(
                    RetrofitClient.createHttpClientWithTokenManager(getTokenManager(context))
                )
            ).also { availabilityApiService = it }
        }
    }

    fun analyticsRepository(context: Context): AnalyticsRepository {
        return analyticsRepository ?: synchronized(this) {
            analyticsRepository ?: AnalyticsRepositoryImpl(
                subjectsApiService = subjectsApiService(context)
            ).also { analyticsRepository = it }
        }
    }

    fun availabilityRepository(context: Context): AvailabilityRepository {
        return availabilityRepository ?: synchronized(this) {
            availabilityRepository ?: AvailabilityRepositoryImpl(
                apiService = availabilityApiService(context)
            ).also { availabilityRepository = it }
        }
    }

    fun loginUseCase(context: Context): LoginUseCase = LoginUseCase(authRepository(context))

    fun registerUseCase(context: Context): RegisterUseCase = RegisterUseCase(authRepository(context))

    fun getAuthTokenUseCase(context: Context): GetAuthTokenUseCase =
        GetAuthTokenUseCase(authRepository(context))

    fun googleLoginUseCase(context: Context): GoogleLoginUseCase =
        GoogleLoginUseCase(authRepository(context))

    fun clearTokenUseCase(context: Context): ClearTokenUseCase =
        ClearTokenUseCase(authRepository(context))

    fun analyticsApiService(context: Context): AnalyticsApiService {
        return _analyticsApiService ?: synchronized(this) {
            _analyticsApiService ?: RetrofitClient.createAnalyticsApiService(
                RetrofitClient.createRetrofit(
                    RetrofitClient.createHttpClientWithTokenManager(getTokenManager(context))
                )
            ).also { _analyticsApiService = it }
        }
    }

    fun telemetryRepository(context: Context): TelemetryRepository {
        return _telemetryRepository ?: synchronized(this) {
            _telemetryRepository ?: TelemetryRepository(
                apiService = _telemetryApiService ?: RetrofitClient.createTelemetryApiService(
                    RetrofitClient.createRetrofit(
                        RetrofitClient.createHttpClientWithTokenManager(getTokenManager(context))
                    )
                ).also { _telemetryApiService = it },
                context = context.applicationContext
            ).also { _telemetryRepository = it }
        }
    }

    // Expose TokenManager publicly
    fun provideTokenManager(context: Context): TokenManager =
        getTokenManager(context)
}
