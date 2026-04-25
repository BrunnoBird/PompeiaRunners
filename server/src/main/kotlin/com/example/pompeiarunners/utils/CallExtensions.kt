package com.example.pompeiarunners.utils

import com.example.pompeiarunners.models.UserRow
import com.example.pompeiarunners.repositories.UserRepository
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.util.AttributeKey
import java.util.UUID

private val UserRowKey = AttributeKey<UserRow>("UserRow")

suspend fun ApplicationCall.currentUser(repo: UserRepository): UserRow {
    return attributes.getOrNull(UserRowKey) ?: run {
        val principal = principal<JWTPrincipal>()!!
        val sub = UUID.fromString(principal.payload.subject)
        val email = principal.payload.getClaim("email")?.asString() ?: ""
        val user = repo.upsertIfAbsent(sub, email)
        attributes.put(UserRowKey, user)
        user
    }
}
