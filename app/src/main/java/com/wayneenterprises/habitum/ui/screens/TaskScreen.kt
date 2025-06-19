package com.wayneenterprises.habitum.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    selectedCategory: String = "", // Cambiar de selectedTaskId a selectedCategory
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

    // Manejar navegación específica a una categoría
    LaunchedEffect(selectedCategory) {
        if (selectedCategory.isNotEmpty() && selectedCategory != "All") {
            println("🎯 TaskScreen - Filtrando por categoría: $selectedCategory")
        }
    }

    val categories = tasks.map { it.category }.distinct()
    val activeTasks = tasks.filter { !it.isCompleted }

    val filteredTasks = if (selectedCategory == "All") {
        tasks
    } else {
        tasks.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header con perfil de usuario
        ScreenHeader(
            title = "Tareas",
            user = authState.currentUser,
            onSignOut = {
                println("🚪 TaskScreen - Solicitando cierre de sesión")
                authViewModel.signOut()
            }
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Welcome Header
            WelcomeHeader(
                userName = authState.currentUser?.name ?: "Usuario",
                activeTasksCount = activeTasks.size
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter
            CategoryFilter(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // All Tasks Title
            Text(
                text = "All Tasks (${filteredTasks.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 0.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

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
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cargando tareas...",
                        color = Color.White
                    )
                }
            }
        }

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
                            taskViewModel.clearError()
                            taskViewModel.refreshTasks()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }

        // Empty state o Tasks List
        if (!isLoading && error == null) {
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📋",
                            fontSize = 48.sp
                        )
                        Text(
                            text = "No hay tareas",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "¡Crea tu primera tarea!",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Tasks List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskItem(
                            task = task,
                            onToggle = {
                                println("🔄 TaskScreen - Toggling tarea: ${task.id}")
                                taskViewModel.toggleTaskCompleted(task.id)
                            },
                            onDelete = {
                                println("🗑️ TaskScreen - Eliminando tarea: ${task.id}")
                                taskViewModel.deleteTask(task.id)
                            },
                            onEdit = {
                                println("✏️ TaskScreen - Editando tarea: ${task.id}")
                                editingTask = task
                                showForm = true
                            }
                        )
                    }
                }
            }
        } else {
            // Spacer para cuando hay loading o error
            Spacer(modifier = Modifier.weight(1f))
        }

        // Add Task Button
        AddTaskButton(
            onClick = {
                println("➕ TaskScreen - Abriendo formulario para nueva tarea")
                editingTask = null
                showForm = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dialog de formulario
    if (showForm) {
        NewTaskDialog(
            taskToEdit = editingTask,
            onDismiss = {
                println("❌ TaskScreen - Cerrando formulario")
                showForm = false
            },
            onSave = { title, category, description, dueDate, completionDate, priority, id ->
                if (id.isBlank()) {
                    println("➕ TaskScreen - Creando nueva tarea: '$title'")
                    taskViewModel.addTask(
                        title = title,
                        description = description,
                        category = category,
                        priority = priority,
                        dueDate = dueDate,
                        completionDate = completionDate
                    )
                } else {
                    println("✏️ TaskScreen - Actualizando tarea: $id")
                    taskViewModel.updateTask(
                        id = id,
                        title = title,
                        description = description,
                        category = category,
                        priority = priority,
                        dueDate = dueDate,
                        completionDate = completionDate
                    )
                }
                showForm = false
            }
        )
    }
}