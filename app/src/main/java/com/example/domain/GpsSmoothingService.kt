package com.example.domain

import com.example.data.model.GeoPointEle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Raw or smoothed GPS waypoint for Striders Howl trail tracking.
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    val timestampMs: Long,
    val accuracyMeters: Double,
    val speedMps: Double
) {
    fun toGeoPointEle(): GeoPointEle = GeoPointEle(
        latitude = latitude,
        longitude = longitude,
        elevationFeet = elevationMeters * 3.28084
    )
}

/**
 * 2D Kalman Filter for real-time geographic coordinate smoothing.
 * Dynamically blends incoming GPS fixes against predicted uncertainty based on accuracy.
 */
class KalmanLocationFilter(
    private val qMetersPerSecond: Double = 3.5
) {
    private var prevLat: Double? = null
    private var prevLng: Double? = null
    private var variance: Double = -1.0
    private var lastTimestampMs: Long? = null

    fun reset() {
        prevLat = null
        prevLng = null
        variance = -1.0
        lastTimestampMs = null
    }

    /**
     * Ingests a raw GPS coordinate and returns smoothed (latitude, longitude).
     */
    fun filter(
        lat: Double,
        lng: Double,
        accuracyMeters: Double,
        timestampMs: Long
    ): Pair<Double, Double> {
        val currentPrevLat = prevLat
        val currentPrevLng = prevLng
        val currentLastTs = lastTimestampMs

        if (variance < 0.0 || currentPrevLat == null || currentPrevLng == null || currentLastTs == null) {
            prevLat = lat
            prevLng = lng
            variance = accuracyMeters * accuracyMeters
            lastTimestampMs = timestampMs
            return lat to lng
        }

        val durationSec = (timestampMs - currentLastTs) / 1000.0
        if (durationSec > 0.0) {
            // Predict error covariance forward in time
            variance += durationSec * qMetersPerSecond * qMetersPerSecond
            lastTimestampMs = timestampMs
        }

        // Measurement variance from GPS accuracy
        val rVariance = accuracyMeters * accuracyMeters
        // Kalman gain K = P / (P + R)
        val kGain = variance / (variance + rVariance)

        // Update state estimate
        val nextLat = currentPrevLat + kGain * (lat - currentPrevLat)
        val nextLng = currentPrevLng + kGain * (lng - currentPrevLng)
        prevLat = nextLat
        prevLng = nextLng

        // Update error covariance
        variance *= (1.0 - kGain)

        return nextLat to nextLng
    }
}

/**
 * Complete 4-stage Trail GPS Smoothing & Drift Rejection Engine:
 * 1. Accuracy & multipath jitter rejection
 * 2. Velocity / teleportation outlier gate
 * 3. 2D Kalman filter coordinate smoothing
 * 4. Elevation Exponential Moving Average (EMA) dampening
 * + Ramer-Douglas-Peucker (RDP) polyline simplification for crisp post-run maps
 */
class GpsSmoothingService(
    qMetersPerSecond: Double = 3.5
) {
    private val kalman = KalmanLocationFilter(qMetersPerSecond = qMetersPerSecond)

    private var lastValidPoint: TrackPoint? = null
    private var smoothedElevationMeters: Double? = null

    var rejectedOutlierCount: Int = 0
        private set

    fun reset() {
        kalman.reset()
        lastValidPoint = null
        smoothedElevationMeters = null
        rejectedOutlierCount = 0
    }

    /**
     * Processes a single real-time location fix.
     * Returns null if the fix is inaccurate, jittery, or a physically impossible jump.
     */
    fun processLivePoint(
        raw: TrackPoint,
        maxSpeedMps: Double = MAX_HUMAN_TRAIL_SPEED_MPS
    ): TrackPoint? {
        // 1. Accuracy gate: discard weak multipath signals
        if (raw.accuracyMeters > MAX_ACCEPTABLE_ACCURACY_METERS || raw.accuracyMeters <= 0.0) {
            rejectedOutlierCount++
            return null
        }

        // 2. Velocity & teleportation outlier gate
        val prev = lastValidPoint
        if (prev != null) {
            val dtSec = (raw.timestampMs - prev.timestampMs) / 1000.0
            if (dtSec > 0.2) {
                val distMeters = calculateHaversineDistance(
                    prev.latitude,
                    prev.longitude,
                    raw.latitude,
                    raw.longitude
                )
                val impliedSpeed = distMeters / dtSec
                if (impliedSpeed > maxSpeedMps) {
                    rejectedOutlierCount++
                    return null
                }
            }
        }

        // 3. 2D Kalman smoothing
        val (smoothedLat, smoothedLng) = kalman.filter(
            lat = raw.latitude,
            lng = raw.longitude,
            accuracyMeters = raw.accuracyMeters,
            timestampMs = raw.timestampMs
        )

        // 4. Elevation Exponential Moving Average (EMA)
        val nextElevation = smoothedElevationMeters?.let { prevEle ->
            (ELEVATION_EMA_ALPHA * raw.elevationMeters) + ((1.0 - ELEVATION_EMA_ALPHA) * prevEle)
        } ?: raw.elevationMeters
        smoothedElevationMeters = nextElevation

        val smoothedPoint = raw.copy(
            latitude = smoothedLat,
            longitude = smoothedLng,
            elevationMeters = nextElevation
        )
        lastValidPoint = smoothedPoint
        return smoothedPoint
    }

    companion object {
        const val MAX_ACCEPTABLE_ACCURACY_METERS = 30.0
        const val MAX_HUMAN_TRAIL_SPEED_MPS = 12.5      // ~28 mph running/hiking cap
        const val MAX_EQUESTRIAN_SPEED_MPS = 19.5       // ~43 mph gallop cap
        const val ELEVATION_EMA_ALPHA = 0.25            // Vertical noise dampening factor

        /**
         * Great-circle distance between two coordinates in meters (Haversine formula).
         */
        fun calculateHaversineDistance(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val earthRadiusMeters = 6_371_000.0
            val dLat = (lat2 - lat1) * (PI / 180.0)
            val dLon = (lon2 - lon1) * (PI / 180.0)

            val sinHalfLat = sin(dLat / 2.0)
            val sinHalfLon = sin(dLon / 2.0)
            val a = sinHalfLat * sinHalfLat +
                cos(lat1 * (PI / 180.0)) *
                cos(lat2 * (PI / 180.0)) *
                sinHalfLon * sinHalfLon

            val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
            return earthRadiusMeters * c
        }

        /**
         * Calculates uphill/downhill grade percentage: (Elevation Gain / Horizontal Distance) * 100.
         * Returns 0.0 when horizontal movement is under 5 meters to avoid stop-jitter division spikes.
         */
        fun calculateGradePercentage(previous: TrackPoint, current: TrackPoint): Double {
            val horizontalDist = calculateHaversineDistance(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
            if (horizontalDist < 5.0) return 0.0
            val verticalGainMeters = current.elevationMeters - previous.elevationMeters
            return ((verticalGainMeters / horizontalDist) * 100.0).coerceIn(-45.0, 45.0)
        }

        /**
         * Grade percentage helper directly on GeoPointEle (converts elevationFeet to meters).
         */
        fun calculateGeoGradePercentage(previous: GeoPointEle, current: GeoPointEle): Double {
            val horizontalDist = calculateHaversineDistance(
                previous.latitude,
                previous.longitude,
                current.latitude,
                current.longitude
            )
            if (horizontalDist < 5.0) return 0.0
            val verticalGainMeters = (current.elevationFeet - previous.elevationFeet) / 3.28084
            return ((verticalGainMeters / horizontalDist) * 100.0).coerceIn(-45.0, 45.0)
        }

        /**
         * Ramer-Douglas-Peucker (RDP) polyline simplification on TrackPoints.
         * Cuts redundant points by 60-80% while preserving sharp mountain switchbacks.
         */
        fun simplifyTrack(
            points: List<TrackPoint>,
            epsilonMeters: Double = 2.5
        ): List<TrackPoint> {
            if (points.size < 3) return points

            var maxDist = 0.0
            var splitIndex = 0
            val last = points.lastIndex

            for (i in 1 until last) {
                val dist = perpendicularDistanceMeters(
                    lat = points[i].latitude,
                    lon = points[i].longitude,
                    lat1 = points[0].latitude,
                    lon1 = points[0].longitude,
                    lat2 = points[last].latitude,
                    lon2 = points[last].longitude
                )
                if (dist > maxDist) {
                    maxDist = dist
                    splitIndex = i
                }
            }

            return if (maxDist > epsilonMeters) {
                val left = simplifyTrack(points.subList(0, splitIndex + 1), epsilonMeters)
                val right = simplifyTrack(points.subList(splitIndex, last + 1), epsilonMeters)
                left.dropLast(1) + right
            } else {
                listOf(points.first(), points.last())
            }
        }

        /**
         * Ramer-Douglas-Peucker (RDP) polyline simplification on GeoPointEle routes.
         */
        fun simplifyGeoRoute(
            points: List<GeoPointEle>,
            epsilonMeters: Double = 2.5
        ): List<GeoPointEle> {
            if (points.size < 3) return points

            var maxDist = 0.0
            var splitIndex = 0
            val last = points.lastIndex

            for (i in 1 until last) {
                val dist = perpendicularDistanceMeters(
                    lat = points[i].latitude,
                    lon = points[i].longitude,
                    lat1 = points[0].latitude,
                    lon1 = points[0].longitude,
                    lat2 = points[last].latitude,
                    lon2 = points[last].longitude
                )
                if (dist > maxDist) {
                    maxDist = dist
                    splitIndex = i
                }
            }

            return if (maxDist > epsilonMeters) {
                val left = simplifyGeoRoute(points.subList(0, splitIndex + 1), epsilonMeters)
                val right = simplifyGeoRoute(points.subList(splitIndex, last + 1), epsilonMeters)
                left.dropLast(1) + right
            } else {
                listOf(points.first(), points.last())
            }
        }

        private fun perpendicularDistanceMeters(
            lat: Double,
            lon: Double,
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val dx = lon2 - lon1
            val dy = lat2 - lat1

            if (dx == 0.0 && dy == 0.0) {
                return calculateHaversineDistance(lat, lon, lat1, lon1)
            }

            val t = (((lon - lon1) * dx) + ((lat - lat1) * dy)) / (dx * dx + dy * dy)
            val clampedT = t.coerceIn(0.0, 1.0)
            val projLon = lon1 + clampedT * dx
            val projLat = lat1 + clampedT * dy

            return calculateHaversineDistance(lat, lon, projLat, projLon)
        }
    }
}
