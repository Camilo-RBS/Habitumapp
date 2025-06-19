package com.wayneenterprises.habitum.repository

import com.wayneenterprises.habitum.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import java.util.*

class TaskRepository {
    private val supabaseRepository = SupabaseRepository()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadUserTasks(userId: String) {
        _isLoading.value = true
        _error.value = null

        supabaseRepository.getUserTasks(userId).fold(
            onSuccess = { supabaseTasks ->
                val localTasks = supabaseTasks.map { it.toTask() }
                _tasks.value = localTasks
                _isLoading.value = false
            },
            onFailure = { exception ->
                _error.value = exception.message ?: "Error al cargar tareas"
                _isLoading.value = false
                // Mantener tareas locales si las hay
            }
        )
    }

    suspend fun addTask(
        userId: String,
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Date?,
        completionDate: Date?
    ): Result<Task> {
        _isLoading.value = true
        _error.value = null

        // Crear tarea local primero
        val localTask = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            category = category,
            priority = priority,
            dueDate = dueDate,
            completionDate = completionDate,
            isCompleted = false
        )

        // Agregar a la lista local inmediatamente para mejor UX
        _tasks.value = _tasks.value + localTask

        // Intentar guardar en Supabase
        val supabaseTaskInsert = localTask.toSupabaseInsert(userId)

        return supabaseRepository.insertTask(supabaseTaskInsert).fold(
            onSuccess = { supabaseTask ->
                // Actualizar con el ID real de Supabase
                val updatedTask = supabaseTask.toTask()
                _tasks.value = _tasks.value.map {
                    if (it.id == localTask.id) updatedTask else it
                }
                _isLoading.value = false
                Result.success(updatedTask)
            },
            onFailure = { exception ->
                // Revertir cambio local si falla
                _tasks.value = _tasks.value.filter { it.id != localTask.id }
                _error.value = exception.message ?: "Error al crear tarea"
                _isLoading.value = false
                Result.failure(exception)
            }
        )
    }

    suspend fun updateTask(
        id: String,
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Date?,
        completionDate: Date?
    ): Result<Task> {
        _isLoading.value = true
        _error.value = null

        // Actualizar localmente primero
        val currentTasks = _tasks.value
        val taskToUpdate = currentTasks.find { it.id == id }

        if (taskToUpdate == null) {
            _error.value = "Tarea no encontrada"
            _isLoading.value = false
            return Result.failure(Exception("Tarea no encontrada"))
        }

        val updatedLocalTask = taskToUpdate.copy(
            title = title,
            description = description,
            category = category,
            priority = priority,
            dueDate = dueDate,
            completionDate = completionDate
        )

        _tasks.value = currentTasks.map {
            if (it.id == id) updatedLocalTask else it
        }

        // Actualizar en Supabase
        val update = SupabaseTaskUpdate(
            title = title,
            description = description,
            category = category,
            priority = priority.name,
            dueDate = dueDate?.let {
                java.time.Instant.ofEpochMilli(it.time).toString()
            },
            completionDate = completionDate?.let {
                java.time.Instant.ofEpochMilli(it.time).toString()
            },
            updatedAt = Clock.System.now().toString()
        )

        return supabaseRepository.updateTask(id, update).fold(
            onSuccess = { supabaseTask ->
                val finalTask = supabaseTask.toTask()
                _tasks.value = _tasks.value.map {
                    if (it.id == id) finalTask else it
                }
                _isLoading.value = false
                Result.success(finalTask)
            },
            onFailure = { exception ->
                // Revertir cambio local si falla
                _tasks.value = currentTasks
                _error.value = exception.message ?: "Error al actualizar tarea"
                _isLoading.value = false
                Result.failure(exception)
            }
        )
    }

    suspend fun toggleTaskCompleted(taskId: String): Result<Unit> {
        val currentTasks = _tasks.value
        val task = currentTasks.find { it.id == taskId }

        if (task == null) {
            _error.value = "Tarea no encontrada"
            return Result.failure(Exception("Tarea no encontrada"))
        }

        val newCompletionStatus = !task.isCompleted
        val completionDate = if (newCompletionStatus) Date() else null

        // Actualizar localmente
        _tasks.value = currentTasks.map {
            if (it.id == taskId) it.copy(
                isCompleted = newCompletionStatus,
                completionDate = completionDate
            ) else it
        }

        // Actualizar en Supabase
        val update = SupabaseTaskUpdate(
            isCompleted = newCompletionStatus,
            completionDate = completionDate?.let {
                java.time.Instant.ofEpochMilli(it.time).toString()
            },
            updatedAt = Clock.System.now().toString()
        )

        return supabaseRepository.updateTask(taskId, update).fold(
            onSuccess = { supabaseTask ->
                val updatedTask = supabaseTask.toTask()
                _tasks.value = _tasks.value.map {
                    if (it.id == taskId) updatedTask else it
                }
                Result.success(Unit)
            },
            onFailure = { exception ->
                // Revertir cambio local si falla
                _tasks.value = currentTasks
                _error.value = exception.message ?: "Error al actualizar estado"
                Result.failure(exception)
            }
        )
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        val currentTasks = _tasks.value

        // Eliminar localmente primero
        _tasks.value = currentTasks.filter { it.id != taskId }

        return supabaseRepository.deleteTask(taskId).fold(
            onSuccess = {
                Result.success(Unit)
            },
            onFailure = { exception ->
                // Revertir cambio local si falla
                _tasks.value = currentTasks
                _error.value = exception.message ?: "Error al eliminar tarea"
                Result.failure(exception)
            }
        )
    }

    fun clearError() {
        _error.value = null
    }
}