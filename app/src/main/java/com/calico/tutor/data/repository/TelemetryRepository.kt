package com.calico.tutor.data.repository

import android.os.Build
import android.util.Log
import com.calico.tutor.data.datasource.remote.TelemetryApiService
import com.calico.tutor.data.dto.request.BugReportRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TelemetryRepository(
    private val apiService: TelemetryApiService
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun reportBug(message: String) {
        scope.launch {
            try {
                val request = BugReportRequest(
                    type = "BUG",
                    message = message,
                    deviceModel = Build.MODEL
                )
                apiService.reportBug(request)
                Log.d("TelemetryRepository", "Bug report sent: $message")
            } catch (e: Exception) {
                Log.e("TelemetryRepository", "Failed to send bug report", e)
            }
        }
    }

    fun reportCrash(stackTrace: String) {
        scope.launch {
            try {
                val request = BugReportRequest(
                    type = "CRASH",
                    message = stackTrace,
                    deviceModel = Build.MODEL
                )
                apiService.reportBug(request)
                Log.d("TelemetryRepository", "Crash report sent")
            } catch (e: Exception) {
                Log.e("TelemetryRepository", "Failed to send crash report", e)
            }
        }
    }

    fun reportLatency(endpoint: String, latencyMs: Long) {
        scope.launch {
            try {
                val request = BugReportRequest(
                    type = "LATENCY",
                    message = "Endpoint: $endpoint, Latency: ${latencyMs}ms",
                    deviceModel = Build.MODEL
                )
                apiService.reportBug(request)
            } catch (e: Exception) {
                Log.e("TelemetryRepository", "Failed to send latency report", e)
            }
        }
    }
}
