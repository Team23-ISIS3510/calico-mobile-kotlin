package com.calico.tutor.data.datasource.local

import android.content.Context
<<<<<<< HEAD
=======
import android.content.SharedPreferences
>>>>>>> 9b46475fd1d2470b23d1665fc3193d065caf78c8
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(private val context: Context) {
    private val TAG = "TokenManager"
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
<<<<<<< HEAD
        val saveTime = System.currentTimeMillis()
        Log.d(TAG, "💾 Guardando token. Expira en: $expiresIn segundos")
        encryptedSharedPreferences.edit().apply {
            putString(KEY_ID_TOKEN, idToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRES_IN, expiresIn)
            putLong(KEY_SAVE_TIME, saveTime)
            apply()
=======
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
>>>>>>> 9b46475fd1d2470b23d1665fc3193d065caf78c8
        }
    }

    fun saveEmail(email: String) {
<<<<<<< HEAD
        Log.d(TAG, "📧 Guardando email: ${email.take(5)}...")
        encryptedSharedPreferences.edit().apply {
            putString(KEY_EMAIL, email)
            apply()
=======
        try {
            sharedPreferences?.edit()?.apply {
                putString(KEY_EMAIL, email)
                apply()
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Error saving email", e)
>>>>>>> 9b46475fd1d2470b23d1665fc3193d065caf78c8
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

    /**
     * Verifica si el token está disponible y aún es válido
     * @return true si el token existe y no ha expirado
     */
    fun isTokenValid(): Boolean {
        val idToken = getIdToken() ?: return false
        val expiresIn = getExpiresIn()
        val saveTime = getSaveTime()
        
        val elapsedSeconds = (System.currentTimeMillis() - saveTime) / 1000
        val isValid = elapsedSeconds < expiresIn
        
        Log.d(TAG, "✅ Token válido: $isValid (Transcurrido: $elapsedSeconds seg, Expira en: $expiresIn seg)")
        return isValid
    }

    /**
     * Verifica si el token está por expirar (en menos de 5 minutos)
     * @return true si el token expira en menos de 5 minutos
     */
    fun isTokenExpiringSoon(): Boolean {
        val expiresIn = getExpiresIn()
        val saveTime = getSaveTime()
        val RENEWAL_THRESHOLD_SECONDS = 300 // 5 minutos
        
        val elapsedSeconds = (System.currentTimeMillis() - saveTime) / 1000
        val timeRemainingSeconds = expiresIn - elapsedSeconds
        val expiringSoon = timeRemainingSeconds < RENEWAL_THRESHOLD_SECONDS && timeRemainingSeconds > 0
        
        if (expiringSoon) {
            Log.w(TAG, "⏰ Token expirará en $timeRemainingSeconds segundos")
        }
        return expiringSoon
    }

    private fun getSaveTime(): Long = encryptedSharedPreferences.getLong(KEY_SAVE_TIME, 0L)

    fun clearToken() {
<<<<<<< HEAD
        Log.d(TAG, "🗑️ Limpiando todos los tokens")
        encryptedSharedPreferences.edit().apply {
            remove(KEY_ID_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_EXPIRES_IN)
            remove(KEY_SAVE_TIME)
            remove(KEY_EMAIL)
            apply()
=======
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
>>>>>>> 9b46475fd1d2470b23d1665fc3193d065caf78c8
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
        private const val KEY_SAVE_TIME = "save_time"
    }
}
