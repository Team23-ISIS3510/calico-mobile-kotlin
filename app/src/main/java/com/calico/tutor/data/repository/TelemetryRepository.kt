package com.calico.tutor.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.calico.tutor.data.datasource.remote.TelemetryApiService
import com.calico.tutor.data.dto.HomepageLoadRequest
import com.calico.tutor.data.dto.request.BugReportRequest
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TelemetryRepository(
    private val apiService: TelemetryApiService,
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val latencyThresholdMs = 2000L
    private val gson = GsonBuilder().serializeNulls().create()

    fun reportBug(
        message: String,
        feature: String,
        action: String
    ) {
        scope.launch {
            try {
                val request = BugReportRequest(
                    type = "BUG",
                    message = message,
                    deviceModel = Build.MODEL,
                    timestamp = currentTimestampIso(),
                    feature = feature,
                    action = action,
                    networkType = getNetworkType()
                )
                val json = gson.toJson(request)
                Log.d("TelemetryRepository", "Sending BUG report: $json")
                val response = apiService.reportBug(request)
                Log.d("TelemetryRepository", "Bug response: ${response.code()}")
                if (response.isSuccessful) {
                    Log.d("TelemetryRepository", "Bug report sent: $message")
                } else {
                    Log.e(
                        "TelemetryRepository",
                        "Bug report rejected: ${response.code()} ${response.errorBody()?.string()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("TelemetryRepository", "Failed to send bug report", e)
            }
        }
    }

    fun reportCrash(
        stackTrace: String,
        feature: String = "app",
        action: String = "uncaught_exception"
    ) {
        scope.launch {
            try {
                val request = BugReportRequest(
                    type = "CRASH",
                    message = stackTrace.take(12000),
                    deviceModel = Build.MODEL,
                    timestamp = currentTimestampIso(),
                    feature = feature,
                    action = action,
                    networkType = getNetworkType()
                )
                val response = apiService.reportBug(request)
                if (response.isSuccessful) {
                    Log.d("TelemetryRepository", "Crash report sent")
                } else {
                    Log.e(
                        "TelemetryRepository",
                        "Crash report rejected: ${response.code()} ${response.errorBody()?.string()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("TelemetryRepository", "Failed to send crash report", e)
            }
        }
    }

    fun reportCrashBlocking(
        stackTrace: String,
        feature: String = "app",
        action: String = "uncaught_exception"
    ) {
        runBlocking(Dispatchers.IO) {
            try {
                val request = BugReportRequest(
                    type = "CRASH",
                    message = stackTrace.take(12000),
                    deviceModel = Build.MODEL,
                    timestamp = currentTimestampIso(),
                    feature = feature,
                    action = action,
                    networkType = getNetworkType()
                )
                val response = apiService.reportBug(request)
                if (!response.isSuccessful) {
                    Log.e(
                        "TelemetryRepository",
                        "Blocking crash report rejected: ${response.code()} ${response.errorBody()?.string()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("TelemetryRepository", "Failed to send blocking crash report", e)
            }
        }
    }

    fun reportLatency(
        endpoint: String,
        latencyMs: Long,
        feature: String,
        action: String,
        method: String? = null,
        statusCode: Int? = null
    ) {
        if (latencyMs < latencyThresholdMs) {
            Log.d(
                "TelemetryRepository",
                "Latency below threshold ($latencyMs ms < $latencyThresholdMs ms), not reporting"
            )
            return
        }

        scope.launch {
            try {
                val request = BugReportRequest(
                    type = "LATENCY",
                    message = "Slow operation in $feature/$action on $endpoint (${latencyMs}ms)",
                    deviceModel = Build.MODEL,
                    timestamp = currentTimestampIso(),
                    feature = feature,
                    action = action,
                    networkType = getNetworkType(),
                    endpoint = endpoint,
                    durationMs = latencyMs,
                    method = method,
                    statusCode = statusCode
                )
                val json = gson.toJson(request)
                Log.d("TelemetryRepository", "Sending LATENCY report: $json")
                val response = apiService.reportBug(request)
                Log.d("TelemetryRepository", "Latency response: ${response.code()}")
                if (!response.isSuccessful) {
                    Log.e(
                        "TelemetryRepository",
                        "Latency report rejected: ${response.code()} ${response.errorBody()?.string()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("TelemetryRepository", "Failed to send latency report", e)
            }
        }
    }

    fun reportHomepageLoad(
        loadTimeMs: Long,
        connectivityStatus: String,
        userId: String?
    ) {
        scope.launch {
            val request = HomepageLoadRequest(
                loadTimeMs = loadTimeMs,
                connectivityStatus = connectivityStatus,
                userId = userId
            )

            try {
                Log.d("TelemetryRepository", "Sending HOMEPAGE_LOAD telemetry: ${gson.toJson(request)}")
                var response = apiService.reportHomepageLoad(request)
                if (!response.isSuccessful) {
                    // Retry once without surfacing any UI error.
                    response = apiService.reportHomepageLoad(request)
                }
            } catch (_: Exception) {
                // Fail silently by requirement.
            }
        }
    }

    private fun currentTimestampIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date())

    private fun getNetworkType(): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "UNKNOWN"
        val network = connectivityManager.activeNetwork ?: return "OFFLINE"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "UNKNOWN"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "OTHER"
        }
    }
}
