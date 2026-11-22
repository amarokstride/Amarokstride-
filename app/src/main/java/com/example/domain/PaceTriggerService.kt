package com.example.domain

/**
 * Rolling 12-second pace zones relative to the runner's target pace.
 */
enum class PaceZone(val label: String) {
    HUNTING_SURGE("Hunting Surge"),
    ON_TARGET("On Target"),
    FALLING_BEHIND("Falling Behind")
}

data class PaceEvaluationResult(
    val rollingPaceSecPerMile: Long,
    val currentZone: PaceZone,
    val triggeredZoneTransition: PaceZone?
)

/**
 * Evaluates a runner's 12-second rolling average pace against target pace,
 * firing audio zone transitions with a 60-second cooldown to prevent GPS-jitter spam.
 */
class PaceTriggerService(
    var targetPaceSecPerMile: Double = 570.0 // Default 9:30 /mi
) {
    private val window = ArrayDeque<TrackPoint>()
    private var lastAudioTriggerMs: Long? = null
    private var lastZone: PaceZone = PaceZone.ON_TARGET

    fun reset() {
        window.clear()
        lastAudioTriggerMs = null
        lastZone = PaceZone.ON_TARGET
    }

    fun setTargetPace(minutes: Int, seconds: Int) {
        targetPaceSecPerMile = ((minutes * 60) + seconds).toDouble().coerceIn(240.0, 1200.0)
    }

    fun setTargetPaceSeconds(totalSecPerMile: Long) {
        targetPaceSecPerMile = totalSecPerMile.toDouble().coerceIn(240.0, 1200.0)
    }

    /**
     * Ingests a smoothed TrackPoint into the 12-second rolling window and returns
     * rolling pace + whether a new PaceZone audio cue should fire.
     */
    fun updatePace(newPoint: TrackPoint): PaceEvaluationResult? {
        window.addLast(newPoint)

        // Prune points older than 12 seconds
        val cutoffMs = newPoint.timestampMs - WINDOW_DURATION_MS
        while (window.isNotEmpty() && window.first().timestampMs < cutoffMs) {
            window.removeFirst()
        }

        if (window.size < 3) return null

        val oldest = window.first()
        val durationSec = (newPoint.timestampMs - oldest.timestampMs) / 1000.0
        val distMeters = GpsSmoothingService.calculateHaversineDistance(
            oldest.latitude,
            oldest.longitude,
            newPoint.latitude,
            newPoint.longitude
        )

        // Filter out standing still or insufficient window duration
        if (distMeters < 5.0 || durationSec < 3.0) return null

        val metersPerSec = distMeters / durationSec
        if (metersPerSec <= 0.1) return null

        val rollingPaceSec = (1609.344 / metersPerSec).coerceIn(120.0, 3600.0)
        val deltaSec = rollingPaceSec - targetPaceSecPerMile

        val currentZone = when {
            deltaSec < -PACE_DELTA_THRESHOLD_SEC -> PaceZone.HUNTING_SURGE
            deltaSec > PACE_DELTA_THRESHOLD_SEC -> PaceZone.FALLING_BEHIND
            else -> PaceZone.ON_TARGET
        }

        val lastTrigger = lastAudioTriggerMs
        val hasCooledDown = lastTrigger == null ||
            (newPoint.timestampMs - lastTrigger) >= TRIGGER_COOLDOWN_MS

        val triggered = if (currentZone != lastZone && hasCooledDown) {
            lastZone = currentZone
            lastAudioTriggerMs = newPoint.timestampMs
            currentZone
        } else {
            null
        }

        return PaceEvaluationResult(
            rollingPaceSecPerMile = rollingPaceSec.toLong(),
            currentZone = currentZone,
            triggeredZoneTransition = triggered
        )
    }

    companion object {
        const val WINDOW_DURATION_MS = 12_000L
        const val TRIGGER_COOLDOWN_MS = 60_000L
        const val PACE_DELTA_THRESHOLD_SEC = 15.0

        /**
         * Maps an incline grade percentage to a normalized growl volume (0.0 to 1.0)
         * using a quadratic ease-in curve so flat/gentle terrain stays quiet while steep
         * ascents surge smoothly up to 100% at maxGrade (15%+).
         */
        fun calculateGrowlVolume(
            gradePercent: Double,
            minGrade: Double = 4.0,
            maxGrade: Double = 15.0
        ): Double {
            if (gradePercent <= minGrade) return 0.0
            if (gradePercent >= maxGrade) return 1.0
            val span = (maxGrade - minGrade).coerceAtLeast(1.0)
            val normalized = ((gradePercent - minGrade) / span).coerceIn(0.0, 1.0)
            return (normalized * normalized).coerceIn(0.0, 1.0)
        }
    }
}
