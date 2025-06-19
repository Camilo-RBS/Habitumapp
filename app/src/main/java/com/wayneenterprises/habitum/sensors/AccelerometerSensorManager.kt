package com.wayneenterprises.habitum.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class AccelerometerSensorManager(
    context: Context,
    private val detector: StepDetector
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun start() {
        sensorManager.registerListener(detector, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(detector)
    }
}
