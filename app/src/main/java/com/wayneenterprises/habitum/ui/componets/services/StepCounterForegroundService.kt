package com.wayneenterprises.habitum.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.wayneenterprises.habitum.MainActivity
import com.wayneenterprises.habitum.R
import com.wayneenterprises.habitum.sensors.AccelerometerSensorManager
import com.wayneenterprises.habitum.sensors.StepDetector
import com.wayneenterprises.habitum.sensors.StepListener
import com.wayneenterprises.habitum.viewmodel.StepViewModel
import kotlinx.coroutines.*

class StepCounterForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "step_counter_channel"
        private const val ACTION_START_SERVICE = "START_STEP_COUNTING"
        private const val ACTION_STOP_SERVICE = "STOP_STEP_COUNTING"

        fun startService(context: Context) {
            val intent = Intent(context, StepCounterForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StepCounterForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    private var stepDetector: StepDetector? = null
    private var accelerometerManager: AccelerometerSensorManager? = null
    private var stepViewModel: StepViewModel? = null
    private var isRunning = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoSaveJob: Job? = null
    private var notificationUpdateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        println("🚀 StepCounterForegroundService creado")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startStepCounting()
            }
            ACTION_STOP_SERVICE -> {
                stopStepCounting()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startStepCounting() {
        if (isRunning) {
            println("🔄 Servicio ya está funcionando")
            return
        }

        try {
            stepDetector = StepDetector()
            accelerometerManager = AccelerometerSensorManager(this, stepDetector!!)
            stepViewModel = StepViewModel.getInstance()

            stepDetector!!.registerListener(object : StepListener {
                override fun step(timeNs: Long) {
                    println("👣 Paso detectado en servicio foreground")
                    stepViewModel?.incrementDailyStep()
                    updateNotificationWithSteps()
                }
            })

            stepViewModel?.initializeDailySteps()
            accelerometerManager?.start()
            isRunning = true

            val notification = createStepNotification(0, 8000)
            startForeground(NOTIFICATION_ID, notification)

            startAutoSave()
            startNotificationUpdates()

            println("✅ Servicio foreground iniciado correctamente")

        } catch (e: Exception) {
            println("❌ Error iniciando servicio: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun stopStepCounting() {
        if (!isRunning) return

        try {
            runBlocking {
                stepViewModel?.forceSave()
                delay(1000)
            }

            accelerometerManager?.stop()
            autoSaveJob?.cancel()
            notificationUpdateJob?.cancel()

            stepDetector = null
            accelerometerManager = null
            isRunning = false

            println("⏹️ Servicio detenido")

        } catch (e: Exception) {
            println("❌ Error deteniendo servicio: ${e.message}")
        }
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()

        autoSaveJob = serviceScope.launch {
            while (isActive && isRunning) {
                try {
                    delay(30000)
                    println("💾 Auto-guardado desde servicio foreground...")
                    stepViewModel?.forceSave()
                } catch (e: Exception) {
                    println("❌ Error en auto-guardado: ${e.message}")
                }
            }
        }
    }

    private fun startNotificationUpdates() {
        notificationUpdateJob?.cancel()

        notificationUpdateJob = serviceScope.launch {
            while (isActive && isRunning) {
                try {
                    delay(5000)
                    updateNotificationWithSteps()
                } catch (e: Exception) {
                    println("❌ Error actualizando notificación: ${e.message}")
                }
            }
        }
    }

    private fun updateNotificationWithSteps() {
        stepViewModel?.let { viewModel ->
            val currentSteps = viewModel.dailySteps.value
            val goal = viewModel.dailyGoal.value
            val notification = createStepNotification(currentSteps, goal)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Contador de Pasos",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoreo continuo de pasos diarios"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createStepNotification(currentSteps: Int, goal: Int): Notification {
        val progress = if (goal > 0) (currentSteps * 100 / goal).coerceAtMost(100) else 0
        val remaining = (goal - currentSteps).coerceAtLeast(0)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, StepCounterForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("👣 Pasos de Hoy: ${String.format("%,d", currentSteps)}")
            .setContentText(
                when {
                    progress >= 100 -> "🎉 ¡Meta alcanzada! ¡Excelente trabajo!"
                    progress >= 75 -> "💪 ¡Casi ahí! Solo faltan $remaining pasos"
                    progress >= 50 -> "🚀 ¡Vas genial! $progress% completado"
                    progress >= 25 -> "👍 ¡Buen comienzo! Sigue así"
                    else -> "🌟 Meta: ${String.format("%,d", goal)} pasos ($progress%)"
                }
            )
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Detener",
                stopPendingIntent
            )
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .setAutoCancel(false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("💀 Destruyendo servicio foreground...")

        runBlocking {
            try {
                stepViewModel?.forceSave()
                delay(500)
            } catch (e: Exception) {
                println("❌ Error en guardado final: ${e.message}")
            }
        }

        stopStepCounting()
        serviceScope.cancel()

        println("✅ Servicio foreground destruido")
    }
}