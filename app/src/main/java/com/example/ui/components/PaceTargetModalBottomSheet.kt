package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DistanceUnit
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.BasaltObsidian
import com.example.ui.theme.FrostedRim
import com.example.ui.theme.GlacialCyan
import kotlinx.coroutines.launch
import java.util.Locale

private data class PacePreset(val title: String, val min: Int, val sec: Int)

private val PackPacePresets = listOf(
    PacePreset("Pack Cruise", 8, 30),
    PacePreset("Timberline", 7, 15),
    PacePreset("Alpha Sprint", 6, 0)
)

/**
 * Striders Howl Midnight-Frost Target Pace Selector Modal Bottom Sheet.
 * Features dual snapping wheel pickers (4-14 min & :00-:55 sec), one-tap pack presets,
 * audio cues toggle, and a Glacial Cyan lock button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaceTargetModalBottomSheet(
    initialMinutes: Int,
    initialSeconds: Int,
    initialAudioEnabled: Boolean,
    unit: DistanceUnit,
    onDismiss: () -> Unit,
    onPaceLocked: (minutes: Int, seconds: Int, audioEnabled: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val minutesList = remember { (4..14).toList() }
    val secondsList = remember { listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55) }

    val startMinIdx = remember(initialMinutes) {
        minutesList.indexOf(initialMinutes.coerceIn(4, 14)).coerceAtLeast(0)
    }
    val roundedStartSec = remember(initialSeconds) {
        ((initialSeconds + 2) / 5 * 5).coerceIn(0, 55)
    }
    val startSecIdx = remember(roundedStartSec) {
        secondsList.indexOf(roundedStartSec).coerceAtLeast(0)
    }

    var selectedMinutes by remember { mutableIntStateOf(minutesList[startMinIdx]) }
    var selectedSeconds by remember { mutableIntStateOf(secondsList[startSecIdx]) }
    var audioCuesEnabled by remember { mutableStateOf(initialAudioEnabled) }

    val minListState = rememberLazyListState(initialFirstVisibleItemIndex = startMinIdx)
    val secListState = rememberLazyListState(initialFirstVisibleItemIndex = startSecIdx)

    val centeredMinIdx by remember {
        derivedStateOf {
            val layout = minListState.layoutInfo
            val centerOffset = layout.viewportStartOffset + layout.viewportSize.height / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - centerOffset)
            }?.index ?: startMinIdx
        }
    }
    val centeredSecIdx by remember {
        derivedStateOf {
            val layout = secListState.layoutInfo
            val centerOffset = layout.viewportStartOffset + layout.viewportSize.height / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - centerOffset)
            }?.index ?: startSecIdx
        }
    }

    LaunchedEffect(centeredMinIdx) {
        val next = minutesList.getOrNull(centeredMinIdx) ?: return@LaunchedEffect
        if (next != selectedMinutes) {
            selectedMinutes = next
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(centeredSecIdx) {
        val next = secondsList.getOrNull(centeredSecIdx) ?: return@LaunchedEffect
        if (next != selectedSeconds) {
            selectedSeconds = next
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun applyPreset(min: Int, sec: Int) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selectedMinutes = min
        selectedSeconds = sec
        val mIdx = minutesList.indexOf(min)
        val sIdx = secondsList.indexOf(sec)
        scope.launch {
            if (mIdx >= 0) minListState.animateScrollToItem(mIdx)
        }
        scope.launch {
            if (sIdx >= 0) secListState.animateScrollToItem(sIdx)
        }
    }

    val unitSuffixUpper = unit.paceSuffix.uppercase(Locale.US)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0F1D),
        contentColor = ArcticFrost,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ArcticFrost.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
                .testTag("pace_target_modal_sheet")
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Target Pack Pace",
                    tint = GlacialCyan,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "SET TARGET PACK PACE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.8.sp,
                    color = ArcticFrost
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tune audio coaching & 12s pack speed triggers",
                style = MaterialTheme.typography.labelSmall,
                color = AlpineMist
            )
            Spacer(Modifier.height(18.dp))

            // Dual Snapping Wheel Pickers with Glacial Cyan Highlight Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, FrostedRim, RoundedCornerShape(18.dp))
            ) {
                // Center Selection Highlight Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlacialCyan.copy(alpha = 0.14f))
                        .border(1.2.dp, GlacialCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Minutes Wheel
                    LazyColumn(
                        state = minListState,
                        flingBehavior = rememberSnapFlingBehavior(lazyListState = minListState),
                        contentPadding = PaddingValues(vertical = 54.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(156.dp)
                            .testTag("pace_wheel_minutes")
                    ) {
                        itemsIndexed(minutesList) { idx, minVal ->
                            val isSelected = idx == centeredMinIdx
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        scope.launch { minListState.animateScrollToItem(idx) }
                                    }
                            ) {
                                Text(
                                    text = "$minVal min",
                                    fontSize = if (isSelected) 21.sp else 16.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) GlacialCyan else AlpineMist.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }

                    Text(
                        text = ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = GlacialCyan,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Seconds Wheel
                    LazyColumn(
                        state = secListState,
                        flingBehavior = rememberSnapFlingBehavior(lazyListState = secListState),
                        contentPadding = PaddingValues(vertical = 54.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(156.dp)
                            .testTag("pace_wheel_seconds")
                    ) {
                        itemsIndexed(secondsList) { idx, secVal ->
                            val isSelected = idx == centeredSecIdx
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        scope.launch { secListState.animateScrollToItem(idx) }
                                    }
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%02d sec %s", secVal, unit.paceSuffix),
                                    fontSize = if (isSelected) 19.sp else 15.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) GlacialCyan else AlpineMist.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick Pack Presets Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PackPacePresets.forEach { preset ->
                    val isSelected = selectedMinutes == preset.min && selectedSeconds == preset.sec
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) GlacialCyan.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) GlacialCyan else FrostedRim
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { applyPreset(preset.min, preset.sec) }
                            .testTag("pace_preset_${preset.title.lowercase(Locale.US).replace(" ", "_")}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) GlacialCyan else ArcticFrost,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                text = String.format(Locale.US, "%d:%02d", preset.min, preset.sec),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) ArcticFrost else AlpineMist
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Audio Cues Toggle Row
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, FrostedRim.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Pace Audio Cues",
                            tint = GlacialCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Pace Audio Cues & Wolf Howls",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ArcticFrost
                        )
                    }
                    Switch(
                        checked = audioCuesEnabled,
                        onCheckedChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            audioCuesEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BasaltObsidian,
                            checkedTrackColor = GlacialCyan
                        ),
                        modifier = Modifier.testTag("pace_modal_audio_switch")
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Lock Pace Primary CTA
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPaceLocked(selectedMinutes, selectedSeconds, audioCuesEnabled)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlacialCyan,
                    contentColor = Color(0xFF0A0F1D)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("lock_target_pace_button")
            ) {
                Text(
                    text = String.format(
                        Locale.US,
                        "LOCK PACE (%d:%02d %s)",
                        selectedMinutes,
                        selectedSeconds,
                        unitSuffixUpper
                    ),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.4.sp
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
