package com.wayneenterprises.habitum.services

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.wayneenterprises.habitum.viewmodel.StepViewModel
import kotlinx.coroutines.*

object StepCounterService : DefaultLifecycleObserver {

    private var stepViewModel: StepViewModel? = null
    private var isInitialized = false
    private var context: Context? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun initialize(context: Context, viewModel: StepViewModel) {
        if (isInitialized) {
            println("🔄 StepCounterService ya está inicializado")
            stepViewModel = viewModel
            return
        }

        println("🚀 Inicializando StepCounterService MEJORADO...")

        try {
            this.context = context.applicationContext
            stepViewModel = viewModel

            ProcessLifecycleOwner.get().lifecycle.addObserver(this)

            // ✅ INICIAR EL FOREGROUND SERVICE INMEDIATAMENTE
            StepCounterForegroundService.startService(context)

            isInitialized = true
            println("✅ StepCounterService inicializado con Foreground Service")

        } catch (e: Exception) {
            println("❌ Error inicializando StepCounterService: ${e.message}")
            e.printStackTrace()
        }
    }

    fun startDetection() {
        if (!isInitialized) {
            println("⚠️ StepCounterService no está inicializado")
            return
        }

        context?.let { ctx ->
            println("✅ Iniciando detección a través de Foreground Service")
            StepCounterForegroundService.startService(ctx)
        }
    }

    fun stopDetection() {
        context?.let { ctx ->
            println("⏹️ Deteniendo Foreground Service")
            StepCounterForegroundService.stopService(ctx)
        }
    }

    fun updateViewModel(viewModel: StepViewModel) {
        stepViewModel = viewModel
        println("🔄 ViewModel actualizado en StepCounterService")
    }

    fun isActive(): Boolean = isInitialized

    fun getStatus(): String {
        return when {
            !isInitialized -> "No inicializado"
            else -> "Activo con Foreground Service"
        }
    }

    fun cleanup() {
        println("🧹 Limpiando StepCounterService...")

        try {
            runBlocking {
                try {
                    println("💾 Guardado final de pasos...")
                    stepViewModel?.forceSave()
                    delay(1000)
                } catch (e: Exception) {
                    println("❌ Error en guardado final: ${e.message}")
                }
            }

            context?.let { ctx ->
                StepCounterForegroundService.stopService(ctx)
            }

            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
            stepViewModel = null
            context = null
            isInitialized = false

            serviceScope.cancel()

            println("✅ StepCounterService limpiado")
        } catch (e: Exception) {
            println("❌ Error limpiando StepCounterService: ${e.message}")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        println("🌟 App en primer plano - Foreground Service ya activo")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        println("🌙 App en segundo plano - Foreground Service continuará funcionando")

        stepViewModel?.let { viewModel ->
            serviceScope.launch {
                try {
                    viewModel.forceSave()
                } catch (e: Exception) {
                    println("❌ Error guardando en onStop: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        println("💀 App destruida - Manteniendo Foreground Service activo")
    }
}