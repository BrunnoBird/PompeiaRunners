package com.example.pompeiarunners.routes

import com.example.pompeiarunners.models.UpdateProfileRequest
import com.example.pompeiarunners.models.toResponse
import com.example.pompeiarunners.repositories.UserRepository
import com.example.pompeiarunners.utils.currentUser
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch

fun Route.userRoutes(repo: UserRepository) {
    authenticate("supabase") {
        get("/users/me") {
            val user = call.currentUser(repo)
            call.respond(user.toResponse())
        }
        patch("/users/me") {
            val req = call.receive<UpdateProfileRequest>()
            val user = call.currentUser(repo)
            val updated = repo.updateProfile(user.id, req.name, req.phone, req.photoUrl)
            call.respond(updated.toResponse())
        }
    }
}
