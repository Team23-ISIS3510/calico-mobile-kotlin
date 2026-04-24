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
import com.calico.tutor.data.dto.request.UpdateAvailabilityRequest
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.FileManager
import com.calico.tutor.di.ServiceLocator
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

private const val TAG = "PendingWorker"

/**
 * WorkManager worker for eventual-connectivity sync of offline availability actions.
 *
 * Runs only when network is available (NetworkType.CONNECTED).
 * Handles CREATE, UPDATE, and DELETE actions stored in pending_availabilities.
 * On partial failure → Result.retry() with linear backoff.
 */
class PendingAvailabilitiesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db          = CacheDatabase(applicationContext)
            val fileManager = FileManager(applicationContext)
            val pending     = db.getAllPending()

            if (pending.isEmpty()) return Result.success()

            fileManager.appendLog("WorkManager: syncing ${pending.size} pending action(s)")

            val apiService = ServiceLocator.availabilityApiService(applicationContext)
            val gson       = Gson()
            var allSynced  = true

            for (item in pending) {
                try {
                    val synced = when (item.actionType) {
                        CacheDatabase.ACTION_CREATE -> {
                            val request  = gson.fromJson(item.json, CreateAvailabilityRequest::class.java)
                            val response = apiService.createAvailability(request)
                            response.isSuccessful.also { ok ->
                                if (!ok) Log.w(TAG, "CREATE rejected id=${item.id}: HTTP ${response.code()}")
                            }
                        }
                        CacheDatabase.ACTION_UPDATE -> {
                            val availId  = item.availabilityId
                            if (availId == null) {
                                Log.w(TAG, "UPDATE id=${item.id} has no availabilityId — skipping")
                                true // drop malformed entry
                            } else {
                                val request  = gson.fromJson(item.json, UpdateAvailabilityRequest::class.java)
                                val response = apiService.updateAvailability(availId, request)
                                response.isSuccessful.also { ok ->
                                    if (!ok) Log.w(TAG, "UPDATE rejected id=${item.id}: HTTP ${response.code()}")
                                }
                            }
                        }
                        CacheDatabase.ACTION_DELETE -> {
                            val tutorId  = item.availabilityId
                            if (tutorId == null) {
                                Log.w(TAG, "DELETE id=${item.id} has no tutorId — skipping")
                                true
                            } else {
                                val response = apiService.deleteAvailabilitiesByTutor(tutorId)
                                response.isSuccessful.also { ok ->
                                    if (!ok) Log.w(TAG, "DELETE rejected id=${item.id}: HTTP ${response.code()}")
                                }
                            }
                        }
                        else -> {
                            Log.w(TAG, "Unknown action_type '${item.actionType}' for id=${item.id} — dropping")
                            true
                        }
                    }

                    if (synced) {
                        db.deletePending(item.id)
                        fileManager.appendLog("Synced ${item.actionType} id=${item.id}")
                    } else {
                        allSynced = false
                    }
                } catch (e: Exception) {
                    allSynced = false
                    Log.e(TAG, "Exception syncing id=${item.id}: ${e.message}")
                    fileManager.appendLog("Exception syncing ${item.actionType} id=${item.id}: ${e.message}")
                }
            }

            if (allSynced) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "sync_pending_availabilities"

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
