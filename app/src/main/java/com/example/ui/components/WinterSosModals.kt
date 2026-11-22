package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.GeoPointEle
import com.example.data.model.SummitFormatters
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.CrimsonSurge
import com.example.ui.theme.GlacialCyan
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Full-screen 45-second high-alert cancelable SOS Countdown Modal triggered by hard fall
 * detection or manual "Howl for Help" press.
 */
@Composable
fun SosCountdownDialog(
    onTriggerSos: () -> Unit,
    onCancelSos: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var secondsLeft by remember { mutableIntStateOf(45) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(1000L)
            secondsLeft -= 1
        }
        onTriggerSos()
    }

    Dialog(
        onDismissRequest = onCancelSos,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A050A))
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .testTag("sos_countdown_modal")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Emergency Fall Alert",
                        tint = CrimsonSurge,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "HARD FALL DETECTED",
                        color = ArcticFrost,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.0.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Activating acoustic SAR wolf howl beacon & emergency GPS dispatch in:",
                        color = ArcticFrost.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { (secondsLeft / 45f).coerceIn(0f, 1f) },
                        color = CrimsonSurge,
                        trackColor = Color.White.copy(alpha = 0.14f),
                        strokeWidth = 10.dp,
                        modifier = Modifier.size(184.dp)
                    )
                    Text(
                        text = "$secondsLeft",
                        color = ArcticFrost,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCancelSos,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlacialCyan,
                            contentColor = Color(0xFF0A0F1D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("sos_cancel_button")
                    ) {
                        Text(
                            text = "I'M OK — CANCEL SOS",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 1.0.sp
                        )
                    }

                    TextButton(
                        onClick = onTriggerSos,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sos_send_now_button")
                    ) {
                        Text(
                            text = "SEND SOS NOW",
                            color = CrimsonSurge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.0.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Active Winter Trail SOS Beacon screen modeled directly after `striders_howl_sos_beacon_screen.png`:
 * - Pure OLED dark winter backdrop with pulsing crimson & cyan dual-spectrum rescue rings
 * - Top "ACTIVE EMERGENCY BEACON: TRANSMITTING" crimson banner
 * - Striders Howl wolf crest | Winter Trail SOS: Howl for Help header
 * - Live GPS coordinates card (N/S, E/W in cyan) + Altitude, Battery Level, live Time Active
 * - Center dual-ring Acoustic Distress Howl Beacon (100% Volume) with animated waveform
 * - Mute Howl / Keep GPS Active toggle pill
 * - Emergency Contacts Notified SMS/Satellite action bar
 * - Slide-to-Cancel Emergency tactile drag slider to prevent accidental cancellations
 */
@Composable
fun ActiveSosBeaconDialog(
    lastLocation: GeoPointEle?,
    batteryPercent: Int,
    sosStartedAtMs: Long,
    acousticBeaconActive: Boolean,
    emergencyContacts: String,
    onToggleAcousticBeacon: () -> Unit,
    onDispatchSms: () -> Unit,
    onEndSos: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    DisposableEffect(context) {
        val window = context.findActivityWindow()
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var activeSeconds by remember(sosStartedAtMs) {
        mutableLongStateOf(((System.currentTimeMillis() - sosStartedAtMs) / 1000L).coerceAtLeast(0L))
    }
    LaunchedEffect(sosStartedAtMs) {
        while (true) {
            activeSeconds = ((System.currentTimeMillis() - sosStartedAtMs) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val infinite = rememberInfiniteTransition(label = "sos_beacon_pulse")
    val strobeAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobe_alpha"
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val contactsCount = remember(emergencyContacts) {
        emergencyContacts.split(",", ";").map { it.trim() }.count { it.isNotEmpty() }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("active_sos_beacon_screen")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Top Pulsing Red Banner: ACTIVE EMERGENCY BEACON: TRANSMITTING
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF5A0D18).copy(alpha = 0.65f + 0.35f * strobeAlpha),
                    border = BorderStroke(1.5.dp, Color(0xFFFF3B5C).copy(alpha = 0.7f + 0.3f * strobeAlpha)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ACTIVE EMERGENCY BEACON: TRANSMITTING",
                        color = ArcticFrost,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.0.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp)
                    )
                }

                // 2. Brand Row: Wolf Crest + Striders Howl | Winter Trail SOS: Howl for Help
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_striders_howl_logo),
                        contentDescription = "Striders Howl Emblem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, GlacialCyan, CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Striders", color = ArcticFrost, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 16.sp)
                        Text("Howl", color = ArcticFrost, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 16.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(ArcticFrost.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Winter Trail SOS:", color = ArcticFrost, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp)
                        Text("Howl for Help", color = AlpineMist, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)
                    }
                }

                // 3. Live GPS Coordinates + Altitude / Battery / Time Active Telemetry Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0A111A),
                    border = BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(listOf(GlacialCyan, Color(0xFFFF3B5C)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Live GPS coordinates:",
                            style = MaterialTheme.typography.labelSmall,
                            color = AlpineMist
                        )
                        if (lastLocation != null) {
                            val latDir = if (lastLocation.latitude >= 0) "N" else "S"
                            val lonDir = if (lastLocation.longitude >= 0) "E" else "W"
                            val latVal = String.format(Locale.US, "%.4f°", abs(lastLocation.latitude))
                            val lonVal = String.format(Locale.US, "%.4f°", abs(lastLocation.longitude))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = ArcticFrost, fontWeight = FontWeight.ExtraBold)) {
                                        append("$latVal ")
                                    }
                                    withStyle(SpanStyle(color = GlacialCyan, fontWeight = FontWeight.Black)) {
                                        append("$latDir, ")
                                    }
                                    withStyle(SpanStyle(color = ArcticFrost, fontWeight = FontWeight.ExtraBold)) {
                                        append("$lonVal ")
                                    }
                                    withStyle(SpanStyle(color = GlacialCyan, fontWeight = FontWeight.Black)) {
                                        append(lonDir)
                                    }
                                },
                                fontSize = 21.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Acquiring GPS satellite lock...",
                                color = GlacialCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SosStatColumn(
                                label = "Altitude:",
                                value = "${String.format(Locale.US, "%,d", lastLocation?.elevationFeet?.roundToInt() ?: 0)} ft"
                            )
                            SosStatColumn(
                                label = "Battery Level:",
                                value = "$batteryPercent%"
                            )
                            SosStatColumn(
                                label = "Time Active:",
                                value = SummitFormatters.formatDuration(activeSeconds)
                            )
                        }
                    }
                }

                // 4. Center Dual-Spectrum Acoustic Distress Ring + Live Waveform + Mute Pill
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height * 0.44f
                        val baseRadius = size.minDimension * 0.34f
                        val ringBrush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF5E3A), Color(0xFFFF2A54), GlacialCyan)
                        )

                        // Ambient glow halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF2A54).copy(alpha = if (acousticBeaconActive) 0.22f * strobeAlpha else 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(cx - baseRadius * 0.2f, cy),
                                radius = baseRadius * 1.9f
                            ),
                            radius = baseRadius * 1.9f,
                            center = Offset(cx - baseRadius * 0.2f, cy)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GlacialCyan.copy(alpha = if (acousticBeaconActive) 0.22f * strobeAlpha else 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(cx + baseRadius * 0.2f, cy),
                                radius = baseRadius * 1.9f
                            ),
                            radius = baseRadius * 1.9f,
                            center = Offset(cx + baseRadius * 0.2f, cy)
                        )

                        // Left & Right animated distress audio waveforms
                        val waveAmp = if (acousticBeaconActive) 28f * (0.65f + 0.35f * strobeAlpha) else 4f
                        val leftPath = Path().apply {
                            moveTo(0f, cy)
                            for (i in 0..24) {
                                val x = (cx - baseRadius * 1.15f) * (i / 24f)
                                val envelope = sin(PI * (i / 24.0)).toFloat()
                                val spike = sin(wavePhase + i * 1.3f) * waveAmp * envelope
                                lineTo(x, cy + spike)
                            }
                        }
                        drawPath(
                            path = leftPath,
                            color = Color(0xFFFF5E3A),
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )

                        val rightStartX = cx + baseRadius * 1.15f
                        val rightSpan = size.width - rightStartX
                        val rightPath = Path().apply {
                            moveTo(rightStartX, cy)
                            for (i in 0..24) {
                                val x = rightStartX + rightSpan * (i / 24f)
                                val envelope = sin(PI * (i / 24.0)).toFloat()
                                val spike = sin(-wavePhase + i * 1.3f) * waveAmp * envelope
                                lineTo(x, cy + spike)
                            }
                        }
                        drawPath(
                            path = rightPath,
                            color = GlacialCyan,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )

                        // Concentric glowing rings
                        drawCircle(
                            brush = ringBrush,
                            radius = baseRadius * (1.16f + 0.05f * strobeAlpha),
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.5f)
                        )
                        drawCircle(
                            brush = ringBrush,
                            radius = baseRadius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 6f)
                        )
                        drawCircle(
                            color = Color(0xFF0B0E14),
                            radius = baseRadius - 4f,
                            center = Offset(cx, cy)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 56.dp)
                    ) {
                        Text(
                            text = "Acoustic Distress\nHowl Beacon:",
                            color = ArcticFrost,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = if (acousticBeaconActive) "100% Volume" else "Muted (GPS Active)",
                            color = if (acousticBeaconActive) ArcticFrost.copy(alpha = 0.85f) else AlpineMist,
                            fontSize = 11.sp
                        )
                    }

                    // Mute Howl / Keep GPS Active Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1A222E).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleAcousticBeacon()
                            }
                            .testTag("sos_toggle_acoustic_beacon_button")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Mute Howl / Keep GPS Active",
                                color = ArcticFrost,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (acousticBeaconActive) {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    },
                                    contentDescription = "Toggle Acoustic Howl",
                                    tint = if (acousticBeaconActive) ArcticFrost else AlpineMist,
                                    modifier = Modifier.size(18.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (acousticBeaconActive) CrimsonSurge else Color(0xFF2B3442)
                                ) {
                                    Text(
                                        text = if (acousticBeaconActive) "ACTIVE" else "MUTED",
                                        color = ArcticFrost,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Emergency Contacts Notified / Dispatch SMS & Satellite Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF121C28),
                    border = BorderStroke(
                        width = 1.4.dp,
                        brush = Brush.horizontalGradient(listOf(GlacialCyan, CrimsonSurge))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDispatchSms()
                        }
                        .testTag("sos_dispatch_sms_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp)
                    ) {
                        Text(
                            text = if (contactsCount > 0) {
                                "Emergency Contacts Ready: $contactsCount"
                            } else {
                                "Dispatch Emergency SOS Payload"
                            },
                            color = ArcticFrost,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap to send via SMS / Satellite Relay",
                            color = AlpineMist,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // 6. Slide to Cancel Emergency Tactile Drag Track
                SlideToCancelEmergencyBar(
                    onConfirmedCancel = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEndSos()
                    }
                )
            }
        }
    }
}

@Composable
private fun SosStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = AlpineMist)
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = ArcticFrost)
    }
}

@Composable
private fun SlideToCancelEmergencyBar(
    onConfirmedCancel: () -> Unit
) {
    val density = LocalDensity.current
    val thumbSizeDp = 48.dp
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(Color(0xFF280B12))
            .border(1.5.dp, Color(0xFFFF3B5C).copy(alpha = 0.75f), RoundedCornerShape(29.dp))
            .padding(5.dp)
            .testTag("sos_end_beacon_button")
    ) {
        val maxDragPx = with(density) { (maxWidth - thumbSizeDp).toPx().coerceAtLeast(1f) }

        Text(
            text = "SLIDE TO CANCEL EMERGENCY",
            color = ArcticFrost.copy(alpha = (1f - (dragOffsetPx / maxDragPx) * 0.7f).coerceIn(0.25f, 1f)),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
                .size(thumbSizeDp)
                .clip(CircleShape)
                .background(CrimsonSurge)
                .border(1.5.dp, ArcticFrost.copy(alpha = 0.65f), CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragOffsetPx = (dragOffsetPx + delta).coerceIn(0f, maxDragPx)
                    },
                    onDragStopped = {
                        if (dragOffsetPx >= maxDragPx * 0.78f) {
                            onConfirmedCancel()
                        }
                        dragOffsetPx = 0f
                    }
                )
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardDoubleArrowRight,
                contentDescription = "Slide to Cancel SOS",
                tint = ArcticFrost,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun EmergencyContactsDialog(
    initialContacts: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var draft by remember(initialContacts) { mutableStateOf(initialContacts) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Winter SOS Emergency Contacts", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter one or more phone numbers separated by commas. They will be pre-filled in your offline SMS / Satellite SOS beacon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Emergency Phone Numbers") },
                    placeholder = { Text("e.g. +15551234567, +15559876543") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sos_contacts_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(draft)
                    onDismiss()
                },
                modifier = Modifier.testTag("sos_contacts_save_button")
            ) {
                Text("Save Contacts")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private tailrec fun Context.findActivityWindow(): android.view.Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findActivityWindow()
    else -> null
}
