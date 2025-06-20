package com.wayneenterprises.habitum.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

// Singleton para mantener referencias globales del detector
object StepCounter {
    private var stepDetectorInstance: Any? = null
    private var accelerometerInstance: Any? = null

    fun setStepDetector(detector: Any) {
        stepDetectorInstance = detector
    }

    fun setAccelerometer(accelerometer: Any) {
        accelerometerInstance = accelerometer
    }

    fun getStepDetector(): Any? = stepDetectorInstance
    fun getAccelerometer(): Any? = accelerometerInstance

    fun isInitialized(): Boolean = stepDetectorInstance != null
}

class StepViewModel : ViewModel() {

    companion object {
        // ⭐ ESTADOS GLOBALES - Se mantienen entre cambios de pantalla
        private val _globalDailySteps = MutableStateFlow(0)
        private val _globalWeeklySteps = MutableStateFlow(mutableListOf(0, 0, 0, 0, 0, 0, 0))
        private val _globalDailyGoal = MutableStateFlow(8000)
        private val _globalLastResetDate = MutableStateFlow(getCurrentDateStringStatic())
        private val _globalDailyStepsHistory = MutableStateFlow<Map<String, Int>>(emptyMap())
        private val _globalInitialized = MutableStateFlow(false)

        private fun getCurrentDateStringStatic(): String {
            val calendar = Calendar.getInstance()
            return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
        }
    }

    // Estados existentes para el contador de pasos básico (locales)
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _startTime = MutableStateFlow(System.currentTimeMillis())
    val startTime: StateFlow<Long> = _startTime

    // ⭐ ESTADOS PRINCIPALES - Ahora apuntan a los globales
    val dailySteps: StateFlow<Int> = _globalDailySteps.asStateFlow()
    val weeklySteps: StateFlow<List<Int>> = _globalWeeklySteps.asStateFlow()
    val dailyGoal: StateFlow<Int> = _globalDailyGoal.asStateFlow()

    init {
        // Inicializar solo una vez globalmente
        if (!_globalInitialized.value) {
            initializeGlobalData()
            _globalInitialized.value = true
            println("🚀 StepViewModel - Inicialización GLOBAL completada")
        } else {
            println("♻️ StepViewModel - Usando datos globales existentes: ${_globalDailySteps.value} pasos")
        }
    }

    // Funciones existentes mejoradas
    fun incrementStep() {
        _steps.value += 1
    }

    fun resetSteps() {
        _steps.value = 0
        _startTime.value = System.currentTimeMillis()
    }

    fun startTimer() {
        _startTime.value = System.currentTimeMillis()
    }

    // ⭐ FUNCIÓN PRINCIPAL: Incrementar pasos GLOBALES
    fun incrementDailyStep() {
        checkAndResetDailyIfNeeded()

        _globalDailySteps.value += 1
        updateWeeklyData()
        saveDailyStepsToHistory()

        println("📊 GLOBAL - Pasos incrementados: ${_globalDailySteps.value}")
    }

    // ⭐ Verificar si es un nuevo día usando datos GLOBALES
    private fun checkAndResetDailyIfNeeded() {
        val currentDate = getCurrentDateString()
        if (_globalLastResetDate.value != currentDate) {
            // Es un nuevo día - guardar pasos del día anterior y resetear
            saveDailyStepsToHistory()
            _globalDailySteps.value = 0
            _globalLastResetDate.value = currentDate
            println("🗓️ GLOBAL - Nuevo día detectado: $currentDate - Pasos reseteados")

            // Resetear el día actual en datos semanales
            val currentData = _globalWeeklySteps.value.toMutableList()
            val today = getTodayIndex()
            if (today < currentData.size) {
                currentData[today] = 0
                _globalWeeklySteps.value = currentData
            }
        }
    }

    // Obtener fecha actual como string
    private fun getCurrentDateString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun getTodayIndex(): Int {
        val calendar = Calendar.getInstance()
        return when(calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    // ⭐ Guardar pasos diarios en historial GLOBAL
    private fun saveDailyStepsToHistory() {
        val currentHistory = _globalDailyStepsHistory.value.toMutableMap()
        currentHistory[_globalLastResetDate.value] = _globalDailySteps.value
        _globalDailyStepsHistory.value = currentHistory
    }

    // Función para obtener el porcentaje de progreso diario
    fun getDailyProgress(): Float {
        val progress = (_globalDailySteps.value.toFloat() / _globalDailyGoal.value.toFloat()).coerceIn(0f, 1f)
        return progress
    }

    // ⭐ ACTUALIZAR DATOS SEMANALES usando datos GLOBALES
    private fun updateWeeklyData() {
        val currentData = _globalWeeklySteps.value.toMutableList()
        val today = getTodayIndex()

        // Actualizar solo el día actual
        if (today < currentData.size) {
            currentData[today] = _globalDailySteps.value
            _globalWeeklySteps.value = currentData
        }
    }

    // ⭐ CARGAR DATOS HISTÓRICOS usando datos GLOBALES
    fun loadWeeklyDataFromHistory() {
        val calendar = Calendar.getInstance()
        val currentData = mutableListOf(0, 0, 0, 0, 0, 0, 0)

        // Obtener el lunes de esta semana
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY

        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        // Cargar datos de cada día de la semana
        for (i in 0..6) {
            val dateString = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
            val stepsForDay = _globalDailyStepsHistory.value[dateString] ?: 0

            currentData[i] = stepsForDay
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Asegurar que hoy tenga los pasos actuales
        val today = getTodayIndex()
        currentData[today] = _globalDailySteps.value

        _globalWeeklySteps.value = currentData
    }

    // ⭐ Inicialización de datos GLOBALES
    private fun initializeGlobalData() {
        checkAndResetDailyIfNeeded()
        loadWeeklyDataFromHistory()

        // Simular algunos datos históricos si no existen
        val currentData = _globalWeeklySteps.value.toMutableList()
        for (i in 0 until 7) {
            if (currentData[i] == 0 && i != getTodayIndex()) {
                currentData[i] = (0..500).random() // Datos simulados para días anteriores
            }
        }
        _globalWeeklySteps.value = currentData

        println("✅ Datos GLOBALES inicializados - Pasos: ${_globalDailySteps.value}, Semana: ${_globalWeeklySteps.value}")
    }

    fun resetDailySteps() {
        _globalDailySteps.value = 0
        updateWeeklyData()
    }

    fun setDailyGoal(goal: Int) {
        _globalDailyGoal.value = goal
    }

    // Función para obtener el total de pasos de la semana
    fun getWeeklyTotal(): Int {
        return _globalWeeklySteps.value.sum()
    }

    // Función para obtener el promedio diario de la semana
    fun getWeeklyAverage(): Int {
        val total = getWeeklyTotal()
        val daysWithSteps = _globalWeeklySteps.value.count { it > 0 }
        return if (daysWithSteps > 0) total / daysWithSteps else 0
    }

    // Función para simular pasos (útil para testing)
    fun simulateSteps(count: Int) {
        repeat(count) {
            incrementDailyStep()
        }
        println("🎯 Simulados $count pasos. Total GLOBAL: ${_globalDailySteps.value}")
    }

    // ⭐ FUNCIÓN PRINCIPAL para llamar desde las pantallas (segura de llamar múltiples veces)
    fun initializeDailySteps() {
        if (!_globalInitialized.value) {
            initializeGlobalData()
            _globalInitialized.value = true
        }
        println("📱 StepViewModel iniciado en pantalla - Pasos GLOBALES: ${_globalDailySteps.value}")
    }

    // Función para obtener estadísticas
    fun getStepStatistics(): Map<String, Any> {
        return mapOf(
            "dailySteps" to _globalDailySteps.value,
            "dailyGoal" to _globalDailyGoal.value,
            "dailyProgress" to (getDailyProgress() * 100).toInt(),
            "weeklyTotal" to getWeeklyTotal(),
            "weeklyAverage" to getWeeklyAverage(),
            "stepsToGoal" to (_globalDailyGoal.value - _globalDailySteps.value).coerceAtLeast(0),
            "isGoalReached" to (_globalDailySteps.value >= _globalDailyGoal.value)
        )
    }

    // Para debugging
    fun printCurrentState() {
        println("📊 Estado GLOBAL del StepViewModel:")
        println("   Pasos diarios: ${_globalDailySteps.value}")
        println("   Meta diaria: ${_globalDailyGoal.value}")
        println("   Progreso: ${(getDailyProgress() * 100).toInt()}%")
        println("   Pasos semanales: ${_globalWeeklySteps.value}")
        println("   Total semanal: ${getWeeklyTotal()}")
        println("   Inicializado globalmente: ${_globalInitialized.value}")
    }
}