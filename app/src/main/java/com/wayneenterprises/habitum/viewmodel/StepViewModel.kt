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

    fun cleanup() {
        stepDetectorInstance = null
        accelerometerInstance = null
    }
}

class StepViewModel : ViewModel() {

    companion object {
        // ⭐ ESTADOS GLOBALES COMPARTIDOS - Se mantienen entre cambios de pantalla
        private val _globalDailySteps = MutableStateFlow(0)
        private val _globalWeeklySteps = MutableStateFlow(mutableListOf(0, 0, 0, 0, 0, 0, 0))
        private val _globalDailyGoal = MutableStateFlow(8000)
        private val _globalLastResetDate = MutableStateFlow(getCurrentDateStringStatic())
        private val _globalInitialized = MutableStateFlow(false)
        private val _globalIsLoading = MutableStateFlow(false)

        // ⭐ INSTANCIA COMPARTIDA DEL VIEWMODEL
        @Volatile
        private var INSTANCE: StepViewModel? = null

        private fun getCurrentDateStringStatic(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        /**
         * Obtener la instancia compartida del ViewModel (Singleton)
         */
        fun getInstance(): StepViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepViewModel().also { INSTANCE = it }
            }
        }
    }

    // 🆕 REPOSITORIOS
    private val dailyStepsRepository = DailyStepsRepository()
    private val authRepository = SupabaseRepository()

    // Estados existentes para el contador de pasos básico (locales)
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _startTime = MutableStateFlow(System.currentTimeMillis())
    val startTime: StateFlow<Long> = _startTime

    // ⭐ ESTADOS PRINCIPALES - Ahora apuntan a los estados compartidos globales
    val dailySteps: StateFlow<Int> = _globalDailySteps.asStateFlow()
    val weeklySteps: StateFlow<List<Int>> = _globalWeeklySteps.asStateFlow()
    val dailyGoal: StateFlow<Int> = _globalDailyGoal.asStateFlow()
    val isLoading: StateFlow<Boolean> = _globalIsLoading.asStateFlow()

    init {
        // Inicializar solo una vez globalmente
        if (!_globalInitialized.value) {
            println("🚀 StepViewModel - NUEVA inicialización (instancia: ${hashCode()})")
            initializeGlobalDataWithDatabase()
            _globalInitialized.value = true
        } else {
            println("♻️ StepViewModel - Reutilizando datos globales: ${_globalDailySteps.value} pasos (instancia: ${hashCode()})")
        }
    }

    // 🆕 INICIALIZACIÓN MEJORADA CON BASE DE DATOS
    private fun initializeGlobalDataWithDatabase() {
        viewModelScope.launch {
            _globalIsLoading.value = true
            println("🔄 Iniciando carga de datos desde BD...")

            try {
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        user?.let { currentUser ->
                            println("👤 Usuario encontrado: ${currentUser.id}")

                            // ✅ CARGAR PASOS EXISTENTES ANTES DE VERIFICAR NUEVO DÍA
                            dailyStepsRepository.loadUserDailySteps(currentUser.id)

                            // Esperar un poco para que se carguen los datos
                            kotlinx.coroutines.delay(500)

                            // Obtener pasos de hoy desde el repositorio
                            val todaySteps = dailyStepsRepository.getTodaySteps()
                            println("📊 Pasos encontrados para hoy: $todaySteps")

                            // Verificar si es un nuevo día DESPUÉS de cargar
                            checkAndResetDailyIfNeeded(currentUser.id)

                            // Actualizar con los pasos cargados
                            _globalDailySteps.value = dailyStepsRepository.getTodaySteps()

                            // Cargar datos semanales
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

    // 🆕 MODO OFFLINE PARA CUANDO NO HAY CONEXIÓN
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

    // 🆕 CARGAR DATOS SEMANALES DESDE BASE DE DATOS
    private fun loadWeeklyDataFromDatabase() {
        val weeklySteps = dailyStepsRepository.getWeeklySteps().toMutableList()

        while (weeklySteps.size < 7) {
            weeklySteps.add(0)
        }

        // Asegurar que los pasos de hoy estén actualizados
        val todaySteps = dailyStepsRepository.getTodaySteps()
        val today = getTodayIndex()

        if (today < weeklySteps.size) {
            weeklySteps[today] = todaySteps
        }

        _globalWeeklySteps.value = weeklySteps
        println("📊 Datos semanales cargados: $weeklySteps")
        println("📊 Pasos de hoy en array semanal: ${weeklySteps[today]}")
    }

    // ⭐ FUNCIÓN PRINCIPAL: Incrementar pasos GLOBALES con BD
    fun incrementDailyStep() {
        viewModelScope.launch {
            try {
                authRepository.getCurrentUser().fold(
                    onSuccess = { user ->
                        user?.let { currentUser ->
                            checkAndResetDailyIfNeeded(currentUser.id)

                            // ✅ INCREMENTAR EN ESTADOS GLOBALES COMPARTIDOS
                            val newSteps = _globalDailySteps.value + 1
                            _globalDailySteps.value = newSteps

                            updateWeeklyData()

                            // 💾 GUARDAR CADA 10 PASOS para no sobrecargar la BD
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

    // 🆕 GUARDAR PASOS EN BASE DE DATOS
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

    // ⭐ Verificar si es un nuevo día usando datos GLOBALES
    private suspend fun checkAndResetDailyIfNeeded(userId: String) {
        val currentDate = getCurrentDateString()
        if (_globalLastResetDate.value != currentDate) {
            println("🗓️ NUEVO DÍA detectado: $currentDate")

            // Guardar pasos del día anterior si había alguno
            val previousSteps = _globalDailySteps.value
            if (previousSteps > 0) {
                println("💾 Guardando pasos del día anterior: $previousSteps")
                dailyStepsRepository.saveTodaySteps(userId, previousSteps)
            }

            // Resetear para el nuevo día
            _globalDailySteps.value = 0
            _globalLastResetDate.value = currentDate

            // Recargar datos para el nuevo día
            println("🔄 Recargando datos para nuevo día...")
            dailyStepsRepository.loadUserDailySteps(userId)

            // Esperar a que se carguen
            kotlinx.coroutines.delay(300)

            // Obtener pasos del nuevo día (debería ser 0 o los pasos ya registrados)
            val newDaySteps = dailyStepsRepository.getTodaySteps()
            _globalDailySteps.value = newDaySteps

            loadWeeklyDataFromDatabase()

            println("✅ Datos reseteados para nuevo día. Pasos cargados: $newDaySteps")
        }
    }

    // ⭐ ACTUALIZAR DATOS SEMANALES usando datos GLOBALES
    private fun updateWeeklyData() {
        val currentData = _globalWeeklySteps.value.toMutableList()
        val today = getTodayIndex()

        if (today < currentData.size) {
            currentData[today] = _globalDailySteps.value
            _globalWeeklySteps.value = currentData
        }
    }

    // ⭐ FUNCIÓN PRINCIPAL para llamar desde las pantallas
    fun initializeDailySteps() {
        if (!_globalInitialized.value) {
            println("🔄 Inicializando desde pantalla...")
            initializeGlobalDataWithDatabase()
            _globalInitialized.value = true
        } else {
            // Verificar si necesitamos recargar datos (por ejemplo, si han pasado mucho tiempo)
            println("📱 StepViewModel ya inicializado - Pasos GLOBALES: ${_globalDailySteps.value}")

            // Opcional: Forzar una recarga si los datos parecen vacíos
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

    // 🆕 FUNCIÓN PARA SINCRONIZAR MANUALMENTE
    fun syncWithDatabase() {
        viewModelScope.launch {
            _globalIsLoading.value = true
            println("🔄 Sincronización manual iniciada...")

            authRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    user?.let { currentUser ->
                        println("🔄 Sincronizando con BD...")

                        // ✅ SIEMPRE guardar pasos actuales, incluso si son 0
                        saveDailyStepsToDatabase(currentUser.id)

                        // Esperar a que se guarde
                        kotlinx.coroutines.delay(500)

                        // Recargar desde BD
                        dailyStepsRepository.loadUserDailySteps(currentUser.id)

                        // Esperar a que se cargue
                        kotlinx.coroutines.delay(500)

                        // Actualizar datos locales
                        val refreshedSteps = dailyStepsRepository.getTodaySteps()
                        _globalDailySteps.value = refreshedSteps

                        loadWeeklyDataFromDatabase()

                        println("✅ Sincronización completada - Pasos actuales: $refreshedSteps")
                    }
                },
                onFailure = { exception ->
                    println("❌ Error en sincronización: ${exception.message}")
                }
            )

            _globalIsLoading.value = false
        }
    }

    // 🆕 FUNCIÓN PARA FORZAR GUARDADO INMEDIATO (para el servicio)
    fun forceSave() {
        viewModelScope.launch {
            authRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    user?.let { currentUser ->
                        val stepsToSave = _globalDailySteps.value
                        println("💾 GUARDADO FORZADO de $stepsToSave pasos")
                        saveDailyStepsToDatabase(currentUser.id)

                        // Esperar a que se complete el guardado
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

    fun resetDailySteps() {
        viewModelScope.launch {
            _globalDailySteps.value = 0
            updateWeeklyData()

            authRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    user?.let { saveDailyStepsToDatabase(it.id) }
                },
                onFailure = { }
            )
        }
    }

    fun setDailyGoal(goal: Int) {
        _globalDailyGoal.value = goal
    }

    fun simulateSteps(count: Int) {
        repeat(count) {
            incrementDailyStep()
        }
        println("🎯 Simulados $count pasos. Total GLOBAL: ${_globalDailySteps.value}")
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

    fun getDailyProgress(): Float {
        val progress = (_globalDailySteps.value.toFloat() / _globalDailyGoal.value.toFloat()).coerceIn(0f, 1f)
        return progress
    }

    fun getWeeklyTotal(): Int {
        return _globalWeeklySteps.value.sum()
    }

    fun getWeeklyAverage(): Int {
        val total = getWeeklyTotal()
        val daysWithSteps = _globalWeeklySteps.value.count { it > 0 }
        return if (daysWithSteps > 0) total / daysWithSteps else 0
    }

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

    fun printCurrentState() {
        println("📊 Estado GLOBAL del StepViewModel:")
        println("   Pasos diarios: ${_globalDailySteps.value}")
        println("   Meta diaria: ${_globalDailyGoal.value}")
        println("   Progreso: ${(getDailyProgress() * 100).toInt()}%")
        println("   Pasos semanales: ${_globalWeeklySteps.value}")
        println("   Total semanal: ${getWeeklyTotal()}")
        println("   Inicializado globalmente: ${_globalInitialized.value}")
        println("   Loading: ${_globalIsLoading.value}")

        dailyStepsRepository.debugState()
    }

    override fun onCleared() {
        super.onCleared()
        println("🧹 StepViewModel limpiado (instancia: ${hashCode()})")
        // NO limpiar la instancia singleton ni los datos globales
    }
}