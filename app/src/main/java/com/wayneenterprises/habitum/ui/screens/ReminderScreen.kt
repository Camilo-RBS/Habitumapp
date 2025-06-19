package com.wayneenterprises.habitum.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wayneenterprises.habitum.model.ReminderType
import com.wayneenterprises.habitum.ui.componets.ReminderCard
import com.wayneenterprises.habitum.ui.components.ScreenHeader
import com.wayneenterprises.habitum.viewmodel.ReminderViewModel
import com.wayneenterprises.habitum.viewmodel.AuthViewModel
import java.util.Calendar

@Composable
fun ReminderScreen(
    selectedReminderId: String = "", // Nuevo parámetro para navegación específica
    reminderViewModel: ReminderViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val reminders by reminderViewModel.reminders.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val isLoading by reminderViewModel.isLoading.collectAsState()
    val error by reminderViewModel.error.collectAsState()
    val context = LocalContext.current

    var showForm by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<String?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedType by remember { mutableStateOf(ReminderType.GENERAL) }

    // Manejar navegación específica a un recordatorio
    LaunchedEffect(selectedReminderId, reminders) {
        if (selectedReminderId.isNotEmpty() && reminders.isNotEmpty()) {
            println("🎯 ReminderScreen - Buscando recordatorio específico: $selectedReminderId")
            val selectedReminder = reminders.find { it.id == selectedReminderId }
            selectedReminder?.let { reminder ->
                println("✅ ReminderScreen - Recordatorio encontrado: ${reminder.title}")
                title = reminder.title
                description = reminder.description
                selectedDateTime = reminder.dateTime
                selectedType = reminder.type
                editingReminder = selectedReminderId
                showForm = true
            } ?: run {
                println("❌ ReminderScreen - Recordatorio no encontrado con ID: $selectedReminderId")
            }
        }
    }

    // Reset form when closing
    LaunchedEffect(showForm) {
        if (!showForm) {
            title = ""
            description = ""
            selectedDateTime = System.currentTimeMillis()
            selectedType = ReminderType.GENERAL
            editingReminder = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header con perfil de usuario
        ScreenHeader(
            title = "Recordatorios",
            user = authState.currentUser,
            onSignOut = {
                println("🚪 ReminderScreen - Solicitando cierre de sesión")
                authViewModel.signOut()
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    // Tarjeta de bienvenida
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFE8A1A1),
                                            Color(0xFFD4A574)
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Tu Bienestar, Prioridad",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Diaria",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Establece recordatorios en sincronización con tu cuerpo. Pequeños pasos, grandes resultados.",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        println("➕ ReminderScreen - Abriendo formulario para nuevo recordatorio")
                                        showForm = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text(
                                        text = "Añadir Nuevo Recordatorio",
                                        color = Color(0xFFE8A1A1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Loading indicator
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cargando recordatorios...")
                            }
                        }
                    }
                }

                item {
                    // Error message
                    error?.let { errorMessage ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "❌ Error:",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = errorMessage,
                                    color = Color.Red
                                )
                                Button(
                                    onClick = {
                                        reminderViewModel.clearError()
                                        reminderViewModel.refreshReminders()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                }

                item {
                    // Título de la sección
                    Text(
                        text = "Recordatorios de Hoy (${reminders.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (!isLoading && error == null) {
                    if (reminders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🔔",
                                        fontSize = 48.sp
                                    )
                                    Text(
                                        text = "No hay recordatorios",
                                        fontSize = 18.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "¡Crea tu primer recordatorio!",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    } else {
                        items(reminders) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onComplete = {
                                    println("✅ ReminderScreen - Completando recordatorio: $it")
                                    reminderViewModel.completeReminder(it)
                                },
                                onOmit = {
                                    println("⏭️ ReminderScreen - Omitiendo recordatorio: $it")
                                    reminderViewModel.omitReminder(it)
                                },
                                onEdit = { reminderId ->
                                    println("✏️ ReminderScreen - Editando recordatorio: $reminderId")
                                    val reminderToEdit = reminders.find { it.id == reminderId }
                                    reminderToEdit?.let {
                                        title = it.title
                                        description = it.description
                                        selectedDateTime = it.dateTime
                                        selectedType = it.type
                                        editingReminder = reminderId
                                        showForm = true
                                    }
                                },
                                onDelete = {
                                    println("🗑️ ReminderScreen - Eliminando recordatorio: $it")
                                    reminderViewModel.deleteReminder(it)
                                }
                            )
                        }
                    }
                }
            }

            // Dialog del formulario
            if (showForm) {
                Dialog(onDismissRequest = {
                    println("❌ ReminderScreen - Cerrando formulario")
                    showForm = false
                }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (editingReminder != null) "Editar Recordatorio" else "Nuevo Recordatorio",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { showForm = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Título") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Descripción") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Selector de tipo
                            Text(
                                text = "Tipo de recordatorio",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Primera fila con 3 tipos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(ReminderType.WATER, ReminderType.EXERCISE, ReminderType.REST).forEach { type ->
                                    FilterChip(
                                        onClick = { selectedType = type },
                                        label = {
                                            Text(
                                                when (type) {
                                                    ReminderType.WATER -> "Agua"
                                                    ReminderType.EXERCISE -> "Ejercicio"
                                                    ReminderType.REST -> "Descanso"
                                                    else -> ""
                                                },
                                                fontSize = 12.sp
                                            )
                                        },
                                        selected = selectedType == type,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Segunda fila con 2 tipos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(ReminderType.MEDICINE, ReminderType.GENERAL).forEach { type ->
                                    FilterChip(
                                        onClick = { selectedType = type },
                                        label = {
                                            Text(
                                                when (type) {
                                                    ReminderType.MEDICINE -> "Medicina"
                                                    ReminderType.GENERAL -> "General"
                                                    else -> ""
                                                },
                                                fontSize = 12.sp
                                            )
                                        },
                                        selected = selectedType == type,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Spacer para balancear la fila
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    val dateDialog = DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val timeDialog = TimePickerDialog(
                                                context,
                                                { _, hour, minute ->
                                                    calendar.set(year, month, day, hour, minute)
                                                    selectedDateTime = calendar.timeInMillis
                                                },
                                                calendar.get(Calendar.HOUR_OF_DAY),
                                                calendar.get(Calendar.MINUTE),
                                                true
                                            )
                                            timeDialog.show()
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    )
                                    dateDialog.show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                )
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Elegir Fecha y Hora")
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showForm = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar")
                                }
                                Button(
                                    onClick = {
                                        if (title.isNotBlank() && description.isNotBlank()) {
                                            if (editingReminder != null) {
                                                println("✏️ ReminderScreen - Actualizando recordatorio: '$title'")
                                                reminderViewModel.updateReminder(editingReminder!!, title, description, selectedDateTime, selectedType)
                                            } else {
                                                println("➕ ReminderScreen - Creando nuevo recordatorio: '$title'")
                                                reminderViewModel.addReminder(title, description, selectedDateTime, selectedType)
                                            }
                                            showForm = false
                                        } else {
                                            println("⚠️ ReminderScreen - Formulario incompleto")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    )
                                ) {
                                    Text(if (editingReminder != null) "Actualizar" else "Guardar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}