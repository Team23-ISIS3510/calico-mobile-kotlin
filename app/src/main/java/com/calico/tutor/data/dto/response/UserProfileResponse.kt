package com.calico.tutor.data.dto.response

import com.google.gson.annotations.SerializedName

data class UserProfileResponse(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("profileImage")
    val profileImage: String? = null,
    @SerializedName("profilePictureUrl")
    val profilePictureUrl: String? = null,
    @SerializedName(value = "avatarUrl", alternate = ["avatar", "photoURL", "photoUrl"])
    val avatarUrl: String? = null,
    @SerializedName("isTutor")
    val isTutor: Boolean = false
)