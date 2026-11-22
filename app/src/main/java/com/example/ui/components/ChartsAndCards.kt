package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityRecord
import com.example.data.model.ActivityType
import com.example.data.model.DistanceUnit
import com.example.data.model.GeoPointEle
import com.example.data.model.SummitFormatters
import com.example.ui.theme.MetricCardStyle
import com.example.ui.theme.SummitGold
import com.example.ui.theme.TrailHikeAccent
import com.example.ui.theme.TrailRunAccent

@Composable
fun WeeklyMileageRing(
    loggedMiles: Double,
    targetMiles: Double,
    unit: DistanceUnit,
    modifier: Modifier = Modifier
) {
    val fraction = if (targetMiles <= 0.0) 0f else (loggedMiles / targetMiles).toFloat().coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = fraction * 360f,
        animationSpec = spring(stiffness = 220f, dampingRatio = 0.78f),
        label = "WeeklyRingSweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(124.dp)
    ) {
        Canvas(modifier = Modifier.size(124.dp)) {
            val stroke = 13.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(primaryColor, secondaryColor, SummitGold, primaryColor)
                ),
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = SummitFormatters.formatDistanceShort(loggedMiles, unit),
                style = MetricCardStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "of ${SummitFormatters.formatDistanceShort(targetMiles, unit)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyDailyBarChart(
    dailyMiles: List<Double>,
    unit: DistanceUnit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    val safeValues = if (dailyMiles.size == 7) dailyMiles else List(7) { 0.0 }
    val maxVal = (safeValues.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val barColor = MaterialTheme.colorScheme.secondary
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        safeValues.forEachIndexed { idx, miles ->
            val ratio = (miles / maxVal).toFloat().coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                if (miles > 0.0) {
                    Text(
                        text = SummitFormatters.formatDistanceShort(miles, unit).substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height((10f + 56f * ratio).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(if (miles > 0.0) barColor else emptyBarColor)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = labels[idx],
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ElevationProfileChart(
    points: List<GeoPointEle>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return
    val elevations = points.map { it.elevationFeet.toFloat() }
    val minEle = elevations.minOrNull() ?: 0f
    val maxEle = (elevations.maxOrNull() ?: 1f).coerceAtLeast(minEle + 10f)
    val range = maxEle - minEle
    val strokeColor = MaterialTheme.colorScheme.primary
    val fillTop = strokeColor.copy(alpha = 0.28f)
    val fillBottom = strokeColor.copy(alpha = 0.02f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val linePath = Path()
        val fillPath = Path()
        elevations.forEachIndexed { idx, ele ->
            val x = (idx.toFloat() / (elevations.size - 1).coerceAtLeast(1)) * size.width
            val y = size.height - ((ele - minEle) / range) * (size.height - 16f) - 8f
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(listOf(fillTop, fillBottom))
        )
        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SplitPacesChart(
    splitsSec: List<Long>,
    unit: DistanceUnit,
    modifier: Modifier = Modifier
) {
    if (splitsSec.isEmpty()) return
    val fastest = splitsSec.minOrNull() ?: 480L
    val slowest = (splitsSec.maxOrNull() ?: 600L).coerceAtLeast(fastest + 30L)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        splitsSec.forEachIndexed { index, splitSec ->
            val isFastest = splitSec == fastest
            val widthRatio = (0.38f + 0.62f * ((splitSec - fastest).toFloat() / (slowest - fastest).toFloat())).coerceIn(0.35f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "M${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(34.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(widthRatio)
                            .height(22.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFastest) SummitGold else MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = SummitFormatters.formatPaceRaw(splitSec, unit),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isFastest) FontWeight.Bold else FontWeight.Medium,
                    color = if (isFastest) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ActivitySummaryCard(
    activity: ActivityRecord,
    unit: DistanceUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = activity.typeEnum()
    val badgeColor = when (type) {
        ActivityType.HIKE -> TrailHikeAccent
        ActivityType.EQUESTRIAN -> com.example.ui.theme.TrailEquestrianAccent
        ActivityType.RUN -> TrailRunAccent
    }
    val typeIcon = when (type) {
        ActivityType.HIKE -> Icons.Filled.Landscape
        ActivityType.EQUESTRIAN -> Icons.Filled.Terrain
        ActivityType.RUN -> Icons.AutoMirrored.Filled.DirectionsRun
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("activity_card_${activity.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = type.displayName,
                        tint = badgeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${SummitFormatters.formatDate(activity.startedAtEpochMs)} · ${activity.sourceEnum().displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricMiniColumn(
                    label = "Distance",
                    value = SummitFormatters.formatDistance(activity.distanceMiles, unit),
                    icon = Icons.Filled.TrendingUp
                )
                MetricMiniColumn(
                    label = "Avg Pace",
                    value = SummitFormatters.formatPace(activity.avgPaceSecPerMile, unit),
                    icon = Icons.Filled.Timer
                )
                MetricMiniColumn(
                    label = "Elevation",
                    value = SummitFormatters.formatElevation(activity.elevationGainFeet, unit),
                    icon = Icons.Filled.Terrain
                )
            }
        }
    }
}

@Composable
private fun MetricMiniColumn(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MetricCardStyle.fontFamily),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLogActivitySheet(
    unit: DistanceUnit,
    gearItems: List<com.example.data.local.GearItemEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        type: ActivityType,
        distance: Double,
        durationMinutes: Long,
        elevation: Long,
        gearId: String?,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember { mutableStateOf(ActivityType.RUN) }
    var selectedGearId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var distanceText by remember { mutableStateOf("3.5") }
    var durationText by remember { mutableStateOf("32") }
    var elevationText by remember { mutableStateOf("240") }
    var notes by remember { mutableStateOf("") }

    val matchingGear = gearItems.filter { !it.retired && ((selectedType == ActivityType.EQUESTRIAN && it.category == "SADDLE") || (selectedType != ActivityType.EQUESTRIAN && it.category == "SHOE")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Log Quest (Run, Hike, or Ride)",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            selectedGearId = null
                        },
                        label = { Text(type.displayName) },
                        modifier = Modifier.testTag("manual_type_${type.name.lowercase()}")
                    )
                }
            }

            if (matchingGear.isNotEmpty()) {
                Text(
                    text = if (selectedType == ActivityType.EQUESTRIAN) "Track Saddle Mileage" else "Track Shoe Mileage",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    matchingGear.take(3).forEach { gear ->
                        FilterChip(
                            selected = selectedGearId == gear.id,
                            onClick = { selectedGearId = if (selectedGearId == gear.id) null else gear.id },
                            label = { Text(gear.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(100) },
                label = { Text("Quest title (e.g., Sunset Ridge Run)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_title_input")
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("Distance (${unit.shortLabel})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_distance_input")
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_duration_input")
                )
            }

            OutlinedTextField(
                value = elevationText,
                onValueChange = { elevationText = it },
                label = { Text("Elevation gain (${unit.elevationLabel})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_elevation_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(500) },
                label = { Text("Trail notes (optional)") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_notes_input")
            )

            val parsedDist = distanceText.toDoubleOrNull()
            val parsedMins = durationText.toLongOrNull()
            val canSave = parsedDist != null && parsedDist > 0.05 && parsedMins != null && parsedMins >= 1L

            Button(
                onClick = {
                    if (canSave) {
                        onSave(
                            title,
                            selectedType,
                            parsedDist!!,
                            parsedMins!!,
                            elevationText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                            selectedGearId,
                            notes
                        )
                        onDismiss()
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("manual_save_button")
            ) {
                Text("Save Quest to Tavern Ledger")
            }
        }
    }
}
