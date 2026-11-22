package com.example.data.safety

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.sqrt

enum class GloveGestureType {
    DOUBLE_CHOP,
    QUICK_FLICK
}

/**
 * Cold-weather wrist & handheld motion gesture detector.
 * Uses TYPE_LINEAR_ACCELERATION (gravity removed) to recognize deliberate ~2.5g double-chop
 * wrist flicks inside 150ms..700ms while filtering out normal trail running stride bounce.
 */
class GloveGestureDetector(
    private val context: Context
) : SensorEventListener {

    companion object {
        const val GESTURE_THRESHOLD_MPS2 = 24.0 // ~2.5g deliberate downward jerk
        const val MIN_DOUBLE_CHOP_MS = 150L
        const val MAX_DOUBLE_CHOP_MS = 700L
        const val COOLDOWN_MS = 2000L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var linearAccelSensor: Sensor? = null
    private var isFallbackGravitySensor = false
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f

    private var firstChopMs: Long? = null
    private var lastTriggerMs: Long = 0L
    private var onGestureRecognized: ((GloveGestureType) -> Unit)? = null
    private var listening = false

    fun startListening(onGesture: (GloveGestureType) -> Unit) {
        if (listening) return
        onGestureRecognized = onGesture
        firstChopMs = null
        lastTriggerMs = 0L

        val linear = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (linear != null) {
            linearAccelSensor = linear
            isFallbackGravitySensor = false
        } else {
            linearAccelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            isFallbackGravitySensor = true
        }
        val sensor = linearAccelSensor ?: return
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        listening = true
    }

    fun stopListening() {
        if (!listening) return
        sensorManager?.unregisterListener(this)
        listening = false
        firstChopMs = null
        onGestureRecognized = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val rawX = event.values[0]
        val rawY = event.values[1]
        val rawZ = event.values[2]

        val lx: Double
        val ly: Double
        val lz: Double
        if (isFallbackGravitySensor) {
            val alpha = 0.8f
            gravityX = alpha * gravityX + (1f - alpha) * rawX
            gravityY = alpha * gravityY + (1f - alpha) * rawY
            gravityZ = alpha * gravityZ + (1f - alpha) * rawZ
            lx = (rawX - gravityX).toDouble()
            ly = (rawY - gravityY).toDouble()
            lz = (rawZ - gravityZ).toDouble()
        } else {
            lx = rawX.toDouble()
            ly = rawY.toDouble()
            lz = rawZ.toDouble()
        }

        val detected = processLinearAccelSample(lx, ly, lz, System.currentTimeMillis())
        if (detected != null) {
            triggerDoubleHeavyHaptic()
            onGestureRecognized?.invoke(detected)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /**
     * Pure deterministic evaluator for unit testing & live IMU streaming.
     */
    fun processLinearAccelSample(
        xMps2: Double,
        yMps2: Double,
        zMps2: Double,
        nowMs: Long
    ): GloveGestureType? {
        if (nowMs - lastTriggerMs < COOLDOWN_MS) return null

        val jerkMagnitude = sqrt(xMps2 * xMps2 + yMps2 * yMps2 + zMps2 * zMps2)
        if (jerkMagnitude <= GESTURE_THRESHOLD_MPS2) return null

        val first = firstChopMs
        return if (first == null) {
            firstChopMs = nowMs
            null
        } else {
            val diff = nowMs - first
            if (diff in MIN_DOUBLE_CHOP_MS..MAX_DOUBLE_CHOP_MS) {
                firstChopMs = null
                lastTriggerMs = nowMs
                GloveGestureType.DOUBLE_CHOP
            } else if (diff > MAX_DOUBLE_CHOP_MS) {
                firstChopMs = nowMs
                null
            } else {
                null
            }
        }
    }

    private fun triggerDoubleHeavyHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0L, 55L, 70L, 85L),
                        intArrayOf(0, 220, 0, 255),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0L, 55L, 70L, 85L), -1)
            }
        } catch (_: Exception) {
        }
    }
}
