package com.calico.tutor.data.datasource.remote

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.tasks.await

class GoogleSignInManager(
    private val context: Context,
    private val webClientId: String
) {
    private val googleSignInClient: GoogleSignInClient
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

    suspend fun silentSignIn(): String? {
        return try {
            val account = googleSignInClient.silentSignIn().await()
            account?.idToken
        } catch (e: Exception) {
            Log.e(TAG, "Silent sign-in falló: ${e.message}")
            null
        }
    }

    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar sesión de Google: ${e.message}")
        }
    }
}
