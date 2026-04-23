package com.calico.tutor.data.dto.request

import com.google.gson.annotations.SerializedName

data class CreateAvailabilityRequest(
    @SerializedName("tutorId")   val tutorId: String,
    @SerializedName("title")     val title: String,
    @SerializedName("date")      val date: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime")   val endTime: String,
    @SerializedName("location")  val location: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("course")    val course: String? = null
)

data class UpdateAvailabilityRequest(
    @SerializedName("title")     val title: String? = null,
    @SerializedName("date")      val date: String? = null,
    @SerializedName("startTime") val startTime: String? = null,
    @SerializedName("endTime")   val endTime: String? = null,
    @SerializedName("location")  val location: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("course")    val course: String? = null
)
