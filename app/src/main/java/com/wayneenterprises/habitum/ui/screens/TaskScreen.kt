package com.wayneenterprises.habitum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wayneenterprises.habitum.viewmodel.TaskViewModel
import com.wayneenterprises.habitum.viewmodel.AuthViewModel
import com.wayneenterprises.habitum.ui.components.*
import com.wayneenterprises.habitum.model.Task

@Composable
fun TaskScreen(
    selectedCategory: String = "",
    taskViewModel: TaskViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val isLoading by taskViewModel.isLoading.collectAsState()
    val error by taskViewModel.error.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var selectedCategory by remember { mutableStateOf(selectedCategory.ifEmpty { "All" }) }

    val categories = tasks.map { it.category }.distinct()
    val activeTasks = tasks.filter { !it.isCompleted }

    val filteredTasks = if (selectedCategory == "All") tasks else tasks.filter { it.category == selectedCategory }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3C72),
                        Color(0xFF2A5298)
                    )
                )
            )
    ) {
        // Header
        ScreenHeader(
            title = "Tareas",
            user = authState.currentUser,
            onSignOut = {
                authViewModel.signOut()
            }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            WelcomeHeader(
                userName = authState.currentUser?.name ?: "Usuario",
                activeTasksCount = activeTasks.size
            )

            Spacer(modifier = Modifier.height(8.dp))

            CategoryFilter(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Todas las tareas (${filteredTasks.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF90CAF9) // Azul claro
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargando tareas...", color = Color.White)
                }
            }
        }

        // Error
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEF9A9A)) // Rojo claro
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("❌ Error:", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text(errorMessage, color = Color.Red)
                    Button(
                        onClick = {
                            taskViewModel.clearError()
                            taskViewModel.refreshTasks()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }

        // Lista de tareas o vacío
        if (!isLoading && error == null) {
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Text("No hay tareas", fontSize = 18.sp, color = Color.White)
                        Text("¡Crea tu primera tarea!", fontSize = 14.sp, color = Color.White.copy(0.7f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { taskViewModel.toggleTaskCompleted(task.id) },
                            onDelete = { taskViewModel.deleteTask(task.id) },
                            onEdit = {
                                editingTask = task
                                showForm = true
                            }
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        AddTaskButton(
            onClick = {
                editingTask = null
                showForm = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showForm) {
        NewTaskDialog(
            taskToEdit = editingTask,
            onDismiss = { showForm = false },
            onSave = { title, category, description, dueDate, completionDate, priority, id ->
                if (id.isBlank()) {
                    taskViewModel.addTask(title, description, category, priority, dueDate, completionDate)
                } else {
                    taskViewModel.updateTask(id, title, description, category, priority, dueDate, completionDate)
                }
                showForm = false
            }
        )
    }
}
