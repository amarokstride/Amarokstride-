package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.maps.OfflineMapTileManager
import com.example.data.model.DistanceUnit
import com.example.domain.GpxTrailSummary
import com.example.domain.GpxWaypoint
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.FrostedRim
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.SummitGold
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Full-screen GPX Route Importer & Trail Elevation Preview screen matching
 * `striders_howl_gpx_import_preview.png`.
 */
@Composable
fun GpxPreviewModal(
    summary: GpxTrailSummary,
    targetPaceSecPerMile: Long,
    unit: DistanceUnit,
    tileManager: OfflineMapTileManager,
    isDownloadingOffline: Boolean,
    onDownloadOfflineMap: () -> Unit,
    onConfirmStartRun: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val estSeconds = remember(summary, targetPaceSecPerMile) {
        summary.estimateFinishSeconds(targetPaceSecPerMile)
    }
    val estHours = estSeconds / 3600L
    val estMins = (estSeconds % 3600L) / 60L
    val estTimeStr = if (estHours > 0) "${estHours}h ${estMins}m" else "${estMins}m"

    val displayDist = if (unit == DistanceUnit.KILOMETERS) {
        String.format(Locale.US, "%.1f KM", summary.totalDistanceMiles * 1.60934)
    } else {
        String.format(Locale.US, "%.1f MI", summary.totalDistanceMiles)
    }
    val displayGain = if (unit == DistanceUnit.KILOMETERS) {
        "+${(summary.totalElevationGainFeet / 3.28084).roundToInt()} M"
    } else {
        "+${String.format(Locale.US, "%,d", summary.totalElevationGainFeet.roundToInt())} FT"
    }
    val displayPeak = if (unit == DistanceUnit.KILOMETERS) {
        "Max: ${(summary.maxElevationFeet / 3.28084).roundToInt()} m"
    } else {
        "Max: ${String.format(Locale.US, "%,d", summary.maxElevationFeet.roundToInt())} ft"
    }

    val routeGeo = remember(summary) { summary.toGeoRoute() }
    val (centerLat, centerLon) = remember(summary) { summary.centerCoordinate() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1D))
                .safeDrawingPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .testTag("gpx_preview_screen")
        ) {
            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("gpx_preview_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close GPX Preview",
                        tint = ArcticFrost
                    )
                }
                Text(
                    text = summary.trailName.uppercase(Locale.US),
                    color = ArcticFrost,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                )
            }

            // 1. Route Metric Summary Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)
                ) {
                    GpxMetricColumn("DISTANCE", displayDist)
                    Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.12f)))
                    GpxMetricColumn("ELEV GAIN", displayGain)
                    Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.12f)))
                    GpxMetricColumn("EST. FINISH", estTimeStr)
                }
            }

            // 2. Topographical Map Breadcrumb Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(4f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OfflineTopoMapView(
                        tileManager = tileManager,
                        centerLat = centerLat,
                        centerLon = centerLon,
                        routePoints = routeGeo,
                        currentLocation = routeGeo.firstOrNull(),
                        offlineOnlyMode = false,
                        initialZoom = 13,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0A0F1D).copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.ShowChart, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${summary.waypoints.size} Waypoints · ${summary.steepClimbPoints.size} Steep Climbs (>7%)",
                                color = ArcticFrost,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Elevation Profile Gradient Area Chart (with Steep Climb Markers)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, FrostedRim.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ELEVATION PROFILE (STEEP CLIMBS MARKED)",
                            color = AlpineMist,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = displayPeak,
                            color = GlacialCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    GpxElevationProfileChart(
                        waypoints = summary.waypoints,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            // 4. Backcountry Actions: Save Offline Map & Start Pack Run
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDownloadOfflineMap()
                    },
                    enabled = !isDownloadingOffline,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, FrostedRim),
                    modifier = Modifier
                        .weight(2f)
                        .height(54.dp)
                        .testTag("gpx_save_offline_map_button")
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isDownloadingOffline) "Caching..." else "Offline Map",
                        color = ArcticFrost,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirmStartRun()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlacialCyan,
                        contentColor = Color(0xFF0A0F1D)
                    ),
                    modifier = Modifier
                        .weight(3f)
                        .height(54.dp)
                        .testTag("gpx_start_pack_run_button")
                ) {
                    Text(
                        text = "START PACK RUN",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.4.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GpxMetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AlpineMist, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = ArcticFrost, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GpxElevationProfileChart(
    waypoints: List<GpxWaypoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (waypoints.size < 2) return@Canvas
        val totalDist = max(waypoints.last().cumulativeDistanceMeters, 1.0)
        val minEle = waypoints.minOf { it.elevationMeters }
        val maxEle = waypoints.maxOf { it.elevationMeters }
        val eleRange = max(maxEle - minEle, 1.0)

        val linePath = Path()
        val fillPath = Path()
        val steepMarkers = mutableListOf<Offset>()

        fillPath.moveTo(0f, size.height)

        // Downsample smoothly for rendering while retaining steep climb flags
        val stride = (waypoints.size / 140).coerceAtLeast(1)
        var first = true
        for (i in waypoints.indices step stride) {
            val pt = waypoints[i]
            val x = ((pt.cumulativeDistanceMeters / totalDist) * size.width).toFloat()
            val normEle = ((pt.elevationMeters - minEle) / eleRange).toFloat()
            val y = size.height - (normEle * (size.height - 14f)) - 4f

            if (first) {
                linePath.moveTo(x, y)
                fillPath.lineTo(x, y)
                first = false
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (pt.gradePercentage >= 7.0 && (steepMarkers.isEmpty() || x - steepMarkers.last().x > 26f)) {
                steepMarkers.add(Offset(x, y))
            }
        }

        // Ensure final endpoint connects cleanly
        val lastPt = waypoints.last()
        val lastY = size.height - (((lastPt.elevationMeters - minEle) / eleRange).toFloat() * (size.height - 14f)) - 4f
        linePath.lineTo(size.width, lastY)
        fillPath.lineTo(size.width, lastY)
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(GlacialCyan.copy(alpha = 0.36f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = GlacialCyan,
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Highlight steep climbs (>7% grade) with glowing gold beacons
        steepMarkers.forEach { pt ->
            drawCircle(color = SummitGold.copy(alpha = 0.28f), radius = 10f, center = pt)
            drawCircle(color = SummitGold, radius = 4.5f, center = pt)
        }
    }
}
