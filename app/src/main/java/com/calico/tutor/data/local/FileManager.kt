package com.calico.tutor.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Estrategia 3 de almacenamiento local: Archivos Locales.
 *
 * Responsabilidades:
 * - Guarda respaldos JSON de la cola pending_availabilities en filesDir/backups/.
 * - Genera logs exportables de errores y eventos en filesDir/logs.txt.
 * - Limpia backups viejos automáticamente (mantiene solo los últimos 5).
 *
 * Todas las operaciones son suspend con Dispatchers.IO para no bloquear el hilo principal.
 */
class FileManager(private val context: Context) {

    private val backupsDir: File
        get() = File(context.filesDir, "backups").also { it.mkdirs() }

    private val logsFile: File
        get() = File(context.filesDir, "logs.txt")

    /**
     * Guarda un respaldo JSON del contenido dado y limpia archivos viejos.
     * Nombre del archivo: pending_{timestamp}.json
     */
    suspend fun saveBackup(jsonContent: String) = withContext(Dispatchers.IO) {
        try {
            File(backupsDir, "pending_${System.currentTimeMillis()}.json").writeText(jsonContent)
            cleanOldBackups()
        } catch (e: Exception) {
            Log.e("FileManager", "Error guardando backup: ${e.message}")
        }
    }

    /** Agrega una línea con timestamp al archivo de logs. */
    suspend fun appendLog(message: String) = withContext(Dispatchers.IO) {
        try {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            logsFile.appendText("[$ts] $message\n")
        } catch (e: Exception) {
            Log.e("FileManager", "Error escribiendo log: ${e.message}")
        }
    }

    /** Lee el contenido completo del archivo de logs. */
    suspend fun readLogs(): String = withContext(Dispatchers.IO) {
        if (logsFile.exists()) logsFile.readText() else ""
    }

    /**
     * Elimina backups viejos manteniendo solo los [maxFiles] más recientes.
     * Se ejecuta automáticamente al guardar un nuevo backup.
     */
    private fun cleanOldBackups(maxFiles: Int = 5) {
        backupsDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(maxFiles)
            ?.forEach { it.delete() }
    }
}
