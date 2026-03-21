package com.calico.tutor.data.mapper

import com.calico.tutor.data.dto.response.AuthResponse
import com.calico.tutor.domain.model.AuthToken

object AuthMapper {
    fun toAuthToken(response: AuthResponse): AuthToken {
        return AuthToken(
            idToken = response.idToken ?: "user",
            refreshToken = response.refreshToken ?: "",
            expiresIn = response.expiresIn
        )
    }
}
