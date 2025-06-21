package com.wayneenterprises.habitum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wayneenterprises.habitum.navigation.AppNavigation
import com.wayneenterprises.habitum.services.StepCounterService
import com.wayneenterprises.habitum.ui.theme.HabitumTheme
import com.wayneenterprises.habitum.viewmodel.StepViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 🚀 INICIALIZAR SERVICIO GLOBAL DE PASOS
        initializeStepCounterService()

        setContent {
            HabitumTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun initializeStepCounterService() {
        try {
            println("🚀 MainActivity - Inicializando servicio global de pasos...")

            // Obtener la instancia singleton del ViewModel
            val stepViewModel = StepViewModel.getInstance()

            // Inicializar el servicio global
            StepCounterService.initialize(this, stepViewModel)

            // Inicializar datos del ViewModel
            stepViewModel.initializeDailySteps()

            println("✅ MainActivity - Servicio global inicializado")
            println("📊 Status: ${StepCounterService.getStatus()}")

        } catch (e: Exception) {
            println("❌ MainActivity - Error inicializando servicio: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        println("📱 MainActivity.onResume")

        if (!StepCounterService.isActive()) {
            println("🔄 MainActivity - Reactivando servicio")
            StepCounterService.startDetection()
        }
    }

    override fun onPause() {
        super.onPause()
        println("📱 MainActivity.onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("💀 MainActivity.onDestroy")
    }
}