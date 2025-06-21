package com.wayneenterprises.habitum.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayneenterprises.habitum.repository.DailyStepsRepository
import com.wayneenterprises.habitum.repository.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class StepViewModel : ViewModel() {

    companion object {
        private val _globalDailySteps = MutableStateFlow(0)
        private val _globalWeeklySteps = MutableStateFlow(mutableListOf(0, 0, 0, 0, 0, 0, 0))
        private val _globalDailyGoal = MutableStateFlow(8000)
        private val _globalLastResetDate = MutableStateFlow(getCurrentDateStringStatic())
        private val _globalInitialized = MutableStateFlow(false)
        private val _globalIsLoading = MutableStateFlow(false)

        @Volatile
        private var INSTANCE: StepViewModel? = null

        private fun getCurrentDateStringStatic(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun getInstance(): StepViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepViewModel().also { INSTANCE = it }
            }
        }
    }

    private val dailyStepsRepository = DailyStepsRepository()
    private val authRepository = SupabaseRepository()

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _startTime = MutableStateFlow(System.currentTimeMillis())
    val startTime: StateFlow<Long> = _startTime

    val dailySteps: StateFlow<Int> = _globalDailySteps.asStateFlow()
    val weeklySteps: StateFlow<List<Int>> = _globalWeeklySteps.asStateFlow()
    val dailyGoal: StateFlow<Int> = _globalDailyGoal.asStateFlow()
    val isLoading: StateFlow<Boolean> = _globalIsLoading.asStateFlow()

    init {
        if (!_globalInitialized.value) {
            println("🚀 StepViewModel - NUEVA inicialización (instancia: ${hashCode()})")
            initializeGlobalDataWithDatabase()
            _globalInitialized.value = true
        } else {
            println("♻️ StepViewModel - Reutilizando datos globales: ${_globalDailySteps.value} pasos (instancia: ${hashCode()})")
        }
    }

    private fun initializeGlobalDataWithDatabase() {
        viewModelScope.launch {
            _globalIsLoading.value = true
            println("🔄 Iniciando carga de datos desde BD...")

            try {
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        user?.let { currentUser ->
                            println("👤 Usuario encontrado: ${currentUser.id}")

                            dailyStepsRepository.loadUserDailySteps(currentUser.id)

                            kotlinx.coroutines.delay(500)

                            val todaySteps = dailyStepsRepository.getTodaySteps()
                            println("📊 Pasos encontrados para hoy: $todaySteps")

                            checkAndResetDailyIfNeeded(currentUser.id)

                            _globalDailySteps.value = dailyStepsRepository.getTodaySteps()

                            loadWeeklyDataFromDatabase()

                            println("✅ Datos cargados - Pasos hoy: ${_globalDailySteps.value}")
                        } ?: run {
                            println("⚠️ No hay usuario autenticado")
                            initializeOfflineMode()
                        }
                    },
                    onFailure = { exception ->
                        println("❌ Error obteniendo usuario: ${exception.message}")
                        initializeOfflineMode()
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en inicialización: ${e.message}")
                e.printStackTrace()
                initializeOfflineMode()
            } finally {
                _globalIsLoading.value = false
                println("🏁 Inicialización completada - Pasos finales: ${_globalDailySteps.value}")
            }
        }
    }

    private fun initializeOfflineMode() {
        println("📱 Iniciando en modo offline")
        val currentDate = getCurrentDateString()
        if (_globalLastResetDate.value != currentDate) {
            _globalDailySteps.value = 0
            _globalLastResetDate.value = currentDate
        }

        val demoWeekData = mutableListOf(0, 0, 0, 0, 0, 0, 0)
        val today = getTodayIndex()
        demoWeekData[today] = _globalDailySteps.value
        _globalWeeklySteps.value = demoWeekData
    }

    private fun loadWeeklyDataFromDatabase() {
        val weeklySteps = dailyStepsRepository.getWeeklySteps().toMutableList()

        while (weeklySteps.size < 7) {
            weeklySteps.add(0)
        }

        val todaySteps = dailyStepsRepository.getTodaySteps()
        val today = getTodayIndex()

        if (today < weeklySteps.size) {
            weeklySteps[today] = todaySteps
        }

        _globalWeeklySteps.value = weeklySteps
        println("📊 Datos semanales cargados: $weeklySteps")
        println("📊 Pasos de hoy en array semanal: ${weeklySteps[today]}")
    }

    fun incrementDailyStep() {
        viewModelScope.launch {
            try {
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        user?.let { currentUser ->
                            checkAndResetDailyIfNeeded(currentUser.id)

                            val newSteps = _globalDailySteps.value + 1
                            _globalDailySteps.value = newSteps

                            updateWeeklyData()

                            if (newSteps % 10 == 0) {
                                saveDailyStepsToDatabase(currentUser.id)
                                println("💾 Auto-guardado cada 10 pasos: $newSteps")
                            }

                            println("📊 Pasos GLOBALES incrementados: $newSteps")
                        } ?: run {
                            _globalDailySteps.value += 1
                            updateWeeklyData()
                            println("📊 Pasos offline incrementados: ${_globalDailySteps.value}")
                        }
                    },
                    onFailure = {
                        _globalDailySteps.value += 1
                        updateWeeklyData()
                        println("📊 Pasos fallback incrementados: ${_globalDailySteps.value}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error en incrementDailyStep: ${e.message}")
                _globalDailySteps.value += 1
                updateWeeklyData()
            }
        }
    }

    private fun saveDailyStepsToDatabase(userId: String) {
        viewModelScope.launch {
            try {
                val currentSteps = _globalDailySteps.value
                println("💾 Guardando $currentSteps pasos en BD...")

                dailyStepsRepository.saveTodaySteps(userId, currentSteps).fold(
                    onSuccess = {
                        println("✅ Pasos guardados en BD: $currentSteps")
                    },
                    onFailure = { exception ->
                        println("⚠️ Error guardando pasos: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                println("❌ Excepción guardando pasos: ${e.message}")
            }
        }
    }

    private suspend fun checkAndResetDailyIfNeeded(userId: String) {
        val currentDate = getCurrentDateString()
        if (_globalLastResetDate.value != currentDate) {
            println("🗓️ NUEVO DÍA detectado: $currentDate")

            val previousSteps = _globalDailySteps.value
            if (previousSteps > 0) {
                println("💾 Guardando pasos del día anterior: $previousSteps")
                dailyStepsRepository.saveTodaySteps(userId, previousSteps)
            }

            _globalDailySteps.value = 0
            _globalLastResetDate.value = currentDate

            println("🔄 Recargando datos para nuevo día...")
            dailyStepsRepository.loadUserDailySteps(userId)

            kotlinx.coroutines.delay(300)

            val newDaySteps = dailyStepsRepository.getTodaySteps()
            _globalDailySteps.value = newDaySteps

            loadWeeklyDataFromDatabase()

            println("✅ Datos reseteados para nuevo día. Pasos cargados: $newDaySteps")
        }
    }

    private fun updateWeeklyData() {
        val currentData = _globalWeeklySteps.value.toMutableList()
        val today = getTodayIndex()

        if (today < currentData.size) {
            currentData[today] = _globalDailySteps.value
            _globalWeeklySteps.value = currentData
        }
    }

    fun initializeDailySteps() {
        if (!_globalInitialized.value) {
            println("🔄 Inicializando desde pantalla...")
            initializeGlobalDataWithDatabase()
            _globalInitialized.value = true
        } else {
            println("📱 StepViewModel ya inicializado - Pasos GLOBALES: ${_globalDailySteps.value}")

            if (_globalDailySteps.value == 0 && !_globalIsLoading.value) {
                println("🔄 Datos vacíos detectados, forzando recarga...")
                viewModelScope.launch {
                    try {
                        authRepository.getCurrentUser().fold(
                            onSuccess = { user ->
                                user?.let { currentUser ->
                                    dailyStepsRepository.loadUserDailySteps(currentUser.id)
                                    kotlinx.coroutines.delay(200)
                                    val todaySteps = dailyStepsRepository.getTodaySteps()
                                    if (todaySteps > 0) {
                                        _globalDailySteps.value = todaySteps
                                        loadWeeklyDataFromDatabase()
                                        println("🔄 Datos recargados: $todaySteps pasos")
                                    }
                                }
                            },
                            onFailure = { }
                        )
                    } catch (e: Exception) {
                        println("❌ Error en recarga: ${e.message}")
                    }
                }
            }
        }
    }



    fun forceSave() {
        viewModelScope.launch {
            authRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    user?.let { currentUser ->
                        val stepsToSave = _globalDailySteps.value
                        println("💾 GUARDADO FORZADO de $stepsToSave pasos")
                        saveDailyStepsToDatabase(currentUser.id)

                        kotlinx.coroutines.delay(300)

                        println("✅ Guardado forzado completado")
                    }
                },
                onFailure = {
                    println("❌ No se puede forzar guardado: usuario no autenticado")
                }
            )
        }
    }

    // Funciones utilitarias
    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
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


    fun getWeeklyTotal(): Int {
        return _globalWeeklySteps.value.sum()
    }

    fun getWeeklyAverage(): Int {
        val total = getWeeklyTotal()
        val daysWithSteps = _globalWeeklySteps.value.count { it > 0 }
        return if (daysWithSteps > 0) total / daysWithSteps else 0
    }



    override fun onCleared() {
        super.onCleared()
        println("🧹 StepViewModel limpiado (instancia: ${hashCode()})")
    }
}