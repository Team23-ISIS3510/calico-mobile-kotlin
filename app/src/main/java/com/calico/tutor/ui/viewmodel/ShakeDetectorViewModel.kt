package com.calico.tutor.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calico.tutor.di.ServiceLocator
import com.calico.tutor.domain.model.BugReportData
import com.calico.tutor.domain.model.ShakeDetectorState
import com.calico.tutor.util.EmailIntentHelper
import com.calico.tutor.util.ShakeDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShakeDetectorViewModel(
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<ShakeDetectorState>(ShakeDetectorState.Idle)
    val state: StateFlow<ShakeDetectorState> = _state.asStateFlow()

    private var currentScreen: String = "Unknown"
    private var userEmail: String? = null

    private val telemetryRepository = ServiceLocator.telemetryRepository(context)

    private val shakeDetector = ShakeDetector(context) { force ->
        onShakeDetected(force)
    }

    fun startListening() {
        if (shakeDetector.isAccelerometerAvailable()) {
            shakeDetector.startListening()
        }
    }

    fun stopListening() {
        shakeDetector.stopListening()
    }

    fun setCurrentScreen(screenName: String) {
        currentScreen = screenName
    }

    fun setUserEmail(email: String?) {
        userEmail = email
    }

    private fun onShakeDetected(force: Float) {
        viewModelScope.launch {
            // Send telemetry to backend
            telemetryRepository.reportBug(
                message = "User Shake Report",
                feature = "shake_report",
                action = "device_shake_detected"
            )
            
            val bugReportData = generateBugReportData()
            _state.value = ShakeDetectorState.ShakeDetected(bugReportData)
        }
    }

    fun dismissDialog() {
        _state.value = ShakeDetectorState.Idle
    }

    fun confirmReport(bugReportData: BugReportData) {
        EmailIntentHelper.sendBugReport(context, bugReportData)
        _state.value = ShakeDetectorState.ReportSent
        
        // Reset to idle after a short delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _state.value = ShakeDetectorState.Idle
        }
    }

    private fun generateBugReportData(): BugReportData {
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }

        val versionName = packageInfo?.versionName ?: "Unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toInt() ?: 0
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode ?: 0
        }

        return BugReportData(
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = versionName,
            appVersionCode = versionCode,
            timestamp = EmailIntentHelper.formatTimestamp(System.currentTimeMillis()),
            currentScreen = currentScreen,
            userEmail = userEmail
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
