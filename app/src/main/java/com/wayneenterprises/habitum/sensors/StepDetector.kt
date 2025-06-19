package com.wayneenterprises.habitum.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.sqrt
import kotlin.math.abs

class StepDetector : SensorEventListener {

    private val listeners = mutableListOf<StepListener>()

    private var lastStepTimeNs: Long = 0
    private var lastMagnitude: Float = 0f

    // Sensibilidad ajustada
    private val threshold = 1.6f                 // Mayor = menos sensible
    private val minStepIntervalNs = 300_000_000L // 300ms entre pasos

    private var firstStepIgnored = false  // Evita contar paso falso inicial

    fun registerListener(listener: StepListener) {
        listeners.add(listener)
        firstStepIgnored = false // Reinicia la bandera al registrar
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x * x + y * y + z * z)
        val delta = abs(magnitude - lastMagnitude)

        if (delta > threshold) {
            val currentTimeNs = event.timestamp
            if (currentTimeNs - lastStepTimeNs > minStepIntervalNs) {
                if (!firstStepIgnored) {
                    firstStepIgnored = true
                } else {
                    listeners.forEach { it.step(currentTimeNs) }
                }
                lastStepTimeNs = currentTimeNs
            }
        }

        lastMagnitude = magnitude
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
