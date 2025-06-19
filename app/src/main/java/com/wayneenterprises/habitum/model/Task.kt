package com.wayneenterprises.habitum.model

import java.util.Date

enum class TaskPriority(val displayName: String, val color: androidx.compose.ui.graphics.Color) {
    LOW("Low", androidx.compose.ui.graphics.Color(0xFF4CAF50)),
    MEDIUM("Medium", androidx.compose.ui.graphics.Color(0xFFFF9800)),
    HIGH("High", androidx.compose.ui.graphics.Color(0xFFF44336))
}

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: TaskPriority = TaskPriority.LOW,
    val isCompleted: Boolean = false,
    val dueDate: Date? = null,
    val completionDate: Date? = null
)