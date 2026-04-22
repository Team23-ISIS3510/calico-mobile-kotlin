package com.calico.tutor.data.datasource.remote

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleSignInManager(
    private val context: Context,
    private val webClientId: String
) {
    private val googleSignInClient: GoogleSignInClient
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "GoogleSignInManager"

    init {
        Log.d(TAG, "Inicializando GoogleSignInManager con Web Client ID: ${webClientId.take(20)}...")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
        Log.d(TAG, "GoogleSignInClient configurado correctamente")
    }

    fun getSignInIntent() = googleSignInClient.signInIntent

    suspend fun signInWithGoogle(idToken: String): GoogleSignInResult {
        return try {
            Log.d(TAG, "Iniciando autenticación con Firebase usando idToken")
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            
            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "Usuario autenticado en Firebase: ${user.email}")
                val token = user.getIdToken(false).await().token
                
                Log.d(TAG, "Token obtenido exitosamente")
                GoogleSignInResult.Success(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "",
                    idToken = token ?: idToken
                )
            } else {
                Log.e(TAG, "Usuario no encontrado después de autenticación en Firebase")
                GoogleSignInResult.Error("Usuario no encontrado después de autenticación en Firebase")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante Google Sign-In: ${e.message}", e)
            val errorMessage = when (e) {
                is ApiException -> "Error de API de Google: ${e.message}"
                else -> "Error durante autenticación: ${e.message}"
            }
            GoogleSignInResult.Error(errorMessage)
        }
    }

    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            firebaseAuth.signOut()
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }

    sealed class GoogleSignInResult {
        data class Success(
            val userId: String,
            val email: String,
            val displayName: String,
            val idToken: String
        ) : GoogleSignInResult()

        data class Error(val message: String) : GoogleSignInResult()
    }
}
