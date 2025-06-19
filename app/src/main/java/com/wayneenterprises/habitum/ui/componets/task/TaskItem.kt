package com.wayneenterprises.habitum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayneenterprises.habitum.model.Task
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h a", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (task.isCompleted) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and check button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    if (task.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completada",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    Color(0xFF5C6BC0),
                                    RoundedCornerShape(50)
                                )
                                .padding(2.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    Color.Transparent,
                                    RoundedCornerShape(50)
                                )
                                .background(
                                    Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getCategoryColor(task.category),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = task.category,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = getCategoryTextColor(task.category),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Due date and priority row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Due date
                task.dueDate?.let { date ->
                    val today = Calendar.getInstance()
                    val taskDate = Calendar.getInstance().apply { time = date }

                    val dateText = when {
                        isSameDay(today, taskDate) -> "Today, ${timeFormat.format(date)}"
                        isNextDay(today, taskDate) -> "Tomorrow, ${timeFormat.format(date)}"
                        else -> dateFormat.format(date)
                    }

                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                // Priority
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priority: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = task.priority.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = task.priority.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "work" -> Color(0xFFE3F2FD)
        "entertainment" -> Color(0xFFF5E8ED)
        "study" -> Color(0xFFF3E5F5)
        "personal" -> Color(0xFFFFF3E0)
        else -> Color(0xFFF5F5F5)
    }
}

@Composable
private fun getCategoryTextColor(category: String): Color {
    return when (category.lowercase()) {
        "work" -> Color(0xFF1976D2)
        "entertainment" -> Color(0xFFE91E63)
        "study" -> Color(0xFF7B1FA2)
        "personal" -> Color(0xFFF57C00)
        else -> Color(0xFF616161)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isNextDay(today: Calendar, other: Calendar): Boolean {
    val tomorrow = today.clone() as Calendar
    tomorrow.add(Calendar.DAY_OF_YEAR, 1)
    return isSameDay(tomorrow, other)
}