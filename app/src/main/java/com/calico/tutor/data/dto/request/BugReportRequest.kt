package com.calico.tutor.data.dto.request

data class BugReportRequest(
    val type: String, // CRASH, BUG, or LATENCY
    val message: String,
    val deviceModel: String,
    val timestamp: String,
    val feature: String? = null,
    val action: String? = null,
    val networkType: String? = null,
    val endpoint: String? = null,
    val durationMs: Long? = null,
    val method: String? = null,
    val statusCode: Int? = null
)
