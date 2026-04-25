package com.example.pompeiarunners.plugins

import com.example.pompeiarunners.config.AppConfig
import com.example.pompeiarunners.db.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase(config: AppConfig) {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.databaseUrl
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 5
    }
    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)
    transaction {
        SchemaUtils.create(UsersTable)
        exec("""
            ALTER TABLE public.users
            ADD CONSTRAINT IF NOT EXISTS users_role_check CHECK (role IN ('runner', 'coach', 'admin')),
            ADD CONSTRAINT IF NOT EXISTS users_status_check CHECK (status IN ('pending', 'approved'))
        """)
    }
    log.info("Database connected")
}
