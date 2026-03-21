package com.calico.tutor.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShakeDetected: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    companion object {
        private const val SHAKE_THRESHOLD = 30f // Higher threshold to prevent false positives
        private const val SHAKE_COOLDOWN_MS = 3000L // 3 seconds between shakes
    }

    fun startListening(): Boolean {
        return accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            true
        } ?: false
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTime = System.currentTimeMillis()
                
                // Skip if we recently detected a shake (cooldown period)
                if (currentTime - lastShakeTime < SHAKE_COOLDOWN_MS) {
                    return
                }

                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                // Calculate the change in acceleration
                val deltaX = x - lastX
                val deltaY = y - lastY
                val deltaZ = z - lastZ

                lastX = x
                lastY = y
                lastZ = z

                // Calculate total acceleration change (G-force)
                val acceleration = sqrt(
                    deltaX * deltaX + 
                    deltaY * deltaY + 
                    deltaZ * deltaZ
                )

                // Detect shake if acceleration exceeds threshold
                if (acceleration > SHAKE_THRESHOLD) {
                    lastShakeTime = currentTime
                    onShakeDetected(acceleration)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    fun isAccelerometerAvailable(): Boolean = accelerometer != null
}
