package com.example.pompeiarunners.routes

import com.example.pompeiarunners.repositories.UserRepository
import com.example.pompeiarunners.utils.currentUser
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

suspend fun ApplicationCall.requireRole(repo: UserRepository, vararg allowed: String): Boolean {
    val user = currentUser(repo)
    if (user.role !in allowed) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "insufficient_role"))
        return false
    }
    return true
}
