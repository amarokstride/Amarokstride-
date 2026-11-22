package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GearItemEntity
import com.example.data.model.DistanceUnit
import com.example.data.model.MilestoneRecord
import com.example.data.model.SummitFormatters
import com.example.data.model.UiState
import com.example.data.sharing.MilestoneShareRenderer
import com.example.domain.StorySurvivalBadge
import com.example.ui.components.BadgeUnlockModal
import com.example.ui.components.PackProtectorMedallionCanvas
import com.example.ui.components.StorySurvivalBadgeGrid
import com.example.ui.components.WolfAchievementCrest
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.SummitGold
import com.example.ui.theme.TrailEquestrianAccent
import com.example.ui.theme.TrailHikeAccent
import com.example.ui.theme.WolfFrostBlue
import com.example.ui.viewmodel.SummitViewModel
import java.util.Locale
import kotlin.math.roundToInt

private data class BadgeShowcaseMeta(
    val badgeKey: String,
    val title: String,
    val subtitle: String,
    val metricHint: String
)

private val ALL_WOLF_ACHIEVEMENTS = listOf(
    BadgeShowcaseMeta("first_howl_activity", "First Howl", "Log your first run, hike, or equestrian ride", "1+ mi"),
    BadgeShowcaseMeta("5k_first_frost", "5K First Frost", "Complete a 5K (3.1 mi) outing", "5K"),
    BadgeShowcaseMeta("10k_ice_runner", "10K Ice Runner", "Conquer a 10K (6.2 mi) trail", "10K"),
    BadgeShowcaseMeta("half_marathon_alpha_strider", "Half Marathon 13.1", "Alpha Strider 13.1 mi quest", "13.1 mi"),
    BadgeShowcaseMeta("marathon_26_winter_wolf", "Marathon 26.2", "Winter Wolf 26.2 mi epic", "26.2 mi"),
    BadgeShowcaseMeta("50k_ultra_strider", "50K Ultra", "Finish a 31+ mile alpine ultra", "50K"),
    BadgeShowcaseMeta("ultra_legend_100m", "Ultra Legend 100M", "Reach 100 lifetime Pack miles", "100 mi"),
    BadgeShowcaseMeta("sub_7_pack_leader", "Sub-7 Min Leader", "Hold sub-7:00/mi average pace", "<7:00/mi"),
    BadgeShowcaseMeta("dragonspine_endurance_hunter", "Endurance Hunter", "Conquer a mountain ridge hike", "Peak"),
    BadgeShowcaseMeta("cavalry_outrider_initiate", "Cavalry Outrider", "Complete an Equestrian Endurance ride", "Ride")
)

@Composable
fun ChallengesMilestonesScreen(
    viewModel: SummitViewModel,
    contentPadding: PaddingValues,
    onOpenConservation: () -> Unit = {},
    onOpenChronicles: () -> Unit = {},
    onOpenSeasonalChallenge: () -> Unit = {},
    onOpenSeasonalLeaderboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val weeklyPlan by viewModel.weeklyPlanState.collectAsStateWithLifecycle()
    val milestonesState by viewModel.milestonesState.collectAsStateWithLifecycle()
    val yearlySummary by viewModel.yearlySummaryState.collectAsStateWithLifecycle()
    val gearList by viewModel.gearState.collectAsStateWithLifecycle()
    val packChronicle by viewModel.packChronicleState.collectAsStateWithLifecycle()
    val seasonalChallenge by viewModel.seasonalChallengeState.collectAsStateWithLifecycle()
    val donorBadge by viewModel.donorBadgeState.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val unit = profile?.unitEnum() ?: DistanceUnit.MILES
    val athleteName = profile?.displayName ?: "Wolf Strider"
    val unlockedMilestones = (milestonesState as? UiState.Success)?.data.orEmpty()
    val unlockedByKey = remember(unlockedMilestones) { unlockedMilestones.associateBy { it.badgeKey } }

    var baselineSlider by remember(profile?.weeklyBaselineMiles) {
        mutableFloatStateOf((profile?.weeklyBaselineMiles ?: 10.0).toFloat().coerceIn(3f, 60f))
    }
    var showAddGearDialog by remember { mutableStateOf(false) }
    var inspectedStoryBadge by remember { mutableStateOf<StorySurvivalBadge?>(null) }

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
        // 1. Progressive Weekly Pack Challenge Card + Baseline Slider
        item {
            val pct = (weeklyPlan.loggedMiles / weeklyPlan.targetMiles.coerceAtLeast(0.1)).toFloat().coerceIn(0f, 1f)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.2.dp, WolfFrostBlue.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "PROGRESSIVE WEEKLY PACK QUEST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = WolfFrostBlue
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${SummitFormatters.formatDistanceShort(weeklyPlan.loggedMiles, unit)} / ${SummitFormatters.formatDistanceShort(weeklyPlan.targetMiles, unit)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = weeklyPlan.difficultyTier.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = SummitGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { pct },
                        color = WolfFrostBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                    Text(
                        text = weeklyPlan.progressionReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Weekly Baseline Target: ${baselineSlider.roundToInt()} mi",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = baselineSlider,
                        onValueChange = { baselineSlider = it },
                        onValueChangeFinished = { viewModel.updateWeeklyBaseline(baselineSlider.roundToInt().toDouble()) },
                        valueRange = 3f..60f
                    )
                }
            }
        }

        // 1b. The Deep Freeze: Winter Solstice Expedition + Seasonal Standings + Conservation Hub
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0C1726),
                border = BorderStroke(1.3.dp, GlacialCyan.copy(alpha = 0.55f)),
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.AcUnit, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = "SEASONAL EXPEDITION · ${seasonalChallenge.daysRemaining} DAYS LEFT",
                                color = GlacialCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.1f", seasonalChallenge.effectiveExpeditionMiles)} / 100 MI",
                            color = SummitGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "The Deep Freeze: Winter Solstice",
                        color = ArcticFrost,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "4-stage migration path with Polar Vortex +20% bonus & live Pack/Solo Standings.",
                        color = AlpineMist,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onOpenSeasonalChallenge,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("quests_open_seasonal_challenge_button")
                        ) {
                            Text("Expedition", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onOpenSeasonalLeaderboard,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("quests_open_seasonal_leaderboard_button")
                        ) {
                            Icon(Icons.Filled.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Standings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 1c. Featured Pack Protector Conservation Donor Badge + Conservation & Pack Chronicles Links
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0B1A26),
                border = BorderStroke(
                    width = if (donorBadge != null) 1.5.dp else 1.dp,
                    color = if (donorBadge != null) SummitGold.copy(alpha = 0.75f) else GlacialCyan.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    PackProtectorMedallionCanvas(size = 76.dp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (donorBadge != null) "PACK PROTECTOR · EQUIPPED" else "PACK PROTECTOR DONOR MEDAL",
                            color = if (donorBadge != null) SummitGold else GlacialCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = donorBadge?.organizationName?.let { "Benefactor of $it" }
                                ?: "Support a 501(c)(3) Wild Wolf Sanctuary",
                            color = ArcticFrost,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onOpenConservation,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("quests_open_conservation_button")
                            ) {
                                Icon(Icons.Filled.VolunteerActivism, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Wolf Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onOpenChronicles,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("quests_open_chronicles_button")
                            ) {
                                Icon(Icons.Filled.AutoStories, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Chronicles", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 1d. 6 Custom Runic Story & Survival Achievement Badges
        if (packChronicle.storyBadges.isNotEmpty()) {
            item {
                StorySurvivalBadgeGrid(
                    badges = packChronicle.storyBadges,
                    onBadgeClick = { badge ->
                        inspectedStoryBadge = badge
                        viewModel.playBadgeUnlockStinger()
                    }
                )
            }
        }

        // 2. Striders Howl Achievements Showcase (with WolfAchievementCrest + Social Share)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Striders Howl Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${unlockedMilestones.size} / ${ALL_WOLF_ACHIEVEMENTS.size} Unlocked",
                    style = MaterialTheme.typography.labelMedium,
                    color = WolfFrostBlue
                )
            }
        }

        items(ALL_WOLF_ACHIEVEMENTS.chunked(2)) { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                pair.forEach { badge ->
                    val unlockedRecord: MilestoneRecord? = unlockedByKey[badge.badgeKey]
                    val isUnlocked = unlockedRecord != null
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(
                            if (isUnlocked) 1.5.dp else 1.dp,
                            if (isUnlocked) SummitGold else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            WolfAchievementCrest(
                                badgeKey = badge.badgeKey,
                                unlocked = isUnlocked,
                                size = 76.dp
                            )
                            Text(
                                text = badge.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = unlockedRecord?.metricHighlight ?: badge.metricHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUnlocked) WolfFrostBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (unlockedRecord != null) {
                                FilledTonalButton(
                                    onClick = {
                                        MilestoneShareRenderer.shareMilestone(context, unlockedRecord, athleteName)
                                        viewModel.markMilestoneShared(unlockedRecord.id)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share achievement", modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Text(
                                    text = "Locked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // 3. End-of-Year Journey Recap Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.2.dp, SummitGold.copy(alpha = 0.6f)),
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
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = SummitGold)
                            Text(
                                text = "${yearlySummary.year} End-of-Year Journey Recap",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SummitGold
                            )
                        }
                        IconButton(
                            onClick = {
                                val text = "My ${yearlySummary.year} Striders Howl Journey: " +
                                    "${SummitFormatters.formatDistance(yearlySummary.totalMiles, unit)} across " +
                                    "${yearlySummary.totalSessions} quests (+${yearlySummary.totalElevationFeet} ft)! " +
                                    "${yearlySummary.realmJourneyComparison} #StridersHowl"
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(share, "Share Yearly Journey Recap"))
                            },
                            modifier = Modifier.testTag("share_year_recap_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share yearly recap", tint = WolfFrostBlue)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RecapMiniTile(
                            label = "Year Distance",
                            value = SummitFormatters.formatDistanceShort(yearlySummary.totalMiles, unit),
                            modifier = Modifier.weight(1f)
                        )
                        RecapMiniTile(
                            label = "Quests",
                            value = "${yearlySummary.totalSessions}",
                            modifier = Modifier.weight(1f)
                        )
                        RecapMiniTile(
                            label = "Elevation",
                            value = SummitFormatters.formatElevation(yearlySummary.totalElevationFeet, unit),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "${yearlySummary.realmJourneyComparison} · Peak Month: ${yearlySummary.topMonthLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Gear Mileage Rack (Running Shoes & Equestrian Saddles — Room SQLite)
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
                    Icon(Icons.Filled.Hiking, contentDescription = null, tint = TrailEquestrianAccent)
                    Text(
                        text = "Gear Rack (Shoes & Saddles)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                FilledTonalButton(
                    onClick = { showAddGearDialog = true },
                    modifier = Modifier.testTag("add_gear_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Gear")
                }
            }
        }

        if (gearList.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Track mileage on your trail running shoes and equestrian saddles. Tap 'Add Gear' to monitor wear automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(gearList, key = { it.id }) { gear ->
                GearMileageCard(
                    gear = gear,
                    unit = unit,
                    onToggleRetire = { viewModel.toggleGearRetired(gear.id, !gear.retired) },
                    onDelete = { viewModel.deleteGear(gear.id) }
                )
            }
        }
    }

    if (showAddGearDialog) {
        AddGearDialog(
            onDismiss = { showAddGearDialog = false },
            onSave = { name, category, brand, startMi, maxMi ->
                viewModel.addGearItem(name, category, brand, startMi, maxMi)
                showAddGearDialog = false
            }
        )
    }

    val activeStoryBadge = inspectedStoryBadge
    if (activeStoryBadge != null) {
        BadgeUnlockModal(
            badge = activeStoryBadge,
            onDismiss = { inspectedStoryBadge = null },
            onPlayStinger = { viewModel.playBadgeUnlockStinger() }
        )
    }
}

@Composable
private fun RecapMiniTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = WolfFrostBlue)
        }
    }
}

@Composable
private fun GearMileageCard(
    gear: GearItemEntity,
    unit: DistanceUnit,
    onToggleRetire: () -> Unit,
    onDelete: () -> Unit
) {
    val barColor = when {
        gear.wearFraction >= 0.9f -> MaterialTheme.colorScheme.error
        gear.category == "SADDLE" -> TrailEquestrianAccent
        else -> TrailHikeAccent
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${if (gear.category == "SADDLE") "EQUESTRIAN SADDLE" else "RUNNING / TRAIL SHOE"}${if (gear.retired) " · RETIRED" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(gear.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(gear.brand, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    TextButton(onClick = onToggleRetire) {
                        Text(if (gear.retired) "Activate" else "Retire")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete gear")
                    }
                }
            }
            LinearProgressIndicator(
                progress = { gear.wearFraction },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Text(
                text = String.format(
                    Locale.US,
                    "%s / %s (%.0f%% lifespan)",
                    SummitFormatters.formatDistanceShort(gear.loggedMiles, unit),
                    SummitFormatters.formatDistanceShort(gear.maxMiles, unit),
                    gear.wearFraction * 100f
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddGearDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, brand: String, startMiles: Double, maxMiles: Double) -> Unit
) {
    var category by remember { mutableStateOf("SHOE") }
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var startMiles by remember { mutableStateOf("0") }
    var maxMiles by remember { mutableStateOf("400") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Running Shoe or Saddle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == "SHOE",
                        onClick = {
                            category = "SHOE"
                            maxMiles = "400"
                        },
                        label = { Text("Shoe") }
                    )
                    FilterChip(
                        selected = category == "SADDLE",
                        onClick = {
                            category = "SADDLE"
                            maxMiles = "1500"
                        },
                        label = { Text("Equestrian Saddle") }
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (category == "SADDLE") "Saddle Name (e.g. Endurance Tree)" else "Shoe Name (e.g. Trail Lone Peak)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand / Saddler") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startMiles,
                        onValueChange = { startMiles = it },
                        label = { Text("Current mi") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxMiles,
                        onValueChange = { maxMiles = it },
                        label = { Text("Alert at mi") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        category,
                        brand,
                        startMiles.toDoubleOrNull() ?: 0.0,
                        maxMiles.toDoubleOrNull() ?: if (category == "SADDLE") 1500.0 else 400.0
                    )
                }
            ) {
                Text("Save Gear")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
