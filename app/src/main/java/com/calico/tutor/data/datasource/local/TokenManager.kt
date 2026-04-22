package com.calico.tutor.data.datasource.local

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(private val context: Context) {
    private val TAG = "TokenManager"
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(idToken: String, refreshToken: String, expiresIn: Long) {
        val saveTime = System.currentTimeMillis()
        Log.d(TAG, "💾 Guardando token. Expira en: $expiresIn segundos")
        encryptedSharedPreferences.edit().apply {
            putString(KEY_ID_TOKEN, idToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRES_IN, expiresIn)
            putLong(KEY_SAVE_TIME, saveTime)
            apply()
        }
    }

    fun saveEmail(email: String) {
        Log.d(TAG, "📧 Guardando email: ${email.take(5)}...")
        encryptedSharedPreferences.edit().apply {
            putString(KEY_EMAIL, email)
            apply()
        }
    }

    fun getEmail(): String? = encryptedSharedPreferences.getString(KEY_EMAIL, null)

    fun getIdToken(): String? = encryptedSharedPreferences.getString(KEY_ID_TOKEN, null)

    fun getRefreshToken(): String? = encryptedSharedPreferences.getString(KEY_REFRESH_TOKEN, null)

    fun getExpiresIn(): Long = encryptedSharedPreferences.getLong(KEY_EXPIRES_IN, 0L)

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
        Log.d(TAG, "🗑️ Limpiando todos los tokens")
        encryptedSharedPreferences.edit().apply {
            remove(KEY_ID_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_EXPIRES_IN)
            remove(KEY_SAVE_TIME)
            remove(KEY_EMAIL)
            apply()
        }
    }

    fun isTokenAvailable(): Boolean = getIdToken() != null

    companion object {
        private const val PREFS_NAME = "calico_auth_prefs"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_EMAIL = "email"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_SAVE_TIME = "save_time"
    }
}
        private const val KEY_EXPIRES_IN = "expires_in"
    }
}
