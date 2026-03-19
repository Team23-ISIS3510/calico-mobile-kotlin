package com.calico.tutor.data.datasource.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(private val context: Context) {
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
        encryptedSharedPreferences.edit().apply {
            putString(KEY_ID_TOKEN, idToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRES_IN, expiresIn)
            apply()
        }
    }

    fun getIdToken(): String? = encryptedSharedPreferences.getString(KEY_ID_TOKEN, null)

    fun getRefreshToken(): String? = encryptedSharedPreferences.getString(KEY_REFRESH_TOKEN, null)

    fun getExpiresIn(): Long = encryptedSharedPreferences.getLong(KEY_EXPIRES_IN, 0L)

    fun clearToken() {
        encryptedSharedPreferences.edit().apply {
            remove(KEY_ID_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_EXPIRES_IN)
            apply()
        }
    }

    fun isTokenAvailable(): Boolean = getIdToken() != null

    companion object {
        private const val PREFS_NAME = "calico_auth_prefs"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_IN = "expires_in"
    }
}
