package com.calico.tutor.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.calico.tutor.ui.viewmodel.AuthState
import com.calico.tutor.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val authState = viewModel.authState.collectAsState()
    val (showLogin, setShowLogin) = remember { mutableStateOf(true) }

    if (showLogin) {
        val errorState = authState.value as? AuthState.Error
        LoginScreen(
            onLoginClick = { email, password ->
                viewModel.login(email, password)
            },
            onRegisterClick = { setShowLogin(false) },
            isLoading = authState.value is AuthState.Loading,
            errorMessage = errorState?.message,
            isRetryable = errorState?.retryable == true,
            onRetry = { viewModel.retryFailedOperation() }
        )
    } else {
        val errorState = authState.value as? AuthState.Error
        RegisterScreen(
            onRegisterClick = { email, password, name, phone, isTutor ->
                viewModel.register(email, password, name, phone, isTutor)
            },
            onBackClick = {
                setShowLogin(true)
                viewModel.resetState()
            },
            isLoading = authState.value is AuthState.Loading,
            errorMessage = errorState?.message,
            isRetryable = errorState?.retryable == true,
            onRetry = { viewModel.retryFailedOperation() }
        )
    }
}
