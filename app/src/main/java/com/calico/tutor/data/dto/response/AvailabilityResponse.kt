package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class AvailabilityResponse(
    @SerializedName("id")        val id: Int,
    @SerializedName("title")     val title: String,
    @SerializedName("date")      val date: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime")   val endTime: String,
    @SerializedName("location")  val location: String = "Online",
    @SerializedName("description") val description: String? = null,
    @SerializedName("course")    val course: String? = null
)
