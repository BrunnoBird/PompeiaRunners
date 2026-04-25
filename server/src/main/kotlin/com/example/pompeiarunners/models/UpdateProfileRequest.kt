package com.example.pompeiarunners.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
)
