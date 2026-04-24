package com.calico.tutor.domain.usecase

import com.calico.tutor.domain.model.AuthToken
import com.calico.tutor.domain.repository.AuthRepository
import com.calico.tutor.domain.utils.Result

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthToken> {
        if (email.isBlank() || password.isBlank()) {
            return Result.Error(
                IllegalArgumentException("Email and password cannot be empty"),
                "Please fill in all fields"
            )
        }

        if (!isValidEmail(email)) {
            return Result.Error(
                IllegalArgumentException("Invalid email format"),
                "Please enter a valid email address"
            )
        }

        return authRepository.login(email, password)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$"))
    }
}

class RegisterUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        name: String,
        phone: String,
        isTutor: Boolean,
        courses: List<String>? = null
    ): Result<AuthToken> {
        if (email.isBlank() || password.isBlank() || name.isBlank() || phone.isBlank()) {
            return Result.Error(
                IllegalArgumentException("All fields are required"),
                "Please fill in all fields"
            )
        }

        if (!isValidEmail(email)) {
            return Result.Error(
                IllegalArgumentException("Invalid email format"),
                "Please enter a valid email address"
            )
        }

        if (password.length < 6) {
            return Result.Error(
                IllegalArgumentException("Password too short"),
                "Password must be at least 6 characters"
            )
        }

        if (!isValidPhone(phone)) {
            return Result.Error(
                IllegalArgumentException("Invalid phone format"),
                "Please enter a valid phone number"
            )
        }

        return authRepository.register(
            email = email,
            password = password,
            name = name,
            phone = phone,
            isTutor = isTutor,
            courses = courses
        )
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$"))
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length >= 10 && phone.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }
    }
}

class GetAuthTokenUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<AuthToken?> {
        return authRepository.getStoredToken()
    }
}

class GoogleLoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String, email: String? = null): Result<AuthToken> {
        if (idToken.isBlank()) {
            return Result.Error(
                IllegalArgumentException("ID token cannot be empty"),
                "Invalid Google authentication token"
            )
        }

        return authRepository.loginWithGoogle(idToken, email)
    }
}

class ClearTokenUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.clearToken()
    }
}
