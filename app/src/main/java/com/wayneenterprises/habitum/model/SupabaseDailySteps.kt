package com.wayneenterprises.habitum.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseDailySteps(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val date: String = "", // formato: "2024-12-19"
    val steps: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class SupabaseDailyStepsInsert(
    @SerialName("user_id") val userId: String,
    val date: String,
    val steps: Int
)

@Serializable
data class SupabaseDailyStepsUpdate(
    val steps: Int,
    @SerialName("updated_at") val updatedAt: String
)

// Extensiones para conversión
fun DailySteps.toSupabaseInsert(): SupabaseDailyStepsInsert {
    return SupabaseDailyStepsInsert(
        userId = userId,
        date = date,
        steps = steps
    )
}

fun SupabaseDailySteps.toDailySteps(): DailySteps {
    return DailySteps(
        id = id,
        date = date,
        steps = steps,
        userId = userId
    )
}