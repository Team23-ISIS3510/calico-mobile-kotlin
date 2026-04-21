package com.calico.tutor.data.datasource.remote

import android.content.Context
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

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent() = googleSignInClient.signInIntent

    suspend fun signInWithGoogle(idToken: String): GoogleSignInResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            
            val user = authResult.user
            if (user != null) {
                val token = user.getIdToken(false).await().token
                GoogleSignInResult.Success(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "",
                    idToken = token ?: idToken
                )
            } else {
                GoogleSignInResult.Error("User not found after authentication")
            }
        } catch (e: Exception) {
            GoogleSignInResult.Error(e.message ?: "Unknown error during Google Sign-In")
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
