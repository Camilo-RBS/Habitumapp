package com.wayneenterprises.habitum.ui.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.wayneenterprises.habitum.model.Reminder
import com.wayneenterprises.habitum.model.ReminderStatus
import com.wayneenterprises.habitum.model.Task
import com.wayneenterprises.habitum.model.TaskPriority
import com.wayneenterprises.habitum.model.User
import com.wayneenterprises.habitum.services.StepCounterService
import com.wayneenterprises.habitum.viewmodel.AuthViewModel
import com.wayneenterprises.habitum.viewmodel.ReminderViewModel
import com.wayneenterprises.habitum.viewmodel.StepViewModel
import com.wayneenterprises.habitum.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    taskViewModel: TaskViewModel = viewModel(),
    reminderViewModel: ReminderViewModel = viewModel(),
    stepViewModel: StepViewModel = StepViewModel.getInstance(),
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        println("🏠 HomeScreen - Conectando con servicio global")

        StepCounterService.updateViewModel(stepViewModel)

        if (!StepCounterService.isActive()) {
            println("🔄 HomeScreen - Reactivando servicio")
            StepCounterService.startDetection()
        }

        println("📊 HomeScreen - Status del servicio: ${StepCounterService.getStatus()}")

        stepViewModel.initializeDailySteps()
    }

    // OBSERVAR DATOS GLOBALES PERSISTENTES
    val tasks by taskViewModel.tasks.collectAsState()
    val reminders by reminderViewModel.reminders.collectAsState()
    val dailySteps by stepViewModel.dailySteps.collectAsState() // ✅ Datos GLOBALES
    val dailyGoal by stepViewModel.dailyGoal.collectAsState()   // ✅ Datos GLOBALES
    val isLoading by stepViewModel.isLoading.collectAsState()   // ✅ Estado de carga
    val authState by authViewModel.uiState.collectAsState()

    val upcomingTasks = remember(tasks) {
        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val nextWeek = calendar.time

        tasks

            .filter { task ->
                task.dueDate?.let { dueDate ->
                    dueDate >= now && dueDate <= nextWeek
                } ?: true
            }
            .sortedBy { it.dueDate ?: Date(Long.MAX_VALUE) }
            .take(3)
    }

    val todayReminders = remember(reminders) {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        calendar.add(Calendar.HOUR_OF_DAY, -1)
        val oneHourAgo = calendar.timeInMillis

        calendar.time = Date()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        reminders
            .filter { it.dateTime >= oneHourAgo && it.dateTime <= endOfDay }
            .sortedBy { it.dateTime }
            .take(3)
    }

    Scaffold(
        topBar = {
            CustomTopBarWithMenu(
                user = authState.currentUser,
                onSignOut = {
                    println("🚪 HomeScreen - Solicitando cierre de sesión")
                    authViewModel.signOut()
                }
            )
        },
        containerColor = Color(0xFFF8F9FA),
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // 🔄 Indicador de carga global
                if (isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF8B5CF6)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sincronizando pasos...",
                                color = Color(0xFF8B5CF6),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            item {
                // STEPS CARD CON DATOS GLOBALES REALES
                StepsCardPersistent(
                    steps = dailySteps,    // ✅ Usar datos globales
                    goal = dailyGoal,      // ✅ Usar datos globales
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SectionHeader(
                    title = "Próximas Tareas (${upcomingTasks.size})",
                    actionText = "Ver Todas",
                    onActionClick = {
                        navController.navigate("tasks")
                    }
                )
            }

            item {
                if (upcomingTasks.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        upcomingTasks.forEach { task ->
                            TaskCardSimple(
                                task = task,
                                onToggle = {
                                    taskViewModel.toggleTaskCompleted(task.id)
                                },
                                onTaskClick = {
                                    navController.navigate("task_detail?selectedCategory=${task.category}")
                                }
                            )
                        }
                    }
                } else {
                    val fallbackTasks = tasks.filter { !it.isCompleted }.take(3)
                    if (fallbackTasks.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fallbackTasks.forEach { task ->
                                TaskCardSimple(
                                    task = task,
                                    onToggle = {
                                        taskViewModel.toggleTaskCompleted(task.id)
                                    },
                                    onTaskClick = {
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
                SectionHeader(
                    title = "Recordatorios de Hoy (${todayReminders.size})",
                    actionText = "Gestionar",
                    onActionClick = {
                        navController.navigate("reminders")
                    }
                )
            }

            item {
                if (todayReminders.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        todayReminders.forEach { reminder ->
                            ReminderCardSimple(
                                reminder = reminder,
                                onComplete = {
                                    reminderViewModel.completeReminder(reminder.id)
                                },
                                onReminderClick = {
                                    navController.navigate("reminders")
                                }
                            )
                        }
                    }
                } else {
                    val fallbackReminders = reminders.take(3)
                    if (fallbackReminders.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fallbackReminders.forEach { reminder ->
                                ReminderCardSimple(
                                    reminder = reminder,
                                    onComplete = {
                                        reminderViewModel.completeReminder(reminder.id)
                                    },
                                    onReminderClick = {
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

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTopBarWithMenu(
    user: User?,
    onSignOut: () -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Salud Diaria",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (user != null) "¡Hola, ${user.name}!" else "¡Bienvenido!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        },
        actions = {
            Box(
                modifier = Modifier.padding(end = 16.dp) // Separación del borde derecho
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .clickable { showDropdownMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (user != null && user.name.isNotEmpty()) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 18.sp, // Texto más grande para mejor balance
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // MENÚ DESPLEGABLE MEJORADO
                DropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = { showDropdownMenu = false },
                    modifier = Modifier.background(
                        Color.White
                    )
                ) {
                    // Información del usuario
                    if (user != null) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Text(
                                text = user.email,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color.Gray.copy(alpha = 0.2f)
                        )
                    }

                    // Opción de cerrar sesión
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚪",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Cerrar sesión",
                                    color = Color(0xFF8B5CF6),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onSignOut()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.background(
            Brush.horizontalGradient(
                colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
            )
        )
    )
}

@Composable
private fun StepsCardPersistent(
    steps: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).toInt()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseInOutCubic
        ), label = ""
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f))
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6))
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )

            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👣",
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Pasos de Hoy",
                        fontSize = 16.sp,
                        color = Color(0xFF4B5563),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = String.format("%,d", steps),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5CF6)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF8B5CF6),
                                        Color(0xFFEC4899),
                                        Color(0xFFF97316)
                                    )
                                ),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Meta: ${String.format("%,d", goal)} pasos",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "$percentage% completado",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCardSimple(
    task: Task,
    onToggle: () -> Unit,
    onTaskClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        when (task.priority) {
                            TaskPriority.HIGH -> Color(0xFFEC4899)
                            TaskPriority.MEDIUM -> Color(0xFFF97316)
                            TaskPriority.LOW -> Color(0xFF8B5CF6)
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
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (task.isCompleted) Color.Gray else Color.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(
                        Color(0xFF8B5CF6).copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = task.category,
                    color = Color(0xFF8B5CF6),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ReminderCardSimple(
    reminder: Reminder,
    onComplete: () -> Unit,
    onReminderClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReminderClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    tint = Color(0xFFEC4899),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = reminder.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatTime(reminder.dateTime),
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }

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
                        Color(0xFFE0E0E0),
                    modifier = Modifier.size(20.dp)
                )
            }
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        TextButton(onClick = onActionClick) {
            Text(
                text = "$actionText >",
                color = Color(0xFF8B5CF6),
                fontSize = 14.sp
            )
        }
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
                .padding(20.dp),
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