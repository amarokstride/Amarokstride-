package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.ui.components.DonorBadgeAwardModal
import com.example.ui.components.DonorTier
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.FrostedRim
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.SummitGold
import com.example.ui.viewmodel.SummitViewModel
import java.util.Locale
import kotlin.math.roundToInt

private data class WolfCorridorTier(
    val id: String,
    val title: String,
    val regionLabel: String,
    val acresLabel: String,
    val progress: Float,
    val isUnlocked: Boolean,
    val statusText: String,
    val telemetrySummary: String
)

private data class AccreditedWolfOrg(
    val id: String,
    val name: String,
    val category: String,
    val location: String,
    val tagLine: String,
    val briefSummary: String,
    val impactHighlights: List<String>,
    val donateUrl: String,
    val sponsorWolfUrl: String? = null
)

private val AccreditedWolfNonProfits = listOf(
    AccreditedWolfOrg(
        id = "wcc",
        name = "Wolf Conservation Center",
        category = "Species Survival & Wild Reintroduction",
        location = "South Salem, New York",
        tagLine = "Protecting North America's Rarest Canids",
        briefSummary = "The WCC plays a vital role in federal Species Survival Plans (SSP) through dedicated recovery and captive breeding facilities for critically endangered Mexican gray wolves and American red wolves, preparing them for release into wild native ranges.",
        impactHighlights = listOf(
            "Captive breeding and genetic recovery for red and Mexican gray wolves",
            "Direct pre-release behavioral prep and wild reintroduction releases",
            "Live webcams, educational programs, and legislative wolf tracking"
        ),
        donateUrl = "https://nywolf.org/donate/",
        sponsorWolfUrl = "https://nywolf.org/adopt-a-wolf/"
    ),
    AccreditedWolfOrg(
        id = "cwc",
        name = "California Wolf Center",
        category = "Wild Range Recovery & Rancher Coexistence",
        location = "Julian, California",
        tagLine = "Championing Western Wolf Recovery & Peaceful Coexistence",
        briefSummary = "For nearly 50 years, the California Wolf Center has worked toward the natural return of gray wolves to historical ranges across the American West while partnering with ranchers to advance non-lethal coexistence methods.",
        impactHighlights = listOf(
            "$25: Feeds a resident wolf for one full day",
            "$150: Covers annual veterinary vaccinations and wellness care for one wolf",
            "$500: Supplies non-lethal predator deterrent tools for regional ranching communities"
        ),
        donateUrl = "https://www.californiawolfcenter.org/donate",
        sponsorWolfUrl = "https://www.californiawolfcenter.org/sponsor-a-wolf"
    ),
    AccreditedWolfOrg(
        id = "wolf_haven",
        name = "Wolf Haven International",
        category = "Sanctuary Care & SAFE Subspecies Recovery",
        location = "Tenino, Washington",
        tagLine = "Lifetime Dignity & Mexican Wolf Recovery",
        briefSummary = "An AZA-accredited sanctuary participating in the Saving Animals From Extinction (SAFE) program. Wolf Haven provides lifetime sanctuary care and has successfully released 22 Mexican gray wolves into the wild.",
        impactHighlights = listOf(
            "Lifelong sanctuary care for rescued captive-born wolves",
            "Active campaign supporting pairing care and enclosure enrichment for Mexican wolf Hodari (M1458) and F2046",
            "Participation in wild recovery and native prairie habitat restoration"
        ),
        donateUrl = "https://www.wolfhaven.org/support/appeals",
        sponsorWolfUrl = "https://shop.wolfhaven.org/products/sponsor-a-wolf"
    ),
    AccreditedWolfOrg(
        id = "iwc",
        name = "International Wolf Center",
        category = "Global Science Education & Corridor Advocacy",
        location = "Ely, Minnesota",
        tagLine = "Advancing Worldwide Wolf Ecology & Education",
        briefSummary = "The International Wolf Center advances science-based understanding of wolf ecology, pack behavior, and human-wolf relations through world-renowned exhibits, field research, and wildland corridor awareness.",
        impactHighlights = listOf(
            "Comprehensive science-based public education and interactive school curricula",
            "Global wildlife news monitoring and the International Wolf Symposium",
            "Wolf Guardian animal sponsorships supporting ambassador exhibit packs"
        ),
        donateUrl = "https://wolf.org/support/donate/",
        sponsorWolfUrl = "https://wolf.org/support/wolf-guardian-sponsorship/"
    ),
    AccreditedWolfOrg(
        id = "ewc",
        name = "Endangered Wolf Center",
        category = "AZA Species Survival & Reproductive Research",
        location = "Eureka, Missouri",
        tagLine = "Saving Critically Endangered Canids Through Science",
        briefSummary = "Certified by the Association of Zoos and Aquariums (AZA), the Endangered Wolf Center pioneers canid reproductive science, fosters wild cross-fostering releases, and protects native wolf bloodlines.",
        impactHighlights = listOf(
            "AZA-certified Mexican gray wolf and red wolf Species Survival Plan leader",
            "Pioneering wild pup cross-fostering to boost genetic diversity in the wild",
            "Dedicated veterinary care and habitat conservation outreach"
        ),
        donateUrl = "https://www.endangeredwolfcenter.org/donate/",
        sponsorWolfUrl = null
    ),
    AccreditedWolfOrg(
        id = "lww",
        name = "Living with Wolves",
        category = "Field Research & Policy Advocacy",
        location = "Sun Valley / Ketchum, Idaho",
        tagLine = "Truth, Science, and Peaceful Predator Protection",
        briefSummary = "Founded by Jim and Jamie Dutcher following years of documenting the Sawtooth Pack, Living with Wolves conducts field research on pack dynamics and advocates for policies that safeguard wild packs and their migratory habitats.",
        impactHighlights = listOf(
            "Distribution of classroom educational curriculum and live community conflict presentations",
            "Scientific research into wild pack social structures to guide state wildlife agencies",
            "Living with Wolves Endowment and educational museum exhibitions"
        ),
        donateUrl = "https://www.livingwithwolves.org/donate/",
        sponsorWolfUrl = null
    )
)

/**
 * Striders Howl Wild Wolf Conservation Impact Dashboard (`striders_howl_conservation_dashboard.png`).
 * Features collective mileage habitat pool, interactive wild wolf telemetry corridor map,
 * corridor funding tiers, and verified 501(c)(3) non-profit donation portals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConservationDashboardScreen(
    viewModel: SummitViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val activitiesState by viewModel.activitiesState.collectAsStateWithLifecycle()
    val soloRankings by viewModel.soloRankingsState.collectAsStateWithLifecycle()

    val userMiles = remember(activitiesState) {
        (activitiesState as? UiState.Success)?.data.orEmpty().sumOf { it.distanceMiles }
    }
    val communityBoardMiles = remember(soloRankings) {
        soloRankings.sumOf { it.totalMiles }
    }
    // Combine real logged miles onto the global seasonal pack conservation pool
    val totalPackMiles = 124_850L + maxOf(userMiles, communityBoardMiles).roundToInt().toLong()
    val acresProtected = (2_497 + (maxOf(userMiles, communityBoardMiles) / 50.0).toInt()).coerceAtMost(5_000)
    val acresTarget = 5_000
    val poolFraction = (acresProtected.toFloat() / acresTarget.toFloat()).coerceIn(0f, 1f)
    val userAcresContributed = userMiles / 50.0

    val tiers = remember(userMiles) {
        val cascadesProgress = (0.68f + (userMiles / 100_000.0).toFloat()).coerceIn(0.68f, 1.0f)
        listOf(
            WolfCorridorTier(
                id = "bitterroot",
                title = "Bitterroot Range Winter Corridor",
                regionLabel = "MT / ID Alpine Spine",
                acresLabel = "1,200 Acres",
                progress = 1.0f,
                isUnlocked = true,
                statusText = "FUNDED & PROTECTED ($25,000 THRESHOLD REACHED)",
                telemetrySummary = "14 GPS-collared pack members active along wind-scoured elk migration passes."
            ),
            WolfCorridorTier(
                id = "cascades",
                title = "North Cascades Alpine Pass",
                regionLabel = "WA Northern Cascades",
                acresLabel = "2,000 Acres",
                progress = cascadesProgress,
                isUnlocked = false,
                statusText = "${(cascadesProgress * 100).roundToInt()}% COMPLETE (34,120 Miles Left)",
                telemetrySummary = "Active trail-camera grid & non-lethal coexistence fencing along winter valleys."
            ),
            WolfCorridorTier(
                id = "yukon",
                title = "Yukon Boreal Wolf Sanctuary",
                regionLabel = "Sub-Arctic Interior",
                acresLabel = "5,000 Acres",
                progress = 0.15f,
                isUnlocked = false,
                statusText = "LOCKED · OPENS AT TIER 3",
                telemetrySummary = "Deep boreal migration corridor slated for next community telemetry expansion."
            )
        )
    }

    var selectedCorridorIdx by remember { mutableIntStateOf(1) }
    var showDonorSheet by remember { mutableStateOf(false) }
    var selectedOrgDetail by remember { mutableStateOf<AccreditedWolfOrg?>(null) }
    var pendingDonorAwardOrg by remember { mutableStateOf<Pair<String, DonorTier>?>(null) }

    fun openExternalPortal(url: String) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open portal: $url", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyPortalUrl(url: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Wolf Conservation Portal", url))
        Toast.makeText(context, "Portal link copied to clipboard!", Toast.LENGTH_SHORT).show()
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
                        modifier = Modifier.testTag("conservation_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = "WILD WOLF CONSERVATION",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.0.sp
                    )
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("conservation_dashboard_screen")
        ) {
            // 1. Collective Habitat Pool Radial Gauge Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(92.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { poolFraction },
                                strokeWidth = 8.dp,
                                color = GlacialCyan,
                                trackColor = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = Icons.Filled.Pets,
                                contentDescription = "Wolf Habitat Pool",
                                tint = GlacialCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "COLLECTIVE HABITAT POOL",
                                color = AlpineMist,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "${String.format(Locale.US, "%,d", acresProtected)} ACRES",
                                color = ArcticFrost,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${String.format(Locale.US, "%,d", acresTarget)} Acre Target · ${String.format(Locale.US, "%,d", totalPackMiles)} Pack Miles Logged",
                                color = ArcticFrost.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Your Contribution: ${String.format(Locale.US, "%.1f", userMiles)} mi (${String.format(Locale.US, "%.2f", userAcresContributed)} acres funded)",
                                color = GlacialCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Interactive Wild Wolf Habitat Telemetry Corridor Map
            item {
                WildWolfTelemetryMapCard(
                    tiers = tiers,
                    selectedIndex = selectedCorridorIdx,
                    onSelectCorridor = { selectedCorridorIdx = it }
                )
            }

            // 3. Corridor Funding Tiers
            item {
                Text(
                    text = "CORRIDOR FUNDING TIERS",
                    color = ArcticFrost.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }

            items(tiers, key = { it.id }) { tier ->
                CorridorTierCard(
                    tier = tier,
                    isHighlighted = tiers[selectedCorridorIdx].id == tier.id,
                    onClick = {
                        val idx = tiers.indexOfFirst { it.id == tier.id }
                        if (idx >= 0) selectedCorridorIdx = idx
                    }
                )
            }

            // 4. Telemetry Reserve Note
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, FrostedRim.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Every 50 miles you log advances non-invasive trail cameras, GPS telemetry collars, and rancher coexistence initiatives led by accredited 501(c)(3) biologists.",
                            color = ArcticFrost.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 5. Primary Advocacy Action CTA (Opens Verified 501(c)(3) Donor Sheet)
            item {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDonorSheet = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlacialCyan,
                        contentColor = Color(0xFF0A0F1D)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("conservation_donate_cta_button")
                ) {
                    Icon(Icons.Filled.VolunteerActivism, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "DONATE TO WOLF CORRIDOR FUND",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.4.sp
                    )
                }
            }

            // 6. Verified 501(c)(3) Partner Organizations Directory
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ACCREDITED 501(C)(3) WOLF ORGANIZATIONS",
                        color = ArcticFrost.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = "Direct verified external portals supporting SSP breeding, wild telemetry, and rancher coexistence.",
                        color = AlpineMist,
                        fontSize = 11.sp
                    )
                }
            }

            items(AccreditedWolfNonProfits, key = { it.id }) { org ->
                AccreditedOrgCard(
                    org = org,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedOrgDetail = org
                    }
                )
            }
        }
    }

    if (showDonorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDonorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0A0F1D),
            contentColor = ArcticFrost
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("conservation_donor_sheet")
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "SUPPORT WILD WOLF RECOVERY",
                            color = GlacialCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.6.sp
                        )
                        Text(
                            text = "Tap any accredited 501(c)(3) non-profit to view impact tiers, sponsor a wolf, or open their official donation portal.",
                            color = AlpineMist,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                items(AccreditedWolfNonProfits, key = { "sheet_${it.id}" }) { org ->
                    AccreditedOrgCard(
                        org = org,
                        onClick = {
                            showDonorSheet = false
                            selectedOrgDetail = org
                        }
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    val activeOrg = selectedOrgDetail
    if (activeOrg != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedOrgDetail = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0A0F1D),
            contentColor = ArcticFrost
        ) {
            WolfOrgDetailSheetContent(
                org = activeOrg,
                onDonateClick = {
                    val orgName = activeOrg.name
                    selectedOrgDetail = null
                    openExternalPortal(activeOrg.donateUrl)
                    pendingDonorAwardOrg = orgName to DonorTier.GUARDIAN
                },
                onSponsorClick = {
                    val orgName = activeOrg.name
                    val url = activeOrg.sponsorWolfUrl
                    selectedOrgDetail = null
                    if (url != null) {
                        openExternalPortal(url)
                        pendingDonorAwardOrg = orgName to DonorTier.ALPHA_SPONSOR
                    }
                },
                onCopyLink = { copyPortalUrl(activeOrg.donateUrl) }
            )
        }
    }

    val awardPrompt = pendingDonorAwardOrg
    if (awardPrompt != null) {
        DonorBadgeAwardModal(
            organizationName = awardPrompt.first,
            onEquipBadge = {
                viewModel.awardPackProtectorBadge(awardPrompt.first, awardPrompt.second)
            },
            onDismiss = { pendingDonorAwardOrg = null }
        )
    }
}

@Composable
private fun WildWolfTelemetryMapCard(
    tiers: List<WolfCorridorTier>,
    selectedIndex: Int,
    onSelectCorridor: (Int) -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "wolf_telemetry_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "collar_pulse"
    )
    val active = tiers[selectedIndex]

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0D1726),
        border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.35f)),
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
                    Icon(Icons.Filled.Radar, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "WILD WOLF HABITAT TELEMETRY",
                        color = ArcticFrost,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.3.sp
                    )
                }
                Text(
                    text = active.regionLabel,
                    color = GlacialCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Styled Winter Corridor Telemetry Radar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF08111E), Color(0xFF102033))
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Topographic winter contour lines
                    for (r in 1..4) {
                        val y = h * (r / 5f)
                        val contour = Path().apply {
                            moveTo(0f, y)
                            cubicTo(w * 0.25f, y - 16f, w * 0.65f, y + 18f, w, y - 6f)
                        }
                        drawPath(contour, color = Color.White.copy(alpha = 0.06f), style = Stroke(width = 1.5f))
                    }

                    // Protected sanctuary dashed perimeter polygon
                    val borderPath = Path().apply {
                        moveTo(w * 0.12f, h * 0.25f)
                        lineTo(w * 0.85f, h * 0.18f)
                        lineTo(w * 0.90f, h * 0.78f)
                        lineTo(w * 0.16f, h * 0.84f)
                        close()
                    }
                    drawPath(
                        path = borderPath,
                        color = GlacialCyan.copy(alpha = 0.26f),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                        )
                    )

                    // Wolf migration corridor path connecting the 3 tiers
                    val p0 = Offset(w * 0.24f, h * 0.68f) // Bitterroot
                    val p1 = Offset(w * 0.52f, h * 0.44f) // Cascades
                    val p2 = Offset(w * 0.78f, h * 0.28f) // Yukon
                    val corridorPath = Path().apply {
                        moveTo(p0.x, p0.y)
                        quadraticTo(w * 0.36f, h * 0.38f, p1.x, p1.y)
                        quadraticTo(w * 0.64f, h * 0.50f, p2.x, p2.y)
                    }
                    drawPath(
                        path = corridorPath,
                        brush = Brush.horizontalGradient(listOf(GlacialCyan, SummitGold.copy(alpha = 0.8f))),
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val nodes = listOf(p0, p1, p2)
                    nodes.forEachIndexed { idx, pt ->
                        val isSelected = idx == selectedIndex
                        val nodeColor = when {
                            idx == 0 -> GlacialCyan
                            idx == 1 -> SummitGold
                            else -> AlpineMist
                        }
                        if (isSelected) {
                            drawCircle(
                                color = nodeColor.copy(alpha = 0.25f * pulse),
                                radius = 26f * (0.85f + 0.25f * pulse),
                                center = pt
                            )
                        }
                        drawCircle(color = nodeColor, radius = if (isSelected) 9f else 6f, center = pt)
                        drawCircle(color = Color(0xFF0A0F1D), radius = 3f, center = pt)
                    }
                }
            }

            // Corridor selector pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Bitterroot", "Cascades", "Yukon").forEachIndexed { idx, label ->
                    val activePill = idx == selectedIndex
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (activePill) GlacialCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, if (activePill) GlacialCyan else FrostedRim.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectCorridor(idx) }
                    ) {
                        Text(
                            text = label,
                            color = if (activePill) GlacialCyan else AlpineMist,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                        )
                    }
                }
            }

            Text(
                text = active.telemetrySummary,
                color = ArcticFrost.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CorridorTierCard(
    tier: WolfCorridorTier,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = if (isHighlighted) 0.06f else 0.03f),
        border = BorderStroke(
            width = if (isHighlighted) 1.4.dp else 1.dp,
            color = when {
                tier.isUnlocked -> GlacialCyan.copy(alpha = 0.55f)
                isHighlighted -> SummitGold.copy(alpha = 0.55f)
                else -> Color.White.copy(alpha = 0.08f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (tier.isUnlocked) Icons.Filled.CheckCircle else if (tier.progress <= 0.2f) Icons.Filled.Lock else Icons.Filled.Pets,
                        contentDescription = null,
                        tint = if (tier.isUnlocked) GlacialCyan else if (tier.progress > 0.2f) SummitGold else AlpineMist,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tier.title,
                        color = ArcticFrost,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = tier.acresLabel,
                    color = if (tier.isUnlocked) GlacialCyan else AlpineMist,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            LinearProgressIndicator(
                progress = { tier.progress },
                color = if (tier.isUnlocked) GlacialCyan else GlacialCyan.copy(alpha = 0.65f),
                trackColor = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Text(
                text = tier.statusText,
                color = if (tier.isUnlocked) GlacialCyan else AlpineMist,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp
            )
        }
    }
}

@Composable
private fun AccreditedOrgCard(
    org: AccreditedWolfOrg,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, FrostedRim.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("org_card_${org.id}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GlacialCyan.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = org.category.uppercase(Locale.US),
                        color = GlacialCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.0.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Inspect ${org.name}",
                    tint = AlpineMist,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = org.name,
                color = ArcticFrost,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AlpineMist, modifier = Modifier.size(14.dp))
                Text(
                    text = org.location,
                    color = AlpineMist,
                    fontSize = 11.sp
                )
            }

            Text(
                text = org.tagLine,
                color = ArcticFrost.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (org.sponsorWolfUrl != null) {
                        "TAP FOR BRIEF, SPONSOR A WOLF & DONATE"
                    } else {
                        "TAP FOR BRIEF & GIVING PORTAL"
                    },
                    color = GlacialCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp
                )
                if (org.sponsorWolfUrl != null) {
                    Icon(Icons.Filled.Pets, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun WolfOrgDetailSheetContent(
    org: AccreditedWolfOrg,
    onDonateClick: () -> Unit,
    onSponsorClick: () -> Unit,
    onCopyLink: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("wolf_org_detail_sheet_${org.id}")
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = GlacialCyan.copy(alpha = 0.14f)
        ) {
            Text(
                text = org.category.uppercase(Locale.US),
                color = GlacialCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = org.name,
                color = ArcticFrost,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AlpineMist, modifier = Modifier.size(15.dp))
                Text(text = org.location, color = AlpineMist, fontSize = 12.sp)
            }
        }

        Text(
            text = org.briefSummary,
            color = ArcticFrost.copy(alpha = 0.86f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Text(
            text = "CORE INITIATIVES & GIVING IMPACT",
            color = AlpineMist,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            org.impactHighlights.forEach { bullet ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = GlacialCyan,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(15.dp)
                    )
                    Text(
                        text = bullet,
                        color = ArcticFrost.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (org.sponsorWolfUrl != null) {
                OutlinedButton(
                    onClick = onSponsorClick,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.4.dp, GlacialCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("org_sponsor_wolf_button_${org.id}")
                ) {
                    Icon(Icons.Filled.Pets, contentDescription = null, tint = GlacialCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "SPONSOR WOLF",
                        color = GlacialCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onDonateClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlacialCyan,
                    contentColor = Color(0xFF0A0F1D)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("org_donate_button_${org.id}")
            ) {
                Icon(Icons.Filled.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "DONATE",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.0.sp
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = onCopyLink,
                modifier = Modifier.testTag("org_copy_link_button_${org.id}")
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = AlpineMist, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Copy Donation Portal Link",
                    color = AlpineMist,
                    fontSize = 11.sp
                )
            }
        }
    }
}
