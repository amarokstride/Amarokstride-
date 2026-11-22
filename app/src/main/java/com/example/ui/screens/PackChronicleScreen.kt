package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.ChronicleDispatch
import com.example.domain.PackEventSeverity
import com.example.domain.PackMember
import com.example.domain.StorySurvivalBadge
import com.example.ui.components.BadgeUnlockModal
import com.example.ui.components.StorySurvivalBadgeGrid
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.CrimsonSurge
import com.example.ui.theme.FrostedRim
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.SummitGold
import com.example.ui.viewmodel.SummitViewModel
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The Living Pack Chronicles — interactive biological wolf pack survival simulation
 * driven by the runner's real logged workouts.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PackChronicleScreen(
    viewModel: SummitViewModel,
    onNavigateBack: () -> Unit,
    onStartPatrolRun: () -> Unit
) {
    val snapshot by viewModel.packChronicleState.collectAsStateWithLifecycle()
    val packState = snapshot.packState
    val dispatches = snapshot.dispatches
    var inspectedBadge by remember { mutableStateOf<StorySurvivalBadge?>(null) }

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
                        modifier = Modifier.testTag("chronicle_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = packState.packName.uppercase(Locale.US),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.8.sp,
                            color = ArcticFrost
                        )
                        Text(
                            text = "${packState.season.displayName.uppercase(Locale.US)} · DAY ${packState.daysSurviving}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlacialCyan,
                            letterSpacing = 1.0.sp
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("pack_chronicle_screen")
        ) {
            // 1. Pack Vital Signs Telemetry Bar
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp)
                    ) {
                        ChronicleVitalStat(
                            label = "TERRITORY",
                            value = "${packState.territorySquareMiles.roundToInt()} SQ MI",
                            accent = GlacialCyan
                        )
                        Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.12f)))
                        ChronicleVitalStat(
                            label = "MEAT STORES",
                            value = "${packState.caloricReservesDays} DAYS",
                            accent = if (packState.caloricReservesDays <= 2) CrimsonSurge else ArcticFrost
                        )
                        Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.12f)))
                        ChronicleVitalStat(
                            label = "PACK HEALTH",
                            value = packState.packHealthTier,
                            accent = SummitGold
                        )
                    }
                }
            }

            // 2. Pack Roster Vital Energy Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "VANGUARD PACK ROSTER",
                        color = AlpineMist,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(packState.members, key = { it.name }) { member ->
                            PackMemberVitalCard(member)
                        }
                    }
                }
            }

            // 3. Active Territory Waypoints Secured
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, FrostedRim.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Explore, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "PATROLLED WINTER CORRIDORS (${packState.activeTerritoryWaypoints.size})",
                                color = ArcticFrost,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            packState.activeTerritoryWaypoints.forEach { waypoint ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = GlacialCyan.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = waypoint,
                                        color = ArcticFrost,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3b. Custom Runic Story & Survival Achievement Badges (tap to preview + play Pack Howl Stinger)
            if (snapshot.storyBadges.isNotEmpty()) {
                item {
                    StorySurvivalBadgeGrid(
                        badges = snapshot.storyBadges,
                        onBadgeClick = { badge ->
                            inspectedBadge = badge
                            viewModel.playBadgeUnlockStinger()
                        }
                    )
                }
            }

            // 4. Story Dispatch Timeline Header
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
                        Icon(Icons.Filled.AutoStories, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "THE WINTER CHRONICLES",
                            color = ArcticFrost.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Text(
                        text = "${dispatches.size} entries",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlpineMist
                    )
                }
            }

            // 5. Chronological Dispatches or Awakening Empty State
            if (dispatches.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.03f),
                        border = BorderStroke(1.dp, FrostedRim.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Filled.Pets, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(36.dp))
                            Text(
                                text = "No winter patrols recorded yet. Lace up and log your first miles to awaken the Blacktail Ridge Pack.",
                                color = AlpineMist,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onStartPatrolRun,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlacialCyan,
                                    contentColor = Color(0xFF0A0F1D)
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("chronicle_start_patrol_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("START PATROL RUN", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            } else {
                items(dispatches, key = { it.id }) { dispatch ->
                    ChronicleStoryCard(dispatch = dispatch)
                }
            }
        }

        val activeBadge = inspectedBadge
        if (activeBadge != null) {
            BadgeUnlockModal(
                badge = activeBadge,
                onDismiss = { inspectedBadge = null },
                onPlayStinger = { viewModel.playBadgeUnlockStinger() }
            )
        }
    }
}

@Composable
private fun ChronicleVitalStat(
    label: String,
    value: String,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AlpineMist, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
        Spacer(Modifier.height(5.dp))
        Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PackMemberVitalCard(member: PackMember) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, FrostedRim.copy(alpha = 0.55f)),
        modifier = Modifier.width(146.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(member.name, color = ArcticFrost, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Icon(Icons.Filled.Pets, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(14.dp))
            }
            Text(member.role, color = GlacialCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(member.coatColor, color = AlpineMist, fontSize = 10.sp)
            Spacer(Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { member.energyLevel },
                color = if (member.energyLevel < 0.45f) CrimsonSurge else GlacialCyan,
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun ChronicleStoryCard(dispatch: ChronicleDispatch) {
    val accentColor = when (dispatch.severity) {
        PackEventSeverity.TRIUMPH -> GlacialCyan
        PackEventSeverity.PERIL -> CrimsonSurge
        PackEventSeverity.CRITICAL -> SummitGold
        PackEventSeverity.ROUTINE -> ArcticFrost.copy(alpha = 0.8f)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dispatch.dateString,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = dispatch.severity.badgeLabel,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = dispatch.title,
                color = ArcticFrost,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = dispatch.journalBody,
                color = ArcticFrost.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.04f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Text(
                        text = dispatch.runnerImpactSummary,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
