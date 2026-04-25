package com.example.pompeiarunners.repositories

import com.example.pompeiarunners.db.UsersTable
import com.example.pompeiarunners.models.UserRow
import com.example.pompeiarunners.plugins.ProfileSyncException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID
import kotlinx.datetime.Instant

class UserRepository {
    suspend fun upsertIfAbsent(id: UUID, email: String): UserRow {
        newSuspendedTransaction {
            UsersTable.insertIgnore {
                it[UsersTable.id] = id
                it[UsersTable.email] = email
                it[UsersTable.createdAt] = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            }
        }
        val row = newSuspendedTransaction {
            UsersTable.selectAll().toList().firstOrNull { it[UsersTable.id] == id }
        } ?: throw ProfileSyncException()
        return row.toUserRow()
    }

    suspend fun findById(id: UUID): UserRow? {
        return newSuspendedTransaction {
            UsersTable.selectAll().toList().firstOrNull { it[UsersTable.id] == id }?.toUserRow()
        }
    }

    suspend fun updateProfile(id: UUID, name: String?, phone: String?, photoUrl: String?): UserRow {
        newSuspendedTransaction {
            UsersTable.update({ UsersTable.id.eq(id) }) {
                if (name != null) it[UsersTable.name] = name
                if (phone != null) it[UsersTable.phone] = phone
                if (photoUrl != null) it[UsersTable.photoUrl] = photoUrl
            }
        }
        return findById(id)!!
    }

    suspend fun findPendingUsers(): List<UserRow> {
        return newSuspendedTransaction {
            UsersTable.selectAll()
                .toList()
                .filter { it[UsersTable.status] == "pending" }
                .map { it.toUserRow() }
        }
    }

    suspend fun approveUser(id: UUID): UserRow? {
        newSuspendedTransaction {
            UsersTable.update({ UsersTable.id.eq(id) }) {
                it[UsersTable.status] = "approved"
            }
        }
        return findById(id)
    }

    private fun ResultRow.toUserRow() = UserRow(
        id = this[UsersTable.id],
        name = this[UsersTable.name],
        email = this[UsersTable.email],
        phone = this[UsersTable.phone],
        photoUrl = this[UsersTable.photoUrl],
        role = this[UsersTable.role],
        status = this[UsersTable.status],
        createdAt = this[UsersTable.createdAt],
    )
}
