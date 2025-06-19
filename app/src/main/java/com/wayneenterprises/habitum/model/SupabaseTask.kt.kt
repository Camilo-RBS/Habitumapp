package com.wayneenterprises.habitum.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTask(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priority: String = "LOW", // "LOW", "MEDIUM", "HIGH"
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("due_date") val dueDate: String? = null, // ISO string
    @SerialName("completion_date") val completionDate: String? = null, // ISO string
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class SupabaseTaskInsert(
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("completion_date") val completionDate: String? = null
)

@Serializable
data class SupabaseTaskUpdate(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val priority: String? = null,
    @SerialName("is_completed") val isCompleted: Boolean? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("completion_date") val completionDate: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)