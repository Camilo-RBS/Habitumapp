package com.wayneenterprises.habitum.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class StepViewModel : ViewModel() {
    // Estados existentes para el contador de pasos básico
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _startTime = MutableStateFlow(System.currentTimeMillis())
    val startTime: StateFlow<Long> = _startTime

    // Nuevos estados para el menú de actividades (pantalla principal)
    private val _dailySteps = MutableStateFlow(9876) // Valor inicial como en la imagen
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    private val _weeklySteps = MutableStateFlow(
        listOf(6800, 8200, 7500, 9100, 11500, 12800, 9200) // Datos de ejemplo para la semana
    )
    val weeklySteps: StateFlow<List<Int>> = _weeklySteps.asStateFlow()

    // Meta diaria de pasos
    private val _dailyGoal = MutableStateFlow(12000)
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    // Funciones existentes
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

    // Nuevas funciones para el menú de actividades
    fun incrementDailyStep() {
        _dailySteps.value += 1
        updateWeeklyData()
    }

    fun resetDailySteps() {
        _dailySteps.value = 0
    }

    fun setDailyGoal(goal: Int) {
        _dailyGoal.value = goal
    }

    // Función para obtener el porcentaje de progreso diario
    fun getDailyProgress(): Int {
        val progress = (_dailySteps.value.toFloat() / _dailyGoal.value.toFloat() * 100).toInt()
        return progress.coerceIn(0, 100)
    }

    // Función para actualizar los datos semanales
    private fun updateWeeklyData() {
        val currentData = _weeklySteps.value.toMutableList()
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        // Convertir el día de la semana al índice correcto (Lunes = 0)
        val todayIndex = when(today) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        if (todayIndex < currentData.size) {
            currentData[todayIndex] = _dailySteps.value
            _weeklySteps.value = currentData
        }
    }

    // Función para simular datos históricos (opcional)
    fun loadHistoricalData() {
        // Aquí podrías cargar datos desde una base de datos o API
        // Por ahora mantenemos los datos de ejemplo
    }

    // Función para obtener el total de pasos de la semana
    fun getWeeklyTotal(): Int {
        return _weeklySteps.value.sum()
    }

    // Función para obtener el promedio diario de la semana
    fun getWeeklyAverage(): Int {
        val total = getWeeklyTotal()
        return if (_weeklySteps.value.isNotEmpty()) total / _weeklySteps.value.size else 0
    }
}