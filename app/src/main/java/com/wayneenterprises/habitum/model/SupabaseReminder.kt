package com.wayneenterprises.habitum.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SupabaseReminder(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("date_time") val dateTime: String = "", // ISO string
    val status: String = "PENDING", // "PENDING", "COMPLETED", "MISSED", "OMITTED"
    val type: String = "GENERAL", // "WATER", "EXERCISE", "REST", "MEDICINE", "GENERAL"
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class SupabaseReminderInsert(
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String,
    @SerialName("date_time") val dateTime: String,
    val status: String = "PENDING",
    val type: String = "GENERAL"
)

@Serializable
data class SupabaseReminderUpdate(
    val title: String? = null,
    val description: String? = null,
    @SerialName("date_time") val dateTime: String? = null,
    val status: String? = null,
    val type: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)