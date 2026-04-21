package com.calico.tutor.data.datasource.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences? = try {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("TokenManager", "Failed to initialize EncryptedSharedPreferences, clearing corrupted data", e)
        // Clear the corrupted preferences file and retry
        context.deleteSharedPreferences(PREFS_NAME)
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (retryException: Exception) {
            Log.e("TokenManager", "Failed to recover EncryptedSharedPreferences", retryException)
            null
        }
    }

    fun saveToken(idToken: String, refreshToken: String, expiresIn: Long) {
        try {
            sharedPreferences?.edit()?.apply {
                putString(KEY_ID_TOKEN, idToken)
                putString(KEY_REFRESH_TOKEN, refreshToken)
                putLong(KEY_EXPIRES_IN, expiresIn)
                apply()
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Error saving token", e)
            handleEncryptionError()
        }
    }

    fun saveEmail(email: String) {
        try {
            sharedPreferences?.edit()?.apply {
                putString(KEY_EMAIL, email)
                apply()
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Error saving email", e)
        }
    }

    fun saveFirebaseUid(uid: String) {
        try {
            sharedPreferences?.edit()?.apply {
                putString(KEY_FIREBASE_UID, uid)
                apply()
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Error saving Firebase UID", e)
        }
    }

    fun getEmail(): String? = try {
        sharedPreferences?.getString(KEY_EMAIL, null)
    } catch (e: Exception) {
        Log.e("TokenManager", "Error reading email", e)
        handleEncryptionError()
        null
    }

    fun getFirebaseUid(): String? = try {
        sharedPreferences?.getString(KEY_FIREBASE_UID, null)
    } catch (e: Exception) {
        Log.e("TokenManager", "Error reading Firebase UID", e)
        handleEncryptionError()
        null
    }

    fun getIdToken(): String? = try {
        sharedPreferences?.getString(KEY_ID_TOKEN, null)
    } catch (e: Exception) {
        Log.e("TokenManager", "Error reading ID token", e)
        handleEncryptionError()
        null
    }

    fun getRefreshToken(): String? = try {
        sharedPreferences?.getString(KEY_REFRESH_TOKEN, null)
    } catch (e: Exception) {
        Log.e("TokenManager", "Error reading refresh token", e)
        handleEncryptionError()
        null
    }

    fun getExpiresIn(): Long = try {
        sharedPreferences?.getLong(KEY_EXPIRES_IN, 0L) ?: 0L
    } catch (e: Exception) {
        Log.e("TokenManager", "Error reading expiration", e)
        handleEncryptionError()
        0L
    }

    fun clearToken() {
        try {
            sharedPreferences?.edit()?.apply {
                remove(KEY_ID_TOKEN)
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_EXPIRES_IN)
                remove(KEY_EMAIL)
                remove(KEY_FIREBASE_UID)
                apply()
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Error clearing token", e)
            handleEncryptionError()
        }
    }

    fun isTokenAvailable(): Boolean = try {
        getIdToken() != null
    } catch (e: Exception) {
        Log.e("TokenManager", "Error checking token availability", e)
        false
    }

    /**
     * Handles encryption/decryption errors by clearing the corrupted data
     */
    private fun handleEncryptionError() {
        try {
            Log.w("TokenManager", "Clearing corrupted encrypted preferences")
            context.deleteSharedPreferences(PREFS_NAME)
        } catch (e: Exception) {
            Log.e("TokenManager", "Error clearing preferences", e)
        }
    }

    companion object {
        private const val PREFS_NAME = "calico_auth_prefs"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_EMAIL = "email"
        private const val KEY_FIREBASE_UID = "firebase_uid"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_IN = "expires_in"
    }
}
