package com.wayneenterprises.habitum.repository

import com.wayneenterprises.habitum.Config.SupabaseConfig
import com.wayneenterprises.habitum.model.*
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.Clock

class SupabaseRepository {

    private val client = SupabaseConfig.client

    // ================================
    // AUTH FUNCTIONS
    // ================================

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            println("🔐 SupabaseRepository.signIn - Iniciando login para: $email")

            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val currentUser = client.auth.currentUserOrNull()
            if (currentUser != null) {
                println("✅ SupabaseRepository - Usuario autenticado: ${currentUser.id}")
                val userFromDb = getUserByAuthId(currentUser.id)
                userFromDb
            } else {
                println("❌ SupabaseRepository - No se pudo obtener usuario autenticado")
                Result.failure(Exception("Error al obtener usuario autenticado"))
            }
        } catch (e: Exception) {
            println("❌ SupabaseRepository.signIn - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            println("📝 SupabaseRepository.signUp - Registrando usuario: $email")

            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val currentUser = client.auth.currentUserOrNull()
            if (currentUser != null) {
                println("✅ SupabaseRepository - Usuario registrado: ${currentUser.id}")

                val userType = if (email == "wayneenterprises@gmail.com") "admin" else "normal"

                val userInsert = UserInsertWithAuthId(
                    authId = currentUser.id,
                    name = name,
                    email = email,
                    userType = userType
                )

                println("📝 SupabaseRepository - Insertando en tabla usuarios...")
                client.from("usuarios").insert(userInsert)

                getUserByAuthId(currentUser.id)
            } else {
                println("❌ SupabaseRepository - No se pudo crear usuario")
                Result.failure(Exception("Error al crear usuario"))
            }
        } catch (e: Exception) {
            println("❌ SupabaseRepository.signUp - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            println("🚪 SupabaseRepository.signOut - Cerrando sesión...")
            client.auth.signOut()
            println("✅ SupabaseRepository - Sesión cerrada correctamente")
        } catch (e: Exception) {
            println("⚠️ SupabaseRepository.signOut - Error: ${e.message}")
        }
    }

    fun getCurrentAuthUser() = client.auth.currentUserOrNull()

    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val authUser = client.auth.currentUserOrNull()
            if (authUser != null) {
                println("👤 SupabaseRepository.getCurrentUser - Usuario auth encontrado: ${authUser.id}")
                getUserByAuthId(authUser.id)
            } else {
                println("👤 SupabaseRepository.getCurrentUser - No hay usuario autenticado")
                Result.success(null)
            }
        } catch (e: Exception) {
            println("❌ SupabaseRepository.getCurrentUser - Error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun getUserByAuthId(authId: String): Result<User> {
        return try {
            println("🔍 SupabaseRepository.getUserByAuthId - Buscando usuario con auth_id: $authId")

            val response = client.from("usuarios")
                .select {
                    filter {
                        eq("auth_id", authId)
                    }
                }
                .decodeSingle<UserWithAuthId>()

            println("✅ SupabaseRepository - Usuario encontrado: ${response.name}")
            Result.success(response.toUser())
        } catch (e: Exception) {
            println("❌ SupabaseRepository.getUserByAuthId - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            println("👥 SupabaseRepository.getAllUsers - Obteniendo todos los usuarios")

            val users = client.from("usuarios")
                .select()
                .decodeList<UserWithAuthId>()

            println("✅ SupabaseRepository - Obtenidos ${users.size} usuarios")
            Result.success(users.map { it.toUser() })
        } catch (e: Exception) {
            println("❌ SupabaseRepository.getAllUsers - Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUserName(userId: String, newName: String): Result<Unit> {
        return try {
            val userUpdate = UserUpdate(
                name = newName,
                updatedAt = Clock.System.now().toString()
            )

            client.from("usuarios").update(userUpdate) {
                filter {
                    eq("id", userId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            client.from("usuarios").delete {
                filter {
                    eq("id", userId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAdminUserIfNeeded(): Result<Unit> {
        return try {
            println("🔧 SupabaseRepository.createAdminUserIfNeeded - Verificando admin...")

            val adminEmail = "wayneenterprises@gmail.com"
            val adminPassword = "batman2507"

            val existingAdmin = client.from("usuarios")
                .select {
                    filter {
                        eq("email", adminEmail)
                    }
                }
                .decodeList<UserWithAuthId>()

            if (existingAdmin.isEmpty()) {
                println("👑 SupabaseRepository - Creando usuario admin...")
                val result = signUp(adminEmail, adminPassword, "Wayne Enterprises Admin")
                if (result.isSuccess) {
                    println("✅ SupabaseRepository - Admin creado correctamente")
                    Result.success(Unit)
                } else {
                    println("❌ SupabaseRepository - Error creando admin")
                    Result.failure(result.exceptionOrNull() ?: Exception("Fallo creando admin"))
                }
            } else {
                println("✅ SupabaseRepository - Admin ya existe")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            println("❌ SupabaseRepository.createAdminUserIfNeeded - Error: ${e.message}")
            Result.failure(e)
        }
    }

    // ================================
    // FUNCIONES PARA TASKS
    // ================================

    suspend fun insertTask(task: SupabaseTaskInsert): Result<SupabaseTask> {
        return try {
            println("📝 SupabaseRepository.insertTask - Insertando tarea: ${task.title}")
            println("📝 SupabaseRepository - Task data: $task")

            val insertedTasks = client.from("tasks")
                .insert(task) {
                    select()
                }
                .decodeList<SupabaseTask>()

            if (insertedTasks.isNotEmpty()) {
                val insertedTask = insertedTasks.first()
                println("✅ SupabaseRepository - Tarea insertada con ID: ${insertedTask.id}")
                Result.success(insertedTask)
            } else {
                println("❌ SupabaseRepository - No se pudo obtener la tarea insertada")
                Result.failure(Exception("No se pudo obtener la tarea insertada"))
            }
        } catch (e: Exception) {
            println("❌ SupabaseRepository.insertTask - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getUserTasks(userId: String): Result<List<SupabaseTask>> {
        return try {
            println("📋 SupabaseRepository.getUserTasks - Obteniendo tareas para usuario: $userId")

            val tasks = client.from("tasks")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<SupabaseTask>()

            println("✅ SupabaseRepository - Obtenidas ${tasks.size} tareas")
            Result.success(tasks)
        } catch (e: Exception) {
            println("❌ SupabaseRepository.getUserTasks - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateTask(taskId: String, update: SupabaseTaskUpdate): Result<SupabaseTask> {
        return try {
            println("✏️ SupabaseRepository.updateTask - Actualizando tarea: $taskId")

            val updatedTasks = client.from("tasks")
                .update(update) {
                    filter {
                        eq("id", taskId)
                    }
                    select()
                }
                .decodeList<SupabaseTask>()

            if (updatedTasks.isNotEmpty()) {
                val updatedTask = updatedTasks.first()
                println("✅ SupabaseRepository - Tarea actualizada")
                Result.success(updatedTask)
            } else {
                println("❌ SupabaseRepository - No se pudo obtener la tarea actualizada")
                Result.failure(Exception("No se pudo obtener la tarea actualizada"))
            }
        } catch (e: Exception) {
            println("❌ SupabaseRepository.updateTask - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            println("🗑️ SupabaseRepository.deleteTask - Eliminando tarea: $taskId")

            client.from("tasks").delete {
                filter {
                    eq("id", taskId)
                }
            }

            println("✅ SupabaseRepository - Tarea eliminada")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ SupabaseRepository.deleteTask - Error: ${e.message}")
            Result.failure(e)
        }
    }

    // ================================
    // FUNCIONES PARA REMINDERS
    // ================================

    suspend fun insertReminder(reminder: SupabaseReminderInsert): Result<SupabaseReminder> {
        return try {
            println("🔔 SupabaseRepository.insertReminder - Insertando recordatorio: ${reminder.title}")
            println("🔔 SupabaseRepository - Reminder data: $reminder")

            val insertedReminders = client.from("reminders")
                .insert(reminder) {
                    select()
                }
                .decodeList<SupabaseReminder>()

            if (insertedReminders.isNotEmpty()) {
                val insertedReminder = insertedReminders.first()
                println("✅ SupabaseRepository - Recordatorio insertado con ID: ${insertedReminder.id}")
                Result.success(insertedReminder)
            } else {
                println("❌ SupabaseRepository - No se pudo obtener el recordatorio insertado")
                Result.failure(Exception("No se pudo obtener el recordatorio insertado"))
            }
        } catch (e: Exception) {
            println("❌ SupabaseRepository.insertReminder - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getUserReminders(userId: String): Result<List<SupabaseReminder>> {
        return try {
            println("🔔 SupabaseRepository.getUserReminders - Obteniendo recordatorios para usuario: $userId")

            val reminders = client.from("reminders")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<SupabaseReminder>()

            println("✅ SupabaseRepository - Obtenidos ${reminders.size} recordatorios")
            Result.success(reminders)
        } catch (e: Exception) {
            println("❌ SupabaseRepository.getUserReminders - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateReminder(reminderId: String, update: SupabaseReminderUpdate): Result<SupabaseReminder> {
        return try {
            println("✏️ SupabaseRepository.updateReminder - Actualizando recordatorio: $reminderId")

            val updatedReminders = client.from("reminders")
                .update(update) {
                    filter {
                        eq("id", reminderId)
                    }
                    select()
                }
                .decodeList<SupabaseReminder>()

            if (updatedReminders.isNotEmpty()) {
                val updatedReminder = updatedReminders.first()
                println("✅ SupabaseRepository - Recordatorio actualizado")
                Result.success(updatedReminder)
            } else {
                println("❌ SupabaseRepository - No se pudo obtener el recordatorio actualizado")
                Result.failure(Exception("No se pudo obtener el recordatorio actualizado"))
            }
        } catch (e: Exception) {
            println("❌ SupabaseRepository.updateReminder - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        return try {
            println("🗑️ SupabaseRepository.deleteReminder - Eliminando recordatorio: $reminderId")

            client.from("reminders").delete {
                filter {
                    eq("id", reminderId)
                }
            }

            println("✅ SupabaseRepository - Recordatorio eliminado")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ SupabaseRepository.deleteReminder - Error: ${e.message}")
            Result.failure(e)
        }
    }

    // ================================
    // FUNCIONES PARA DAILY STEPS - CORREGIDAS
    // ================================

    suspend fun getUserDailySteps(userId: String): Result<List<SupabaseDailySteps>> {
        return try {
            println("📊 SupabaseRepository.getUserDailySteps - Obteniendo pasos para usuario: $userId")

            val steps = client.from("daily_steps")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<SupabaseDailySteps>()

            // Ordenar por fecha en código Kotlin (más reciente primero)
            val sortedSteps = steps.sortedByDescending { it.date }

            println("✅ SupabaseRepository - Obtenidos ${sortedSteps.size} registros de pasos")
            Result.success(sortedSteps)
        } catch (e: Exception) {
            println("❌ SupabaseRepository.getUserDailySteps - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun upsertDailySteps(userId: String, date: String, steps: Int): Result<SupabaseDailySteps> {
        return try {
            println("📊 SupabaseRepository.upsertDailySteps - Usuario: $userId, Fecha: $date, Pasos: $steps")

            // Primero verificar si ya existe un registro para esta fecha
            val existingSteps = client.from("daily_steps")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("date", date)
                    }
                }
                .decodeList<SupabaseDailySteps>()

            if (existingSteps.isNotEmpty()) {
                // Actualizar registro existente
                println("🔄 Actualizando registro existente")

                val update = SupabaseDailyStepsUpdate(
                    steps = steps,
                    updatedAt = Clock.System.now().toString()
                )

                val updatedSteps = client.from("daily_steps")
                    .update(update) {
                        filter {
                            eq("user_id", userId)
                            eq("date", date)
                        }
                        select()
                    }
                    .decodeList<SupabaseDailySteps>()

                if (updatedSteps.isNotEmpty()) {
                    println("✅ Registro actualizado exitosamente")
                    Result.success(updatedSteps.first())
                } else {
                    throw Exception("No se pudo obtener el registro actualizado")
                }
            } else {
                // Crear nuevo registro
                println("➕ Creando nuevo registro")

                val insert = SupabaseDailyStepsInsert(
                    userId = userId,
                    date = date,
                    steps = steps
                )

                val insertedSteps = client.from("daily_steps")
                    .insert(insert) {
                        select()
                    }
                    .decodeList<SupabaseDailySteps>()

                if (insertedSteps.isNotEmpty()) {
                    println("✅ Nuevo registro creado exitosamente")
                    Result.success(insertedSteps.first())
                } else {
                    throw Exception("No se pudo obtener el registro insertado")
                }
            }
        } catch (e: Exception) {
            println("❌ SupabaseRepository.upsertDailySteps - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getDailyStepsForDateRange(userId: String, startDate: String, endDate: String): Result<List<SupabaseDailySteps>> {
        return try {
            println("📊 SupabaseRepository.getDailyStepsForDateRange - Usuario: $userId, Rango: $startDate a $endDate")

            val steps = client.from("daily_steps")
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", startDate)
                        lte("date", endDate)
                    }
                }
                .decodeList<SupabaseDailySteps>()

            // Ordenar por fecha en código Kotlin (más antiguo primero para rangos)
            val sortedSteps = steps.sortedBy { it.date }

            println("✅ SupabaseRepository - Obtenidos ${sortedSteps.size} registros en el rango")
            Result.success(sortedSteps)
        } catch (e: Exception) {
            println("❌ SupabaseRepository.getDailyStepsForDateRange - Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteDailyStepsForDate(userId: String, date: String): Result<Unit> {
        return try {
            println("🗑️ SupabaseRepository.deleteDailyStepsForDate - Usuario: $userId, Fecha: $date")

            client.from("daily_steps").delete {
                filter {
                    eq("user_id", userId)
                    eq("date", date)
                }
            }

            println("✅ SupabaseRepository - Registro eliminado")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ SupabaseRepository.deleteDailyStepsForDate - Error: ${e.message}")
            Result.failure(e)
        }
    }
}