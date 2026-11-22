package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ActivityRecord
import com.example.data.model.ActivityType
import com.example.data.model.CoachUiState
import com.example.data.model.DistanceUnit
import com.example.data.model.SummitFormatters
import com.example.data.model.UiState
import com.example.data.model.WolfCoat
import com.example.ui.components.ActivitySummaryCard
import com.example.ui.components.AnimatedWolfAvatar
import com.example.ui.components.ManualLogActivitySheet
import com.example.ui.components.WolfFeatureKind
import com.example.ui.components.WolfFeatureTileIcon
import com.example.ui.theme.SummitGold
import com.example.ui.theme.TerracottaSecondaryDark
import com.example.ui.theme.TrailEquestrianAccent
import com.example.ui.theme.TrailHikeAccent
import com.example.ui.theme.WolfFrostBlue
import com.example.ui.viewmodel.SummitViewModel

@Composable
fun HomeScreen(
    viewModel: SummitViewModel,
    contentPadding: PaddingValues,
    onOpenActivityDetail: (String) -> Unit,
    onNavigateToTrack: () -> Unit,
    onNavigateToPackHall: () -> Unit,
    onNavigateToTrainingPlans: () -> Unit,
    onNavigateToTrailDesigner: () -> Unit,
    onNavigateToQuests: () -> Unit,
    onNavigateToConservation: () -> Unit = {},
    onNavigateToChronicles: () -> Unit = {},
    onNavigateToSeasonalChallenge: () -> Unit = {}
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val activitiesState by viewModel.activitiesState.collectAsStateWithLifecycle()
    val weeklyPlan by viewModel.weeklyPlanState.collectAsStateWithLifecycle()
    val coachState by viewModel.coachState.collectAsStateWithLifecycle()
    val rank by viewModel.adventurerRankState.collectAsStateWithLifecycle()
    val storyChapter by viewModel.wolfStoryState.collectAsStateWithLifecycle()
    val packChronicle by viewModel.packChronicleState.collectAsStateWithLifecycle()
    val seasonalChallenge by viewModel.seasonalChallengeState.collectAsStateWithLifecycle()
    val gearList by viewModel.gearState.collectAsStateWithLifecycle()
    val party by viewModel.tavernPartyState.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val unit = profile?.unitEnum() ?: DistanceUnit.MILES
    val wolfCoat = profile?.wolfCoatEnum() ?: WolfCoat.TIMBER_GREY
    val activities = (activitiesState as? UiState.Success)?.data.orEmpty()
    val totalMiles = activities.sumOf { it.distanceMiles }

    var showManualLogSheet by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Old Wolf Tavern Hero Header + Animated Wolf Coat Avatar & Level XP
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, WolfFrostBlue.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.img_tavern_quest_banner_1795304781052),
                        contentDescription = "Old Wolf Tavern Quest Hearth",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(212.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xA609121A),
                                        Color(0xCC0D1822),
                                        Color(0xF50B131B)
                                    )
                                )
                            )
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedWolfAvatar(coat = wolfCoat, size = 60.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "STRIDERS HOWL · RUN WITH THE PACK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WolfFrostBlue
                                )
                                Text(
                                    text = profile?.displayName ?: "Wolf Strider",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF6FCFF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Lv.${rank.level} · ${rank.packClass} · ${profile?.roleEnum()?.label ?: "My Adventurer"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SummitGold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xCC122836),
                                border = BorderStroke(1.dp, WolfFrostBlue.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.LocalFireDepartment,
                                        contentDescription = "Weekly streak",
                                        tint = TerracottaSecondaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${weeklyPlan.computedStreakWeeks}w",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // XP bar + lifetime mileage
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Pack XP: ${rank.xpIntoLevel} / ${rank.xpForNextLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFC5DCEB)
                                )
                                Text(
                                    text = "Lifetime: ${SummitFormatters.formatDistanceShort(totalMiles, unit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WolfFrostBlue
                                )
                            }
                            LinearProgressIndicator(
                                progress = { rank.progressFraction },
                                color = WolfFrostBlue,
                                trackColor = Color(0xFF1D3445),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        // Primary Launch & Log CTA row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onNavigateToTrack,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WolfFrostBlue,
                                    contentColor = Color(0xFF041D26)
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("home_start_quest_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Start Live Quest", fontWeight = FontWeight.ExtraBold)
                            }
                            OutlinedButton(
                                onClick = { showManualLogSheet = true },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, WolfFrostBlue.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_log_manual_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Log Miles")
                            }
                        }
                    }
                }
            }
        }

        // 2. Story-Driven Living Pack Chronicles & Wolf Saga Chapter Card
        item {
            val chapterSpan = (storyChapter.nextMilestoneMiles - storyChapter.minMiles).coerceAtLeast(1.0)
            val chapterProgress = ((totalMiles - storyChapter.minMiles) / chapterSpan).toFloat().coerceIn(0f, 1f)
            val packSim = packChronicle.packState
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, SummitGold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onNavigateToChronicles)
                    .testTag("home_pack_chronicles_card")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.AutoStories, contentDescription = null, tint = SummitGold, modifier = Modifier.size(20.dp))
                            Text(
                                text = storyChapter.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SummitGold
                            )
                        }
                        Text(
                            text = "${packSim.territorySquareMiles.toInt()} SQ MI · ${packSim.caloricReservesDays}d MEAT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = WolfFrostBlue
                        )
                    }
                    Text(
                        text = storyChapter.narrative,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Chapter ${storyChapter.chapterNumber} of 6 · Open Living Chronicles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Next at ${SummitFormatters.formatDistanceShort(storyChapter.nextMilestoneMiles, unit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WolfFrostBlue
                        )
                    }
                    LinearProgressIndicator(
                        progress = { chapterProgress },
                        color = SummitGold,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // 3. Striders Howl Quick Action Icon Tiles (matching user's custom icon set)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickTavernTile(
                    kind = WolfFeatureKind.PACE_SPEED,
                    title = "AI Plans",
                    subtitle = "Adaptive",
                    onClick = onNavigateToTrainingPlans,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_tile_training_plans")
                )
                QuickTavernTile(
                    kind = WolfFeatureKind.TRAIL_MAP,
                    title = "Trail GPX",
                    subtitle = "To COROS",
                    onClick = onNavigateToTrailDesigner,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_tile_trail_designer")
                )
                QuickTavernTile(
                    kind = WolfFeatureKind.PACK_PARTY,
                    title = "Pack Hall",
                    subtitle = if (party != null) "Linked" else "Rankings",
                    onClick = onNavigateToPackHall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_tile_pack_hall")
                )
                QuickTavernTile(
                    kind = WolfFeatureKind.HEART_ENDURANCE,
                    title = "Quests",
                    subtitle = "Badges",
                    onClick = onNavigateToQuests,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_tile_quests")
                )
            }
        }

        // 3b. The Deep Freeze: Winter Solstice Expedition & Wild Wolf Conservation Hub Banners
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0E1D2D),
                    border = BorderStroke(1.2.dp, WolfFrostBlue.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onNavigateToSeasonalChallenge)
                        .testTag("home_seasonal_challenge_card")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.AcUnit, contentDescription = null, tint = WolfFrostBlue, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${seasonalChallenge.daysRemaining}d LEFT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = SummitGold
                            )
                        }
                        Text(
                            text = "The Deep Freeze",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Solstice Expedition · +20% Vortex",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC5DCEB),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0D1E28),
                    border = BorderStroke(1.2.dp, TrailHikeAccent.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onNavigateToConservation)
                        .testTag("home_conservation_hub_card")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = WolfFrostBlue, modifier = Modifier.size(16.dp))
                            Text(
                                text = "501(C)(3) HUB",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = WolfFrostBlue
                            )
                        }
                        Text(
                            text = "Wolf Corridors",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Donate & Sponsor a Wolf",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC5DCEB),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 4. Active Weekly Pack Quest Progress
        item {
            val pct = (weeklyPlan.loggedMiles / weeklyPlan.targetMiles.coerceAtLeast(0.1)).toFloat().coerceIn(0f, 1f)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToQuests)
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
                                text = "WEEKLY PACK QUEST · ${weeklyPlan.difficultyTier.label.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WolfFrostBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${SummitFormatters.formatDistanceShort(weeklyPlan.loggedMiles, unit)} / ${SummitFormatters.formatDistanceShort(weeklyPlan.targetMiles, unit)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${(pct * 100).toInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = WolfFrostBlue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { pct },
                        color = WolfFrostBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                    Text(
                        text = weeklyPlan.progressionReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5. Encouraging AI Pace Coach Card
        item {
            PaceCoachTavernCard(
                coachState = coachState,
                onRequestCoaching = viewModel::requestPaceCoaching
            )
        }

        // 6. Recent Quest Log
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Recent Tavern Quests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${activities.size} logged",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (activitiesState is UiState.Loading) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator(color = WolfFrostBlue)
                }
            }
        } else if (activities.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(22.dp)
                    ) {
                        Icon(Icons.Filled.Pets, contentDescription = null, tint = WolfFrostBlue, modifier = Modifier.size(34.dp))
                        Text(
                            text = "No Quests Logged Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Start a live GPS run, hike, or equestrian ride—or sync from Health Connect, COROS, Garmin, or Strava!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activities.take(10), key = { it.id }) { act ->
                ActivitySummaryCard(
                    activity = act,
                    unit = unit,
                    onClick = { onOpenActivityDetail(act.id) }
                )
            }
        }
    }

    if (showManualLogSheet) {
        ManualLogActivitySheet(
            unit = unit,
            gearItems = gearList.filter { !it.retired },
            onDismiss = { showManualLogSheet = false },
            onSave = { title, type, dist, mins, elev, gearId, notes ->
                viewModel.logManualActivity(
                    title = title,
                    activityType = type,
                    distanceValueInUserUnit = dist,
                    durationMinutes = mins,
                    elevationInUserUnit = elev,
                    notes = notes,
                    gearId = gearId
                )
                showManualLogSheet = false
            }
        )
    }
}

@Composable
private fun QuickTavernTile(
    kind: WolfFeatureKind,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, kind.accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            WolfFeatureTileIcon(kind = kind, size = 40.dp)
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PaceCoachTavernCard(
    coachState: CoachUiState,
    onRequestCoaching: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, WolfFrostBlue.copy(alpha = 0.45f)),
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
                    Text(
                        text = "AI Pack Pace Coach",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onRequestCoaching,
                    modifier = Modifier.testTag("home_refresh_coach_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh AI coaching", tint = WolfFrostBlue)
                }
            }

            when (coachState) {
                is CoachUiState.Idle -> {
                    Text(
                        text = "Tap below for warm, personalized pace feedback and your next target workout range.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = onRequestCoaching,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_get_coaching_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ask Pack Coach")
                    }
                }
                is CoachUiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = WolfFrostBlue, modifier = Modifier.size(20.dp))
                        Text("Analyzing your pace splits & weekly quest...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is CoachUiState.Ready -> {
                    Text(
                        text = coachState.insight.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WolfFrostBlue
                    )
                    Text(coachState.insight.encouragement, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Focus: ${coachState.insight.improvementFocus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Next Target Pace: ${coachState.insight.targetPaceSuggestion}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = WolfFrostBlue
                            )
                            Text(
                                text = coachState.insight.nextSessionDrill,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is CoachUiState.Error -> {
                    Text(
                        text = coachState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(onClick = onRequestCoaching) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
