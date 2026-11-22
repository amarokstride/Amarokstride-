package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.integrations.BleHeartRateUiState
import com.example.data.integrations.ExternalFitnessConnectors
import com.example.data.integrations.HealthConnectAvailability
import com.example.data.integrations.HealthConnectManager
import com.example.data.model.AccountRole
import com.example.data.model.ActivitySource
import com.example.data.model.DistanceUnit
import com.example.data.model.SeasonTheme
import com.example.data.model.SummitFormatters
import com.example.data.model.UiState
import com.example.data.model.WolfCoat
import com.example.ui.components.AlwaysOnDisplayLaunchCard
import com.example.ui.components.AlwaysOnWatchFaceDialog
import com.example.ui.components.AnimatedWolfAvatar
import com.example.ui.components.ContactDeveloperButton
import com.example.ui.components.ContactDeveloperDialog
import com.example.ui.components.DonationThankYouDialog
import com.example.ui.components.FuelDeveloperButton
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.components.ProtectWildPackButton
import com.example.ui.components.WatchCompanionPreviewRow
import com.example.ui.components.WolfAchievementCrest
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.AuroraMint
import com.example.ui.theme.BasaltObsidian
import com.example.ui.theme.CrimsonSurge
import com.example.ui.theme.FrostedRim
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.SummitGold
import com.example.ui.theme.TrailHikeAccent
import com.example.ui.theme.WolfFrostBlue
import com.example.ui.viewmodel.SummitViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SyncProfileScreen(
    viewModel: SummitViewModel,
    contentPadding: PaddingValues,
    onOpenTrailDesigner: () -> Unit,
    onSwitchLoginRole: (AccountRole) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val activitiesState by viewModel.activitiesState.collectAsStateWithLifecycle()
    val milestonesState by viewModel.milestonesState.collectAsStateWithLifecycle()
    val partyState by viewModel.tavernPartyState.collectAsStateWithLifecycle()
    val integrations by viewModel.integrationsState.collectAsStateWithLifecycle()
    val bleHeartRate by viewModel.bleHeartRateState.collectAsStateWithLifecycle()
    val soundboardState by viewModel.soundboardState.collectAsStateWithLifecycle()
    val rank by viewModel.adventurerRankState.collectAsStateWithLifecycle()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val activities = (activitiesState as? UiState.Success)?.data.orEmpty()
    val unlockedKeys = (milestonesState as? UiState.Success)?.data.orEmpty().map { it.badgeKey }.toSet()
    val unit = profile?.unitEnum() ?: DistanceUnit.MILES
    val wolfCoat = profile?.wolfCoatEnum() ?: WolfCoat.TIMBER_GREY
    val seasonTheme = profile?.seasonEnum() ?: SeasonTheme.AUTO
    val activeRole = profile?.roleEnum() ?: AccountRole.PRIMARY

    val totalMiles = activities.sumOf { it.distanceMiles }
    val totalRuns = activities.size
    val avgPaceSec = if (activities.isNotEmpty()) {
        val totalDist = totalMiles.coerceAtLeast(0.01)
        val totalDur = activities.sumOf { it.durationSeconds }
        (totalDur / totalDist).toLong().coerceIn(180L, 3600L)
    } else 0L

    var pendingImportSource by remember { mutableStateOf(ActivitySource.COROS) }
    var donationModalCause by remember { mutableStateOf<String?>(null) }
    var donationModalAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var usernameDraft by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "Wolf Strider") }
    var showContactDevDialog by remember { mutableStateOf(false) }
    var showAlwaysOnDisplay by remember { mutableStateOf(false) }
    var showHealthConnectPrivacyDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteConfirmationInput by remember { mutableStateOf("") }
    var quickJoinCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshIntegrationStatus()
    }

    val hcPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        viewModel.refreshIntegrationStatus()
        if (grantedPermissions.isNotEmpty()) {
            viewModel.syncFromHealthConnect()
        } else {
            viewModel.postMessage(
                "Google Health Connect permissions were not granted. You can enable them anytime in Health Connect settings."
            )
        }
    }

    val gpxFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importWatchGpxFile(context, uri, pendingImportSource)
        }
    }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            viewModel.startBleWatchScan()
        } else {
            viewModel.postMessage("Bluetooth permission is needed to stream live watch Heart Rate.")
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Striders Howl Profile Hero + Telemetry + Trophy Room + Join Pack with Code
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MidnightSlate,
                border = BorderStroke(1.4.dp, GlacialCyan.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1A2837), MidnightSlate, BasaltObsidian)
                            )
                        )
                        .padding(16.dp)
                ) {
                    // Profile Hero Row (Animated Wolf Avatar + Glowing Cyan Username + Pack Subtitle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AnimatedWolfAvatar(coat = wolfCoat, size = 76.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = profile?.displayName ?: "Wolf Strider",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GlacialCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(
                                    onClick = {
                                        usernameDraft = profile?.displayName ?: ""
                                        showEditUsernameDialog = true
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("edit_username_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Strider Username",
                                        tint = GlacialCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = partyState?.resolvedPackName ?: "Lv.${rank.level} · ${rank.packClass}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ArcticFrost.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "Slot: ${activeRole.label} · Lv.${rank.level}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlpineMist
                            )
                        }
                    }

                    // 3-Column Live Telemetry Strip (Total Miles · Runs · Avg Pace)
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileTelemetryStat(
                            icon = Icons.Filled.LocationOn,
                            value = SummitFormatters.formatDistanceShort(totalMiles, unit).substringBefore(" "),
                            label = if (unit == DistanceUnit.KILOMETERS) "Total km" else "Total Miles",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileTelemetryStat(
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            value = totalRuns.toString(),
                            label = "Quests",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileTelemetryStat(
                            icon = Icons.Filled.Timer,
                            value = if (avgPaceSec > 0L) SummitFormatters.formatPaceRaw(avgPaceSec, unit) else "--:--",
                            label = "Avg Pace",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Glowing Glacial Cyan Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, GlacialCyan, Color.Transparent)
                                )
                            )
                    )

                    // Trophy Room 3-Card Showcase
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Trophy Room",
                                tint = GlacialCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Trophy Room",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF8EF7FF)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val showcaseBadges = listOf(
                                Triple("marathon_26", "Summit Alpha\nMarathon", "marathon_26" in unlockedKeys),
                                Triple("fifty_km_ironhearth", "Timberline\n50K", "fifty_km_ironhearth" in unlockedKeys),
                                Triple("first_howl_activity", "Wilderness\nGuardian", unlockedKeys.isNotEmpty())
                            )
                            showcaseBadges.forEach { (badgeKey, label, unlocked) ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF131D28),
                                    border = BorderStroke(1.dp, if (unlocked) GlacialCyan.copy(alpha = 0.7f) else FrostedRim),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                                    ) {
                                        WolfAchievementCrest(
                                            badgeKey = badgeKey,
                                            unlocked = unlocked,
                                            size = 62.dp
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (unlocked) ArcticFrost else AlpineMist,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Glowing "Join Pack with Code" frosted module (matching mockup!)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF121E2A),
                        border = BorderStroke(1.8.dp, GlacialCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = GlacialCyan, spotColor = GlacialCyan)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = if (partyState != null) "Active Pack · ${partyState?.partyCode}" else "Join Pack with Code",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF8CF7FF)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = quickJoinCode,
                                    onValueChange = { quickJoinCode = it.uppercase(Locale.US) },
                                    placeholder = { Text("Enter Invite Code (e.g. WOLF-4821)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GlacialCyan,
                                        unfocusedBorderColor = FrostedRim,
                                        focusedContainerColor = BasaltObsidian,
                                        unfocusedContainerColor = BasaltObsidian
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("profile_join_pack_input")
                                )
                                IconButton(
                                    onClick = {
                                        if (quickJoinCode.trim().length >= 6) {
                                            viewModel.joinPackParty(quickJoinCode)
                                            quickJoinCode = ""
                                        }
                                    },
                                    enabled = quickJoinCode.trim().length >= 6,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (quickJoinCode.trim().length >= 6) GlacialCyan else FrostedRim)
                                        .testTag("profile_join_pack_submit")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Join Pack",
                                        tint = if (quickJoinCode.trim().length >= 6) BasaltObsidian else AlpineMist
                                    )
                                }
                            }
                        }
                    }

                    // Wolf Coat Color Selector (drives BOTH 2D AnimatedWolfAvatar AND 3D WolfMascot3DView!)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Wolf Companion Coat (${wolfCoat.displayName})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WolfCoat.entries.forEach { coat ->
                                val selected = coat == wolfCoat
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.updateWolfCoat(coat) }
                                        .padding(vertical = 4.dp)
                                        .testTag("coat_${coat.name.lowercase()}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(coat.bodyHex))
                                            .border(
                                                width = if (selected) 3.dp else 1.dp,
                                                color = if (selected) GlacialCyan else Color(coat.frostHex).copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = coat.displayName.substringBefore(" "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) GlacialCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Distance Units
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Distance Units", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DistanceUnit.entries.forEach { u ->
                                FilterChip(
                                    selected = unit == u,
                                    onClick = { viewModel.updateUnit(u) },
                                    label = { Text(if (u == DistanceUnit.MILES) "Miles (mi)" else "Kilometers (km)") }
                                )
                            }
                        }
                    }

                    // Dual Account Switcher ("My Login" <-> "Partner Login")
                    val otherRole = if (activeRole == AccountRole.PRIMARY) AccountRole.PARTNER else AccountRole.PRIMARY
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalButton(
                            onClick = { onSwitchLoginRole(otherRole) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("switch_login_role_button")
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (otherRole == AccountRole.PARTNER) "Partner Login" else "My Login")
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        // 2. Seasonal Tavern Weather Overlay Selector
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Seasonal Tavern Weather Overlay",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = seasonTheme.effectSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SeasonTheme.entries.forEach { season ->
                            FilterChip(
                                selected = seasonTheme == season,
                                onClick = { viewModel.updateSeasonTheme(season) },
                                label = { Text(season.label) }
                            )
                        }
                    }

                    // Opening Lone Wolf Howl Startup Audio Preference
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Opening Lone Wolf Howl",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Play atmospheric wolf howl & synced haptics on app launch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextButton(
                                onClick = viewModel::previewOpeningHowl,
                                modifier = Modifier.testTag("preview_opening_howl_button")
                            ) {
                                Text("Preview", color = GlacialCyan, fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = soundboardState.launchSoundEnabled,
                                onCheckedChange = viewModel::toggleLaunchHowlEnabled,
                                modifier = Modifier.testTag("launch_howl_toggle")
                            )
                        }
                    }
                }
            }
        }

        // 3. Connected Watches & Fitness Apps (Health Connect, COROS, Garmin, Strava, Pathbound, Google Maps, Music/Audiobooks)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Watches, Fitness Apps & Trail Audio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    WatchCompanionPreviewRow(
                        paceLabel = if (avgPaceSec > 0L) SummitFormatters.formatPace(avgPaceSec, unit) else "7:14 ${unit.paceSuffix}",
                        distanceLabel = SummitFormatters.formatDistanceShort(totalMiles, unit),
                        garminConnected = profile?.garminConnected == true,
                        corosConnected = profile?.corosConnected == true,
                        onTapGarmin = {
                            val uri = viewModel.externalConnectors.buildGarminAuthorizeUri()
                            if (uri != null) {
                                ExternalFitnessConnectors.openUrl(context, uri.toString())
                            } else {
                                pendingImportSource = ActivitySource.GARMIN
                                gpxFileLauncher.launch(arrayOf("*/*"))
                            }
                        },
                        onTapCoros = {
                            val oauthIntent = viewModel.corosManager.buildOAuthAuthorizeIntent()
                            if (oauthIntent != null) {
                                context.startActivity(oauthIntent)
                            } else {
                                pendingImportSource = ActivitySource.COROS
                                gpxFileLauncher.launch(arrayOf("*/*"))
                            }
                        }
                    )

                    AlwaysOnDisplayLaunchCard(
                        onClick = { showAlwaysOnDisplay = true }
                    )

                    DirectBleHeartRateWatchCard(
                        bleState = bleHeartRate,
                        onToggleScan = {
                            if (bleHeartRate.isScanning) {
                                viewModel.stopBleWatchScan()
                            } else {
                                val requiredPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_SCAN,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    )
                                } else {
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                                blePermissionLauncher.launch(requiredPerms)
                            }
                        },
                        onConnectWatch = { address, name ->
                            viewModel.connectBleWatch(address, name)
                        },
                        onDisconnect = {
                            viewModel.disconnectBleWatch()
                        }
                    )

                    // Google Health Connect (Dedicated Connection & Live Telemetry Card)
                    val hcSnapshot = integrations.healthConnectSnapshot
                    val hcConnected = integrations.healthConnectPermissionsGranted || hcSnapshot.grantedPermissionsCount > 0
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.2.dp,
                            if (hcConnected) AuroraMint.copy(alpha = 0.7f) else GlacialCyan.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_health_connect_card")
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hcConnected) AuroraMint.copy(alpha = 0.18f)
                                                else GlacialCyan.copy(alpha = 0.16f)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.HealthAndSafety,
                                            contentDescription = "Google Health Connect",
                                            tint = if (hcConnected) AuroraMint else GlacialCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Google Health Connect",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = when {
                                                integrations.healthConnectPermissionsGranted ->
                                                    "Connected (${hcSnapshot.grantedPermissionsCount}/${hcSnapshot.totalRequiredPermissions} permissions) · Syncs runs, hikes & watches"
                                                hcSnapshot.grantedPermissionsCount > 0 ->
                                                    "Partially connected (${hcSnapshot.grantedPermissionsCount}/${hcSnapshot.totalRequiredPermissions} permissions)"
                                                integrations.healthConnectAvailability == HealthConnectAvailability.AVAILABLE ->
                                                    "Ready · Connect to sync workouts with Garmin, COROS, Fitbit & Android"
                                                integrations.healthConnectAvailability == HealthConnectAvailability.UPDATE_REQUIRED ->
                                                    "Health Connect app update required from Google Play"
                                                else ->
                                                    "Not supported on this Android version (requires Android 8.0+)"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showHealthConnectPrivacyDialog = true },
                                    modifier = Modifier.testTag("health_connect_privacy_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Health Connect privacy info",
                                        tint = AlpineMist
                                    )
                                }
                            }

                            if (hcConnected) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("7-DAY DIST", style = MaterialTheme.typography.labelSmall, color = AlpineMist)
                                            Text(
                                                SummitFormatters.formatDistanceShort(hcSnapshot.weeklyDistanceMiles, unit),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AuroraMint
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("7-DAY CLIMB", style = MaterialTheme.typography.labelSmall, color = AlpineMist)
                                            Text(
                                                SummitFormatters.formatElevation(hcSnapshot.weeklyElevationFeet, unit),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GlacialCyan
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("SESSIONS", style = MaterialTheme.typography.labelSmall, color = AlpineMist)
                                            Text(
                                                "${hcSnapshot.recentSessionCount}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SummitGold
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        when {
                                            integrations.healthConnectAvailability == HealthConnectAvailability.UPDATE_REQUIRED -> {
                                                runCatching {
                                                    context.startActivity(viewModel.healthConnectManager.buildInstallOrUpdateIntent())
                                                }.onFailure {
                                                    ExternalFitnessConnectors.openUrl(
                                                        context,
                                                        "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
                                                    )
                                                }
                                            }
                                            integrations.healthConnectAvailability == HealthConnectAvailability.UNAVAILABLE -> {
                                                viewModel.postMessage("Google Health Connect is unavailable on this device.")
                                            }
                                            integrations.healthConnectPermissionsGranted -> {
                                                viewModel.syncFromHealthConnect()
                                            }
                                            else -> {
                                                hcPermissionLauncher.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                                            }
                                        }
                                    },
                                    enabled = !integrations.isSyncingHealthConnect,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("health_connect_primary_button")
                                ) {
                                    if (integrations.isSyncingHealthConnect) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Syncing...")
                                    } else {
                                        Icon(
                                            imageVector = if (integrations.healthConnectPermissionsGranted) Icons.Filled.Sync else Icons.Filled.HealthAndSafety,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            when {
                                                integrations.healthConnectAvailability == HealthConnectAvailability.UPDATE_REQUIRED -> "Install / Update"
                                                integrations.healthConnectPermissionsGranted -> "Sync Now"
                                                hcSnapshot.grantedPermissionsCount > 0 -> "Grant Remaining"
                                                else -> "Connect Health Connect"
                                            }
                                        )
                                    }
                                }

                                if (integrations.healthConnectAvailability == HealthConnectAvailability.AVAILABLE) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(viewModel.healthConnectManager.buildHealthConnectSettingsIntent())
                                            }.onFailure {
                                                viewModel.postMessage("Open Android Settings -> Health Connect to manage permissions.")
                                            }
                                        },
                                        modifier = Modifier.testTag("health_connect_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "Manage Health Connect permissions",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Auto-export completed GPS quests to Health Connect",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = profile?.healthConnectAutoExport == true,
                                    onCheckedChange = viewModel::toggleHealthConnectAutoExport,
                                    modifier = Modifier.testTag("health_connect_auto_export_switch")
                                )
                            }
                        }
                    }

                    // COROS Watch (Cloud Functions OAuth2 + GPX Import + Trail Designer Export)
                    IntegrationRowCard(
                        title = "COROS Watch",
                        subtitle = when {
                            integrations.hasCorosSession -> "Connected via secure Cloud Functions · Tap Sync to refresh & pull workouts"
                            profile?.corosConnected == true -> "Connected · Import GPX/TCX or send custom trails to watch"
                            integrations.corosOAuthConfigured -> "Connect via COROS OAuth (Cloud Functions) or import GPX/TCX"
                            else -> "Import COROS GPX/TCX files or sync via Google Health Connect"
                        },
                        actionLabel = when {
                            integrations.hasCorosSession -> "Sync Now"
                            integrations.corosOAuthConfigured -> "Connect API"
                            else -> "Import GPX"
                        },
                        isLoading = integrations.isSyncingCoros,
                        onAction = {
                            if (integrations.hasCorosSession) {
                                viewModel.syncConnectedCoros()
                            } else {
                                val oauthIntent = viewModel.corosManager.buildOAuthAuthorizeIntent()
                                if (oauthIntent != null) {
                                    context.startActivity(oauthIntent)
                                } else {
                                    pendingImportSource = ActivitySource.COROS
                                    gpxFileLauncher.launch(arrayOf("*/*"))
                                }
                            }
                        }
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                pendingImportSource = ActivitySource.COROS
                                gpxFileLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("COROS GPX")
                        }
                        FilledTonalButton(
                            onClick = onOpenTrailDesigner,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export Trail")
                        }
                    }

                    // Garmin Connect (Cloud Functions OAuth2 PKCE + Health Connect + GPX Import)
                    IntegrationRowCard(
                        title = "Garmin Connect",
                        subtitle = when {
                            integrations.hasGarminSession -> "Linked via secure Cloud Functions · Tap Refresh or sync via Health Connect"
                            profile?.garminConnected == true -> "Connected · Ready for Health Connect & GPX/TCX watch sync"
                            integrations.garminOAuthConfigured -> "Authorize Garmin OAuth2 (Cloud Functions) or import GPX/TCX"
                            else -> "Sync Garmin via Google Health Connect or import GPX/TCX exports"
                        },
                        actionLabel = when {
                            integrations.hasGarminSession -> "Refresh"
                            integrations.garminOAuthConfigured -> "Connect"
                            else -> "Import GPX"
                        },
                        isLoading = integrations.isSyncingGarmin,
                        onAction = {
                            if (integrations.hasGarminSession) {
                                viewModel.refreshConnectedGarmin()
                            } else {
                                val uri = viewModel.externalConnectors.buildGarminAuthorizeUri()
                                if (uri != null) {
                                    ExternalFitnessConnectors.openUrl(context, uri.toString())
                                } else {
                                    pendingImportSource = ActivitySource.GARMIN
                                    gpxFileLauncher.launch(arrayOf("*/*"))
                                }
                            }
                        }
                    )

                    // Strava (Cloud Functions OAuth2 + GPX Import)
                    IntegrationRowCard(
                        title = "Strava",
                        subtitle = when {
                            integrations.hasStravaSession -> "Connected via secure Cloud Functions · Tap Sync to refresh & pull activities"
                            profile?.stravaConnected == true -> "Connected · Syncs runs, hikes & rides"
                            integrations.stravaOAuthConfigured -> "Authorize Strava OAuth (Cloud Functions) or import Strava GPX"
                            else -> "Import Strava GPX files or sync via Google Health Connect"
                        },
                        actionLabel = when {
                            integrations.hasStravaSession -> "Sync Now"
                            integrations.stravaOAuthConfigured -> "Connect"
                            else -> "Import GPX"
                        },
                        isLoading = integrations.isSyncingStrava,
                        onAction = {
                            if (integrations.hasStravaSession) {
                                viewModel.syncConnectedStrava()
                            } else {
                                val uri = viewModel.externalConnectors.buildStravaAuthorizeUri()
                                if (uri != null) {
                                    ExternalFitnessConnectors.openUrl(context, uri.toString())
                                } else {
                                    pendingImportSource = ActivitySource.STRAVA
                                    gpxFileLauncher.launch(arrayOf("*/*"))
                                }
                            }
                        }
                    )

                    // Pathbound Active Bridge
                    IntegrationRowCard(
                        title = "Pathbound",
                        subtitle = if (profile?.pathboundConnected == true) "Linked · Share quest recaps with Pathbound" else "Connect or share active trail quests with Pathbound",
                        actionLabel = "Open Bridge",
                        isLoading = false,
                        onAction = {
                            viewModel.markPathboundConnected()
                            ExternalFitnessConnectors.openPathboundBridge(
                                context,
                                "Striders Howl Pack Sync · Lv.${rank.level} ${rank.packClass}"
                            )
                        }
                    )

                    // Trail Maps & Audio Apps
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ExternalFitnessConnectors.openGoogleMapsRouteSearch(
                                    context,
                                    "scenic running hiking equestrian trails"
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Google Maps Routes")
                        }
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
                    }

                    integrations.lastSyncSummary?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = TrailHikeAccent
                        )
                    }
                }
            }
        }

        // 4. Bespoke Donation Buttons (Protect the Wild Pack & Fuel the Developer -> Wilderness Guardian Modal!)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Support Wildlife & Indie Trails",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ProtectWildPackButton(
                    onClick = {
                        donationModalCause = "Wolf Conservation & Habitat Protection"
                        donationModalAction = { ExternalFitnessConnectors.openWolfConservationDonation(context) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donate_wolf_conservation_button")
                )
                FuelDeveloperButton(
                    onClick = {
                        donationModalCause = "Indie App Development & Future Trail Features"
                        donationModalAction = { ExternalFitnessConnectors.openDeveloperSupportDonation(context) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donate_developer_button")
                )
                ContactDeveloperButton(
                    onClick = { showContactDevDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 5. Privacy, Data Safety & Account Deletion (Google Play Compliance)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Policy,
                            contentDescription = "Privacy and Account",
                            tint = GlacialCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Privacy & Account Controls",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Review how Striders Howl handles GPS, Health Connect, and Firebase cloud data, or permanently delete your account and all stored records.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalButton(
                            onClick = { showPrivacyPolicyDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_privacy_policy_button")
                        ) {
                            Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Privacy Policy", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = {
                                deleteConfirmationInput = ""
                                showDeleteAccountDialog = true
                            },
                            enabled = !isDeletingAccount,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("delete_account_button")
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete Account", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // 6. Cloud & SQLite Database + Android OS Version Sync Status
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudDone, contentDescription = null, tint = WolfFrostBlue, modifier = Modifier.size(18.dp))
                        Text("System & Sync Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · Target SDK 36 Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = WolfFrostBlue
                    )
                    Text(
                        text = "Cloud Sync: Firebase Auth & Firestore · Local DB: Room SQLite (Gear, AI Plans, Route Analysis, Custom Trails)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Custom Strider Username Editor Dialog
    if (showEditUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("Create Your Strider Username", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your custom username appears live on the Solo & Pack Leaderboards and Tavern Voice Chat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = usernameDraft,
                        onValueChange = { usernameDraft = it.take(40) },
                        label = { Text("Strider Username") },
                        placeholder = { Text("e.g. FrostPacer, TimberWolf") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_dialog_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDisplayName(usernameDraft)
                        showEditUsernameDialog = false
                    },
                    enabled = usernameDraft.trim().length >= 2,
                    modifier = Modifier.testTag("username_dialog_save")
                ) {
                    Text("Save Username")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsernameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Always-On OLED Watchface Trail Display
    if (showAlwaysOnDisplay) {
        AlwaysOnWatchFaceDialog(
            todayMiles = totalMiles,
            onDismiss = { showAlwaysOnDisplay = false }
        )
    }

    // Contact Developer Modal
    if (showContactDevDialog) {
        ContactDeveloperDialog(
            onDismiss = { showContactDevDialog = false },
            onFeedbackSent = { /* handled via email chooser or clipboard feedback */ }
        )
    }

    // Google Health Connect Privacy Rationale Dialog
    if (showHealthConnectPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showHealthConnectPrivacyDialog = false },
            title = { Text("Google Health Connect Privacy", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Striders Howl requests read/write access to Exercise Sessions, Distance, Speed, and Elevation Gained in Google Health Connect so you can import workouts from Android watches (Garmin, COROS, Pixel Watch, Fitbit, Samsung Health) and optionally export completed GPS trails back to Health Connect. You can revoke permissions anytime in Android Health Connect settings."
                )
            },
            confirmButton = {
                Button(onClick = { showHealthConnectPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Full Privacy Policy & Data Safety Disclosure Dialog
    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    // Mandatory In-App Account & Data Deletion Confirmation Dialog
    if (showDeleteAccountDialog) {
        val isConfirmed = deleteConfirmationInput.trim().equals("DELETE", ignoreCase = true)
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) showDeleteAccountDialog = false
            },
            title = {
                Text(
                    text = "Delete Account & All Data?",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This permanently deletes your Firebase Auth account, cloud activities, challenges, milestones, leaderboard entry, local SQLite gear/trails/plans, cached COROS/Strava/Garmin sessions, and saved SOS contacts. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = deleteConfirmationInput,
                        onValueChange = { deleteConfirmationInput = it.take(12) },
                        label = { Text("Type DELETE to confirm") },
                        singleLine = true,
                        enabled = !isDeletingAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_confirm_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccountAndData(
                            onDeleted = {
                                showDeleteAccountDialog = false
                                onSignOut()
                            }
                        )
                    },
                    enabled = isConfirmed && !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("delete_account_confirm_button")
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text("Delete Forever")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isDeletingAccount
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Donation Thank-You "Wilderness Guardian" Modal (matching user's uploaded mockup!)
    val activeCause = donationModalCause
    if (activeCause != null) {
        DonationThankYouDialog(
            causeLabel = activeCause,
            onDismiss = {
                donationModalCause = null
                donationModalAction = null
            },
            onOpenSupportLink = {
                donationModalAction?.invoke()
            }
        )
    }
}

@Composable
private fun ProfileTelemetryStat(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = GlacialCyan,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = ArcticFrost
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AlpineMist
        )
    }
}

@Composable
private fun IntegrationRowCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    isLoading: Boolean,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(Icons.Filled.Watch, contentDescription = null, tint = WolfFrostBlue, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(
                onClick = onAction,
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun DirectBleHeartRateWatchCard(
    bleState: BleHeartRateUiState,
    onToggleScan: () -> Unit,
    onConnectWatch: (address: String, name: String) -> Unit,
    onDisconnect: () -> Unit
) {
    val isConnected = bleState.connectedAddress != null
    val liveBpm = bleState.liveHeartRateBpm

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF111B26),
        border = BorderStroke(
            width = if (isConnected) 1.5.dp else 1.dp,
            color = if (isConnected) GlacialCyan else FrostedRim
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) GlacialCyan.copy(alpha = 0.16f) else MidnightSlate
                        )
                        .border(
                            1.dp,
                            if (isConnected) GlacialCyan else FrostedRim,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = when {
                            isConnected -> Icons.Filled.BluetoothConnected
                            bleState.isScanning -> Icons.Filled.BluetoothSearching
                            else -> Icons.Filled.Bluetooth
                        },
                        contentDescription = "Direct Watch Bluetooth Heart Rate",
                        tint = if (isConnected || bleState.isScanning) GlacialCyan else AlpineMist,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Direct Watch BLE Heart Rate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = ArcticFrost
                    )
                    Text(
                        text = bleState.statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = AlpineMist
                    )
                }

                if (liveBpm != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CrimsonSurge.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CrimsonSurge.copy(alpha = 0.7f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Live Heart Rate",
                                tint = CrimsonSurge,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$liveBpm BPM",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = ArcticFrost
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onToggleScan,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ble_watch_scan_button")
                ) {
                    if (bleState.isScanning) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = GlacialCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Stop Scan")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Scan Garmin / COROS HR")
                    }
                }

                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("ble_watch_disconnect_button")
                    ) {
                        Text("Disconnect")
                    }
                }
            }

            if (bleState.discoveredWatches.isNotEmpty() && !isConnected) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Detected Heart Rate Broadcasts (0x180D)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GlacialCyan
                    )
                    bleState.discoveredWatches.forEach { watch ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MidnightSlate,
                            border = BorderStroke(1.dp, FrostedRim),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onConnectWatch(watch.address, watch.name) }
                                .testTag("ble_watch_device_${watch.address}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = watch.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ArcticFrost
                                    )
                                    Text(
                                        text = "${watch.address} · Signal ${watch.rssi} dBm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AlpineMist
                                    )
                                }
                                Text(
                                    text = "Connect",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GlacialCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

