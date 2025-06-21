package com.wayneenterprises.habitum.model

data class DailySteps(
    val id: String,
    val date: String, // formato: "2024-12-19"
    val steps: Int,
    val userId: String
)