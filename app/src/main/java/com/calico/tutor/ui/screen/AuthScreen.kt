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
import com.calico.tutor.BuildConfig
import com.calico.tutor.data.datasource.remote.GoogleSignInManager
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.ui.viewmodel.AuthState
import com.calico.tutor.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(viewModel: AuthViewModel, context: Context, activity: androidx.activity.ComponentActivity) {
    val authState = viewModel.authState.collectAsState()
    val (showLogin, setShowLogin) = remember { mutableStateOf(true) }
    val (currentScreen, setCurrentScreen) = remember { mutableStateOf("home") }
    val (errorToShow, setErrorToShow) = remember { mutableStateOf<String?>(null) }

    val tokenManager = remember { ServiceLocator.provideTokenManager(context) }

    val googleSignInManager = remember(activity) {
        try {
            GoogleSignInManager(activity, BuildConfig.GOOGLE_WEB_CLIENT_ID)
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

    // Monitorear expiración del token y refrescar silenciosamente
    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Success && googleSignInManager != null) {
            while (true) {
                delay(60_000L) // revisar cada minuto
                if (tokenManager.isTokenExpiringSoon()) {
                    val newIdToken = googleSignInManager.silentSignIn()
                    if (newIdToken != null) {
                        viewModel.loginWithGoogle(newIdToken)
                    }
                    // Si silentSignIn falla, el usuario seguirá usando el token actual
                    // hasta que expire y reciba un 401
                }
            }
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
                viewModel.loginWithGoogle(account.idToken!!, account.email)
            } else {
                setErrorToShow("Error: No se obtuvo el ID Token de Google")
            }
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                10 -> "Error de configuración: registra el SHA-1 del app en Google Cloud Console"
                12500 -> "Google Play Services está desactualizado"
                12501 -> "El usuario canceló el inicio de sesión"
                12502 -> "Los servicios de Google no están disponibles en tu dispositivo"
                12503 -> "La clave de API de Google no es válida"
                else -> "Error de Google Sign-In (código ${e.statusCode}): ${e.message}"
            }
            setErrorToShow(errorMessage)
        } catch (e: Exception) {
            setErrorToShow("Error inesperado: ${e.localizedMessage}")
        }
    }

    // Si el usuario está autenticado, mostrar HomeScreen
    when (val state = authState.value) {
        is AuthState.Success -> {
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
                            setErrorToShow("Google Sign-In no disponible. Reinicia la app.")
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
