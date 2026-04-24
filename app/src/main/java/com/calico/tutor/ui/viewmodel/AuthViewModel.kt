package com.calico.tutor.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.data.utils.RetryQueue
import com.calico.tutor.domain.model.AuthToken
import com.calico.tutor.domain.usecase.GetAuthTokenUseCase
import com.calico.tutor.domain.usecase.LoginUseCase
import com.calico.tutor.domain.usecase.RegisterUseCase
import com.calico.tutor.domain.utils.Result
import com.calico.tutor.util.EmailValidator
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

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val getAuthTokenUseCase: GetAuthTokenUseCase,
    private val context: Context
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private val retryQueue = RetryQueue()

    private var lastLoginCredentials: Pair<String, String>? = null
    private var lastRegisterData: RegisterData? = null

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
            // Guard: Validate email format
            guard(!EmailValidator.isValidEmail(email.trim())) {
                _authState.value = AuthState.Error("Invalid email format", retryable = false)
                return@launch
            }
            
            // Guard: Validate password length
            guard(password.length < 6) {
                _authState.value = AuthState.Error("Password must be at least 6 characters", retryable = false)
                return@launch
            }
            
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
                        result.message ?: "Invalid email or password",
                        retryable = isNetworkError
                    )
                }
                is Result.Loading -> AuthState.Loading
            }
        }
    }
    
    // Guard pattern helper: Returns if condition is true, otherwise continues
    private inline fun guard(condition: Boolean, block: () -> Unit): Boolean {
        if (condition) {
            block()
            return true
        }
        return false
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
            // Guard: Validate email format
            guard(!EmailValidator.isValidEmail(email.trim())) {
                _authState.value = AuthState.Error("Invalid email format", retryable = false)
                return@launch
            }
            
            // Guard: Validate password length
            guard(password.length < 6) {
                _authState.value = AuthState.Error("Password must be at least 6 characters", retryable = false)
                return@launch
            }
            
            // Guard: Validate name length
            guard(name.trim().length < 2) {
                _authState.value = AuthState.Error("Name must be at least 2 characters", retryable = false)
                return@launch
            }
            
            // Guard: Validate phone length
            guard(phone.trim().length < 10) {
                _authState.value = AuthState.Error("Please enter a valid phone number", retryable = false)
                return@launch
            }
            
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
                        result.message ?: "Registration failed",
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

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        // Clear token using TokenManager (EncryptedSharedPreferences)
        val tokenManager = ServiceLocator.provideTokenManager(context)
        tokenManager.clearToken()
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

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    private val loginUseCase = ServiceLocator.loginUseCase(context)
    private val registerUseCase = ServiceLocator.registerUseCase(context)
    private val getAuthTokenUseCase = ServiceLocator.getAuthTokenUseCase(context)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                loginUseCase = loginUseCase,
                registerUseCase = registerUseCase,
                getAuthTokenUseCase = getAuthTokenUseCase,
                context = context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

