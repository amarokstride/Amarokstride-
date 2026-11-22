package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.audio.ClimbAudioMode
import com.example.data.audio.SoundboardTrigger
import com.example.data.integrations.ExternalFitnessConnectors
import com.example.data.maps.OfflineMapTileManager
import com.example.data.model.ActivityType
import com.example.data.model.DistanceUnit
import com.example.data.model.EquestrianGait
import com.example.data.model.SummitFormatters
import com.example.data.model.UiState
import com.example.data.model.WolfCoat
import com.example.data.model.WolfGait
import com.example.data.safety.GloveGestureType
import com.example.data.safety.HardwareKeyController
import com.example.data.safety.HeadphoneHardwareAction
import com.example.domain.PaceZone
import com.example.ui.components.ActiveSosBeaconDialog
import com.example.ui.components.EmergencyContactsDialog
import com.example.ui.components.GpxPreviewModal
import com.example.ui.components.OfflineTopoMapView
import com.example.ui.components.PaceTargetModalBottomSheet
import com.example.ui.components.SosCountdownDialog
import com.example.ui.components.WolfMascot3DView
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.BasaltObsidian
import com.example.ui.theme.CrimsonSurge
import com.example.ui.theme.FrostedRim
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.SummitGold
import com.example.ui.theme.TrailEquestrianAccent
import com.example.ui.theme.TrailHikeAccent
import com.example.ui.theme.WolfFrostBlue
import com.example.ui.viewmodel.SummitViewModel
import com.example.ui.viewmodel.TrackingStatus
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackScreen(
    viewModel: SummitViewModel,
    contentPadding: PaddingValues,
    onOpenTrailDesigner: () -> Unit,
    onOpenSavedActivity: (String) -> Unit
) {
    val context = LocalContext.current
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
    val offlineMapState by viewModel.offlineMapState.collectAsStateWithLifecycle()
    val gearItems by viewModel.gearState.collectAsStateWithLifecycle()
    val voiceCoachEnabled by viewModel.voiceCoachEnabled.collectAsStateWithLifecycle()
    val targetPaceSecPerMile by viewModel.targetPaceSecPerMile.collectAsStateWithLifecycle()
    val soundboardState by viewModel.soundboardState.collectAsStateWithLifecycle()
    val wolfHowlTrigger by viewModel.wolfHowlTrigger.collectAsStateWithLifecycle()
    val isListeningVoice by viewModel.isListeningVoice.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val recentlySavedId by viewModel.recentlySavedActivityId.collectAsStateWithLifecycle()
    val sosState by viewModel.sosState.collectAsStateWithLifecycle()
    val gloveState by viewModel.gloveControlState.collectAsStateWithLifecycle()
    val previewGpxTrail by viewModel.previewGpxTrail.collectAsStateWithLifecycle()
    val activePlannedTrail by viewModel.activePlannedTrail.collectAsStateWithLifecycle()
    val phoneStepState by viewModel.phoneStepState.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val unit = profile?.unitEnum() ?: DistanceUnit.MILES
    val wolfCoat = profile?.wolfCoatEnum() ?: WolfCoat.TIMBER_GREY

    var showFinishDialog by remember { mutableStateOf(false) }
    var showTargetPaceDialog by remember { mutableStateOf(false) }
    var showEmergencyContactsDialog by remember { mutableStateOf(false) }
    var draftPaceMin by remember(targetPaceSecPerMile) { mutableStateOf((targetPaceSecPerMile / 60L).toInt()) }
    var draftPaceSec by remember(targetPaceSecPerMile) { mutableStateOf((targetPaceSecPerMile % 60L).toInt()) }
    var showOfflinePacks by remember { mutableStateOf(false) }
    var sessionTitle by remember { mutableStateOf("") }
    var sessionNotes by remember { mutableStateOf("") }
    var sessionVoiceNote by remember { mutableStateOf("") }

    // Open Post-Run Completed screen right after saving a tracked session!
    LaunchedEffect(recentlySavedId) {
        val id = recentlySavedId
        if (!id.isNullOrBlank()) {
            viewModel.consumeRecentlySavedActivityId()
            onOpenSavedActivity(id)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val locGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val stepGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms[Manifest.permission.ACTIVITY_RECOGNITION] == true ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
        } else true
        viewModel.startLiveTracking(
            context = context,
            hasLocationPermission = locGranted,
            hasActivityRecognitionPermission = stepGranted,
            stepOnlyMode = !locGranted
        )
    }

    val stepOnlyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.startLiveTracking(
            context = context,
            hasLocationPermission = false,
            hasActivityRecognitionPermission = granted,
            stepOnlyMode = true
        )
    }

    val gpxPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importGpxRouteForPreview(context, uri)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceDictation { text ->
                sessionVoiceNote = if (sessionVoiceNote.isBlank()) text else "$sessionVoiceNote $text"
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasLoc = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasLoc) viewModel.attachSingleLocationFix(context)
    }

    val activeWolfGait = remember(trackingState.status, trackingState.activityType, trackingState.currentPaceSecPerMile, trackingState.equestrianState.currentGait, wolfHowlTrigger) {
        if (wolfHowlTrigger) WolfGait.HOWL else viewModel.currentWolfGait()
    }

    val plannedGeoRoute = remember(activePlannedTrail) { activePlannedTrail?.toGeoRoute().orEmpty() }
    val displayedRoutePoints = if (trackingState.routePoints.isNotEmpty()) trackingState.routePoints else plannedGeoRoute

    val mapCenterLat = trackingState.routePoints.lastOrNull()?.latitude
        ?: plannedGeoRoute.firstOrNull()?.latitude
        ?: trackingState.lastKnownLocation?.latitude
        ?: 40.0150
    val mapCenterLon = trackingState.routePoints.lastOrNull()?.longitude
        ?: plannedGeoRoute.firstOrNull()?.longitude
        ?: trackingState.lastKnownLocation?.longitude
        ?: -105.2705

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Striders Howl Frost Header + Discipline Selector (Run / Hike / Equestrian)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_striders_howl_logo),
                        contentDescription = "Striders Howl Wolf Emblem",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, WolfFrostBlue, CircleShape)
                    )
                    Column {
                        Text(
                            text = "STRIDERS HOWL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF2FAFF)
                        )
                        Text(
                            text = if (trackingState.isStepOnlyMode) "PHONE STEP TRACKER (NO WATCH)" else "GPS + PHONE STEP TRACKER",
                            style = MaterialTheme.typography.labelSmall,
                            color = WolfFrostBlue
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActivityType.entries.forEach { type ->
                        FilterChip(
                            selected = trackingState.activityType == type,
                            onClick = { viewModel.setTrackingActivityType(type) },
                            enabled = trackingState.status == TrackingStatus.IDLE,
                            label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("track_type_${type.name.lowercase(Locale.US)}")
                        )
                    }
                }
            }
        }

        // 2. Dark Frost-Cyan Topographic GPS Map Card with Live Kalman + EMA Filter HUD Badge
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, WolfFrostBlue.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OfflineTopoMapView(
                        tileManager = viewModel.offlineMapTileManager,
                        centerLat = mapCenterLat,
                        centerLon = mapCenterLon,
                        routePoints = displayedRoutePoints,
                        currentLocation = trackingState.lastKnownLocation,
                        offlineOnlyMode = trackingState.offlineOnlyMapMode,
                        initialZoom = 14,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BasaltObsidian.copy(alpha = 0.84f),
                        border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .testTag("gps_smoothing_status_badge")
                    ) {
                        val accText = if (trackingState.gpsAccuracyMeters > 0f) {
                            "±${trackingState.gpsAccuracyMeters.toInt()}m"
                        } else {
                            "READY"
                        }
                        val rejectText = if (trackingState.gpsRejectedOutliers > 0) {
                            " · ${trackingState.gpsRejectedOutliers} DRIFT REJECTED"
                        } else {
                            ""
                        }
                        val gradeText = if (trackingState.status == TrackingStatus.RECORDING) {
                            val sign = if (trackingState.currentGradePercent >= 0) "+" else ""
                            " · $sign${String.format(Locale.US, "%.1f", trackingState.currentGradePercent)}% GRADE"
                        } else {
                            ""
                        }
                        val isSteepClimb = trackingState.currentGradePercent >= soundboardState.climbGrowlThresholdPct
                        Text(
                            text = "KALMAN + EMA GPS · $accText$gradeText$rejectText",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSteepClimb) SummitGold else GlacialCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 3. Striders Howl Frost-Cyan Live Metrics HUD (matching Theme mockup!)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0E1822),
                border = BorderStroke(1.4.dp, Color(0xFF39768C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    val distVal = if (unit == DistanceUnit.KILOMETERS) {
                        trackingState.distanceMiles * 1.60934
                    } else {
                        trackingState.distanceMiles
                    }
                    val displayPaceSec = trackingState.currentPaceSecPerMile
                        .takeIf { it > 0L } ?: trackingState.avgPaceSecPerMile
                    val paceStr = if (displayPaceSec > 0L) {
                        SummitFormatters.formatPaceRaw(displayPaceSec, unit)
                    } else "--:--"

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = String.format(Locale.US, "%.2f", distVal),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WolfFrostBlue
                            )
                            Text(
                                text = if (unit == DistanceUnit.KILOMETERS) "KILOMETERS" else "MILES",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF9CB6C7)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = paceStr,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF6FCFF)
                                )
                                Text(
                                    text = " ${unit.paceSuffix.uppercase()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFC2D7E6),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                            Text(
                                text = "PACE",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF9CB6C7)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = SummitFormatters.formatDuration(trackingState.elapsedSeconds),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF6FCFF)
                            )
                            Text(
                                text = "TIME",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF9CB6C7)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (trackingState.estimatedHeartRateBpm > 0) "${trackingState.estimatedHeartRateBpm}" else "--",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WolfFrostBlue
                                )
                                Text(
                                    text = "BPM",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WolfFrostBlue
                                )
                                // Live frost-cyan ECG pulse wave
                                Canvas(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(24.dp)
                                ) {
                                    val path = Path().apply {
                                        val mid = size.height * 0.55f
                                        moveTo(0f, mid)
                                        lineTo(size.width * 0.22f, mid)
                                        lineTo(size.width * 0.34f, size.height * 0.12f)
                                        lineTo(size.width * 0.48f, size.height * 0.90f)
                                        lineTo(size.width * 0.62f, size.height * 0.30f)
                                        lineTo(size.width * 0.74f, mid)
                                        lineTo(size.width, mid)
                                    }
                                    drawPath(
                                        path = path,
                                        color = WolfFrostBlue,
                                        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                            Text(
                                text = "HEART RATE · EFFORT",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF9CB6C7)
                            )
                        }
                    }

                    // Watch-Free Phone Step Counter & Live Cadence Strip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF101C28),
                        border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("track_phone_steps_strip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = "Phone Steps",
                                    tint = GlacialCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (trackingState.status == TrackingStatus.IDLE) {
                                            "${phoneStepState.todaySteps} STEPS TODAY"
                                        } else {
                                            "${trackingState.stepCount} STEPS"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ArcticFrost
                                    )
                                    Text(
                                        text = "${trackingState.stepSensorLabel} · No Watch Needed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AlpineMist
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (trackingState.cadenceSpm > 0) "${trackingState.cadenceSpm} SPM" else "-- SPM",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SummitGold
                                )
                                Text(
                                    text = "CADENCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AlpineMist
                                )
                            }
                        }
                    }

                    // Mini Dark Grid Live Elevation Profile
                    LiveElevationGridStrip(
                        elevFeet = trackingState.elevationGainFeet.roundToLong(),
                        unit = unit,
                        routeElevations = trackingState.routePoints.map { it.elevationFeet }
                    )

                    // Glowing Cyan Action Controls matching the Theme mockup!
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (trackingState.status) {
                            TrackingStatus.IDLE -> {
                                Button(
                                    onClick = {
                                        val fineGranted = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        val stepGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.ACTIVITY_RECOGNITION
                                            ) == PackageManager.PERMISSION_GRANTED
                                        } else true
                                        if (fineGranted && stepGranted) {
                                            viewModel.startLiveTracking(
                                                context = context,
                                                hasLocationPermission = true,
                                                hasActivityRecognitionPermission = true,
                                                stepOnlyMode = false
                                            )
                                        } else {
                                            val permsToAsk = buildList {
                                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    add(Manifest.permission.ACTIVITY_RECOGNITION)
                                                }
                                            }.toTypedArray()
                                            locationPermissionLauncher.launch(permsToAsk)
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WolfFrostBlue,
                                        contentColor = Color(0xFF041D26)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("track_start_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "START ${trackingState.activityType.displayName.uppercase()} (GPS + STEPS)",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Filled.Pets, contentDescription = null, modifier = Modifier.size(18.dp))
                                }

                                OutlinedButton(
                                    onClick = {
                                        val stepGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.ACTIVITY_RECOGNITION
                                            ) == PackageManager.PERMISSION_GRANTED
                                        } else true
                                        if (stepGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                            viewModel.startLiveTracking(
                                                context = context,
                                                hasLocationPermission = false,
                                                hasActivityRecognitionPermission = stepGranted,
                                                stepOnlyMode = true
                                            )
                                        } else {
                                            stepOnlyPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("track_start_phone_steps_only_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Smartphone,
                                        contentDescription = null,
                                        tint = GlacialCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Track with Phone Steps Only (Indoor / No Watch)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ArcticFrost
                                    )
                                }
                            }
                            TrackingStatus.RECORDING, TrackingStatus.PAUSED -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val isRecording = trackingState.status == TrackingStatus.RECORDING
                                    Button(
                                        onClick = {
                                            if (isRecording) viewModel.pauseTracking() else viewModel.resumeTracking()
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WolfFrostBlue,
                                            contentColor = Color(0xFF041D26)
                                        ),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(50.dp)
                                            .testTag("track_pause_resume_button")
                                    ) {
                                        Icon(
                                            if (isRecording) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isRecording) "PAUSE" else "RESUME",
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.pauseTracking()
                                            sessionTitle = "${trackingState.activityType.questLabel} Quest"
                                            showFinishDialog = true
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, Color(0xFFD3EEF9)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("track_finish_button")
                                    ) {
                                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("FINISH", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Equestrian Endurance 10m Walk / 10m Trot / 10m Canter Progressive Interval HUD
        if (trackingState.activityType == ActivityType.EQUESTRIAN) {
            item {
                val eq = trackingState.equestrianState
                val segProgress = 1f - (eq.secondsRemainingInSegment.toFloat() / eq.segmentDurationSeconds.coerceAtLeast(1L).toFloat())
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF211912),
                    border = BorderStroke(1.5.dp, TrailEquestrianAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "CAVALRY ENDURANCE · CYCLE #${eq.cycleNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TrailEquestrianAccent
                                )
                                Text(
                                    text = "Active Gait: ${eq.currentGait.label.uppercase()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = SummitFormatters.formatDuration(eq.secondsRemainingInSegment),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SummitGold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { segProgress.coerceIn(0f, 1f) },
                            color = TrailEquestrianAccent,
                            trackColor = Color(0xFF3E2F23),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = eq.currentGait.cueText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE7D6C4)
                        )
                        Text(
                            text = eq.stageTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = SummitGold
                        )
                    }
                }
            }
        }

        // 5. Live 3D Wolf Companion reacting to pace, equestrian gait, coat color & victory howl
        item {
            WolfMascot3DView(
                gait = activeWolfGait,
                wolfCoat = wolfCoat,
                onTriggerHowl = viewModel::playVictoryHowl
            )
        }

        // 6. The Alpha · Dynamic Gemini Soundboard + Quick Trail Media & Route Launch Bar
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MidnightSlate,
                border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (soundboardState.isMuted) {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = "The Alpha Soundboard Audio",
                                tint = if (soundboardState.isMuted) AlpineMist else GlacialCyan
                            )
                            Column {
                                Text(
                                    text = "THE ALPHA · DYNAMIC SOUNDBOARD",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ArcticFrost
                                )
                                Text(
                                    text = "Gemini Alpha voice cues + 20% auto-ducked wolf pack acoustics",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AlpineMist
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.toggleSoundboardMute(!soundboardState.isMuted) },
                                modifier = Modifier.testTag("soundboard_mute_button")
                            ) {
                                Icon(
                                    imageVector = if (soundboardState.isMuted) {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    },
                                    contentDescription = if (soundboardState.isMuted) "Unmute Soundboard" else "Mute Soundboard",
                                    tint = if (soundboardState.isMuted) CrimsonSurge else GlacialCyan
                                )
                            }
                            Switch(
                                checked = voiceCoachEnabled,
                                onCheckedChange = viewModel::toggleVoiceCoach,
                                modifier = Modifier.testTag("track_voice_coach_switch")
                            )
                        }
                    }

                    // Live Alpha Gemini Cue Readout Banner
                    if (soundboardState.isGeneratingAlphaCue ||
                        !soundboardState.lastAlphaCueText.isNullOrBlank() ||
                        !soundboardState.lastAlphaError.isNullOrBlank()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BasaltObsidian,
                            border = BorderStroke(
                                1.dp,
                                if (soundboardState.lastAlphaError != null) CrimsonSurge.copy(alpha = 0.7f) else GlacialCyan.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                if (soundboardState.isGeneratingAlphaCue) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = GlacialCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "The Alpha is reading your pace & HR...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlacialCyan
                                    )
                                } else if (soundboardState.lastAlphaError != null) {
                                    Text(
                                        text = soundboardState.lastAlphaError.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrimsonSurge,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "THE ALPHA · ${soundboardState.activeTrigger?.title?.uppercase() ?: "LIVE CUE"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GlacialCyan
                                        )
                                        Text(
                                            text = "\"${soundboardState.lastAlphaCueText}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = ArcticFrost
                                        )
                                    }
                                    IconButton(
                                        onClick = viewModel::stopSoundboardAudio,
                                        modifier = Modifier.testTag("soundboard_stop_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Stop,
                                            contentDescription = "Stop Soundboard Audio",
                                            tint = AlpineMist
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5 Interactive Soundboard Trigger Pads (Trigger Mapping Matrix)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SoundboardTrigger.entries.forEach { trigger ->
                            val isSelected = soundboardState.activeTrigger == trigger
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.triggerAlphaSoundboardCue(trigger) },
                                label = {
                                    Text(
                                        text = trigger.shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                },
                                modifier = Modifier.testTag("soundboard_pad_${trigger.name.lowercase(Locale.US)}")
                            )
                        }
                    }

                    // Ambient Pack FX Volume Slider (ducks to 20% while Alpha speaks)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Pack Hunting FX Volume (Auto-Ducks 20%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlpineMist
                            )
                            Text(
                                text = "${(soundboardState.ambientVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GlacialCyan
                            )
                        }
                        Slider(
                            value = soundboardState.ambientVolume,
                            onValueChange = viewModel::setSoundboardAmbientVolume,
                            colors = SliderDefaults.colors(
                                thumbColor = GlacialCyan,
                                activeTrackColor = GlacialCyan,
                                inactiveTrackColor = FrostedRim
                            ),
                            modifier = Modifier.testTag("soundboard_volume_slider")
                        )
                    }

                    // Steep-Climb Wolf Growl Sensitivity & Audio Behavior Controls
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Steep Climb Wolf Growl Trigger",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ArcticFrost
                            )
                            Text(
                                text = "≥ ${soundboardState.climbGrowlThresholdPct.toInt()}% Grade",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SummitGold
                            )
                        }
                        Slider(
                            value = soundboardState.climbGrowlThresholdPct,
                            onValueChange = viewModel::setClimbGrowlThresholdPct,
                            valueRange = 5f..15f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = SummitGold,
                                activeTrackColor = SummitGold,
                                inactiveTrackColor = FrostedRim
                            ),
                            modifier = Modifier.testTag("climb_grade_slider")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ClimbAudioMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = soundboardState.climbAudioMode == mode,
                                    onClick = { viewModel.setClimbAudioMode(mode) },
                                    label = {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("climb_mode_${mode.name.lowercase(Locale.US)}")
                                )
                            }
                        }

                        // Target Pace & Rolling 12s Pace Zone Selector Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "12s Pace Trigger Target",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcticFrost
                                )
                                val zoneColor = when (trackingState.activePaceZone) {
                                    PaceZone.HUNTING_SURGE -> GlacialCyan
                                    PaceZone.ON_TARGET -> TrailHikeAccent
                                    PaceZone.FALLING_BEHIND -> SummitGold
                                }
                                Text(
                                    text = "Zone: ${trackingState.activePaceZone.label} (±15s window)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = zoneColor
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    draftPaceMin = (targetPaceSecPerMile / 60L).toInt()
                                    draftPaceSec = (targetPaceSecPerMile % 60L).toInt()
                                    showTargetPaceDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("target_pace_selector_button")
                            ) {
                                Text(
                                    text = "Target ${SummitFormatters.formatPaceRaw(targetPaceSecPerMile, unit)} ${unit.paceSuffix}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GlacialCyan
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { ExternalFitnessConnectors.openYouTubeMusic(context) }) {
                            Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("YouTube Music")
                        }
                        OutlinedButton(onClick = { ExternalFitnessConnectors.openLibroAudiobooks(context) }) {
                            Icon(Icons.Filled.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Libro.fm")
                        }
                        OutlinedButton(
                            onClick = {
                                ExternalFitnessConnectors.openGoogleMapsRouteSearch(
                                    context = context,
                                    query = "hiking running equestrian trails near me",
                                    lat = mapCenterLat,
                                    lon = mapCenterLon
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Google Maps")
                        }
                        OutlinedButton(
                            onClick = { gpxPickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.testTag("track_import_gpx_button")
                        ) {
                            Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import GPX")
                        }
                        OutlinedButton(onClick = onOpenTrailDesigner) {
                            Icon(Icons.Filled.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Design -> COROS")
                        }
                    }
                }
            }
        }

        // 6b. Cold-Weather Glove & Hardware Controls (Double-Chop + Shokz/Earbud + Volume Keys)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MidnightSlate,
                border = BorderStroke(
                    1.2.dp,
                    if (gloveState.isGloveModeEnabled) GlacialCyan.copy(alpha = 0.65f) else FrostedRim.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.TouchApp,
                                contentDescription = null,
                                tint = if (gloveState.isGloveModeEnabled) GlacialCyan else AlpineMist
                            )
                            Column {
                                Text(
                                    text = "GLOVE & HARDWARE CONTROLS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ArcticFrost
                                )
                                Text(
                                    text = gloveState.lastHardwareFeedback
                                        ?: "Double-Chop = Scent Mark · 1x Earbud = Split · 2x = Howl · Hold = SOS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (gloveState.isGloveModeEnabled) GlacialCyan else AlpineMist
                                )
                            }
                        }
                        Switch(
                            checked = gloveState.isGloveModeEnabled,
                            onCheckedChange = viewModel::toggleGloveHardwareMode,
                            modifier = Modifier.testTag("track_glove_mode_switch")
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.handleGloveGesture(GloveGestureType.DOUBLE_CHOP) },
                            label = { Text("Double-Chop Scent (${gloveState.scentMarkersPlaced})", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("glove_test_double_chop")
                        )
                        FilterChip(
                            selected = false,
                            onClick = { HardwareKeyController.emitManualSimulation(HeadphoneHardwareAction.SINGLE_TAP_VOICE_CUE) },
                            label = { Text("1x Earbud Split", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("glove_test_earbud_split")
                        )
                        FilterChip(
                            selected = false,
                            onClick = { HardwareKeyController.emitManualSimulation(HeadphoneHardwareAction.DOUBLE_TAP_ALPHA_HOWL) },
                            label = { Text("2x Earbud Howl", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("glove_test_earbud_howl")
                        )
                    }
                }
            }
        }

        // 6c. Winter Trail SOS ("Howl for Help") Backcountry Safety Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1A0E14),
                border = BorderStroke(1.3.dp, CrimsonSurge.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = CrimsonSurge)
                            Column {
                                Text(
                                    text = "WINTER TRAIL SOS · HOWL FOR HELP",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = ArcticFrost
                                )
                                Text(
                                    text = "3.8g Fall Detection + 45s Abort + 2.8kHz Acoustic SAR Beacon",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AlpineMist
                                )
                            }
                        }
                        Switch(
                            checked = sosState.fallDetectionEnabled,
                            onCheckedChange = viewModel::toggleFallDetection,
                            modifier = Modifier.testTag("track_fall_detection_switch")
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showEmergencyContactsDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, FrostedRim),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("track_sos_contacts_button")
                        ) {
                            Icon(Icons.Filled.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            val savedContactsCount = sosState.emergencyContacts
                                .split(",", ";")
                                .count { it.trim().isNotEmpty() }
                            Text("Contacts ($savedContactsCount)", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.triggerManualSosCountdown("Manual Winter Trail SOS") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonSurge,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("track_howl_for_help_sos_button")
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("HOWL FOR HELP (SOS)", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 7. Offline Topo Map Packs toggle & manager
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(14.dp)
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
                            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = TrailHikeAccent)
                            Column {
                                Text("Offline Backcountry Maps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${offlineMapState.savedRegions.size} region pack(s) saved locally",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { showOfflinePacks = !showOfflinePacks }) {
                            Text(if (showOfflinePacks) "Hide" else "Manage")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Airplane / Offline-Only Map Mode", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = trackingState.offlineOnlyMapMode,
                            onCheckedChange = { viewModel.toggleOfflineOnlyMapMode() }
                        )
                    }

                    if (offlineMapState.isDownloading) {
                        Text(
                            "Downloading ${offlineMapState.downloadingName} (${offlineMapState.downloadedTiles}/${offlineMapState.totalTiles} tiles)...",
                            style = MaterialTheme.typography.labelSmall,
                            color = WolfFrostBlue
                        )
                        LinearProgressIndicator(
                            progress = {
                                offlineMapState.downloadedTiles.toFloat() / offlineMapState.totalTiles.coerceAtLeast(1).toFloat()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (showOfflinePacks) {
                        OutlinedButton(
                            onClick = { viewModel.downloadCurrentAreaOffline("Current Trailhead Area", mapCenterLat, mapCenterLon) },
                            enabled = !offlineMapState.isDownloading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download Current Area (3 km Offline Pack)")
                        }
                        viewModel.offlineMapTileManager.presetRegions.forEach { preset ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(preset.name, style = MaterialTheme.typography.labelLarge)
                                    Text(preset.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(
                                    onClick = { viewModel.downloadPresetRegion(preset) },
                                    enabled = !offlineMapState.isDownloading
                                ) {
                                    Text("Download")
                                }
                            }
                        }
                        offlineMapState.savedRegions.forEach { saved ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "${saved.name} (${saved.tileCount} tiles)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.deleteOfflineRegion(saved.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete map pack")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Finish Session Dialog with Gear Selector & Live Voice Note Dictation
    if (showFinishDialog) {
        val activeGear = gearItems.filter { !it.retired }
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Complete & Save Quest", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sessionTitle,
                        onValueChange = { sessionTitle = it },
                        label = { Text("Quest Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { sessionNotes = it },
                        label = { Text("Trail Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Voice Note Transcriber inside save dialog
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isListeningVoice) {
                                    viewModel.stopVoiceDictation()
                                } else {
                                    val ok = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (ok) {
                                        viewModel.startVoiceDictation { spoken ->
                                            sessionVoiceNote = if (sessionVoiceNote.isBlank()) spoken else "$sessionVoiceNote $spoken"
                                        }
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        ) {
                            Icon(if (isListeningVoice) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isListeningVoice) "Stop Voice Note" else "Dictate Voice Note")
                        }
                    }
                    if (isListeningVoice && liveTranscript.isNotBlank()) {
                        Text("Listening: $liveTranscript", style = MaterialTheme.typography.labelSmall, color = WolfFrostBlue)
                    }
                    if (sessionVoiceNote.isNotBlank()) {
                        Text("Transcript: \"$sessionVoiceNote\"", style = MaterialTheme.typography.bodySmall, color = WolfFrostBlue)
                    }
                    if (activeGear.isNotEmpty()) {
                        Text("Log Miles to Gear (Shoe / Saddle):", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeGear.forEach { gear ->
                                FilterChip(
                                    selected = trackingState.selectedGearId == gear.id,
                                    onClick = {
                                        viewModel.selectTrackingGear(
                                            if (trackingState.selectedGearId == gear.id) null else gear.id
                                        )
                                    },
                                    label = { Text(gear.name) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishAndSaveTrackedSession(
                            customTitle = sessionTitle,
                            notes = sessionNotes,
                            voiceTranscript = sessionVoiceNote
                        )
                        sessionNotes = ""
                        sessionVoiceNote = ""
                        showFinishDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_session_button")
                ) {
                    Text("Save & Howl!")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.discardTracking()
                            showFinishDialog = false
                        }
                    ) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showFinishDialog = false }) {
                        Text("Keep Going")
                    }
                }
            }
        )
    }

    // Target Pace Selector Modal Bottom Sheet (Dual Snapping Wheel Pickers + Pack Presets)
    if (showTargetPaceDialog) {
        PaceTargetModalBottomSheet(
            initialMinutes = draftPaceMin,
            initialSeconds = draftPaceSec,
            initialAudioEnabled = voiceCoachEnabled && !soundboardState.isMuted,
            unit = unit,
            onDismiss = { showTargetPaceDialog = false },
            onPaceLocked = { min, sec, audioEnabled ->
                viewModel.setTargetPaceMinSec(min, sec)
                viewModel.toggleVoiceCoach(audioEnabled)
                viewModel.toggleSoundboardMute(!audioEnabled)
            }
        )
    }

    // GPX Route Importer & Elevation Preview Modal
    val gpxPreview = previewGpxTrail
    if (gpxPreview != null) {
        GpxPreviewModal(
            summary = gpxPreview,
            targetPaceSecPerMile = targetPaceSecPerMile,
            unit = unit,
            tileManager = viewModel.offlineMapTileManager,
            isDownloadingOffline = offlineMapState.isDownloading,
            onDownloadOfflineMap = { viewModel.downloadOfflinePackForGpx(gpxPreview) },
            onConfirmStartRun = {
                val hasLoc = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                viewModel.confirmStartPackRunWithGpx(context, hasLoc)
            },
            onDismiss = viewModel::dismissGpxPreview
        )
    }

    // Winter SOS Emergency Contacts Manager
    if (showEmergencyContactsDialog) {
        EmergencyContactsDialog(
            initialContacts = sosState.emergencyContacts,
            onDismiss = { showEmergencyContactsDialog = false },
            onSave = { updated ->
                viewModel.saveEmergencyContacts(updated)
                showEmergencyContactsDialog = false
            }
        )
    }

    // Winter SOS 45-Second Countdown Abort Modal
    if (sosState.isCountdownActive) {
        SosCountdownDialog(
            onTriggerSos = { viewModel.confirmAndActivateSos(context) },
            onCancelSos = viewModel::cancelSosCountdown
        )
    }

    // OLED Active Emergency Beacon ("Howl for Help")
    if (sosState.isBeaconActive) {
        val lastPt = trackingState.routePoints.lastOrNull() ?: trackingState.lastKnownLocation
        ActiveSosBeaconDialog(
            lastLocation = lastPt,
            batteryPercent = sosState.batteryPercent,
            sosStartedAtMs = sosState.sosStartedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis(),
            acousticBeaconActive = sosState.isAcousticBeaconPlaying,
            emergencyContacts = sosState.emergencyContacts,
            onToggleAcousticBeacon = { viewModel.toggleAcousticRescueBeacon(!sosState.isAcousticBeaconPlaying) },
            onDispatchSms = { viewModel.dispatchSosSms(context) },
            onEndSos = { viewModel.deactivateSos(context) }
        )
    }
}

@Composable
private fun LiveElevationGridStrip(
    elevFeet: Long,
    unit: DistanceUnit,
    routeElevations: List<Double>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A121A))
            .border(1.dp, Color(0xFF213A4B), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 10
            val rows = 3
            for (c in 1 until cols) {
                val x = size.width * (c.toFloat() / cols)
                drawLine(Color(0xFF1A2F3D), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            }
            for (r in 1 until rows) {
                val y = size.height * (r.toFloat() / rows)
                drawLine(Color(0xFF1A2F3D), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            val pts = if (routeElevations.size >= 3) {
                routeElevations
            } else {
                listOf(10.0, 14.0, 22.0, 18.0, 34.0, 48.0, 62.0, 51.0, 35.0, 20.0)
            }
            val minE = pts.minOrNull() ?: 0.0
            val maxE = (pts.maxOrNull() ?: 1.0).coerceAtLeast(minE + 1.0)
            val linePath = Path()
            val fillPath = Path()
            pts.forEachIndexed { idx, ele ->
                val x = size.width * (idx.toFloat() / (pts.lastIndex.coerceAtLeast(1)))
                val norm = ((ele - minE) / (maxE - minE)).toFloat()
                val y = size.height * (0.85f - norm * 0.65f)
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
                brush = Brush.verticalGradient(
                    listOf(WolfFrostBlue.copy(alpha = 0.35f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = WolfFrostBlue,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        Text(
            text = SummitFormatters.formatElevation(elevFeet, unit),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = WolfFrostBlue,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
