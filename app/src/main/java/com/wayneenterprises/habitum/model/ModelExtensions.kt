package com.wayneenterprises.habitum.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// Actualizar el modelo User para que coincida con tu esquema
@Serializable
data class UserWithAuthId(
    val id: String = "",
    @SerialName("auth_id") val authId: String = "", // Ahora como String para compatibilidad
    val name: String = "",
    val email: String = "",
    @SerialName("user_type") val userType: String = "normal",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class UserInsertWithAuthId(
    @SerialName("auth_id") val authId: String, // String UUID
    val name: String,
    val email: String,
    @SerialName("user_type") val userType: String = "normal"
)

// Extensiones para convertir entre modelos locales y Supabase
fun Task.toSupabaseInsert(userId: String): SupabaseTaskInsert {
    return SupabaseTaskInsert(
        userId = userId,
        title = title,
        description = description,
        category = category,
        priority = priority.name,
        isCompleted = isCompleted,
        dueDate = dueDate?.let {
            java.time.Instant.ofEpochMilli(it.time).toString()
        },
        completionDate = completionDate?.let {
            java.time.Instant.ofEpochMilli(it.time).toString()
        }
    )
}

fun SupabaseTask.toTask(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        category = category,
        priority = when(priority) {
            "HIGH" -> TaskPriority.HIGH
            "MEDIUM" -> TaskPriority.MEDIUM
            else -> TaskPriority.LOW
        },
        isCompleted = isCompleted,
        dueDate = dueDate?.let {
            try {
                java.util.Date.from(java.time.Instant.parse(it))
            } catch (e: Exception) {
                null
            }
        },
        completionDate = completionDate?.let {
            try {
                java.util.Date.from(java.time.Instant.parse(it))
            } catch (e: Exception) {
                null
            }
        }
    )
}

fun Reminder.toSupabaseInsert(userId: String): SupabaseReminderInsert {
    return SupabaseReminderInsert(
        userId = userId,
        title = title,
        description = description,
        dateTime = java.time.Instant.ofEpochMilli(dateTime).toString(),
        status = status.name,
        type = type.name
    )
}

fun SupabaseReminder.toReminder(): Reminder {
    return Reminder(
        id = id,
        title = title,
        description = description,
        dateTime = try {
            java.time.Instant.parse(dateTime).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        },
        status = when(status) {
            "COMPLETED" -> ReminderStatus.COMPLETED
            "MISSED" -> ReminderStatus.MISSED
            "OMITTED" -> ReminderStatus.OMITTED
            else -> ReminderStatus.PENDING
        },
        type = when(type) {
            "WATER" -> ReminderType.WATER
            "EXERCISE" -> ReminderType.EXERCISE
            "REST" -> ReminderType.REST
            "MEDICINE" -> ReminderType.MEDICINE
            else -> ReminderType.GENERAL
        }
    )
}

// Extensiones para convertir entre User y UserWithAuthId
fun UserWithAuthId.toUser(): User {
    return User(
        id = id,
        authId = authId,
        name = name,
        email = email,
        userType = userType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun User.toUserInsertWithAuthId(): UserInsertWithAuthId {
    return UserInsertWithAuthId(
        authId = authId,
        name = name,
        email = email,
        userType = userType
    )

}