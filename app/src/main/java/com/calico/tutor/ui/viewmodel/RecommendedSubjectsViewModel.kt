package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calico.tutor.data.cache.InMemoryCache
import com.calico.tutor.data.local.CacheDatabase
import com.calico.tutor.data.local.UserPreferencesDataStore
import com.calico.tutor.di.ServiceLocator
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "RecommendedSubjectsVM"

class RecommendedSubjectsViewModel(private val context: Context) : ViewModel() {

    private val _subjectsState = MutableStateFlow<SubjectsState>(SubjectsState.Idle)
    val subjectsState: StateFlow<SubjectsState> = _subjectsState.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val cacheDb = ServiceLocator.cacheDatabase(context)
    private val userPrefs = ServiceLocator.userPreferences(context)
    private val memoryCache = ServiceLocator.inMemoryCache()
    private val gson = Gson()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var monitoredTutorId: String = ""

    init {
        checkCurrentConnectivity()
    }

    private fun checkCurrentConnectivity() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOnline.value = hasInternet
        Log.d(TAG, "Current connectivity status: $hasInternet")
    }

    fun loadData(tutorId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _subjectsState.value = SubjectsState.Loading
            _subjectsState.value = SubjectsCacheLoader.loadSubjects(
                context = context,
                cacheDb = cacheDb,
                memoryCache = memoryCache,
                userPrefs = userPrefs,
                gson = gson
            )
            monitoredTutorId = tutorId
        }
    }

    fun startConnectivityMonitoring(tutorId: String) {
        if (networkCallback != null) return
        monitoredTutorId = tutorId
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                _isOnline.value = true
                val needsLoad = _subjectsState.value is SubjectsState.Error ||
                    _subjectsState.value is SubjectsState.Idle
                if (needsLoad) {
                    viewModelScope.launch(Dispatchers.Main) {
                        Log.d(TAG, "Connectivity restored - reloading recommended subjects")
                        loadData(monitoredTutorId)
                    }
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                _isOnline.value = false
            }
        }

        cm.registerNetworkCallback(req, networkCallback!!)
        Log.d(TAG, "Connectivity monitoring started for $tutorId")
    }

    override fun onCleared() {
        super.onCleared()
        networkCallback?.let { callback ->
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
    }
}

class RecommendedSubjectsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecommendedSubjectsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecommendedSubjectsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}