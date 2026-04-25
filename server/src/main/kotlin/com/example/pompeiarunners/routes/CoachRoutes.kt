package com.example.pompeiarunners.routes

import com.example.pompeiarunners.models.toResponse
import com.example.pompeiarunners.repositories.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

fun Route.coachRoutes(repo: UserRepository) {
    authenticate("supabase") {
        get("/coach/pending-users") {
            if (!call.requireRole(repo, "coach", "admin")) return@get
            val pending = repo.findPendingUsers()
            call.respond(pending.map { it.toResponse() })
        }
        post("/users/{id}/approve") {
            if (!call.requireRole(repo, "coach", "admin")) return@post
            val id = call.parameters["id"]?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            } ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_body"))
                return@post
            }
            val updated = repo.approveUser(id)
                ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "user_not_found"))
                    return@post
                }
            call.respond(updated.toResponse())
        }
    }
}
