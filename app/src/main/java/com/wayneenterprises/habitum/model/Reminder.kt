package com.wayneenterprises.habitum.model

data class Reminder(
    val id: String,
    val title: String,
    val description: String,
    val dateTime: Long, // timestamp para la hora del recordatorio
    val status: ReminderStatus = ReminderStatus.PENDING,
    val type: ReminderType = ReminderType.GENERAL
)

enum class ReminderStatus {
    PENDING,
    COMPLETED,
    MISSED,
    OMITTED
}

enum class ReminderType {
    WATER,      // Agua
    EXERCISE,   // Ejercicio
    REST,       // Descanso
    MEDICINE,   // Medicina
    GENERAL     // General
}