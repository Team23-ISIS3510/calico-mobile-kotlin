package com.calico.tutor.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extensión de Context que crea un único DataStore por proceso para la app.
 * El delegate preferencesDataStore garantiza una sola instancia (singleton por name).
 */
private val Context.userPrefsStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_prefs"
)

/**
 * Estrategia 2 de almacenamiento local: BD Llave/Valor con DataStore Preferences.
 *
 * Almacena:
 * - Tiempo de expiración del caché (configurable, default 5 minutos).
 * - Última hora de sincronización exitosa con el servidor.
 * - Preferencia de notificaciones del usuario.
 *
 * Todas las escrituras son suspend y las lecturas se exponen como Flow
 * para observabilidad reactiva en los ViewModels.
 */
class UserPreferencesDataStore(private val context: Context) {

    companion object {
        val KEY_CACHE_EXPIRY_MS      = longPreferencesKey("cache_expiry_ms")
        val KEY_LAST_SYNC_TIME       = longPreferencesKey("last_sync_time")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

        /** Tiempo de expiración por defecto: 5 minutos. */
        const val DEFAULT_CACHE_EXPIRY_MS = 5 * 60 * 1000L
    }

    /** Flow observable del tiempo de expiración del caché en milisegundos. */
    val cacheExpiryMs: Flow<Long> = context.userPrefsStore.data.map { prefs ->
        prefs[KEY_CACHE_EXPIRY_MS] ?: DEFAULT_CACHE_EXPIRY_MS
    }

    /** Flow observable del timestamp de la última sincronización exitosa. */
    val lastSyncTime: Flow<Long> = context.userPrefsStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIME] ?: 0L
    }

    /** Flow observable de la preferencia de notificaciones. */
    val notificationsEnabled: Flow<Boolean> = context.userPrefsStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setCacheExpiryMs(expiryMs: Long) {
        context.userPrefsStore.edit { it[KEY_CACHE_EXPIRY_MS] = expiryMs }
    }

    suspend fun updateLastSyncTime() {
        context.userPrefsStore.edit { it[KEY_LAST_SYNC_TIME] = System.currentTimeMillis() }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.userPrefsStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }
}
