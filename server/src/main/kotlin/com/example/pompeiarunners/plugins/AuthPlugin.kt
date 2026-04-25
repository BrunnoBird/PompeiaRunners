package com.example.pompeiarunners.plugins

import com.example.pompeiarunners.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

fun Application.configureAuth(config: AppConfig) {
    install(Authentication) {
        jwt("supabase") {
            verifier(
                JWT.require(Algorithm.HMAC256(config.jwtSecret)).build()
            )
            validate { credential ->
                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                val errorBody = when {
                    call.request.headers["Authorization"] == null ->
                        mapOf("error" to "missing_token")
                    else ->
                        mapOf("error" to "invalid_token")
                }
                call.respond(HttpStatusCode.Unauthorized, errorBody)
            }
        }
    }
}
