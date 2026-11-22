package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.SeasonalChallengeEngine
import com.example.domain.SeasonalLeaderboardEntry
import com.example.ui.components.GoldenDonorPawFlair
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.SummitGold
import com.example.ui.viewmodel.SummitViewModel
import java.text.NumberFormat
import java.util.Locale

private val SilverMetallic = Color(0xFFD4E0E8)
private val BronzeMetallic = Color(0xFFCD7F32)

/**
 * Seasonal Challenge Standings Leaderboard matching striders_howl_seasonal_leaderboard.png:
 * Winter Solstice Expedition banner, PACKS / SOLO ALPHAS segmented toggle, illuminated Top-3
 * Podium (Gold / Silver / Bronze), ranked cards with +20% POLAR VORTEX badges, and a docked
 * floating "Your Pack / Your Rank" action footer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalLeaderboardScreen(
    viewModel: SummitViewModel,
    onNavigateBack: () -> Unit,
    onContributeRun: () -> Unit
) {
    val challenge by viewModel.seasonalChallengeState.collectAsStateWithLifecycle()
    val donorBadge by viewModel.donorBadgeState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = PACKS, 1 = SOLO ALPHAS

    val entries = if (selectedTab == 0) challenge.packStandings else challenge.soloStandings
    val userEntry = entries.firstOrNull { it.isCurrentUser }
    val gapSummary = SeasonalChallengeEngine.formatGapToNextRank(entries)
    val nf = NumberFormat.getIntegerInstance(Locale.US)

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
                        modifier = Modifier.testTag("seasonal_leaderboard_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = "SEASONAL STANDINGS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.0.sp,
                        color = ArcticFrost
                    )
                }
            )
        },
        bottomBar = {
            // Docked Floating "Your Pack / Solo Alpha" Status Footer
            Surface(
                color = Color(0xFF0D1426),
                border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.35f)),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlacialCyan.copy(alpha = 0.16f))
                            .border(1.5.dp, GlacialCyan, CircleShape)
                    ) {
                        Text(
                            text = "#${userEntry?.rank ?: 1}",
                            color = GlacialCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedTab == 0) {
                                "YOUR PACK: ${(userEntry?.name ?: "CASCADE WOLVES").uppercase(Locale.US)}"
                            } else {
                                "YOUR STANDING: ${(userEntry?.name ?: "WOLF STRIDER").uppercase(Locale.US)}"
                            },
                            color = ArcticFrost,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", userEntry?.milesLogged ?: 0.0)} MI · $gapSummary",
                            color = AlpineMist,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onContributeRun,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlacialCyan,
                            contentColor = Color(0xFF0A0F1D)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("seasonal_contribute_run_button")
                    ) {
                        Text(
                            text = "CONTRIBUTE RUN",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = innerPadding.calculateTopPadding() + 6.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("seasonal_leaderboard_screen")
        ) {
            // 1. Frosted Winter Solstice Expedition Event Header
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GlacialCyan.copy(alpha = 0.12f),
                    border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(22.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "WINTER SOLSTICE",
                                color = ArcticFrost,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.6.sp
                            )
                            Text(
                                text = "EXPEDITION · ${challenge.daysRemaining} DAYS LEFT",
                                color = GlacialCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Icon(Icons.Filled.AcUnit, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // 2. Two-Way Segmented Pill Toggle: PACKS vs SOLO ALPHAS
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        LeaderboardSegmentPill(
                            title = "PACKS",
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("leaderboard_tab_packs")
                        )
                        LeaderboardSegmentPill(
                            title = "SOLO ALPHAS",
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("leaderboard_tab_solo")
                        )
                    }
                }
            }

            // 3. Illuminated Top-3 Podium
            if (entries.size >= 3) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        PodiumColumn(
                            entry = entries[1],
                            place = 2,
                            accent = SilverMetallic,
                            pedestalHeight = 96.dp,
                            modifier = Modifier.weight(1f)
                        )
                        PodiumColumn(
                            entry = entries[0],
                            place = 1,
                            accent = SummitGold,
                            pedestalHeight = 128.dp,
                            modifier = Modifier.weight(1.1f)
                        )
                        PodiumColumn(
                            entry = entries[2],
                            place = 3,
                            accent = BronzeMetallic,
                            pedestalHeight = 82.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Section Header
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedTab == 0) {
                            "THE DEEP FREEZE: PACK STANDINGS"
                        } else {
                            "THE DEEP FREEZE: SOLO ALPHAS"
                        },
                        color = GlacialCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.3.sp
                    )
                    Text(
                        text = "BONUS",
                        color = AlpineMist,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 5. Ranked Feed (#4 onwards)
            items(entries.drop(3), key = { "${selectedTab}_${it.rank}_${it.name}" }) { entry ->
                SeasonalRankRow(
                    entry = entry,
                    nf = nf,
                    showDonorFlair = (entry.isCurrentUser && donorBadge?.isEquippedAsProfileFlair == true) || entry.rank == 4
                )
            }
        }
    }
}

@Composable
private fun LeaderboardSegmentPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) GlacialCyan else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            color = if (selected) Color(0xFF0A0F1D) else Color.White.copy(alpha = 0.65f),
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun PodiumColumn(
    entry: SeasonalLeaderboardEntry,
    place: Int,
    accent: Color,
    pedestalHeight: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (place == 1) 54.dp else 44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .border(if (place == 1) 2.2.dp else 1.6.dp, accent, CircleShape)
        ) {
            Icon(
                Icons.Filled.Pets,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(if (place == 1) 28.dp else 22.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.name,
            color = ArcticFrost,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
        Text(
            text = "${String.format(Locale.US, "%.1f", entry.milesLogged)} MI",
            color = accent,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.06f))
                    )
                )
                .border(
                    width = 1.3.dp,
                    color = accent.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
        ) {
            Text(
                text = "$place",
                color = accent,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun SeasonalRankRow(
    entry: SeasonalLeaderboardEntry,
    nf: NumberFormat,
    showDonorFlair: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (entry.isCurrentUser) GlacialCyan.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.04f),
        border = BorderStroke(
            width = if (entry.isCurrentUser) 1.4.dp else 1.dp,
            color = if (entry.isCurrentUser) GlacialCyan.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = "#${entry.rank}",
                color = if (entry.isCurrentUser) GlacialCyan else AlpineMist,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier.width(30.dp)
            )
            Icon(
                Icons.Filled.Pets,
                contentDescription = null,
                tint = if (entry.isCurrentUser) GlacialCyan else GlacialCyan.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.name,
                        color = ArcticFrost,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (showDonorFlair) {
                        GoldenDonorPawFlair()
                    }
                }
                Text(
                    text = "${String.format(Locale.US, "%.1f", entry.milesLogged)} MI · ${nf.format(entry.elevationFt)} FT · ${entry.subtitle}",
                    color = AlpineMist,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.hasPolarBonus) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GlacialCyan.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "+20% POLAR VORTEX",
                        color = GlacialCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
