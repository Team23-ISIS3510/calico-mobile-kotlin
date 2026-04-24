package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class AvailabilityResponse(
    @SerializedName("id")        val id: String = "",
    @SerializedName("title")     val title: String = "",
    @SerializedName("date")      val date: String = "",
    @SerializedName("startTime") val startTime: String = "",
    @SerializedName("endTime")   val endTime: String = "",
    @SerializedName("location")  val location: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("course")    val course: String? = null
)

// Wrapper for POST/PUT /availability responses
data class AvailabilityMutationResponse(
    @SerializedName("success")  val success: Boolean = false,
    @SerializedName("message")  val message: String? = null,
    @SerializedName("availability") val availability: AvailabilityResponse? = null
)

// Wrapper for GET /availability - backend returns an object, not a plain array
data class AvailabilityListResponse(
    @SerializedName("data")           val data: List<AvailabilityResponse>? = null,
    @SerializedName("availabilities") val availabilities: List<AvailabilityResponse>? = null,
    @SerializedName("items")          val items: List<AvailabilityResponse>? = null,
    @SerializedName("results")        val results: List<AvailabilityResponse>? = null,
    @SerializedName("success")        val success: Boolean = true
) {
    fun getList(): List<AvailabilityResponse> =
        data ?: availabilities ?: items ?: results ?: emptyList()
}
