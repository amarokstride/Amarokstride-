package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.maps.CorosRouteExporter
import com.example.data.model.DistanceUnit
import com.example.data.model.RouteAnalysisReport
import com.example.data.model.SummitFormatters
import com.example.data.model.UiState
import com.example.data.sharing.MilestoneShareRenderer
import com.example.ui.components.OfflineTopoMapView
import com.example.ui.components.StoryShareCardModal
import com.example.ui.components.StorySharePayload
import com.example.ui.components.WolfAchievementCrest
import com.example.ui.theme.SummitGold
import com.example.ui.theme.TrailHikeAccent
import com.example.ui.theme.WolfFrostBlue
import com.example.ui.viewmodel.SummitViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: String,
    viewModel: SummitViewModel,
    onNavigateBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenChronicles: () -> Unit = {}
) {
    val context = LocalContext.current
    var showStoryShareModal by remember { mutableStateOf(false) }
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val activitiesState by viewModel.activitiesState.collectAsStateWithLifecycle()
    val milestonesState by viewModel.milestonesState.collectAsStateWithLifecycle()
    val soloRankings by viewModel.soloRankingsState.collectAsStateWithLifecycle()
    val rank by viewModel.adventurerRankState.collectAsStateWithLifecycle()
    val recapLoadingId by viewModel.activityRecapLoadingId.collectAsStateWithLifecycle()
    val analysisLoadingId by viewModel.routeAnalysisLoadingId.collectAsStateWithLifecycle()
    val isListeningVoice by viewModel.isListeningVoice.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val routeAnalysis by remember(activityId) {
        viewModel.observeRouteAnalysis(activityId)
    }.collectAsStateWithLifecycle(initialValue = null)

    val profile = (profileState as? UiState.Success)?.data
    val unit = profile?.unitEnum() ?: DistanceUnit.MILES
    val athleteName = profile?.displayName ?: "Wolf Strider"
    val activity = (activitiesState as? UiState.Success)?.data.orEmpty().firstOrNull { it.id == activityId }
    val latestMilestone = (milestonesState as? UiState.Success)?.data.orEmpty().firstOrNull()
    val myRankIndex = soloRankings.indexOfFirst { it.userId == viewModel.currentUserId }.let {
        if (it >= 0) it + 1 else 1
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && activity != null) {
            viewModel.startVoiceDictation { spoken ->
                viewModel.saveVoiceNoteToActivity(activity.id, spoken)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post-Quest Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("activity_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activity != null) {
                        IconButton(
                            onClick = {
                                viewModel.deleteActivity(activity.id)
                                onNavigateBack()
                            }
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete activity")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (activity == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text("Quest not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val routePoints = remember(activity.routePoints) { activity.decodedRoute() }
        val estCalories = (activity.distanceMiles * 105.0).roundToInt().coerceAtLeast(15)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Striders Howl Completed Header (matching Post-Run mockup!)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_striders_howl_logo),
                        contentDescription = "Striders Howl Crest",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(2.dp, WolfFrostBlue, CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activity.typeEnum().displayName} Completed",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = WolfFrostBlue
                        )
                        Text(
                            text = activity.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${SummitFormatters.formatDate(activity.startedAtEpochMs)} · ${activity.sourceEnum().displayName} · ~$estCalories kcal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. 2x2 Frosted Midnight-Slate Glass Metric Cards (Distance / Avg Pace / Duration / Elevation)
            item {
                val distNum = if (unit == DistanceUnit.KILOMETERS) activity.distanceMiles * 1.60934 else activity.distanceMiles
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PostRunGlassMetricCard(
                            bigValue = String.format(Locale.US, "%.2f", distNum),
                            unitSuffix = unit.shortLabel,
                            label = "Distance",
                            icon = Icons.Filled.DirectionsWalk,
                            modifier = Modifier.weight(1f)
                        )
                        PostRunGlassMetricCard(
                            bigValue = SummitFormatters.formatPaceRaw(activity.avgPaceSecPerMile, unit),
                            unitSuffix = unit.paceSuffix,
                            label = "Avg Pace",
                            icon = Icons.Filled.AcUnit,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PostRunGlassMetricCard(
                            bigValue = SummitFormatters.formatDuration(activity.durationSeconds),
                            unitSuffix = "",
                            label = "Duration",
                            icon = Icons.Filled.Timer,
                            modifier = Modifier.weight(1f)
                        )
                        PostRunGlassMetricCard(
                            bigValue = SummitFormatters.formatElevation(activity.elevationGainFeet, unit),
                            unitSuffix = "",
                            label = "Elevation",
                            icon = Icons.Filled.Landscape,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Interactive Frost-Cyan Pace Graph (tap to inspect mile splits!)
            item {
                InteractivePaceSplitGraphCard(
                    splitPacesSec = activity.splitPacesSec,
                    avgPaceSec = activity.avgPaceSecPerMile,
                    unit = unit
                )
            }

            // 4. Route Map (when GPS points exist)
            if (routePoints.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.2.dp, WolfFrostBlue.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        OfflineTopoMapView(
                            tileManager = viewModel.offlineMapTileManager,
                            centerLat = routePoints.first().latitude,
                            centerLon = routePoints.first().longitude,
                            routePoints = routePoints,
                            currentLocation = routePoints.lastOrNull(),
                            offlineOnlyMode = false,
                            initialZoom = 14,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 5. Achievement Unlocked + Live Pack Rank 2-Column Showcase (matching Post-Run mockup!)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF12212E),
                        border = BorderStroke(1.2.dp, Color(0xFF3A798F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "Achievement Unlocked!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEBF7FF)
                            )
                            WolfAchievementCrest(
                                badgeKey = latestMilestone?.badgeKey ?: "first_howl_activity",
                                unlocked = true,
                                size = 76.dp
                            )
                            Text(
                                text = latestMilestone?.title ?: "Summit Alpha",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = latestMilestone?.metricHighlight ?: "Pack Quest Complete",
                                style = MaterialTheme.typography.labelSmall,
                                color = WolfFrostBlue
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenLeaderboard,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF12212E),
                        border = BorderStroke(1.2.dp, SummitGold.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("post_run_pack_rank_card")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "Live Pack Rank",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SummitGold
                            )
                            Image(
                                painter = painterResource(R.drawable.img_striders_howl_logo),
                                contentDescription = "Pack Rank Crest",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, SummitGold, CircleShape)
                            )
                            Text(
                                text = "#$myRankIndex · Lv.${rank.level}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = WolfFrostBlue
                            )
                            Text(
                                text = rank.packClass,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC5DCEB),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 6. Advanced AI Route Analysis (Elevation Gain/Loss, Terrain, Open-Meteo Historical Weather, Incline Slowdown, Best Time)
            item {
                RouteAnalysisCard(
                    report = routeAnalysis,
                    isLoading = analysisLoadingId == activity.id,
                    unit = unit,
                    onRunAnalysis = { viewModel.analyzeRouteForActivity(activity) }
                )
            }

            // 7. AI Pace Coach Recap + Voice Trail Note Transcriber
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = WolfFrostBlue)
                                Text("AI Coach & Voice Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            if (recapLoadingId == activity.id) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                FilledTonalButton(
                                    onClick = { viewModel.generateRecapForActivity(activity) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(if (activity.aiCoachingSummary.isNullOrBlank()) "Get AI Recap" else "Refresh")
                                }
                            }
                        }

                        if (!activity.aiCoachingSummary.isNullOrBlank()) {
                            Text(
                                text = activity.aiCoachingSummary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (!activity.notes.isNullOrBlank()) {
                            Text(
                                text = "Notes: ${activity.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!activity.voiceTranscript.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🎙️ Voice Note: \"${activity.voiceTranscript}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WolfFrostBlue,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        if (isListeningVoice && liveTranscript.isNotBlank()) {
                            Text("Transcribing: $liveTranscript", style = MaterialTheme.typography.labelSmall, color = WolfFrostBlue)
                        }
                        OutlinedButton(
                            onClick = {
                                if (isListeningVoice) {
                                    viewModel.stopVoiceDictation()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        viewModel.startVoiceDictation { spoken ->
                                            viewModel.saveVoiceNoteToActivity(activity.id, spoken)
                                        }
                                    } else {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("detail_record_voice_note_button")
                        ) {
                            Icon(if (isListeningVoice) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isListeningVoice) "Stop Recording Note" else "Record Audio Trail Note (Speech-to-Text)")
                        }
                    }
                }
            }

            // 8. Bottom Action Row: Share to Pack + Export Route to COROS Watch (GPX 1.1)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            MilestoneShareRenderer.shareActivity(context, activity, unit, athleteName)
                            viewModel.sendPublicChat(
                                "Just completed ${activity.title} (${SummitFormatters.formatDistance(activity.distanceMiles, unit)} at ${SummitFormatters.formatPace(activity.avgPaceSecPerMile, unit)})!"
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WolfFrostBlue,
                            contentColor = Color(0xFF041D26)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("post_run_share_pack_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share to Pack", fontWeight = FontWeight.ExtraBold)
                    }

                    OutlinedButton(
                        onClick = {
                            val exportPts = routePoints.ifEmpty {
                                CorosRouteExporter.generateStarterLoop(40.0150, -105.2705, activity.distanceMiles.coerceIn(1.0, 26.2))
                            }
                            CorosRouteExporter.exportAndShareToCorosWatch(
                                context = context,
                                trailName = activity.title,
                                discipline = activity.typeEnum(),
                                points = exportPts
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.4.dp, WolfFrostBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("post_run_export_coros_button")
                    ) {
                        Icon(Icons.Filled.Watch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("COROS GPX", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 8b. 9:16 Story Share Card Generator + Living Pack Chronicles Dispatch Link
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = { showStoryShareModal = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("post_run_story_card_button")
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("9:16 Story Card", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenChronicles,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, SummitGold.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("post_run_open_chronicles_button")
                    ) {
                        Icon(Icons.Filled.AutoStories, contentDescription = null, tint = SummitGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pack Chronicle", color = SummitGold, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

        if (showStoryShareModal) {
            StoryShareCardModal(
                payload = StorySharePayload(
                    distanceStr = SummitFormatters.formatDistanceShort(activity.distanceMiles, unit),
                    subtitleStr = activity.title.uppercase(),
                    durationStr = SummitFormatters.formatDuration(activity.durationSeconds),
                    paceStr = SummitFormatters.formatPace(activity.avgPaceSecPerMile, unit),
                    elevationStr = SummitFormatters.formatElevation(activity.elevationGainFeet, unit),
                    badgeTitle = latestMilestone?.title ?: "WINTER PACK STRIDER",
                    routePoints = routePoints,
                    athleteName = athleteName
                ),
                onDismiss = { showStoryShareModal = false }
            )
        }
    }
}

@Composable
private fun PostRunGlassMetricCard(
    bigValue: String,
    unitSuffix: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF13222F),
        border = BorderStroke(1.2.dp, Color(0xFF3E778D)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A3142), Color(0xFF101D28))
                    )
                )
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WolfFrostBlue.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
            )
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = bigValue,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF6FCFF),
                        maxLines = 1
                    )
                    if (unitSuffix.isNotEmpty()) {
                        Text(
                            text = " $unitSuffix",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFCCE3F0),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = WolfFrostBlue
                )
            }
        }
    }
}

@Composable
private fun InteractivePaceSplitGraphCard(
    splitPacesSec: List<Long>,
    avgPaceSec: Long,
    unit: DistanceUnit
) {
    val points = remember(splitPacesSec, avgPaceSec) {
        if (splitPacesSec.size >= 2) splitPacesSec
        else {
            val base = avgPaceSec.coerceIn(300L, 1200L)
            listOf(base + 18, base + 8, base - 5, base - 12, base + 4, base - 16)
        }
    }
    var selectedIdx by remember(points) { mutableIntStateOf(points.lastIndex / 2) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF101C27),
        border = BorderStroke(1.2.dp, Color(0xFF326276)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Interactive Pace Graph",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF2FAFF)
                    )
                    Text(
                        text = "Tap graph to inspect split (${unit.paceSuffix})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8FB0C2)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF18384A),
                    border = BorderStroke(1.dp, WolfFrostBlue)
                ) {
                    Text(
                        text = "Split ${selectedIdx + 1} · ${SummitFormatters.formatPaceRaw(points[selectedIdx], unit)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WolfFrostBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .pointerInput(points) {
                        detectTapGestures { tap ->
                            val frac = (tap.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            selectedIdx = (frac * points.lastIndex).roundToInt().coerceIn(0, points.lastIndex)
                        }
                    }
            ) {
                val minP = (points.minOrNull() ?: 300L).toFloat()
                val maxP = (points.maxOrNull() ?: (minP.toLong() + 60L)).toFloat().coerceAtLeast(minP + 10f)

                // Horizontal grid lines
                for (r in 0..3) {
                    val y = size.height * (r / 3f)
                    drawLine(Color(0xFF1E3747), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }

                val linePath = Path()
                val fillPath = Path()
                val coords = points.mapIndexed { idx, p ->
                    val x = size.width * (idx.toFloat() / points.lastIndex.coerceAtLeast(1))
                    // Faster pace (lower sec) draws higher on graph!
                    val norm = 1f - ((p - minP) / (maxP - minP))
                    val y = size.height * (0.85f - norm * 0.68f)
                    Offset(x, y)
                }

                coords.forEachIndexed { idx, pt ->
                    if (idx == 0) {
                        linePath.moveTo(pt.x, pt.y)
                        fillPath.moveTo(pt.x, size.height)
                        fillPath.lineTo(pt.x, pt.y)
                    } else {
                        linePath.lineTo(pt.x, pt.y)
                        fillPath.lineTo(pt.x, pt.y)
                    }
                }
                fillPath.lineTo(coords.last().x, size.height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(WolfFrostBlue.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
                drawPath(
                    path = linePath,
                    color = WolfFrostBlue,
                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val sel = coords[selectedIdx]
                drawLine(Color(0x885CF0FF), Offset(sel.x, 0f), Offset(sel.x, size.height), strokeWidth = 2f)
                drawCircle(Color.White, radius = 7f, center = sel)
                drawCircle(WolfFrostBlue, radius = 4f, center = sel)
            }
        }
    }
}

@Composable
private fun RouteAnalysisCard(
    report: RouteAnalysisReport?,
    isLoading: Boolean,
    unit: DistanceUnit,
    onRunAnalysis: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, TrailHikeAccent.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Route, contentDescription = null, tint = TrailHikeAccent)
                    Text("Advanced Route Analysis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                if (isLoading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    FilledTonalButton(
                        onClick = onRunAnalysis,
                        modifier = Modifier.testTag("run_route_analysis_button")
                    ) {
                        Text(if (report == null) "Analyze Route" else "Re-Analyze")
                    }
                }
            }

            if (report == null) {
                Text(
                    text = "Run AI Route Analysis to inspect elevation gain/loss, terrain type, real historical weather (Open-Meteo), steep incline slowdown %, and best time-of-day advice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Terrain: ${report.terrainType}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TrailHikeAccent
                )
                Text(
                    text = "Elevation Gain ${SummitFormatters.formatElevation(report.elevationGainFeet, unit)} · Loss -${report.elevationLossFeet} ft · Incline Slowdown: ${report.inclineSlowdownPct}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.WbSunny, contentDescription = null, tint = SummitGold, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Weather: ${report.weatherSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Best Time of Day: ${report.bestTimeAdvice}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = WolfFrostBlue
                )
                report.insights.forEach { tip ->
                    Text(
                        text = "• $tip",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
