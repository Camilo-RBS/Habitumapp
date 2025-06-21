package com.wayneenterprises.habitum.repository

import com.wayneenterprises.habitum.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import java.text.SimpleDateFormat
import java.util.*

class DailyStepsRepository {
    private val supabaseRepository = SupabaseRepository()

    private val _dailyStepsHistory = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dailyStepsHistory: StateFlow<Map<String, Int>> = _dailyStepsHistory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 📅 Obtener fecha actual en formato string
    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // 📅 Obtener fecha específica en formato string
    private fun getDateString(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }

    // 🔄 Cargar historial de pasos del usuario desde Supabase
    suspend fun loadUserDailySteps(userId: String) {
        _isLoading.value = true
        _error.value = null

        try {
            println("📊 Cargando historial de pasos para usuario: $userId")

            supabaseRepository.getUserDailySteps(userId).fold(
                onSuccess = { supabaseSteps ->
                    val stepsMap = supabaseSteps.associate { it.date to it.steps }
                    _dailyStepsHistory.value = stepsMap
                    _isLoading.value = false

                    // Debug: Mostrar pasos de hoy
                    val today = getCurrentDateString()
                    val todaySteps = stepsMap[today] ?: 0

                    println("✅ Cargados ${stepsMap.size} registros de pasos diarios")
                    println("📅 Pasos para hoy ($today): $todaySteps")
                    println("📊 Historial completo: $stepsMap")
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Error al cargar historial de pasos"
                    _isLoading.value = false
                    println("❌ Error cargando pasos: ${exception.message}")
                    exception.printStackTrace()
                }
            )
        } catch (e: Exception) {
            _error.value = "Error inesperado al cargar pasos"
            _isLoading.value = false
            println("❌ Excepción: ${e.message}")
            e.printStackTrace()
        }
    }

    // 💾 Guardar o actualizar pasos del día actual
    suspend fun saveTodaySteps(userId: String, steps: Int): Result<Unit> {
        val today = getCurrentDateString()

        try {
            println("💾 Intentando guardar $steps pasos para fecha: $today")

            // Intentar guardar en Supabase PRIMERO
            return supabaseRepository.upsertDailySteps(userId, today, steps).fold(
                onSuccess = {
                    println("✅ Pasos guardados exitosamente en Supabase")

                    // Actualizar en memoria DESPUÉS del éxito en BD
                    val currentHistory = _dailyStepsHistory.value.toMutableMap()
                    currentHistory[today] = steps
                    _dailyStepsHistory.value = currentHistory

                    println("💾 Memoria actualizada - pasos para $today: $steps")

                    Result.success(Unit)
                },
                onFailure = { exception ->
                    println("❌ Error guardando en Supabase: ${exception.message}")
                    exception.printStackTrace()

                    // Aún así actualizar en memoria para no perder los datos localmente
                    val currentHistory = _dailyStepsHistory.value.toMutableMap()
                    currentHistory[today] = steps
                    _dailyStepsHistory.value = currentHistory

                    _error.value = "Error sincronizando pasos, pero datos guardados localmente"
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            println("❌ Error general en saveTodaySteps: ${e.message}")
            e.printStackTrace()
            _error.value = "Error guardando pasos del día"
            return Result.failure(e)
        }
    }

    // 📈 Obtener pasos de los últimos 7 días (incluyendo hoy)
    fun getWeeklySteps(): List<Int> {
        val calendar = Calendar.getInstance()
        val weeklySteps = mutableListOf<Int>()

        // Obtener el lunes de esta semana
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY

        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        // Recopilar pasos de cada día de la semana
        for (i in 0..6) {
            val dateString = getDateString(calendar.time)
            val stepsForDay = _dailyStepsHistory.value[dateString] ?: 0
            weeklySteps.add(stepsForDay)
            println("📅 ${getDateString(calendar.time)}: $stepsForDay pasos")
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        println("📊 Pasos semanales compilados: $weeklySteps")
        return weeklySteps
    }

    // 🎯 Obtener pasos de hoy desde el historial
    fun getTodaySteps(): Int {
        val today = getCurrentDateString()
        val todaySteps = _dailyStepsHistory.value[today] ?: 0
        println("📅 getTodaySteps para $today: $todaySteps")
        return todaySteps
    }

    // 📅 Obtener pasos de una fecha específica
    fun getStepsForDate(date: String): Int {
        return _dailyStepsHistory.value[date] ?: 0
    }

    // 🧹 Limpiar errores
    fun clearError() {
        _error.value = null
    }

    // 📊 Obtener estadísticas semanales
    fun getWeeklyStats(): Map<String, Any> {
        val weeklySteps = getWeeklySteps()
        val totalSteps = weeklySteps.sum()
        val daysWithSteps = weeklySteps.count { it > 0 }
        val averageSteps = if (daysWithSteps > 0) totalSteps / daysWithSteps else 0
        val maxSteps = weeklySteps.maxOrNull() ?: 0

        return mapOf(
            "total" to totalSteps,
            "average" to averageSteps,
            "max" to maxSteps,
            "daysActive" to daysWithSteps
        )
    }

    // 🔄 Función para refrescar datos desde la BD
    suspend fun refreshData(userId: String) {
        println("🔄 Refrescando datos desde BD...")
        loadUserDailySteps(userId)
    }

    // 📅 Debug: Imprimir estado actual
    fun debugState() {
        val today = getCurrentDateString()
        val todaySteps = getTodaySteps()

        println("🔍 DailyStepsRepository Debug:")
        println("  - Fecha actual: $today")
        println("  - Pasos hoy: $todaySteps")
        println("  - Historial total: ${_dailyStepsHistory.value.size} días")
        println("  - Historial completo: ${_dailyStepsHistory.value}")
        println("  - Pasos semanales: ${getWeeklySteps()}")
        println("  - Loading: ${_isLoading.value}")
        println("  - Error: ${_error.value}")
    }

    // 🆕 Función para verificar si tenemos datos para hoy
    fun hasTodayData(): Boolean {
        val today = getCurrentDateString()
        return _dailyStepsHistory.value.containsKey(today)
    }

    // 🆕 Función para forzar la actualización de un día específico
    fun updateDaySteps(date: String, steps: Int) {
        val currentHistory = _dailyStepsHistory.value.toMutableMap()
        currentHistory[date] = steps
        _dailyStepsHistory.value = currentHistory
        println("📊 Actualización manual para $date: $steps pasos")
    }
}