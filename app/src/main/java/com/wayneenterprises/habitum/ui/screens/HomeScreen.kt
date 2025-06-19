package com.wayneenterprises.habitum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.wayneenterprises.habitum.model.*
import com.wayneenterprises.habitum.viewmodel.*
import com.wayneenterprises.habitum.ui.components.ScreenHeader
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    navController: NavController,
    taskViewModel: TaskViewModel = viewModel(),
    reminderViewModel: ReminderViewModel = viewModel(),
    stepViewModel: StepViewModel = viewModel(),
    authViewModel: AuthViewModel
) {
    // Observar datos directamente de los ViewModels existentes
    val tasks by taskViewModel.tasks.collectAsState()
    val reminders by reminderViewModel.reminders.collectAsState()
    val dailySteps by stepViewModel.dailySteps.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    // Procesar datos para la HomeScreen - FILTROS CORREGIDOS
    val upcomingTasks = remember(tasks) {
        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.add(Calendar.DAY_OF_YEAR, 7) // Próximos 7 días en lugar de solo mañana
        val nextWeek = calendar.time

        tasks
            .filter { !it.isCompleted }
            .filter { task ->
                task.dueDate?.let { dueDate ->
                    dueDate >= now && dueDate <= nextWeek
                } ?: true // Incluir tareas SIN fecha también
            }
            .sortedBy { it.dueDate ?: Date(Long.MAX_VALUE) } // Tareas sin fecha al final
            .take(3)
    }

    val todayReminders = remember(reminders) {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Ampliar rango: desde hace 1 hora hasta 23:59 de hoy
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        val oneHourAgo = calendar.timeInMillis

        calendar.time = Date() // Reset
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        reminders
            .filter { it.dateTime >= oneHourAgo && it.dateTime <= endOfDay }
            // CAMBIO: No filtrar por status PENDING, mostrar todos (incluso completados)
            .sortedBy { it.dateTime }
            .take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header con perfil de usuario
        ScreenHeader(
            title = "Salud Diaria",
            user = authState.currentUser,
            onSignOut = {
                println("🚪 HomeScreen - Solicitando cierre de sesión")
                authViewModel.signOut()
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                // Steps Today Card
                StepsCard(
                    steps = dailySteps,
                    goal = 10000,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // Upcoming Tasks Section
                SectionHeader(
                    title = "Próximas Tareas (${upcomingTasks.size})",
                    actionText = "Ver Todas",
                    onActionClick = {
                        println("🔗 HomeScreen - Navegando a tasks")
                        navController.navigate("tasks")
                    }
                )
            }

            item {
                // Tasks List - MOSTRAR SIEMPRE AL MENOS LAS PRIMERAS 3 TAREAS
                if (upcomingTasks.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        upcomingTasks.forEach { task ->
                            TaskCard(
                                task = task,
                                onToggle = {
                                    println("🔄 HomeScreen - Toggling tarea: ${task.id}")
                                    taskViewModel.toggleTaskCompleted(task.id)
                                },
                                onTaskClick = {
                                    println("🔗 HomeScreen - Navegando a categoría de tarea: ${task.category}")
                                    navController.navigate("task_detail?selectedCategory=${task.category}")
                                }
                            )
                        }
                    }
                } else {
                    // Si no hay tareas próximas, mostrar las primeras 3 tareas pendientes
                    val fallbackTasks = tasks.filter { !it.isCompleted }.take(3)
                    if (fallbackTasks.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fallbackTasks.forEach { task ->
                                TaskCard(
                                    task = task,
                                    onToggle = {
                                        println("🔄 HomeScreen - Toggling tarea: ${task.id}")
                                        taskViewModel.toggleTaskCompleted(task.id)
                                    },
                                    onTaskClick = {
                                        println("🔗 HomeScreen - Navegando a categoría de tarea: ${task.category}")
                                        navController.navigate("task_detail?selectedCategory=${task.category}")
                                    }
                                )
                            }
                        }
                    } else {
                        EmptyStateCard("No hay tareas pendientes")
                    }
                }
            }

            item {
                // Daily Reminders Section
                SectionHeader(
                    title = "Recordatorios de Hoy (${todayReminders.size})",
                    actionText = "Gestionar",
                    onActionClick = {
                        println("🔗 HomeScreen - Navegando a reminders")
                        navController.navigate("reminders")
                    }
                )
            }

            item {
                // Reminders List - MOSTRAR SIEMPRE AL MENOS LOS PRIMEROS 3 RECORDATORIOS
                if (todayReminders.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        todayReminders.forEach { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onComplete = {
                                    println("✅ HomeScreen - Marcando recordatorio como completado: ${reminder.id}")
                                    // Solo marcar como completado, NO remover de la lista
                                    reminderViewModel.completeReminder(reminder.id)
                                },
                                onReminderClick = {
                                    println("🔗 HomeScreen - Navegando a pantalla de recordatorios")
                                    navController.navigate("reminders")
                                }
                            )
                        }
                    }
                } else {
                    // Si no hay recordatorios de hoy, mostrar los primeros 3 recordatorios (sin filtrar por status)
                    val fallbackReminders = reminders.take(3)
                    if (fallbackReminders.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fallbackReminders.forEach { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    onComplete = {
                                        println("✅ HomeScreen - Marcando recordatorio como completado: ${reminder.id}")
                                        reminderViewModel.completeReminder(reminder.id)
                                    },
                                    onReminderClick = {
                                        println("🔗 HomeScreen - Navegando a pantalla de recordatorios")
                                        navController.navigate("reminders")
                                    }
                                )
                            }
                        }
                    } else {
                        EmptyStateCard("No hay recordatorios")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsCard(
    steps: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).toInt()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF2196F3), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👣",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Steps Today",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = String.format("%,d", steps),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color(0xFFFF9800), RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Goal: ${String.format("%,d", goal)} steps ($percentage% complete)",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        TextButton(onClick = onActionClick) {
            Text(
                text = "$actionText >",
                color = Color(0xFF2196F3),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onTaskClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox con color de la importancia
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (task.isCompleted) {
                            // Si está completada, usar el color de prioridad con opacidad
                            when (task.priority) {
                                TaskPriority.HIGH -> Color(0xFFE53E3E).copy(alpha = 0.7f)
                                TaskPriority.MEDIUM -> Color(0xFFFF9800).copy(alpha = 0.7f)
                                TaskPriority.LOW -> Color(0xFF4CAF50).copy(alpha = 0.7f)
                            }
                        } else {
                            // Si está pendiente, usar color sólido de la prioridad
                            when (task.priority) {
                                TaskPriority.HIGH -> Color(0xFFE53E3E)
                                TaskPriority.MEDIUM -> Color(0xFFFF9800)
                                TaskPriority.LOW -> Color(0xFF4CAF50)
                            }
                        },
                        CircleShape
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // SOLO EL NOMBRE A LA IZQUIERDA
            Text(
                text = task.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (task.isCompleted) Color.Gray else Color.Black,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(12.dp))

            // SOLO CATEGORÍA A LA DERECHA
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFF6366F1).copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = task.category,
                    color = Color(0xFF6366F1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onComplete: () -> Unit, // Cambiar de onDismiss a onComplete
    onReminderClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReminderClick() }, // Hacer clickeable toda la tarjeta
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFFEBEE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Reminder",
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = reminder.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTime(reminder.dateTime),
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }

            // Type Badge
            ReminderTypeBadge(type = reminder.type)

            Spacer(modifier = Modifier.width(8.dp))

            // CHECKBOX en lugar de X
            IconButton(
                onClick = onComplete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (reminder.status == ReminderStatus.COMPLETED)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.Circle,
                    contentDescription = if (reminder.status == ReminderStatus.COMPLETED) "Completed" else "Mark as complete",
                    tint = if (reminder.status == ReminderStatus.COMPLETED)
                        Color(0xFF4CAF50)
                    else
                        Color(0xFFE0E0E0)
                )
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: TaskPriority) {
    Box(
        modifier = Modifier
            .background(priority.color, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = priority.displayName,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ReminderTypeBadge(type: ReminderType) {
    val (color, text) = when (type) {
        ReminderType.WATER -> Color(0xFF03A9F4) to "Water"
        ReminderType.EXERCISE -> Color(0xFF4CAF50) to "Exercise"
        ReminderType.REST -> Color(0xFF9C27B0) to "Rest"
        ReminderType.MEDICINE -> Color(0xFFE91E63) to "Health"
        ReminderType.GENERAL -> Color(0xFF607D8B) to "Personal"
    }

    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            textAlign = TextAlign.Center,
            color = Color(0xFF999999),
            fontSize = 14.sp
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}