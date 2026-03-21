package com.calico.tutor.data.dto.request

data class BugReportRequest(
    val type: String, // CRASH, BUG, or LATENCY
    val message: String,
    val deviceModel: String,
    val timestamp: Long = System.currentTimeMillis()
)
