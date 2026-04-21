package com.calico.tutor.ui.screen

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.calico.tutor.data.datasource.remote.GoogleSignInManager
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.ui.viewmodel.AuthState
import com.calico.tutor.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(viewModel: AuthViewModel, context: Context) {
    val authState = viewModel.authState.collectAsState()
    val (showLogin, setShowLogin) = remember { mutableStateOf(true) }
    val (currentScreen, setCurrentScreen) = remember { mutableStateOf("home") }
    
    // Get the actual Activity from context
    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    val googleSignInManager = remember {
        if (activity != null) {
            // TODO: Replace with your actual Web Client ID from Google Cloud Console
            val webClientId = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
            GoogleSignInManager(activity, webClientId)
        } else null
    }

    // Launcher para Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            if (account?.idToken != null) {
                viewModel.loginWithGoogle(account.idToken!!)
            }
        } catch (e: ApiException) {
            // Handle error
            e.printStackTrace()
        }
    }

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
                        if (googleSignInManager != null) {
                            val signInIntent = googleSignInManager.getSignInIntent()
                            googleSignInLauncher.launch(signInIntent)
                        }
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
