package com.calico.tutor.domain.model

data class AvailabilityItem(
    val id: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val location: String? = null,
    val description: String? = null,
    val course: String? = null
)
