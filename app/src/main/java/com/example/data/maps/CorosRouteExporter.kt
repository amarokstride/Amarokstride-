package com.example.data.maps

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.integrations.CorosIntegrationManager
import com.example.data.model.ActivityType
import com.example.data.model.GeoPointEle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

object CorosRouteExporter {

    fun calculateRouteDistanceMiles(points: List<GeoPointEle>): Double {
        if (points.size < 2) return 0.0
        var meters = 0.0
        for (i in 1 until points.size) {
            meters += CorosIntegrationManager.haversineMeters(
                points[i - 1].latitude,
                points[i - 1].longitude,
                points[i].latitude,
                points[i].longitude
            )
        }
        return meters / 1609.344
    }

    /**
     * Generates a quick loop trail around a starting coordinate so riders/runners
     * can start editing a quest route immediately.
     */
    fun generateStarterLoop(centerLat: Double, centerLon: Double, targetMiles: Double = 3.0): List<GeoPointEle> {
        val radiusDegLat = (targetMiles / 6.28318) / 69.0
        val radiusDegLon = radiusDegLat / cos(Math.toRadians(centerLat)).coerceAtLeast(0.2)
        val count = 8
        return (0..count).map { idx ->
            val angle = (2.0 * Math.PI * idx) / count
            val wobble = if (idx % 2 == 0) 1.0 else 0.85
            GeoPointEle(
                latitude = centerLat + sin(angle) * radiusDegLat * wobble,
                longitude = centerLon + cos(angle) * radiusDegLon * wobble,
                elevationFeet = 420.0 + sin(angle) * 140.0
            )
        }
    }

    /**
     * Builds a standards-compliant GPX 1.1 document containing both <rte> and <trk>
     * elements so COROS App (Explore -> My Routes -> Sync to Watch) and Garmin Explore
     * import turn-by-turn breadcrumb navigation cleanly.
     */
    fun buildGpxXml(
        trailName: String,
        discipline: ActivityType,
        points: List<GeoPointEle>
    ): String {
        val safeName = trailName.trim().ifBlank { "Striders Howl Quest Trail" }
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val nowIso = isoFormat.format(Date())
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="Striders Howl" xmlns="http://www.topografix.com/GPX/1/1">""")
        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>$safeName</name>")
        sb.appendLine("    <desc>${discipline.questLabel} custom route created in Striders Howl for COROS watch navigation.</desc>")
        sb.appendLine("    <time>$nowIso</time>")
        sb.appendLine("  </metadata>")
        sb.appendLine("  <rte>")
        sb.appendLine("    <name>$safeName</name>")
        points.forEachIndexed { idx, pt ->
            val eleMeters = pt.elevationFeet * 0.3048
            sb.appendLine(
                String.format(
                    Locale.US,
                    """    <rtept lat="%.6f" lon="%.6f"><ele>%.1f</ele><name>WP%d</name></rtept>""",
                    pt.latitude,
                    pt.longitude,
                    eleMeters,
                    idx + 1
                )
            )
        }
        sb.appendLine("  </rte>")
        sb.appendLine("  <trk>")
        sb.appendLine("    <name>$safeName</name>")
        sb.appendLine("    <type>${discipline.name.lowercase(Locale.US)}</type>")
        sb.appendLine("    <trkseg>")
        points.forEach { pt ->
            val eleMeters = pt.elevationFeet * 0.3048
            sb.appendLine(
                String.format(
                    Locale.US,
                    """      <trkpt lat="%.6f" lon="%.6f"><ele>%.1f</ele></trkpt>""",
                    pt.latitude,
                    pt.longitude,
                    eleMeters
                )
            )
        }
        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")
        return sb.toString()
    }

    fun exportAndShareToCorosWatch(
        context: Context,
        trailName: String,
        discipline: ActivityType,
        points: List<GeoPointEle>
    ): Result<String> {
        if (points.size < 2) {
            return Result.failure(IllegalArgumentException("Plot at least 2 waypoints on the map first."))
        }
        return try {
            val dir = File(context.cacheDir, "coros_routes").apply { mkdirs() }
            val safeFileBase = trailName.trim()
                .ifBlank { "quest_trail" }
                .lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "_")
                .take(40)
            val gpxFile = File(dir, "${safeFileBase}.gpx")
            gpxFile.writeText(buildGpxXml(trailName, discipline, points))

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                gpxFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gpx+xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$trailName (GPX for COROS Watch)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Open this GPX route in the COROS app -> Explore -> My Routes -> Sync with your COROS watch."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Send Quest Route to COROS Watch")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Result.success(gpxFile.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
