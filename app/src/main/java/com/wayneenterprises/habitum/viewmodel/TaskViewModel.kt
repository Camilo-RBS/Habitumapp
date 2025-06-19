package com.wayneenterprises.habitum.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.wayneenterprises.habitum.model.Task
import com.wayneenterprises.habitum.model.TaskPriority
import com.wayneenterprises.habitum.repository.TaskRepository
import com.wayneenterprises.habitum.repository.SupabaseRepository
import java.util.*

class TaskViewModel : ViewModel() {

    private val repository = TaskRepository()
    private val authRepository = SupabaseRepository()

    val tasks: StateFlow<List<Task>> = repository.tasks
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            try {
                // Obtener el usuario actual
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        user?.let {
                            println("📋 Cargando tareas para usuario: ${it.id}")
                            // Cargar tareas del usuario desde Supabase
                            repository.loadUserTasks(it.id)
                        } ?: run {
                            println("⚠️ No hay usuario autenticado para cargar tareas")
                        }
                    },
                    onFailure = { exception ->
                        println("❌ Error obteniendo usuario actual: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en loadTasks: ${e.message}")
            }
        }
    }

    fun toggleTaskCompleted(taskId: String) {
        viewModelScope.launch {
            try {
                repository.toggleTaskCompleted(taskId).fold(
                    onSuccess = {
                        println("✅ Tarea completada/descompletada: $taskId")
                    },
                    onFailure = { exception ->
                        println("❌ Error toggling task: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en toggleTaskCompleted: ${e.message}")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId).fold(
                    onSuccess = {
                        println("🗑️ Tarea eliminada: $taskId")
                    },
                    onFailure = { exception ->
                        println("❌ Error deleting task: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en deleteTask: ${e.message}")
            }
        }
    }

    fun addTask(
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Date?,
        completionDate: Date?
    ) {
        if (title.isBlank()) {
            println("⚠️ No se puede crear tarea sin título")
            return
        }

        viewModelScope.launch {
            try {
                println("📝 Iniciando creación de tarea: $title")

                // Obtener el usuario actual
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        if (user != null) {
                            println("👤 Usuario encontrado: ${user.id}")

                            repository.addTask(
                                userId = user.id,
                                title = title,
                                description = description,
                                category = category,
                                priority = priority,
                                dueDate = dueDate,
                                completionDate = completionDate
                            ).fold(
                                onSuccess = { task ->
                                    println("✅ Tarea creada exitosamente: ${task.title} con ID: ${task.id}")
                                },
                                onFailure = { exception ->
                                    println("❌ Error creando tarea en Supabase: ${exception.message}")
                                    exception.printStackTrace()
                                }
                            )
                        } else {
                            println("❌ No hay usuario autenticado para crear tarea")
                        }
                    },
                    onFailure = { exception ->
                        println("❌ Error obteniendo usuario actual: ${exception.message}")
                        exception.printStackTrace()
                    }
                )
            } catch (e: Exception) {
                println("❌ Error general en addTask: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun updateTask(
        id: String,
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Date?,
        completionDate: Date?
    ) {
        if (title.isBlank()) {
            println("⚠️ No se puede actualizar tarea sin título")
            return
        }

        viewModelScope.launch {
            try {
                println("✏️ Actualizando tarea: $id")

                repository.updateTask(
                    id = id,
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    dueDate = dueDate,
                    completionDate = completionDate
                ).fold(
                    onSuccess = { task ->
                        println("✅ Tarea actualizada exitosamente: ${task.title}")
                    },
                    onFailure = { exception ->
                        println("❌ Error actualizando tarea: ${exception.message}")
                        exception.printStackTrace()
                    }
                )
            } catch (e: Exception) {
                println("❌ Error general en updateTask: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun clearError() {
        repository.clearError()
    }

    fun refreshTasks() {
        println("🔄 Refrescando tareas...")
        loadTasks()
    }

    // Función para debug - ver estado actual
    fun debugState() {
        viewModelScope.launch {
            println("🔍 DEBUG - Estado actual:")
            println("  - Tareas: ${tasks.value.size}")
            println("  - Loading: ${isLoading.value}")
            println("  - Error: ${error.value}")

            authRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    println("  - Usuario: ${user?.name} (${user?.id})")
                },
                onFailure = {
                    println("  - Usuario: No autenticado")
                }
            )
        }
    }
}