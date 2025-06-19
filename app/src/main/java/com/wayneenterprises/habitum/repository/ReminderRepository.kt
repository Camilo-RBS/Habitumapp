package com.wayneenterprises.habitum.repository

import com.wayneenterprises.habitum.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import java.util.*

class ReminderRepository {
    private val supabaseRepository = SupabaseRepository()

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> get() = _reminders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    suspend fun loadUserReminders(userId: String) {
        _isLoading.value = true
        _error.value = null

        supabaseRepository.getUserReminders(userId).fold(
            onSuccess = { supabaseReminders ->
                val localReminders = supabaseReminders.map { it.toReminder() }
                _reminders.value = localReminders
                _isLoading.value = false
            },
            onFailure = { exception ->
                _error.value = exception.message ?: "Error al cargar recordatorios"
                _isLoading.value = false
            }
        )
    }

    suspend fun addReminder(
        userId: String,
        title: String,
        description: String,
        dateTime: Long,
        type: ReminderType = ReminderType.GENERAL
    ): Result<Reminder> {
        _isLoading.value = true
        _error.value = null

        // Crear recordatorio local primero
        val localReminder = Reminder(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            dateTime = dateTime,
            type = type
        )

        // Agregar a la lista local inmediatamente
        _reminders.value = _reminders.value + localReminder

        // Intentar guardar en Supabase
        val supabaseReminderInsert = localReminder.toSupabaseInsert(userId)

        return supabaseRepository.insertReminder(supabaseReminderInsert).fold(
            onSuccess = { supabaseReminder ->
                // Actualizar con el ID real de Supabase
                val updatedReminder = supabaseReminder.toReminder()
                _reminders.value = _reminders.value.map {
                    if (it.id == localReminder.id) updatedReminder else it
                }
                _isLoading.value = false
                Result.success(updatedReminder)
            },
            onFailure = { exception ->
                // Revertir cambio local si falla
                _reminders.value = _reminders.value.filter { it.id != localReminder.id }
                _error.value = exception.message ?: "Error al crear recordatorio"
                _isLoading.value = false
                Result.failure(exception)
            }
        )
    }

    suspend fun updateReminder(
        id: String,
        title: String,
        description: String,
        dateTime: Long,
        type: ReminderType
    ): Result<Reminder> {
        _isLoading.value = true
        _error.value = null

        // Actualizar localmente primero
        val currentReminders = _reminders.value
        val reminderToUpdate = currentReminders.find { it.id == id }

        if (reminderToUpdate == null) {
            _error.value = "Recordatorio no encontrado"
            _isLoading.value = false
            return Result.failure(Exception("Recordatorio no encontrado"))
        }

        val updatedLocalReminder = reminderToUpdate.copy(
            title = title,
            description = description,
            dateTime = dateTime,
            type = type
        )

        _reminders.value = currentReminders.map {
            if (it.id == id) updatedLocalReminder else it
        }

        // Actualizar en Supabase
        val update = SupabaseReminderUpdate(
            title = title,
            description = description,
            dateTime = java.time.Instant.ofEpochMilli(dateTime).toString(),
            type = type.name,
            updatedAt = Clock.System.now().toString()
        )

        return supabaseRepository.updateReminder(id, update).fold(
            onSuccess = { supabaseReminder ->
                val finalReminder = supabaseReminder.toReminder()
                _reminders.value = _reminders.value.map {
                    if (it.id == id) finalReminder else it
                }
                _isLoading.value = false
                Result.success(finalReminder)
            },
            onFailure = { exception ->
                // Revertir cambio local si falla
                _reminders.value = currentReminders
                _error.value = exception.message ?: "Error al actualizar recordatorio"
                _isLoading.value = false
                Result.failure(exception)
            }
        )
    }

    suspend fun deleteReminder(id: String): Result<Unit> {
        val currentReminders = _reminders.value

        // Eliminar localmente primero
        _reminders.value = currentReminders.filter { it.id != id }

        return supabaseRepository.deleteReminder(id).fold(
            onSuccess = {
                Result.success(Unit)
            },
            onFailure = { exception ->
                // Revertir cambio local si falla
                _reminders.value = currentReminders
                _error.value = exception.message ?: "Error al eliminar recordatorio"
                Result.failure(exception)
            }
        )
    }

    suspend fun updateReminderStatus(id: String, status: ReminderStatus): Result<Unit> {
        val currentReminders = _reminders.value
        val reminder = currentReminders.find { it.id == id }

        if (reminder == null) {
            _error.value = "Recordatorio no encontrado"
            return Result.failure(Exception("Recordatorio no encontrado"))
        }

        // Actualizar localmente
        _reminders.value = currentReminders.map {
            if (it.id == id) it.copy(status = status) else it
        }

        // Actualizar en Supabase
        val update = SupabaseReminderUpdate(
            status = status.name,
            updatedAt = Clock.System.now().toString()
        )

        return supabaseRepository.updateReminder(id, update).fold(
            onSuccess = { supabaseReminder ->
                val updatedReminder = supabaseReminder.toReminder()
                _reminders.value = _reminders.value.map {
                    if (it.id == id) updatedReminder else it
                }
                Result.success(Unit)
            },
            onFailure = { exception ->
                // Revertir cambio local si falla
                _reminders.value = currentReminders
                _error.value = exception.message ?: "Error al actualizar estado"
                Result.failure(exception)
            }
        )
    }

    fun getReminderById(id: String): Reminder? {
        return _reminders.value.find { it.id == id }
    }

    fun clearError() {
        _error.value = null
    }

    // Funciones de conveniencia para estados específicos
    suspend fun completeReminder(id: String) = updateReminderStatus(id, ReminderStatus.COMPLETED)
    suspend fun omitReminder(id: String) = updateReminderStatus(id, ReminderStatus.OMITTED)
    suspend fun markAsMissed(id: String) = updateReminderStatus(id, ReminderStatus.MISSED)
}