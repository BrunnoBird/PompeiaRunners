package com.example.pompeiarunners.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

class DatabaseUnavailableException(message: String = "Database unavailable") : Exception(message)
class ProfileSyncException(message: String = "Profile sync failed") : Exception(message)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<JsonConvertException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_body"))
        }
        exception<DatabaseUnavailableException> { call, _ ->
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "database_unavailable"))
        }
        exception<ProfileSyncException> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "profile_sync_failed"))
        }
    }
}
