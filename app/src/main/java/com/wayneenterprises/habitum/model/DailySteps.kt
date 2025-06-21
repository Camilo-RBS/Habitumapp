package com.wayneenterprises.habitum.model

data class DailySteps(
    val id: String,
    val date: String,
    val steps: Int,
    val userId: String
)