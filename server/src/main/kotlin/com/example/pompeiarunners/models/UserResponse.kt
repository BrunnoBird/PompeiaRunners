package com.example.pompeiarunners.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserResponse(
    val id: String,
    val name: String?,
    val email: String,
    val phone: String?,
    @SerialName("photo_url") val photoUrl: String?,
    val role: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

fun UserRow.toResponse() = UserResponse(
    id = id.toString(),
    name = name,
    email = email,
    phone = phone,
    photoUrl = photoUrl,
    role = role,
    status = status,
    createdAt = createdAt.toString(),
)
