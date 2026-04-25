package com.example.pompeiarunners.models

import java.util.UUID
import kotlinx.datetime.Instant

data class UserRow(
    val id: UUID,
    val name: String?,
    val email: String,
    val phone: String?,
    val photoUrl: String?,
    val role: String,
    val status: String,
    val createdAt: Instant,
)
