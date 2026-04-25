package com.example.pompeiarunners.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : Table("public.users") {
    val id = uuid("id")
    val name = text("name").nullable()
    val email = text("email")
    val phone = text("phone").nullable()
    val photoUrl = text("photo_url").nullable()
    val role = text("role").default("runner")
    val status = text("status").default("pending")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
