package com.calico.tutor.ui.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun rememberIsOnline(context: Context): State<Boolean> {
    val appContext = remember(context) { context.applicationContext }
    val connectivityManager = remember(appContext) {
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val state = remember(connectivityManager) {
        mutableStateOf(isCurrentlyOnline(connectivityManager))
    }

    DisposableEffect(connectivityManager) {
        state.value = isCurrentlyOnline(connectivityManager)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                state.value = isCurrentlyOnline(connectivityManager)
            }

            override fun onLost(network: Network) {
                state.value = isCurrentlyOnline(connectivityManager)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                state.value = isCurrentlyOnline(connectivityManager)
            }

            override fun onUnavailable() {
                state.value = false
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
                // no-op
            }
        }
    }

    return state
}

private fun isCurrentlyOnline(connectivityManager: ConnectivityManager): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val caps = connectivityManager.getNetworkCapabilities(network) ?: return false

    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    return hasInternet && isValidated
}
