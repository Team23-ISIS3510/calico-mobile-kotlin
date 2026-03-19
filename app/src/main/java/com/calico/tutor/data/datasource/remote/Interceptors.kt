package com.calico.tutor.data.datasource.remote

import com.calico.tutor.data.datasource.local.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(private val tokenManager: TokenManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
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
