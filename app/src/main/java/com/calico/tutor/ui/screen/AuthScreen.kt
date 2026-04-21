package com.calico.tutor.ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.ui.viewmodel.AuthState
import com.calico.tutor.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel, context: Context) {
    val authState = viewModel.authState.collectAsState()
    val (showLogin, setShowLogin) = remember { mutableStateOf(true) }
    val (currentScreen, setCurrentScreen) = remember { mutableStateOf("home") }

    // Si el usuario está autenticado, mostrar HomeScreen
    when (val state = authState.value) {
        is AuthState.Success -> {
            // Usuario autenticado - mostrar Home Page
            val tokenManager = ServiceLocator.provideTokenManager(context)
            val email = tokenManager.getEmail() ?: state.token.idToken
            val userName = email
                .substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            
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
                        tutorId = email,
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
                    onGoogleLoginClick = {
                        // TODO: Implement Google Sign-In launcher here
                        // This will be triggered when user clicks "Sign in with Google" button
                    },
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
