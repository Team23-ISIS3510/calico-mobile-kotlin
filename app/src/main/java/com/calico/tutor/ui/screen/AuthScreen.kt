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

    // Si el usuario está autenticado, mostrar HomeScreen
    when (val state = authState.value) {
        is AuthState.Success -> {
            // Usuario autenticado - mostrar Home Page
            val userName = state.token.idToken
                .substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            
            HomeScreen(
                userName = userName,
                onLogout = {
                    viewModel.resetState()
                },
                onNavigateToSearch = {
                    // Aquí irá la navegación a búsqueda
                },
                onNavigateToProfile = {
                    // Aquí irá la navegación a perfil
                },
                onNavigateToHistory = {
                    // Aquí irá la navegación a historial
                }
            )
        }
        else -> {
            // Mostrar Login o Register según el estado
            if (showLogin) {
                val errorState = authState as? AuthState.Error
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
                val errorState = authState as? AuthState.Error
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
    }
}
