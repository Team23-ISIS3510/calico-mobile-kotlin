package com.calico.tutor.ui.screen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.calico.tutor.R
import com.calico.tutor.data.datasource.remote.GoogleSignInManager
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.ui.viewmodel.AuthState
import com.calico.tutor.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay

private const val TAG = "AuthScreen"

@Composable
fun AuthScreen(viewModel: AuthViewModel, context: Context, activity: androidx.activity.ComponentActivity) {
    val authState = viewModel.authState.collectAsState()
    val (showLogin, setShowLogin) = remember { mutableStateOf(true) }
    val (errorToShow, setErrorToShow) = remember { mutableStateOf<String?>(null) }
    val (isGoogleLoading, setGoogleLoading) = remember { mutableStateOf(false) }

    val tokenManager = remember { ServiceLocator.provideTokenManager(context) }

    val googleSignInManager = remember(activity) {
        try {
            Log.d(TAG, "Inicializando GoogleSignInManager")
            val googleWebClientId = context.getString(R.string.google_web_client_id)
            GoogleSignInManager(activity, googleWebClientId)
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando GoogleSignInManager: ${e.message}", e)
            setErrorToShow("Error initializing Google Sign-In: ${e.localizedMessage}")
            null
        }
    }

    // Mostrar errores con Toast
    LaunchedEffect(errorToShow) {
        errorToShow?.let {
            Log.w(TAG, "Mostrando error: $it")
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Mostrar errores del backend (AuthState.Error) como Toast
    LaunchedEffect(authState.value) {
        val state = authState.value
        Log.d(TAG, "AuthState cambió a: ${state::class.simpleName}")
        if (state is AuthState.Error) {
            Log.e(TAG, "Error de autenticación: ${state.message}")
            setGoogleLoading(false)
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
        }
        if (state is AuthState.Success) {
            Log.d(TAG, "Login exitoso, navegando a HomeScreen")
            setGoogleLoading(false)
        }
    }

    // Monitorear expiración del token y refrescar silenciosamente
    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Success && googleSignInManager != null) {
            while (true) {
                delay(60_000L)
                try {
                    if (tokenManager.isTokenExpiringSoon()) {
                        Log.d(TAG, "Token por expirar, intentando silent sign-in")
                        val newIdToken = googleSignInManager.silentSignIn()
                        if (newIdToken != null) {
                            viewModel.loginWithGoogle(newIdToken)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing token: ${e.message}")
                }
            }
        }
    }

    // Launcher para Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google Sign-In callback recibido. ResultCode: ${result.resultCode}")
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Cuenta Google obtenida: ${account?.email}, idToken presente: ${account?.idToken != null}")

            if (account?.idToken != null) {
                Log.d(TAG, "Enviando idToken al backend")
                viewModel.loginWithGoogle(account.idToken!!, account.email)
            } else {
                Log.e(TAG, "idToken es null después de Google Sign-In exitoso")
                setGoogleLoading(false)
                setErrorToShow("Error: Google ID token was not received. Check the Web Client ID configuration.")
            }
        } catch (e: ApiException) {
            setGoogleLoading(false)
            val errorMessage = when (e.statusCode) {
                10    -> "Configuration error (code 10): incorrect SHA-1 or Client ID"
                12500 -> "Google Play Services is out of date"
                12501 -> "The user canceled sign-in"
                12502 -> "Google Play Services is unavailable on this device"
                12503 -> "Invalid Google API key"
                else  -> "Google Sign-In error (code ${e.statusCode}): ${e.message}"
            }
            Log.e(TAG, "ApiException en Google Sign-In: código=${e.statusCode}, mensaje=${e.message}")
            setErrorToShow(errorMessage)
        } catch (e: Exception) {
            setGoogleLoading(false)
            Log.e(TAG, "Excepción inesperada en Google Sign-In callback: ${e.message}", e)
            setErrorToShow("Unexpected error: ${e.localizedMessage}")
        }
    }

    // Routing según AuthState
    when (val state = authState.value) {
        is AuthState.Success -> {
            val email = tokenManager.getEmail() ?: state.token.idToken
            val userName = email
                .substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            MainScreen(
                userName = userName,
                tutorId = email,
                userEmail = email,
                context = context,
                onLogout = { viewModel.resetState() }
            )
        }
        else -> {
            if (showLogin) {
                val errorState = authState.value as? AuthState.Error
                LoginScreen(
                    onLoginClick = { email, password ->
                        viewModel.login(email, password)
                    },
                    onRegisterClick = { setShowLogin(false) },
                    onGoogleLoginClick = {
                        if (googleSignInManager == null) {
                            setErrorToShow("Google Sign-In is unavailable. Restart the app.")
                        } else {
                            Log.d(TAG, "Lanzando Google Sign-In intent")
                            setGoogleLoading(true)
                            try {
                                googleSignInLauncher.launch(googleSignInManager.getSignInIntent())
                            } catch (e: Exception) {
                                setGoogleLoading(false)
                                Log.e(TAG, "Error lanzando Google Sign-In: ${e.message}", e)
                                setErrorToShow("Error starting Google Sign-In: ${e.localizedMessage}")
                            }
                        }
                    },
                    isLoading = authState.value is AuthState.Loading && !isGoogleLoading,
                    isGoogleLoading = isGoogleLoading || (authState.value is AuthState.Loading && isGoogleLoading),
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
    }
}
