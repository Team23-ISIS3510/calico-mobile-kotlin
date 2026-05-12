package com.calico.tutor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.calico.tutor.data.datasource.remote.GoogleSignInManager
import com.calico.tutor.ui.screen.LoginScreen
import com.calico.tutor.ui.viewmodel.AuthState
import com.calico.tutor.ui.viewmodel.AuthViewModel
import com.calico.tutor.ui.viewmodel.AuthViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

/**
 * EJEMPLO DE INTEGRACIÓN DE GOOGLE LOGIN EN ACTIVITY
 *
 * Este archivo es un ejemplo de cómo integrar Google Login en tu Activity.
 * Adapta según necesites para tu estructura actual.
 *
 * PASOS:
 * 1. Reemplaza "YOUR_WEB_CLIENT_ID" con tu Web Client ID de Google Cloud Console
 * 2. Integra esto en tu MainActivity o LoginActivity actual
 * 3. Asegúrate de que AndroidManifest.xml tenga el permiso INTERNET
 */

class LoginActivityExample : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private val googleSignInManager by lazy {
        val googleClientId = BuildConfig.GOOGLE_CLIENT_ID_ANDROID
        GoogleSignInManager(this, googleClientId)
    }

    // Launcher para iniciar Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this, AuthViewModelFactory(this))
            .get(AuthViewModel::class.java)

        setContent {
            val authState by viewModel.authState.collectAsState()

            LoginScreen(
                onLoginClick = { email, password ->
                    viewModel.login(email, password)
                },
                onRegisterClick = {
                    // Navegar a RegisterScreen
                    // TODO: Implementar navegación
                },
                onGoogleLoginClick = {
                    initiateGoogleSignIn()
                },
                isLoading = authState is AuthState.Loading,
                errorMessage = when (authState) {
                    is AuthState.Error -> (authState as AuthState.Error).message
                    else -> null
                },
                isRetryable = when (authState) {
                    is AuthState.Error -> (authState as AuthState.Error).retryable
                    else -> false
                },
                onRetry = {
                    viewModel.retryFailedOperation()
                }
            )

            // Observar cambios en el estado de autenticación
            when (authState) {
                is AuthState.Success -> {
                    // El usuario se ha autenticado exitosamente
                    navigateToHomeScreen()
                }
                is AuthState.Error -> {
                    val error = authState as AuthState.Error
                    showError(error.message)
                }
                else -> {
                    // Idle o Loading
                }
            }
        }
    }

    /**
     * Inicia el flujo de Google Sign-In
     * Abre la pantalla de login de Google
     */
    private fun initiateGoogleSignIn() {
        try {
            val signInIntent = googleSignInManager.getSignInIntent()
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            showError("Failed to start Google Sign-In: ${e.message}")
        }
    }

    /**
     * Maneja el resultado del login con Google
     * Recibe el idToken y lo envía al ViewModel para autenticación con Firebase
     */
    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            if (account != null) {
                val idToken = account.idToken
                if (idToken != null) {
                    // Enviar el idToken al ViewModel para autenticación
                    viewModel.loginWithGoogle(idToken)
                } else {
                    showError("Failed to get ID token from Google account")
                }
            } else {
                showError("Google Sign-In returned null account")
            }
        } catch (e: ApiException) {
            // Google Sign-In falló
            val errorMessage = when (e.statusCode) {
                12500 -> "Google Play Services is out of date"
                12501 -> "User cancelled the Sign-In flow"
                12502 -> "One of the API scopes required by this application is not available"
                12503 -> "The provided API key is invalid"
                else -> "Google Sign-In error: ${e.message}"
            }
            showError(errorMessage)
        } catch (e: Exception) {
            showError("Unexpected error: ${e.message}")
        }
    }

    /**
     * Muestra un mensaje de error al usuario
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Navega a la pantalla principal después de autenticación exitosa
     */
    private fun navigateToHomeScreen() {
        // TODO: Implementar navegación a HomeScreen
        // startActivity(Intent(this, HomeActivity::class.java))
        // finish()
        Toast.makeText(this, "Login exitoso! Navegando...", Toast.LENGTH_SHORT).show()
    }
}

/**
 * FLUJO COMPLETO DE AUTENTICACIÓN:
 *
 * 1. Usuario hace clic en "Sign in with Google"
 *    └─ Se llama a initiateGoogleSignIn()
 *       └─ Se abre la pantalla de login de Google
 *
 * 2. Usuario se autentica en Google
 *    └─ Google retorna GoogleSignInAccount con idToken
 *
 * 3. handleGoogleSignInResult() procesa el resultado
 *    └─ Extrae el idToken
 *    └─ Llama a viewModel.loginWithGoogle(idToken)
 *
 * 4. AuthViewModel.loginWithGoogle()
 *    └─ Llama a googleLoginUseCase(idToken)
 *
 * 5. GoogleLoginUseCase
 *    └─ Valida el idToken
 *    └─ Llama a authRepository.loginWithGoogle(idToken)
 *
 * 6. AuthRepositoryImpl.loginWithGoogle()
 *    └─ Crea GoogleLoginRequest con el idToken
 *    └─ Envía POST a /auth/google-login
 *    └─ Recibe AuthResponse con tokens
 *    └─ Guarda los tokens en TokenManager
 *
 * 7. AuthViewModel actualiza el estado a Success
 *    └─ Se navega a HomeScreen
 *
 * MANEJO DE ERRORES:
 * - Si algo falla, el estado cambia a Error
 * - Se muestra el mensaje de error al usuario
 * - El usuario puede reintentar si es retryable
 *
 * ENVÍO DE TOKEN AL BACKEND:
 * Todos los headers posteriores deben incluir:
 * Authorization: Bearer {token_obtenido_del_login}
 */
