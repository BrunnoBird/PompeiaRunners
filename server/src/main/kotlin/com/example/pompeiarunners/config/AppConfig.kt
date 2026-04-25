package com.example.pompeiarunners.config

data class AppConfig(
    val databaseUrl: String,
    val jwtSecret: String,
) {
    companion object {
        fun load(): AppConfig {
            val databaseUrl = System.getenv("DATABASE_URL")
                ?: System.getProperty("DATABASE_URL")
                ?: error("Missing required environment variable: DATABASE_URL")
            val jwtSecret = System.getenv("SUPABASE_JWT_SECRET")
                ?: System.getProperty("SUPABASE_JWT_SECRET")
                ?: error("Missing required environment variable: SUPABASE_JWT_SECRET")
            return AppConfig(databaseUrl, jwtSecret)
        }
    }
}
