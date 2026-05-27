package com.calico.tutor.ui.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Composable
fun rememberIsOnline(context: Context): State<Boolean> {
    val appContext = remember(context) { context.applicationContext }
    val connectivityManager = remember(appContext) {
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val initial = remember(connectivityManager) {
        isCurrentlyOnline(connectivityManager)
    }

    val flow = remember(connectivityManager) { connectivityFlow(connectivityManager) }

    return flow.collectAsState(initial = initial)
}

private fun connectivityFlow(connectivityManager: ConnectivityManager): Flow<Boolean> {
    return callbackFlow {
        var lastSent: Boolean? = null
        var pendingOnlineJob: Job? = null

        fun sendIfChanged(value: Boolean) {
            if (lastSent == value) return
            lastSent = value
            trySend(value)
        }

        fun updateFromSystem() {
            val onlineNow = isCurrentlyOnline(connectivityManager)
            if (!onlineNow) {
                pendingOnlineJob?.cancel()
                pendingOnlineJob = null
                sendIfChanged(false)
                return
            }

            // Avoid UI flapping: go offline immediately, but only consider “online” after it’s stable.
            pendingOnlineJob?.cancel()
            pendingOnlineJob = launch {
                delay(750)
                if (isCurrentlyOnline(connectivityManager)) {
                    sendIfChanged(true)
                } else {
                    sendIfChanged(false)
                }
            }
        }

        updateFromSystem()

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = updateFromSystem()
            override fun onLost(network: Network) = updateFromSystem()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = updateFromSystem()
            override fun onUnavailable() {
                pendingOnlineJob?.cancel()
                pendingOnlineJob = null
                sendIfChanged(false)
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            pendingOnlineJob?.cancel()
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
                // no-op
            }
        }
    }
}

private fun isCurrentlyOnline(connectivityManager: ConnectivityManager): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val caps = connectivityManager.getNetworkCapabilities(network) ?: return false

    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    return hasInternet && isValidated
}
