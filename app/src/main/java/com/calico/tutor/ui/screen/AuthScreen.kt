package com.calico.tutor.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun AuthScreen(viewModel: AuthViewModel, context: Context, activity: androidx.activity.ComponentActivity) {
    val authState = viewModel.authState.collectAsState()
    val (showLogin, setShowLogin) = remember { mutableStateOf(true) }
    val (currentScreen, setCurrentScreen) = remember { mutableStateOf("home") }
    val (errorToShow, setErrorToShow) = remember { mutableStateOf<String?>(null) }
    
    // Lazy initialization of Google Sign-In Manager
    val googleSignInManager = remember(activity) {
        try {
            val webClientId = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
            if (webClientId == "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com") {
                setErrorToShow("❌ ERROR: Reemplaza 'YOUR_WEB_CLIENT_ID' con tu Web Client ID de Google Cloud Console")
                null
            } else {
                GoogleSignInManager(activity, webClientId)
            }
        } catch (e: Exception) {
            setErrorToShow("Error inicializando Google Sign-In: ${e.localizedMessage}")
            null
        }
    }

    // Mostrar errores con Toast
    LaunchedEffect(errorToShow) {
        errorToShow?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
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
            } else {
                setErrorToShow("Error: No se obtuvo el ID Token de Google")
            }
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                12500 -> "Google Play Services está desactualizado"
                12501 -> "El usuario canceló el inicio de sesión"
                12502 -> "Los servicios de Google no están disponibles en tu dispositivo"
                12503 -> "La clave de API de Google no es válida"
                else -> "Error de Google Sign-In: ${e.message}"
            }
            setErrorToShow(errorMessage)
        } catch (e: Exception) {
            setErrorToShow("Error inesperado: ${e.localizedMessage}")
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
                        if (googleSignInManager == null) {
                            setErrorToShow("❌ Google Sign-In no está configurado. Reemplaza 'YOUR_WEB_CLIENT_ID' con tu Web Client ID")
                        } else {
                            try {
                                val signInIntent = googleSignInManager.getSignInIntent()
                                googleSignInLauncher.launch(signInIntent)
                            } catch (e: Exception) {
                                setErrorToShow("Error iniciando Google Sign-In: ${e.localizedMessage}")
                            }
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
