package com.example.mythomash

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log

class FlashlightShakeDetector(context: Context) : SensorEventListener {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Shake detection variables
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isFlashlightOn = false

    // Configuration
    private val shakeThreshold = 10f // Adjust this sensitivity as needed
    private val shakeCooldown = 1000L // 500ms cooldown between shakes (prevents multiple detections for one shake)
    private var lastShakeTime: Long = 0
    private val updateThreshold = 100L // Update every 100ms

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        turnOffFlashlight()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val currentTime = System.currentTimeMillis()
            val timeDifference = currentTime - lastUpdate

            if (timeDifference > updateThreshold) {
                lastUpdate = currentTime

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val deltaX = Math.abs(x - lastX)
                val deltaY = Math.abs(y - lastY)
                val deltaZ = Math.abs(z - lastZ)

                // Check if the change in any axis is greater than the threshold
                if ((deltaX > shakeThreshold || deltaY > shakeThreshold || deltaZ > shakeThreshold)
                    && (currentTime - lastShakeTime > shakeCooldown)) {
                    lastShakeTime = currentTime
                    toggleFlashlight()
                }

                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun toggleFlashlight() {
        if (isFlashlightOn) {
            turnOffFlashlight()
        } else {
            turnOnFlashlight()
        }
    }

    private fun turnOnFlashlight() {
        try {
            val cameraId = getCameraId()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, true)
                isFlashlightOn = true
                Log.d("FlashlightShakeDetector", "Flashlight turned ON")
            }
        } catch (e: Exception) {
            Log.e("FlashlightShakeDetector", "Error turning on flashlight", e)
        }
    }

    private fun turnOffFlashlight() {
        try {
            val cameraId = getCameraId()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, false)
                isFlashlightOn = false
                Log.d("FlashlightShakeDetector", "Flashlight turned OFF")
            }
        } catch (e: Exception) {
            Log.e("FlashlightShakeDetector", "Error turning off flashlight", e)
        }
    }

    private fun getCameraId(): String? {
        return try {
            cameraManager.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.e("FlashlightShakeDetector", "Error getting camera ID", e)
            null
        }
    }
}