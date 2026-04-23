package com.calico.tutor.domain.model

data class AvailabilityItem(
    val id: Int,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val description: String?,
    val course: String?
)
