package com.calico.tutor.data.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.calico.tutor.data.dto.request.CreateAvailabilityRequest
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.FileManager
import com.calico.tutor.di.ServiceLocator
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

private const val TAG = "PendingWorker"

/**
 * Worker de WorkManager para sincronización eventual de disponibilidades offline.
 *
 * Flujo:
 * 1. Lee todos los registros de pending_availabilities en SQLite.
 * 2. Por cada uno, intenta POST al endpoint /availability/create.
 * 3. En éxito: elimina el registro de la cola (deletePending).
 * 4. En error parcial: Result.retry() con backoff lineal de 5 minutos.
 *
 * Restricciones: solo se ejecuta cuando hay conexión de red (NetworkType.CONNECTED).
 * Periodicidad: cada 15 minutos (mínimo permitido por WorkManager).
 */
class PendingAvailabilitiesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = CacheDatabase(applicationContext)
            val fileManager = FileManager(applicationContext)
            val pending = db.getAllPending()

            if (pending.isEmpty()) return Result.success()

            fileManager.appendLog("WorkManager: sincronizando ${pending.size} disponibilidades pendientes")

            val apiService = ServiceLocator.availabilityApiService(applicationContext)
            val gson = Gson()
            var allSynced = true

            for ((id, json) in pending) {
                try {
                    val request = gson.fromJson(json, CreateAvailabilityRequest::class.java)
                    val response = apiService.createAvailability(request)
                    if (response.isSuccessful) {
                        db.deletePending(id)
                        Log.d(TAG, "Sincronizada disponibilidad id=$id")
                        fileManager.appendLog("Disponibilidad sincronizada id=$id")
                    } else {
                        allSynced = false
                        Log.w(TAG, "Servidor rechazó id=$id: HTTP ${response.code()}")
                        fileManager.appendLog("Servidor rechazó id=$id: HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    allSynced = false
                    Log.e(TAG, "Excepción al sincronizar id=$id: ${e.message}")
                    fileManager.appendLog("Excepción al sincronizar id=$id: ${e.message}")
                }
            }

            if (allSynced) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Worker falló: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "sync_pending_availabilities"

        /**
         * Registra el trabajo periódico en WorkManager.
         * ExistingPeriodicWorkPolicy.KEEP: no reemplaza si ya existe una tarea programada.
         */
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PendingAvailabilitiesWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
