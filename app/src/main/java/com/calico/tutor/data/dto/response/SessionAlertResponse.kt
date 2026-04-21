package com.calico.tutor.data.dto.response

/**
 * Response object for session alert polling
 * Contains information about upcoming sessions
 */
data class SessionAlertResponse(
    val hasAlert: Boolean,
    val studentName: String? = null,
    val minutesToStart: Int? = null,
    val sessionId: String? = null
)
