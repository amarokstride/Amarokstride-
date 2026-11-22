package com.example.data.safety

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Native Android Winter Trail Accelerometer Fall & Prolonged Immobility Detector.
 *
 * Stage 1: Detects a hard impact spike > 3.8g (~37.28 m/s²).
 * Stage 2: Monitors the subsequent 30-second window for total stillness (< 0.6 m/s² avg movement).
 * Fires [onFallDetected] when both stages confirm a likely fall + immobilization.
 */
class WinterFallDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.applicationContext
        .getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val linearAccelSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val rawAccelSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastImpactG = MutableStateFlow(0.0)
    val lastImpactG: StateFlow<Double> = _lastImpactG.asStateFlow()

    private var onFallDetectedCallback: (() -> Unit)? = null
    private var monitoringImmobility = false
    private var lastHighImpactTimeMs = 0L
    private val postImpactMagnitudes = ArrayList<Double>(256)

    // Simple low-pass gravity estimate when falling back to raw TYPE_ACCELEROMETER
    private val gravity = DoubleArray(3)
    private var usingRawAccelerometer = false

    fun startListening(onFallDetected: () -> Unit) {
        if (_isListening.value) return
        val manager = sensorManager ?: return
        onFallDetectedCallback = onFallDetected
        monitoringImmobility = false
        postImpactMagnitudes.clear()

        val targetSensor = linearAccelSensor ?: rawAccelSensor ?: return
        usingRawAccelerometer = (linearAccelSensor == null)
        gravity.fill(0.0)

        manager.registerListener(this, targetSensor, SensorManager.SENSOR_DELAY_GAME)
        _isListening.value = true
    }

    fun stopListening() {
        if (!_isListening.value) return
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {
        }
        _isListening.value = false
        monitoringImmobility = false
        postImpactMagnitudes.clear()
        onFallDetectedCallback = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val ax: Double
        val ay: Double
        val az: Double

        if (usingRawAccelerometer) {
            val alpha = 0.8
            gravity[0] = alpha * gravity[0] + (1.0 - alpha) * event.values[0]
            gravity[1] = alpha * gravity[1] + (1.0 - alpha) * event.values[1]
            gravity[2] = alpha * gravity[2] + (1.0 - alpha) * event.values[2]
            ax = event.values[0] - gravity[0]
            ay = event.values[1] - gravity[1]
            az = event.values[2] - gravity[2]
        } else {
            ax = event.values[0].toDouble()
            ay = event.values[1].toDouble()
            az = event.values[2].toDouble()
        }

        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        processAccelerationSample(magnitude, System.currentTimeMillis())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /**
     * Pure deterministic 2-stage fall & immobility evaluator (also directly unit-testable).
     */
    internal fun processAccelerationSample(magnitudeMps2: Double, nowMs: Long): Boolean {
        if (!monitoringImmobility) {
            if (magnitudeMps2 > IMPACT_THRESHOLD_MPS2) {
                lastHighImpactTimeMs = nowMs
                monitoringImmobility = true
                postImpactMagnitudes.clear()
                _lastImpactG.value = magnitudeMps2 / GRAVITY_MPS2
            }
            return false
        } else {
            postImpactMagnitudes.add(magnitudeMps2)
            val elapsedMs = nowMs - lastHighImpactTimeMs
            if (elapsedMs >= IMMOBILITY_WINDOW_MS) {
                val avgMovement = if (postImpactMagnitudes.isNotEmpty()) {
                    postImpactMagnitudes.sum() / postImpactMagnitudes.size
                } else {
                    0.0
                }
                monitoringImmobility = false
                postImpactMagnitudes.clear()

                if (avgMovement < IMMOBILITY_AVG_THRESHOLD_MPS2) {
                    onFallDetectedCallback?.invoke()
                    return true
                }
            }
            return false
        }
    }

    companion object {
        const val GRAVITY_MPS2 = 9.81
        const val IMPACT_THRESHOLD_G = 3.8
        const val IMPACT_THRESHOLD_MPS2 = IMPACT_THRESHOLD_G * GRAVITY_MPS2 // ~37.28 m/s²
        const val IMMOBILITY_WINDOW_MS = 30_000L
        const val IMMOBILITY_AVG_THRESHOLD_MPS2 = 0.6
    }
}
