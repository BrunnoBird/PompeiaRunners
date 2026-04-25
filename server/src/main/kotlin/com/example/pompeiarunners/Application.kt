package com.example.pompeiarunners

import com.example.pompeiarunners.config.AppConfig
import com.example.pompeiarunners.plugins.configureAuth
import com.example.pompeiarunners.plugins.configureDatabase
import com.example.pompeiarunners.plugins.configureSerialization
import com.example.pompeiarunners.plugins.configureStatusPages
import com.example.pompeiarunners.repositories.UserRepository
import com.example.pompeiarunners.routes.coachRoutes
import com.example.pompeiarunners.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val config = AppConfig.load()
    val repo = UserRepository()
    configureDatabase(config)
    configureSerialization()
    configureStatusPages()
    configureAuth(config)
    configureRouting(repo)
}

fun Application.configureRouting(repo: UserRepository) {
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        userRoutes(repo)
        coachRoutes(repo)
    }
}