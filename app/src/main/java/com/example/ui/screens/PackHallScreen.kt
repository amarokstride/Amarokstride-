package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DistanceUnit
import com.example.data.model.PackCrest
import com.example.data.model.PackRankingEntry
import com.example.data.model.SoloRankingEntry
import com.example.data.model.SummitFormatters
import com.example.data.model.TavernChatMessage
import com.example.data.model.TavernParty
import com.example.data.model.UiState
import com.example.ui.components.AnimatedWolfAvatar
import com.example.ui.components.CreatePackModalDialog
import com.example.ui.components.CreatePackSocialCard
import com.example.ui.components.JoinPackModalDialog
import com.example.ui.components.JoinPackSocialCard
import com.example.ui.theme.AuroraMint
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.SummitGold
import com.example.ui.theme.WolfFrostBlue
import com.example.ui.viewmodel.SummitViewModel
import java.util.Locale

@Composable
fun PackHallScreen(
    viewModel: SummitViewModel,
    contentPadding: PaddingValues
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val soloRankings by viewModel.soloRankingsState.collectAsStateWithLifecycle()
    val packRankings by viewModel.packRankingsState.collectAsStateWithLifecycle()
    val publicChat by viewModel.publicChatState.collectAsStateWithLifecycle()
    val packChat by viewModel.packChatState.collectAsStateWithLifecycle()
    val party by viewModel.tavernPartyState.collectAsStateWithLifecycle()
    val voiceAutoRead by viewModel.voiceChannelAutoReadEnabled.collectAsStateWithLifecycle()
    val isListeningVoice by viewModel.isListeningVoice.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val unit = profile?.unitEnum() ?: DistanceUnit.MILES

    var selectedTab by remember { mutableIntStateOf(0) }
    var rankingScope by remember { mutableIntStateOf(0) } // 0 = Solo Striders, 1 = Wolf Packs
    var chatScope by remember { mutableIntStateOf(0) }    // 0 = Public Hall, 1 = Private Pack Chat

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Leaderboard") },
                icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("pack_tab_leaderboard")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Chat & Voice") },
                icon = { Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("pack_tab_chat")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("My Pack") },
                icon = { Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("pack_tab_party")
            )
        }

        when (selectedTab) {
            0 -> LeaderboardTabContent(
                soloRankings = soloRankings,
                packRankings = packRankings,
                currentUserId = viewModel.currentUserId,
                currentPartyCode = party?.partyCode,
                unit = unit,
                rankingScope = rankingScope,
                onScopeChange = { rankingScope = it }
            )
            1 -> TavernVoiceChatTabContent(
                publicMessages = publicChat,
                packMessages = packChat,
                hasPack = party != null,
                packCode = party?.partyCode,
                currentUserId = viewModel.currentUserId,
                chatScope = chatScope,
                onChatScopeChange = { chatScope = it },
                voiceAutoRead = voiceAutoRead,
                onToggleVoiceAutoRead = viewModel::toggleVoiceChannelAutoRead,
                isListeningVoice = isListeningVoice,
                liveTranscript = liveTranscript,
                onSendPublic = viewModel::sendPublicChat,
                onSendPack = viewModel::sendPackChat,
                onStartVoiceDictation = viewModel::startVoiceDictation,
                onStopVoiceDictation = viewModel::stopVoiceDictation,
                onSpeakMessage = viewModel::speakChatMessage
            )
            else -> PartnerPackTabContent(
                party = party,
                rankedPacks = packRankings,
                unit = unit,
                onCreatePack = viewModel::createPackParty,
                onRenamePack = viewModel::renamePackParty,
                onJoinPack = viewModel::joinPackParty,
                onSendCheer = viewModel::sendPartyCheer
            )
        }
    }
}

@Composable
private fun LeaderboardTabContent(
    soloRankings: List<SoloRankingEntry>,
    packRankings: List<PackRankingEntry>,
    currentUserId: String,
    currentPartyCode: String?,
    unit: DistanceUnit,
    rankingScope: Int,
    onScopeChange: (Int) -> Unit
) {
    val communityMiles = soloRankings.sumOf { it.totalMiles }
    val wildlifeGoalMiles = 50000.0
    val wildlifeProgress = (communityMiles / wildlifeGoalMiles).toFloat().coerceIn(0.06f, 1f)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Pack Miles for Wildlife frosted glowing progress card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF13222E),
                border = BorderStroke(1.2.dp, Color(0xFF3A7B91)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF162C3C), Color(0xFF101D28))
                            )
                        )
                        .padding(14.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_striders_howl_logo),
                        contentDescription = "Pack Miles for Wildlife Wolf",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(2.dp, WolfFrostBlue, CircleShape)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "Pack Miles for Wildlife: %,.1f / 50,000 %s",
                                communityMiles,
                                unit.shortLabel
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD6F4FF)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0A141D))
                                .border(1.dp, Color(0xFF2E6375), RoundedCornerShape(10.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(wildlifeProgress)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF4BE6F8), Color(0xFFA8FAFF))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Header + Live Solo vs Pack switcher
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "LEADERBOARD",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Updates live as distances are logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = rankingScope == 0,
                        onClick = { onScopeChange(0) },
                        label = { Text("Solo") },
                        modifier = Modifier.testTag("ranking_solo_chip")
                    )
                    FilterChip(
                        selected = rankingScope == 1,
                        onClick = { onScopeChange(1) },
                        label = { Text("Packs") },
                        modifier = Modifier.testTag("ranking_pack_chip")
                    )
                }
            }
        }

        if (rankingScope == 0) {
            if (soloRankings.isEmpty()) {
                item {
                    EmptyLeaderboardCard(
                        title = "Claim the #1 Alpha Spot!",
                        subtitle = "Log or track your first run, hike, or equestrian ride to post your live mileage to the Solo Striders board."
                    )
                }
            } else {
                itemsIndexed(soloRankings, key = { _, e -> e.userId.ifBlank { e.id } }) { idx, entry ->
                    val rankNum = idx + 1
                    val isMe = entry.userId == currentUserId
                    LeaderboardRowCard(
                        rankNum = rankNum,
                        isHighlightedUser = isMe,
                        title = if (isMe) "${entry.displayName} (YOU)" else entry.displayName,
                        subtitle = "Lv.${entry.level} · ${entry.packClass}",
                        distanceText = SummitFormatters.formatDistanceShort(entry.totalMiles, unit),
                        coat = entry.coatEnum()
                    )
                }
            }
        } else {
            if (packRankings.isEmpty()) {
                item {
                    EmptyLeaderboardCard(
                        title = "No Wolf Packs Ranked Yet",
                        subtitle = "Open the My Pack tab to create a Wolf Pack invite code and climb the live Pack Leaderboard with your partner!"
                    )
                }
            } else {
                itemsIndexed(packRankings, key = { _, e -> e.partyCode.ifBlank { e.id } }) { idx, entry ->
                    val rankNum = idx + 1
                    val isMyPack = !currentPartyCode.isNullOrBlank() && entry.partyCode == currentPartyCode
                    LeaderboardRowCard(
                        rankNum = rankNum,
                        isHighlightedUser = isMyPack,
                        title = if (isMyPack) "${entry.packName} (YOUR PACK)" else entry.packName,
                        subtitle = "${entry.memberCount} Wolf Strider(s) · Code ${entry.partyCode}",
                        distanceText = SummitFormatters.formatDistanceShort(entry.combinedMiles, unit),
                        coat = entry.coatEnum()
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun LeaderboardRowCard(
    rankNum: Int,
    isHighlightedUser: Boolean,
    title: String,
    subtitle: String,
    distanceText: String,
    coat: com.example.data.model.WolfCoat
) {
    val bgBrush = when {
        isHighlightedUser -> Brush.horizontalGradient(
            listOf(Color(0xFF42E6FA), Color(0xFF9DF8FF))
        )
        rankNum == 1 -> Brush.horizontalGradient(
            listOf(Color(0xFF1A384B), Color(0xFF152938))
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF16222E), Color(0xFF121C26))
        )
    }
    val borderColor = when {
        isHighlightedUser -> Color(0xFFBDFBFF)
        rankNum == 1 -> WolfFrostBlue
        else -> Color(0xFF2A4152)
    }
    val primaryText = if (isHighlightedUser) Color(0xFF061B26) else Color(0xFFF0F7FC)
    val secondaryText = if (isHighlightedUser) Color(0xFF103447) else Color(0xFF9CB4C6)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(if (rankNum == 1 || isHighlightedUser) 1.6.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(bgBrush)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                text = "#$rankNum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isHighlightedUser) Color(0xFF061B26) else if (rankNum == 1) WolfFrostBlue else Color(0xFFA7BCCB),
                modifier = Modifier.width(34.dp)
            )
            AnimatedWolfAvatar(
                coat = coat,
                size = 42.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (rankNum == 1) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = "Alpha Leader Crown",
                            tint = if (isHighlightedUser) Color(0xFF061B26) else SummitGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = distanceText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = primaryText
            )
        }
    }
}

@Composable
private fun EmptyLeaderboardCard(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = WolfFrostBlue,
                modifier = Modifier.size(36.dp)
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TavernVoiceChatTabContent(
    publicMessages: List<TavernChatMessage>,
    packMessages: List<TavernChatMessage>,
    hasPack: Boolean,
    packCode: String?,
    currentUserId: String,
    chatScope: Int,
    onChatScopeChange: (Int) -> Unit,
    voiceAutoRead: Boolean,
    onToggleVoiceAutoRead: (Boolean) -> Unit,
    isListeningVoice: Boolean,
    liveTranscript: String,
    onSendPublic: (String) -> Unit,
    onSendPack: (String) -> Unit,
    onStartVoiceDictation: ((String) -> Unit) -> Unit,
    onStopVoiceDictation: () -> Unit,
    onSpeakMessage: (TavernChatMessage) -> Unit
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }

    val sendToActiveChannel: (String) -> Unit = { text ->
        if (chatScope == 0) onSendPublic(text) else onSendPack(text)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoiceDictation { spoken ->
                sendToActiveChannel("🎙️ $spoken")
            }
        }
    }

    val activeMessages = if (chatScope == 0) publicMessages else packMessages

    Column(modifier = Modifier.fillMaxSize()) {
        // Channel selector + Hands-Free Voice Channel toggle
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = chatScope == 0,
                        onClick = { onChatScopeChange(0) },
                        label = { Text("Public Tavern Hall") },
                        modifier = Modifier.testTag("chat_public_chip")
                    )
                    FilterChip(
                        selected = chatScope == 1,
                        onClick = { onChatScopeChange(1) },
                        label = { Text(if (hasPack) "Pack (${packCode ?: ""})" else "Pack Chat") },
                        modifier = Modifier.testTag("chat_pack_chip")
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = "Voice Channel",
                            tint = if (voiceAutoRead) WolfFrostBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (voiceAutoRead) "Voice Channel Live (Auto-Read ON)" else "Hands-Free Voice Channel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = voiceAutoRead,
                        onCheckedChange = onToggleVoiceAutoRead,
                        modifier = Modifier.testTag("voice_channel_switch")
                    )
                }
            }
        }

        // Message list
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (chatScope == 1 && !hasPack) {
                item {
                    EmptyLeaderboardCard(
                        title = "Join or Create a Wolf Pack",
                        subtitle = "Open the My Pack tab to link with your partner and unlock private Pack text & voice chat."
                    )
                }
            } else if (activeMessages.isEmpty()) {
                item {
                    EmptyLeaderboardCard(
                        title = if (chatScope == 0) "The Tavern Hall Awaits" else "Private Pack Channel Ready",
                        subtitle = "Type a message below or tap the Voice Chat mic to broadcast your howl to the pack!"
                    )
                }
            } else {
                items(activeMessages, key = { it.id.ifBlank { "${it.sentAtEpochMs}_${it.senderUid}" } }) { msg ->
                    val isMine = msg.senderUid == currentUserId
                    ChatBubbleCard(
                        message = msg,
                        isMine = isMine,
                        onPlayAudio = { onSpeakMessage(msg) }
                    )
                }
            }
        }

        // Live voice listening indicator
        if (isListeningVoice) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = WolfFrostBlue)
                    Text(
                        text = liveTranscript.ifBlank { "Listening to your Pack voice message..." },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text("LIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = WolfFrostBlue)
                }
            }
        }

        // Composer + Push-to-Talk Voice Chat Mic
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = {
                        if (isListeningVoice) {
                            onStopVoiceDictation()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onStartVoiceDictation { spoken ->
                                    sendToActiveChannel("🎙️ $spoken")
                                }
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isListeningVoice) Color(0xFFD94A38) else MaterialTheme.colorScheme.primaryContainer)
                        .testTag("voice_chat_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListeningVoice) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isListeningVoice) "Stop voice chat" else "Speak voice chat message",
                        tint = if (isListeningVoice) Color.White else WolfFrostBlue
                    )
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = {
                        Text(
                            if (chatScope == 0) "Message Public Tavern Hall..."
                            else "Message your Wolf Pack..."
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            sendToActiveChannel(draft)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank() && (chatScope == 0 || hasPack),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send chat message",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleCard(
    message: TavernChatMessage,
    isMine: Boolean,
    onPlayAudio: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedWolfAvatar(coat = message.coatEnum(), size = 38.dp)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isMine) Color(0xFF17384A) else MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                1.dp,
                if (isMine) WolfFrostBlue.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.weight(1f)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isMine) WolfFrostBlue else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = message.senderPackClass,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen to message",
                            tint = WolfFrostBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PartnerPackTabContent(
    party: TavernParty?,
    rankedPacks: List<PackRankingEntry>,
    unit: DistanceUnit,
    onCreatePack: (String, PackCrest, Double, Boolean) -> Unit,
    onRenamePack: (String) -> Unit,
    onJoinPack: (String) -> Unit,
    onSendCheer: () -> Unit
) {
    var showCreatePackModal by remember { mutableStateOf(false) }
    var showJoinPackModal by remember { mutableStateOf(false) }
    var showRenamePackDialog by remember { mutableStateOf(false) }
    var packNameDraft by remember(party?.resolvedPackName) { mutableStateOf(party?.resolvedPackName ?: "") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (party != null) {
            item {
                val goalMiles = party.weeklyGoalMiles.coerceAtLeast(10.0)
                val goalProgress = (party.combinedMiles / goalMiles).toFloat().coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.5.dp, GlacialCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (party.isPublic) "PUBLIC WOLF PACK · ${party.crestEnum().displayName.uppercase()}" else "INVITE-ONLY PACK · ${party.crestEnum().displayName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlacialCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = party.resolvedPackName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    IconButton(
                                        onClick = {
                                            packNameDraft = party.resolvedPackName
                                            showRenamePackDialog = true
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("rename_pack_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Rename Wolf Pack",
                                            tint = GlacialCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Invite Code: ${party.partyCode}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AuroraMint,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${party.cheerCount} Howls",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GlacialCyan,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Collective Weekly Distance Goal Progress
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Collective Pack Goal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${SummitFormatters.formatDistanceShort(party.combinedMiles, unit)} / ${SummitFormatters.formatDistanceShort(goalMiles, unit)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SummitGold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { goalProgress },
                                color = GlacialCyan,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        // Primary Member
                        PackMemberSummaryRow(
                            roleLabel = "Pack Lead",
                            name = party.primaryName,
                            milesText = SummitFormatters.formatDistanceShort(party.primaryTotalMiles, unit),
                            lastQuest = party.primaryLastQuest
                        )

                        // Partner Member
                        if (party.isPartnerJoined) {
                            PackMemberSummaryRow(
                                roleLabel = "Pack Partner",
                                name = party.partnerName ?: "Partner Strider",
                                milesText = SummitFormatters.formatDistanceShort(party.partnerTotalMiles, unit),
                                lastQuest = party.partnerLastQuest ?: "Ready on the trail"
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Share invite code ${party.partyCode} with your partner so they can sign in with Partner Login and link their miles to ${party.resolvedPackName}!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onSendCheer,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlacialCyan,
                                contentColor = Color(0xFF041D26)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pack_send_cheer_button")
                        ) {
                            Icon(Icons.Filled.Pets, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send Rallying Pack Howl", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "STRIDERS HOWL · PACK HQ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = GlacialCyan
                    )
                    Text(
                        text = "Form a custom-named Wolf Pack with your own crest and weekly mileage goal, or join an existing pack to compete live on the Pack Leaderboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CreatePackSocialCard(
                        onClick = { showCreatePackModal = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    JoinPackSocialCard(
                        onClick = { showJoinPackModal = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showCreatePackModal) {
        CreatePackModalDialog(
            unit = unit,
            onDismiss = { showCreatePackModal = false },
            onLaunchPack = { name, crest, weeklyGoal, isPublic ->
                onCreatePack(name, crest, weeklyGoal, isPublic)
            }
        )
    }

    if (showJoinPackModal) {
        JoinPackModalDialog(
            rankedPacks = rankedPacks,
            unit = unit,
            onDismiss = { showJoinPackModal = false },
            onJoinPackCode = onJoinPack
        )
    }

    if (showRenamePackDialog && party != null) {
        AlertDialog(
            onDismissRequest = { showRenamePackDialog = false },
            title = { Text("Rename Your Wolf Pack", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = packNameDraft,
                    onValueChange = { packNameDraft = it.take(60) },
                    label = { Text("Pack Name") },
                    placeholder = { Text("Timberline Striders") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_pack_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenamePack(packNameDraft)
                        showRenamePackDialog = false
                    },
                    enabled = packNameDraft.trim().length >= 2,
                    modifier = Modifier.testTag("rename_pack_save")
                ) {
                    Text("Save Name")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenamePackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PackMemberSummaryRow(
    roleLabel: String,
    name: String,
    milesText: String,
    lastQuest: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(roleLabel, style = MaterialTheme.typography.labelSmall, color = WolfFrostBlue)
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Latest: $lastQuest",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = milesText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = WolfFrostBlue
            )
        }
    }
}
