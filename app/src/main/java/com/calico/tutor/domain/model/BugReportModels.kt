package com.calico.tutor.domain.model

data class ShakeEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val force: Float = 0f
)

data class BugReportData(
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val appVersionCode: Int,
    val timestamp: String,
    val currentScreen: String,
    val userEmail: String?
)

sealed class ShakeDetectorState {
    data object Idle : ShakeDetectorState()
    data class ShakeDetected(val bugReportData: BugReportData) : ShakeDetectorState()
    data object ReportSent : ShakeDetectorState()
}
