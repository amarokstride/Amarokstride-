package com.example.domain

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.data.model.GeoPointEle
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class GpxWaypoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    val cumulativeDistanceMeters: Double,
    val gradePercentage: Double
) {
    fun toGeoPointEle(): GeoPointEle = GeoPointEle(
        latitude = latitude,
        longitude = longitude,
        elevationFeet = elevationMeters * 3.28084
    )
}

data class GpxTrailSummary(
    val trailName: String,
    val waypoints: List<GpxWaypoint>,
    val totalDistanceMiles: Double,
    val totalElevationGainFeet: Double,
    val totalElevationLossFeet: Double,
    val minElevationFeet: Double,
    val maxElevationFeet: Double,
    val steepClimbPoints: List<GpxWaypoint>
) {
    /**
     * Estimates trail finish duration in seconds using target flat pace +
     * Naismith's Rule trail-running climb adjustment (+1 min per 100 ft gain).
     */
    fun estimateFinishSeconds(paceSecPerMile: Long): Long {
        val baseSeconds = totalDistanceMiles * paceSecPerMile.toDouble()
        val climbAdjustmentSeconds = (totalElevationGainFeet / 100.0) * 60.0
        return (baseSeconds + climbAdjustmentSeconds).roundToLong().coerceAtLeast(60L)
    }

    fun toGeoRoute(): List<GeoPointEle> = waypoints.map { it.toGeoPointEle() }

    fun centerCoordinate(): Pair<Double, Double> {
        if (waypoints.isEmpty()) return 40.0150 to -105.2705
        val mid = waypoints[waypoints.size / 2]
        return mid.latitude to mid.longitude
    }
}

/**
 * Native Android GPX Route Parser & Trail Elevation Profile Analyzer.
 * Extracts <name>, <trkpt> / <rtept>, and <ele>, computes cumulative mileage,
 * vertical ascent/descent, and flags steep climbs (>= 7% grade) for wolf growl alerts.
 */
object GpxTrailAnalyzer {

    fun parseFromUri(context: Context, uri: Uri, defaultName: String = "Imported Winter Trail"): Result<GpxTrailSummary> {
        return runCatching {
            val xmlContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalArgumentException("Could not open GPX file.")
            parseGpxString(xmlContent, defaultName = defaultName)
        }
    }

    fun parseGpxString(
        gpxContent: String,
        defaultName: String = "Imported Winter Trail"
    ): GpxTrailSummary {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(gpxContent))

        var trailName = defaultName
        var foundName = false
        val waypoints = mutableListOf<GpxWaypoint>()
        val steepClimbs = mutableListOf<GpxWaypoint>()

        var cumulativeDistanceMeters = 0.0
        var cumulativeGainMeters = 0.0
        var cumulativeLossMeters = 0.0
        var minElevMeters = Double.POSITIVE_INFINITY
        var maxElevMeters = Double.NEGATIVE_INFINITY

        var prevPoint: GpxWaypoint? = null
        var currentLat: Double? = null
        var currentLon: Double? = null
        var currentEle = 0.0
        var insideTrackPoint = false
        var currentTag: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name?.lowercase()
                    currentTag = tag
                    if (tag == "trkpt" || tag == "rtept") {
                        currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        currentEle = 0.0
                        insideTrackPoint = true
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        when {
                            currentTag == "name" && !foundName && !insideTrackPoint -> {
                                trailName = text
                                foundName = true
                            }
                            currentTag == "ele" && insideTrackPoint -> {
                                currentEle = text.toDoubleOrNull() ?: 0.0
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name?.lowercase()
                    if ((tag == "trkpt" || tag == "rtept") && insideTrackPoint) {
                        val lat = currentLat
                        val lon = currentLon
                        if (lat != null && lon != null) {
                            minElevMeters = min(minElevMeters, currentEle)
                            maxElevMeters = max(maxElevMeters, currentEle)

                            var grade = 0.0
                            val prev = prevPoint
                            if (prev != null) {
                                val dist = GpsSmoothingService.calculateHaversineDistance(
                                    prev.latitude,
                                    prev.longitude,
                                    lat,
                                    lon
                                )
                                cumulativeDistanceMeters += dist
                                val deltaEle = currentEle - prev.elevationMeters
                                if (deltaEle > 0.0) {
                                    cumulativeGainMeters += deltaEle
                                } else {
                                    cumulativeLossMeters += abs(deltaEle)
                                }
                                if (dist > 3.0) {
                                    grade = ((deltaEle / dist) * 100.0).coerceIn(-45.0, 45.0)
                                }
                            }

                            val wp = GpxWaypoint(
                                latitude = lat,
                                longitude = lon,
                                elevationMeters = currentEle,
                                cumulativeDistanceMeters = cumulativeDistanceMeters,
                                gradePercentage = grade
                            )
                            if (grade >= 7.0) {
                                steepClimbs.add(wp)
                            }
                            waypoints.add(wp)
                            prevPoint = wp
                        }
                        insideTrackPoint = false
                        currentLat = null
                        currentLon = null
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }

        if (waypoints.isEmpty()) {
            throw IllegalArgumentException("No valid GPS track points (<trkpt> or <rtept>) found in GPX file.")
        }

        return GpxTrailSummary(
            trailName = trailName,
            waypoints = waypoints,
            totalDistanceMiles = cumulativeDistanceMeters / 1609.344,
            totalElevationGainFeet = cumulativeGainMeters * 3.28084,
            totalElevationLossFeet = cumulativeLossMeters * 3.28084,
            minElevationFeet = (if (minElevMeters.isInfinite()) 0.0 else minElevMeters) * 3.28084,
            maxElevationFeet = (if (maxElevMeters.isInfinite()) 0.0 else maxElevMeters) * 3.28084,
            steepClimbPoints = steepClimbs
        )
    }
}
