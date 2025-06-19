package com.wayneenterprises.habitum.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayneenterprises.habitum.model.ReminderStatus
import com.wayneenterprises.habitum.model.ReminderType
import com.wayneenterprises.habitum.repository.ReminderRepository
import com.wayneenterprises.habitum.repository.SupabaseRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReminderViewModel : ViewModel() {
    private val repository = ReminderRepository()
    private val authRepository = SupabaseRepository()

    val reminders: StateFlow<List<com.wayneenterprises.habitum.model.Reminder>> = repository.reminders
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error

    init {
        loadReminders()
    }

    private fun loadReminders() {
        viewModelScope.launch {
            try {
                // Obtener el usuario actual
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        user?.let {
                            println("🔔 Cargando recordatorios para usuario: ${it.id}")
                            // Cargar recordatorios del usuario desde Supabase
                            repository.loadUserReminders(it.id)
                        } ?: run {
                            println("⚠️ No hay usuario autenticado para cargar recordatorios")
                        }
                    },
                    onFailure = { exception ->
                        println("❌ Error obteniendo usuario actual: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en loadReminders: ${e.message}")
            }
        }
    }

    fun addReminder(title: String, description: String, dateTime: Long, type: ReminderType) {
        if (title.isBlank()) {
            println("⚠️ No se puede crear recordatorio sin título")
            return
        }

        viewModelScope.launch {
            try {
                println("🔔 Iniciando creación de recordatorio: $title")

                // Obtener el usuario actual
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        if (user != null) {
                            println("👤 Usuario encontrado: ${user.id}")

                            repository.addReminder(
                                userId = user.id,
                                title = title,
                                description = description,
                                dateTime = dateTime,
                                type = type
                            ).fold(
                                onSuccess = { reminder ->
                                    println("✅ Recordatorio creado exitosamente: ${reminder.title} con ID: ${reminder.id}")
                                },
                                onFailure = { exception ->
                                    println("❌ Error creando recordatorio en Supabase: ${exception.message}")
                                    exception.printStackTrace()
                                }
                            )
                        } else {
                            println("❌ No hay usuario autenticado para crear recordatorio")
                        }
                    },
                    onFailure = { exception ->
                        println("❌ Error obteniendo usuario actual: ${exception.message}")
                        exception.printStackTrace()
                    }
                )
            } catch (e: Exception) {
                println("❌ Error general en addReminder: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun updateReminder(id: String, title: String, description: String, dateTime: Long, type: ReminderType) {
        if (title.isBlank()) {
            println("⚠️ No se puede actualizar recordatorio sin título")
            return
        }

        viewModelScope.launch {
            try {
                println("✏️ Actualizando recordatorio: $id")

                repository.updateReminder(id, title, description, dateTime, type).fold(
                    onSuccess = { reminder ->
                        println("✅ Recordatorio actualizado exitosamente: ${reminder.title}")
                    },
                    onFailure = { exception ->
                        println("❌ Error actualizando recordatorio: ${exception.message}")
                        exception.printStackTrace()
                    }
                )
            } catch (e: Exception) {
                println("❌ Error general en updateReminder: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteReminder(id).fold(
                    onSuccess = {
                        println("🗑️ Recordatorio eliminado: $id")
                    },
                    onFailure = { exception ->
                        println("❌ Error eliminando recordatorio: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en deleteReminder: ${e.message}")
            }
        }
    }

    fun completeReminder(id: String) {
        viewModelScope.launch {
            try {
                repository.completeReminder(id).fold(
                    onSuccess = {
                        println("✅ Recordatorio completado: $id")
                    },
                    onFailure = { exception ->
                        println("❌ Error completando recordatorio: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en completeReminder: ${e.message}")
            }
        }
    }

    fun omitReminder(id: String) {
        viewModelScope.launch {
            try {
                repository.omitReminder(id).fold(
                    onSuccess = {
                        println("⏭️ Recordatorio omitido: $id")
                    },
                    onFailure = { exception ->
                        println("❌ Error omitiendo recordatorio: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en omitReminder: ${e.message}")
            }
        }
    }

    fun markAsMissed(id: String) {
        viewModelScope.launch {
            try {
                repository.markAsMissed(id).fold(
                    onSuccess = {
                        println("❌ Recordatorio marcado como perdido: $id")
                    },
                    onFailure = { exception ->
                        println("❌ Error marcando recordatorio como perdido: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en markAsMissed: ${e.message}")
            }
        }
    }

    // Función para verificar recordatorios perdidos automáticamente
    fun checkMissedReminders() {
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                reminders.value.forEach { reminder ->
                    if (reminder.status == ReminderStatus.PENDING &&
                        reminder.dateTime < currentTime - (30 * 60 * 1000)) { // 30 minutos de gracia
                        markAsMissed(reminder.id)
                    }
                }
            } catch (e: Exception) {
                println("❌ Error en checkMissedReminders: ${e.message}")
            }
        }
    }

    fun clearError() {
        repository.clearError()
    }

    fun refreshReminders() {
        println("🔄 Refrescando recordatorios...")
        loadReminders()
    }

    // Función para debug - ver estado actual
    fun debugState() {
        viewModelScope.launch {
            println("🔍 DEBUG - Estado actual:")
            println("  - Recordatorios: ${reminders.value.size}")
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