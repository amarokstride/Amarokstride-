package com.example.data.safety

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.ActivityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private const val PREFS_PHONE_STEPS = "striders_phone_steps_v1"
private const val KEY_DAY_DATE = "day_date"
private const val KEY_TODAY_STEPS = "today_steps"
private const val KEY_LOGGED_TODAY_STEPS = "logged_today_steps"

data class PhoneStepCounterState(
    val isListening: Boolean = false,
    val isSessionActive: Boolean = false,
    val hasActivityPermission: Boolean = false,
    val hasHardwareStepSensor: Boolean = false,
    val sensorSourceLabel: String = "Phone Pedometer Ready",
    val todaySteps: Long = 0L,
    val loggedTodaySteps: Long = 0L,
    val sessionSteps: Long = 0L,
    val cadenceSpm: Int = 0,
    val strideLengthMeters: Double = 0.84
) {
    val unloggedTodaySteps: Long
        get() = (todaySteps - loggedTodaySteps).coerceAtLeast(0L)

    val todayDistanceMiles: Double
        get() = (todaySteps * strideLengthMeters) / 1609.344

    val unloggedDistanceMiles: Double
        get() = (unloggedTodaySteps * strideLengthMeters) / 1609.344

    val sessionDistanceMiles: Double
        get() = (sessionSteps * strideLengthMeters) / 1609.344
}

/**
 * Hardware phone step counter & cadence tracker for Striders Howl users who don't own a smartwatch.
 *
 * Uses Android's built-in hardware sensors in priority order:
 * 1. `Sensor.TYPE_STEP_DETECTOR` (low-power instant per-step hardware trigger)
 * 2. `Sensor.TYPE_STEP_COUNTER` (hardware cumulative pedometer since boot)
 * 3. `Sensor.TYPE_ACCELEROMETER` (low-pass gravity-filtered stride peak fallback for phones
 *    without a dedicated pedometer coprocessor)
 */
class PhoneStepCounterManager(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepDetectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val prefs = appContext.getSharedPreferences(PREFS_PHONE_STEPS, Context.MODE_PRIVATE)

    private var activeSensorType: Int = -1
    private var counterBaseline: Float? = null
    private var isPaused: Boolean = false

    // Rolling 12-second step timestamp queue for accurate real-time cadence (steps per minute)
    private val recentStepTimestampsMs = ArrayDeque<Long>()

    // Accelerometer fallback filter state (only active if hardware step sensor is unavailable)
    private var gravityMagnitude = 9.81f
    private var lastAccelPeakMs = 0L

    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<PhoneStepCounterState> = _state.asStateFlow()

    private fun buildInitialState(): PhoneStepCounterState {
        val todayKey = currentDayKey()
        val savedDate = prefs.getString(KEY_DAY_DATE, null)
        val savedToday = if (savedDate == todayKey) prefs.getLong(KEY_TODAY_STEPS, 0L) else 0L
        val savedLogged = if (savedDate == todayKey) prefs.getLong(KEY_LOGGED_TODAY_STEPS, 0L) else 0L
        if (savedDate != todayKey) {
            prefs.edit()
                .putString(KEY_DAY_DATE, todayKey)
                .putLong(KEY_TODAY_STEPS, 0L)
                .putLong(KEY_LOGGED_TODAY_STEPS, 0L)
                .apply()
        }
        val hasHw = stepDetectorSensor != null || stepCounterSensor != null
        return PhoneStepCounterState(
            hasActivityPermission = hasRuntimeActivityPermission(),
            hasHardwareStepSensor = hasHw,
            sensorSourceLabel = when {
                stepDetectorSensor != null -> "Phone Step Sensor Ready"
                stepCounterSensor != null -> "Phone Pedometer Ready"
                accelerometerSensor != null -> "Phone Motion Step Counter Ready"
                else -> "Step Sensor Unavailable"
            },
            todaySteps = savedToday,
            loggedTodaySteps = savedLogged
        )
    }

    fun hasRuntimeActivityPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun startSession(
        activityType: ActivityType,
        hasActivityPermission: Boolean = hasRuntimeActivityPermission()
    ) {
        val strideMeters = strideMetersFor(activityType)
        isPaused = false
        recentStepTimestampsMs.clear()
        _state.update {
            it.copy(
                isSessionActive = true,
                sessionSteps = 0L,
                cadenceSpm = 0,
                strideLengthMeters = strideMeters,
                hasActivityPermission = hasActivityPermission
            )
        }
        attachBestAvailableSensor(hasActivityPermission)
    }

    fun startDailyCounter(
        hasActivityPermission: Boolean = hasRuntimeActivityPermission()
    ) {
        isPaused = false
        _state.update { it.copy(hasActivityPermission = hasActivityPermission) }
        if (!_state.value.isListening) {
            attachBestAvailableSensor(hasActivityPermission)
        }
    }

    fun pauseSession() {
        isPaused = true
        _state.update { it.copy(cadenceSpm = 0) }
    }

    fun resumeSession() {
        isPaused = false
        if (!_state.value.isListening) {
            attachBestAvailableSensor(hasRuntimeActivityPermission())
        }
    }

    fun endSession(keepDailyCounterRunning: Boolean = true) {
        isPaused = false
        recentStepTimestampsMs.clear()
        _state.update {
            it.copy(
                isSessionActive = false,
                sessionSteps = 0L,
                cadenceSpm = 0
            )
        }
        if (!keepDailyCounterRunning) {
            stopListening()
        }
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {
        }
        activeSensorType = -1
        counterBaseline = null
        recentStepTimestampsMs.clear()
        _state.update {
            it.copy(
                isListening = false,
                cadenceSpm = 0
            )
        }
    }

    fun markTodayStepsLogged() {
        val today = _state.value.todaySteps
        prefs.edit()
            .putString(KEY_DAY_DATE, currentDayKey())
            .putLong(KEY_LOGGED_TODAY_STEPS, today)
            .apply()
        _state.update { it.copy(loggedTodaySteps = today) }
    }

    fun clearLocalSteps() {
        stopListening()
        prefs.edit().clear().apply()
        _state.update {
            it.copy(
                todaySteps = 0L,
                loggedTodaySteps = 0L,
                sessionSteps = 0L,
                cadenceSpm = 0,
                isSessionActive = false
            )
        }
    }

    private fun attachBestAvailableSensor(hasActivityPermission: Boolean) {
        val manager = sensorManager ?: return
        try {
            manager.unregisterListener(this)
        } catch (_: Exception) {
        }
        counterBaseline = null
        activeSensorType = -1

        val chosenSensor: Sensor?
        val sourceLabel: String

        when {
            hasActivityPermission && stepDetectorSensor != null -> {
                chosenSensor = stepDetectorSensor
                sourceLabel = "Phone Hardware Step Sensor"
            }
            hasActivityPermission && stepCounterSensor != null -> {
                chosenSensor = stepCounterSensor
                sourceLabel = "Phone Hardware Pedometer"
            }
            accelerometerSensor != null -> {
                chosenSensor = accelerometerSensor
                sourceLabel = "Phone Accelerometer Step Counter"
            }
            else -> {
                chosenSensor = null
                sourceLabel = "No Step Sensor on Device"
            }
        }

        if (chosenSensor == null) {
            _state.update {
                it.copy(
                    isListening = false,
                    sensorSourceLabel = sourceLabel
                )
            }
            return
        }

        val rate = if (chosenSensor.type == Sensor.TYPE_ACCELEROMETER) {
            SensorManager.SENSOR_DELAY_GAME
        } else {
            SensorManager.SENSOR_DELAY_FASTEST
        }
        val registered = manager.registerListener(this, chosenSensor, rate)
        if (registered) {
            activeSensorType = chosenSensor.type
            _state.update {
                it.copy(
                    isListening = true,
                    sensorSourceLabel = sourceLabel,
                    hasActivityPermission = hasActivityPermission
                )
            }
        } else {
            _state.update {
                it.copy(
                    isListening = false,
                    sensorSourceLabel = "Step Sensor Unavailable"
                )
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isPaused) return
        val nowMs = System.currentTimeMillis()
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                recordSteps(delta = 1L, nowMs = nowMs)
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSinceBoot = event.values.firstOrNull() ?: return
                val base = counterBaseline
                if (base == null) {
                    counterBaseline = totalSinceBoot
                    return
                }
                val delta = (totalSinceBoot - base).toLong()
                if (delta > 0L) {
                    counterBaseline = totalSinceBoot
                    recordSteps(delta = delta.coerceAtMost(500L), nowMs = nowMs)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = sqrt(x * x + y * y + z * z)
                // Low-pass filter isolates steady gravity (~9.81 m/s²), high-pass extracts stride impulse
                gravityMagnitude = 0.88f * gravityMagnitude + 0.12f * mag
                val dynamicAccel = mag - gravityMagnitude
                if (dynamicAccel > 1.85f && nowMs - lastAccelPeakMs >= 290L) {
                    lastAccelPeakMs = nowMs
                    recordSteps(delta = 1L, nowMs = nowMs)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun recordSteps(delta: Long, nowMs: Long) {
        if (delta <= 0L) return
        val repeatCount = delta.coerceAtMost(20L).toInt()
        repeat(repeatCount) { recentStepTimestampsMs.addLast(nowMs) }
        val cutoff = nowMs - 12_000L
        while (recentStepTimestampsMs.isNotEmpty() && recentStepTimestampsMs.first() < cutoff) {
            recentStepTimestampsMs.removeFirst()
        }
        val windowSeconds = if (recentStepTimestampsMs.size >= 2) {
            ((nowMs - recentStepTimestampsMs.first()).coerceAtLeast(2500L)) / 1000.0
        } else {
            12.0
        }
        val spm = if (recentStepTimestampsMs.size >= 2) {
            ((recentStepTimestampsMs.size / windowSeconds) * 60.0).toInt().coerceIn(30, 230)
        } else {
            _state.value.cadenceSpm
        }

        val todayKey = currentDayKey()
        val storedDate = prefs.getString(KEY_DAY_DATE, null)
        val resetForNewDay = storedDate != todayKey

        _state.update { curr ->
            val nextToday = if (resetForNewDay) delta else curr.todaySteps + delta
            val nextLogged = if (resetForNewDay) 0L else curr.loggedTodaySteps
            val nextSession = if (curr.isSessionActive) curr.sessionSteps + delta else curr.sessionSteps

            prefs.edit()
                .putString(KEY_DAY_DATE, todayKey)
                .putLong(KEY_TODAY_STEPS, nextToday)
                .putLong(KEY_LOGGED_TODAY_STEPS, nextLogged)
                .apply()

            curr.copy(
                todaySteps = nextToday,
                loggedTodaySteps = nextLogged,
                sessionSteps = nextSession,
                cadenceSpm = spm
            )
        }
    }

    private fun strideMetersFor(activityType: ActivityType): Double = when (activityType) {
        ActivityType.RUN -> 1.04
        ActivityType.HIKE -> 0.78
        ActivityType.EQUESTRIAN -> 0.85
    }

    private fun currentDayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
