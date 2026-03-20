package com.calico.tutor.ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.calico.tutor.ui.viewmodel.AuthState
import com.calico.tutor.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel, context: Context) {
    val authState = viewModel.authState.collectAsState()
    val (showLogin, setShowLogin) = remember { mutableStateOf(true) }
    val (demoMode, setDemoMode) = remember { mutableStateOf(false) }
    val (currentScreen, setCurrentScreen) = remember { mutableStateOf("home") }

    // Si está en modo demo, mostrar HomeScreen demo
    if (demoMode) {
        when (currentScreen) {
            "topSubjects" -> {
                TopSubjectsScreen(
                    context = context,
                    onNavigateBack = { setCurrentScreen("home") }
                )
            }
            else -> {
                HomeScreen(
                    userName = "Demo User",
                    tutorId = "demo@example.com",
                    context = context,
                    onLogout = {
                        setDemoMode(false)
                    },
                    onNavigateToTopSubjects = {
                        setCurrentScreen("topSubjects")
                    }
                )
            }
        }
        return
    }

    // Si el usuario está autenticado, mostrar HomeScreen
    when (val state = authState.value) {
        is AuthState.Success -> {
            // Usuario autenticado - mostrar Home Page
            val userName = state.token.idToken
                .substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val tutorId = state.token.idToken
            
            when (currentScreen) {
                "topSubjects" -> {
                    TopSubjectsScreen(
                        context = context,
                        onNavigateBack = { setCurrentScreen("home") }
                    )
                }
                else -> {
                    HomeScreen(
                        userName = userName,
                        tutorId = tutorId,
                        context = context,
                        onLogout = {
                            viewModel.resetState()
                        },
                        onNavigateToTopSubjects = {
                            setCurrentScreen("topSubjects")
                        }
                    )
                }
            }
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
                    onDemoClick = { setDemoMode(true) },
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
