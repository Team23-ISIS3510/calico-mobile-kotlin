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
import com.calico.tutor.domain.model.Session
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "HistoryViewModel"

sealed class HistoryState {
    data object Idle : HistoryState()
    data object Loading : HistoryState()
    data class Success(val sessions: List<Session>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}

class HistoryViewModel(private val context: Context) : ViewModel() {
    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Idle)
    val historyState: StateFlow<HistoryState> = _historyState.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val cacheDb = ServiceLocator.cacheDatabase(context)
    private val userPrefs = ServiceLocator.userPreferences(context)
    private val memoryCache = ServiceLocator.inMemoryCache()
    private val gson = Gson()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var monitoredTutorId: String = ""
    private var monitoredStudentId: String = ""

    init {
        checkCurrentConnectivity()
    }

    fun loadTutorHistory(tutorId: String) {
        monitoredTutorId = tutorId
        viewModelScope.launch(Dispatchers.Main) {
            _historyState.value = HistoryState.Loading
            _historyState.value = HistoryCacheLoader.loadTutorHistory(
                context = context,
                cacheDb = cacheDb,
                memoryCache = memoryCache,
                userPrefs = userPrefs,
                gson = gson,
                tutorId = tutorId
            )
        }
    }

    fun loadStudentHistory(
        studentId: String,
        startDate: String? = null,
        endDate: String? = null,
        course: String? = null,
        limit: Int? = null
    ) {
        monitoredStudentId = studentId
        viewModelScope.launch(Dispatchers.Main) {
            _historyState.value = HistoryState.Loading
            _historyState.value = HistoryCacheLoader.loadStudentHistory(
                context = context,
                cacheDb = cacheDb,
                memoryCache = memoryCache,
                userPrefs = userPrefs,
                gson = gson,
                studentId = studentId,
                startDate = startDate,
                endDate = endDate,
                course = course,
                limit = limit
            )
        }
    }

    fun startTutorConnectivityMonitoring(tutorId: String) {
        if (networkCallback != null) return
        monitoredTutorId = tutorId
        checkCurrentConnectivity()
        registerConnectivityCallback {
            if (monitoredTutorId.isNotBlank()) {
                loadTutorHistory(monitoredTutorId)
            }
        }
        Log.d(TAG, "Tutor connectivity monitoring started for $tutorId")
    }

    fun startStudentConnectivityMonitoring(studentId: String) {
        if (networkCallback != null) return
        monitoredStudentId = studentId
        checkCurrentConnectivity()
        registerConnectivityCallback {
            if (monitoredStudentId.isNotBlank()) {
                loadStudentHistory(monitoredStudentId)
            }
        }
        Log.d(TAG, "Student connectivity monitoring started for $studentId")
    }

    private fun registerConnectivityCallback(onReconnect: () -> Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                _isOnline.value = true
                val shouldReload = _historyState.value is HistoryState.Error || _historyState.value is HistoryState.Idle
                if (shouldReload) {
                    viewModelScope.launch(Dispatchers.Main) {
                        onReconnect()
                    }
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                _isOnline.value = false
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    private fun checkCurrentConnectivity() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOnline.value = hasInternet
        Log.d(TAG, "Current connectivity status: $hasInternet")
    }

    override fun onCleared() {
        super.onCleared()
        networkCallback?.let { callback ->
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
    }
}

class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}