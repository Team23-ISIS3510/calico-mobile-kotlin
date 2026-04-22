package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.datasource.local.TokenManager
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import android.os.SystemClock

class TokenAuthenticator(private val tokenManager: TokenManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
<<<<<<< HEAD
        // No reintentamos con refreshToken como bearer — el backend no acepta ese flujo.
        // El token expirado se maneja proactivamente en AuthScreen con silent sign-in.
=======
        // Don't retry auth/login endpoints - let them fail naturally
        val path = response.request.url.encodedPath
        if (path.contains("/auth") || path.contains("/login")) {
            return null
        }
        
        if (response.code == 401) {
            synchronized(this) {
                val newToken = tokenManager.getRefreshToken()
                
                if (newToken != null) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                }
            }
        }
>>>>>>> 9b46475fd1d2470b23d1665fc3193d065caf78c8
        return null
    }
}

class TokenInterceptor(private val tokenManager: TokenManager) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getIdToken()
        
        val request = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(request)
    }
}

class LatencyTelemetryInterceptor(
    private val onLatencyMeasured: (endpoint: String, method: String, durationMs: Long, statusCode: Int) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val startMs = SystemClock.elapsedRealtime()
        val response = chain.proceed(request)
        val durationMs = SystemClock.elapsedRealtime() - startMs

        // Avoid telemetry self-logging loops.
        if (!path.startsWith("/analytics")) {
            onLatencyMeasured(path, request.method, durationMs, response.code)
        }

        return response
    }
}
