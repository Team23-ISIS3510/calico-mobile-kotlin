package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.data.utils.RetryQueue
import com.calico.tutor.domain.model.AuthToken
import com.calico.tutor.domain.usecase.GetAuthTokenUseCase
import com.calico.tutor.domain.usecase.LoginUseCase
import com.calico.tutor.domain.usecase.RegisterUseCase
import com.calico.tutor.domain.usecase.GoogleLoginUseCase
import com.calico.tutor.domain.usecase.ClearTokenUseCase
import com.calico.tutor.domain.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: AuthToken) : AuthState()
    data class Error(val message: String, val retryable: Boolean = true) : AuthState()
}

private const val NO_INTERNET_AUTH_MESSAGE = "You have no internet connection. Connect and try again."

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val getAuthTokenUseCase: GetAuthTokenUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val clearTokenUseCase: ClearTokenUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val retryQueue = RetryQueue()

    private var lastLoginCredentials: Pair<String, String>? = null
    private var lastRegisterData: RegisterData? = null
    private var lastGoogleIdToken: String? = null

    data class RegisterData(
        val email: String,
        val password: String,
        val name: String,
        val phone: String,
        val isTutor: Boolean,
        val courses: List<String>? = null
    )

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            lastLoginCredentials = email to password

            val result = loginUseCase(email, password)
            _authState.value = when (result) {
                is Result.Success -> {
                    lastLoginCredentials = null
                    AuthState.Success(result.data)
                }
                is Result.Error -> {
                    val isNetworkError = isNetworkRelated(result.exception)
                    if (isNetworkError) {
                        retryQueue.enqueue("login_$email") {
                            loginUseCase(email, password)
                        }
                    }
                    AuthState.Error(
                        if (isNetworkError) NO_INTERNET_AUTH_MESSAGE else result.message ?: "Login failed",
                        retryable = isNetworkError
                    )
                }
                is Result.Loading -> AuthState.Loading
            }
        }
    }

    fun register(
        email: String,
        password: String,
        name: String,
        phone: String,
        isTutor: Boolean,
        courses: List<String>? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            lastRegisterData = RegisterData(email, password, name, phone, isTutor, courses)

            val result = registerUseCase(email, password, name, phone, isTutor, courses)
            _authState.value = when (result) {
                is Result.Success -> {
                    lastRegisterData = null
                    AuthState.Success(result.data)
                }
                is Result.Error -> {
                    val isNetworkError = isNetworkRelated(result.exception)
                    if (isNetworkError) {
                        retryQueue.enqueue("register_$email") {
                            registerUseCase(email, password, name, phone, isTutor, courses)
                        }
                    }
                    AuthState.Error(
                        if (isNetworkError) NO_INTERNET_AUTH_MESSAGE else result.message ?: "Registration failed",
                        retryable = isNetworkError
                    )
                }
                is Result.Loading -> AuthState.Loading
            }
        }
    }

    fun loginWithGoogle(idToken: String, email: String? = null) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "loginWithGoogle llamado. email: ${email?.take(5)}")
            _authState.value = AuthState.Loading
            lastGoogleIdToken = idToken

            val result = googleLoginUseCase(idToken, email)
            Log.d("AuthViewModel", "Resultado de googleLoginUseCase: ${result::class.simpleName}")

            _authState.value = when (result) {
                is Result.Success -> {
                    lastGoogleIdToken = null
                    Log.d("AuthViewModel", "Google login exitoso, token recibido")
                    AuthState.Success(result.data)
                }
                is Result.Error -> {
                    val isNetworkError = isNetworkRelated(result.exception)
                    Log.e("AuthViewModel", "Google login falló: ${result.message}, networkError=$isNetworkError")
                    if (isNetworkError) {
                        retryQueue.enqueue("google_login") {
                            googleLoginUseCase(idToken, email)
                        }
                    }
                    AuthState.Error(
                        if (isNetworkError) NO_INTERNET_AUTH_MESSAGE else result.message ?: "Google login failed",
                        retryable = isNetworkError
                    )
                }
                is Result.Loading -> AuthState.Loading
            }
        }
    }

    fun retryFailedOperation() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            when {
                lastLoginCredentials != null -> {
                    val (email, password) = lastLoginCredentials!!
                    login(email, password)
                }
                lastRegisterData != null -> {
                    val data = lastRegisterData!!
                    register(data.email, data.password, data.name, data.phone, data.isTutor, data.courses)
                }
                lastGoogleIdToken != null -> {
                    loginWithGoogle(lastGoogleIdToken!!)
                }
                retryQueue.getPendingRequests() > 0 -> {
                    val results = retryQueue.retryAll()
                    val hasSuccesses = results.any { it.second is Result.Success }
                    val hasErrors = results.any { it.second is Result.Error }

                    if (hasSuccesses && !hasErrors) {
                        if (lastLoginCredentials != null) {
                            val (email, password) = lastLoginCredentials!!
                            login(email, password)
                        } else {
                            _authState.value = AuthState.Idle
                        }
                    } else {
                        _authState.value = AuthState.Error("Some requests failed. Please try again.", retryable = true)
                    }
                }
            }
        }
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            val result = getAuthTokenUseCase()
            when (result) {
                is Result.Success -> {
                    if (result.data != null) {
                        _authState.value = AuthState.Success(result.data)
                    } else {
                        _authState.value = AuthState.Idle
                    }
                }
                is Result.Error -> _authState.value = AuthState.Error(
                    result.message ?: "Failed to retrieve token",
                    retryable = false
                )
                is Result.Loading -> _authState.value = AuthState.Loading
            }
        }
    }

    fun logout() {
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun getPendingRetries(): Int = retryQueue.getPendingRequests()

    private fun isNetworkRelated(exception: Throwable): Boolean {
        return exception is java.net.ConnectException ||
                exception is java.net.SocketTimeoutException ||
                exception is java.io.IOException ||
                exception.message?.contains("Network", ignoreCase = true) == true ||
                exception.cause?.let { isNetworkRelated(it) } == true
    }
}

class AuthViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val loginUseCase = ServiceLocator.loginUseCase(context)
    private val registerUseCase = ServiceLocator.registerUseCase(context)
    private val getAuthTokenUseCase = ServiceLocator.getAuthTokenUseCase(context)
    private val googleLoginUseCase = ServiceLocator.googleLoginUseCase(context)
    private val clearTokenUseCase = ServiceLocator.clearTokenUseCase(context)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                loginUseCase = loginUseCase,
                registerUseCase = registerUseCase,
                getAuthTokenUseCase = getAuthTokenUseCase,
                googleLoginUseCase = googleLoginUseCase,
                clearTokenUseCase = clearTokenUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
