package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.ChallengeStage
import com.example.domain.ChallengeStageStatus
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.SummitGold
import com.example.ui.viewmodel.SummitViewModel
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Seasonal Challenge Mode: "The Deep Freeze: Winter Solstice Expedition".
 * Features a moonlit alpine wolf hero header, 3-part Pack Survival Telemetry centerpiece,
 * 4-Stage Tiered Milestone Path (25 / 50 / 75 / 100 MI), Polar Vortex +20% Weather Modifier
 * with real Open-Meteo local freeze alert check, and direct links to the Seasonal Standings
 * Leaderboard and active expedition tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalChallengeScreen(
    viewModel: SummitViewModel,
    onNavigateBack: () -> Unit,
    onOpenSeasonalLeaderboard: () -> Unit,
    onLogExpeditionRun: () -> Unit
) {
    val challenge by viewModel.seasonalChallengeState.collectAsStateWithLifecycle()
    val weather = challenge.weatherStatus

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPolarVortexWeather(notifyUser = granted)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPolarVortexWeather(notifyUser = false)
    }

    Scaffold(
        containerColor = Color(0xFF0A0F1D),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0F1D),
                    titleContentColor = ArcticFrost,
                    navigationIconContentColor = ArcticFrost
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("seasonal_challenge_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = challenge.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.2.sp,
                        color = ArcticFrost
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenSeasonalLeaderboard,
                        modifier = Modifier.testTag("seasonal_header_leaderboard_button")
                    ) {
                        Icon(Icons.Filled.Leaderboard, contentDescription = "Seasonal Leaderboard", tint = GlacialCyan)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = innerPadding.calculateTopPadding() + 6.dp,
                bottom = innerPadding.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("seasonal_challenge_screen")
        ) {
            // 1. Moonlit Winter Solstice Hero Card + Event Countdown Pill
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_trail_hero_1795302667119),
                            contentDescription = "Winter Solstice Wolf Expedition",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x5506101F),
                                            Color(0xB8091426),
                                            Color(0xFA0A0F1D)
                                        )
                                    )
                                )
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xCC0D1B2E),
                                border = BorderStroke(1.dp, SummitGold.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Filled.Timer, contentDescription = null, tint = SummitGold, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "LIMITED SEASON EVENT: ${challenge.daysRemaining} DAYS REMAINING",
                                        color = SummitGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.0.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "${challenge.title}:",
                                color = GlacialCyan,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.6.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = challenge.seasonSubtitle,
                                color = ArcticFrost,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.0.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 2. Pack Survival Telemetry Centerpiece (Territory | Glowing Ring | Expedition Tier)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "PACK SURVIVAL TELEMETRY",
                        color = AlpineMist,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.8.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Left: Territory Patrolled
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(GlacialCyan.copy(alpha = 0.12f))
                                    .border(1.2.dp, GlacialCyan.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Explore, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(22.dp))
                            }
                            Text(
                                text = "TERRITORY PATROLLED",
                                color = AlpineMist,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${challenge.territoryPatrolledSqMi} SQ MI",
                                color = ArcticFrost,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Center: Glowing Circular Mileage Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(156.dp)
                                .testTag("seasonal_mileage_ring")
                        ) {
                            val progress = challenge.overallProgressPercent
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 11.dp.toPx()
                                val inset = stroke / 2f + 4.dp.toPx()
                                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                                val topLeft = Offset(inset, inset)
                                drawArc(
                                    color = Color.White.copy(alpha = 0.10f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(GlacialCyan, Color(0xFF7CF7FF), SummitGold, GlacialCyan)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = (progress * 360f).coerceAtLeast(if (progress > 0f) 6f else 0f),
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", challenge.effectiveExpeditionMiles),
                                    color = ArcticFrost,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "/ ${challenge.totalTargetMiles.roundToInt()} MILES",
                                    color = AlpineMist,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.0.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "${(challenge.overallProgressPercent * 100).roundToInt()}% COMPLETED",
                                    color = GlacialCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Right: Expedition Tier
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SummitGold.copy(alpha = 0.12f))
                                    .border(1.2.dp, SummitGold.copy(alpha = 0.55f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Pets, contentDescription = null, tint = SummitGold, modifier = Modifier.size(22.dp))
                            }
                            Text(
                                text = "EXPEDITION TIER",
                                color = AlpineMist,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = challenge.expeditionTier,
                                color = SummitGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 3. Active Weather Modifier Banner + Local Freeze Push Alert Trigger
            item {
                val activeBorder = if (weather.isPolarVortexActive) GlacialCyan else Color.White.copy(alpha = 0.2f)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GlacialCyan.copy(alpha = if (weather.isPolarVortexActive) 0.10f else 0.04f),
                    border = BorderStroke(1.2.dp, activeBorder.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.AcUnit,
                                contentDescription = "Polar Vortex",
                                tint = GlacialCyan,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ACTIVE WEATHER MODIFIER · ${weather.tempFahrenheit?.roundToInt()?.let { "${it}°F" } ?: "SUB-ZERO"} (${weather.locationLabel})",
                                    color = AlpineMist,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.0.sp
                                )
                                Text(
                                    text = weather.activeModifierTitle,
                                    color = ArcticFrost,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.refreshPolarVortexWeather(notifyUser = true)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("seasonal_check_freeze_alert_button")
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Check Live Freeze Temp & Send Polar Vortex Alert",
                                color = GlacialCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. 4-Stage Tiered Milestone Path (Horizontal Glacier Trail + Detailed Nodes)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Terrain, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "4-STAGE TIERED MILESTONE PATH",
                            color = ArcticFrost.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    // Glowing horizontal glacial path preview
                    GlacierMilestoneStrip(stages = challenge.stages)

                    // Vertical detailed stage cards
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        challenge.stages.forEachIndexed { idx, stage ->
                            ExpeditionMilestoneNodeRow(
                                stage = stage,
                                isLast = idx == challenge.stages.lastIndex,
                                onTapStage = {
                                    if (stage.status == ChallengeStageStatus.COMPLETED) {
                                        viewModel.playBadgeUnlockStinger()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 5. Action Buttons: View Seasonal Standings + Log Expedition Run
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onOpenSeasonalLeaderboard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.4.dp, SummitGold.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("seasonal_open_leaderboard_button")
                    ) {
                        Icon(Icons.Filled.Leaderboard, contentDescription = null, tint = SummitGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "VIEW PACK & SOLO STANDINGS",
                            color = SummitGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Button(
                        onClick = onLogExpeditionRun,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlacialCyan,
                            contentColor = Color(0xFF0A0F1D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("seasonal_log_expedition_run_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "LOG EXPEDITION RUN",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlacierMilestoneStrip(stages: List<ChallengeStage>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp)) {
            if (stages.size < 2) return@Canvas
            val stepX = size.width / (stages.size - 1)
            val yPoints = listOf(0.75f, 0.38f, 0.62f, 0.25f).map { size.height * it }

            val basePath = Path().apply {
                moveTo(0f, yPoints[0])
                for (i in 1 until stages.size) {
                    lineTo(stepX * i, yPoints[i.coerceAtMost(yPoints.lastIndex)])
                }
            }
            drawPath(
                path = basePath,
                color = Color.White.copy(alpha = 0.16f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw lit cyan path through completed/in-progress stages
            val litCount = stages.count { it.status != ChallengeStageStatus.LOCKED }.coerceAtLeast(1)
            if (litCount >= 1) {
                val litPath = Path().apply {
                    moveTo(0f, yPoints[0])
                    for (i in 1 until litCount) {
                        lineTo(stepX * i, yPoints[i.coerceAtMost(yPoints.lastIndex)])
                    }
                }
                drawPath(
                    path = litPath,
                    color = GlacialCyan,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            stages.forEachIndexed { idx, stage ->
                val center = Offset(stepX * idx, yPoints[idx.coerceAtMost(yPoints.lastIndex)])
                val nodeColor = when (stage.status) {
                    ChallengeStageStatus.COMPLETED -> GlacialCyan
                    ChallengeStageStatus.IN_PROGRESS -> SummitGold
                    ChallengeStageStatus.LOCKED -> Color.White.copy(alpha = 0.3f)
                }
                if (stage.status != ChallengeStageStatus.LOCKED) {
                    drawCircle(color = nodeColor.copy(alpha = 0.28f), radius = 12.dp.toPx(), center = center)
                }
                drawCircle(color = Color(0xFF0A0F1D), radius = 7.dp.toPx(), center = center)
                drawCircle(
                    color = nodeColor,
                    radius = 7.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.4.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun ExpeditionMilestoneNodeRow(
    stage: ChallengeStage,
    isLast: Boolean,
    onTapStage: () -> Unit
) {
    val nodeColor = when (stage.status) {
        ChallengeStageStatus.COMPLETED -> GlacialCyan
        ChallengeStageStatus.IN_PROGRESS -> SummitGold
        ChallengeStageStatus.LOCKED -> Color.White.copy(alpha = 0.28f)
    }
    val nodeIcon = when (stage.status) {
        ChallengeStageStatus.COMPLETED -> Icons.Filled.CheckCircle
        ChallengeStageStatus.IN_PROGRESS -> Icons.Filled.RadioButtonChecked
        ChallengeStageStatus.LOCKED -> Icons.Filled.Lock
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = stage.status == ChallengeStageStatus.COMPLETED, onClick = onTapStage)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(nodeColor.copy(alpha = 0.16f))
                    .border(2.dp, nodeColor, CircleShape)
            ) {
                Icon(nodeIcon, contentDescription = null, tint = nodeColor, modifier = Modifier.size(18.dp))
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(50.dp)
                        .background(
                            if (stage.status == ChallengeStageStatus.COMPLETED) {
                                GlacialCyan.copy(alpha = 0.6f)
                            } else {
                                Color.White.copy(alpha = 0.12f)
                            }
                        )
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp, bottom = if (isLast) 4.dp else 18.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "STAGE ${stage.stageNumber}: ${stage.title.uppercase(Locale.US)}",
                    color = if (stage.status == ChallengeStageStatus.LOCKED) Color.White.copy(alpha = 0.54f) else ArcticFrost,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${stage.requiredMiles.roundToInt()} MI",
                    color = nodeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stage.rewardDescription,
                color = AlpineMist,
                fontSize = 11.sp
            )
            if (stage.status == ChallengeStageStatus.IN_PROGRESS) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SummitGold.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "ACTIVE STAGE",
                        color = SummitGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
