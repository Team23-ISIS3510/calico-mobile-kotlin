package com.calico.tutor.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Estrategia 1 de almacenamiento local: BD Relacional con SQLiteOpenHelper.
 *
 * Tablas:
 * - cache_home: caché JSON de respuestas de red (upcoming, previous, occupancy, subjects, availabilities).
 * - pending_availabilities: cola de disponibilidades pendientes de sincronización offline.
 *
 * Todas las operaciones de escritura/lectura son suspend y usan Dispatchers.IO
 * para no bloquear el hilo principal (compatible con el patrón corrutinas del proyecto).
 */
class CacheDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "calico_cache.db"
        private const val DATABASE_VERSION = 1

        const val KEY_SESSIONS    = "sessions"
        const val KEY_OCCUPANCY   = "occupancy"
        const val KEY_SUBJECTS    = "subjects_recommended"
        const val KEY_AVAILABILITIES = "availabilities"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE cache_home (
                id TEXT PRIMARY KEY,
                json_data TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE pending_availabilities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                availability_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS cache_home")
        db.execSQL("DROP TABLE IF EXISTS pending_availabilities")
        onCreate(db)
    }

    /** Guarda o reemplaza una entrada de caché (INSERT OR REPLACE). */
    suspend fun saveCache(key: String, json: String) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", key)
            put("json_data", json)
            put("timestamp", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "cache_home", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /** Devuelve (jsonData, timestamp) para la clave dada, o (null, 0) si no existe. */
    suspend fun getCache(key: String): Pair<String?, Long> = withContext(Dispatchers.IO) {
        readableDatabase.query(
            "cache_home", arrayOf("json_data", "timestamp"),
            "id = ?", arrayOf(key),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) to cursor.getLong(1)
            else null to 0L
        }
    }

    /** Inserta una disponibilidad en la cola de pendientes y devuelve su id. */
    suspend fun savePending(availabilityJson: String): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("availability_json", availabilityJson)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert("pending_availabilities", null, values)
    }

    /** Obtiene todos los registros pendientes ordenados por fecha de creación. */
    suspend fun getAllPending(): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<Long, String>>()
        readableDatabase.query(
            "pending_availabilities", arrayOf("id", "availability_json"),
            null, null, null, null, "created_at ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getLong(0) to cursor.getString(1))
        }
        result
    }

    /** Elimina una disponibilidad pendiente por su id tras sincronización exitosa. */
    suspend fun deletePending(id: Long) = withContext(Dispatchers.IO) {
        writableDatabase.delete("pending_availabilities", "id = ?", arrayOf(id.toString()))
    }

    /** Conteo síncrono de pendientes (solo usar cuando ya se está en IO). */
    fun getPendingCountSync(): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM pending_availabilities", null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

    /** Conteo suspend de pendientes, seguro para llamar desde cualquier contexto. */
    suspend fun getPendingCount(): Int = withContext(Dispatchers.IO) {
        getPendingCountSync()
    }
}
