package com.wayneenterprises.habitum.viewmodel


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WalkingUiState(
    val steps: Int = 0,
    val elapsedTimeSeconds: Long = 0,
    val distanceKm: Double = 0.0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val startTime: Long = 0L,
    val progressData: List<Float> = emptyList(),
    val formattedTime: String = "00:00:00"
)

class WalkingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WalkingUiState())
    val uiState: StateFlow<WalkingUiState> = _uiState.asStateFlow()

    private var pausedTime: Long = 0L
    private val stepLength = 0.8 // metros por paso (promedio)

    init {
        start()
    }

    fun incrementStep() {
        val currentState = _uiState.value
        val newSteps = currentState.steps + 1
        val newDistance = (newSteps * stepLength) / 1000.0 // convertir a km

        _uiState.value = currentState.copy(
            steps = newSteps,
            distanceKm = newDistance
        )

        updateProgressData()
    }

    fun start() {
        val currentTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            isRunning = true,
            isPaused = false,
            startTime = currentTime
        )
    }

    fun pauseResume() {
        val currentState = _uiState.value
        if (currentState.isRunning) {
            // Pausar
            pausedTime += System.currentTimeMillis() - currentState.startTime
            _uiState.value = currentState.copy(
                isRunning = false,
                isPaused = true
            )
        } else {
            // Reanudar
            _uiState.value = currentState.copy(
                isRunning = true,
                isPaused = false,
                startTime = System.currentTimeMillis()
            )
        }
    }

    fun stop() {
        _uiState.value = _uiState.value.copy(
            isRunning = false,
            isPaused = false
        )
    }

    fun reset() {
        pausedTime = 0L
        _uiState.value = WalkingUiState()
    }

    fun updateTime() {
        val currentState = _uiState.value
        if (currentState.isRunning) {
            val currentTime = System.currentTimeMillis()
            val totalElapsed = (currentTime - currentState.startTime + pausedTime) / 1000

            _uiState.value = currentState.copy(
                elapsedTimeSeconds = totalElapsed,
                formattedTime = formatTime(totalElapsed)
            )
        }
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun updateProgressData() {
        val currentState = _uiState.value
        val newData = currentState.progressData.toMutableList()

        // Simular datos de progreso basados en pasos
        val progressValue = (currentState.steps % 100).toFloat()
        newData.add(progressValue)

        // Mantener solo los últimos 50 puntos de datos
        if (newData.size > 50) {
            newData.removeAt(0)
        }

        _uiState.value = currentState.copy(progressData = newData)
    }
}