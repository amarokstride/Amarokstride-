package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.WindowManager
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.AuroraMint
import com.example.ui.theme.BasaltObsidian
import com.example.ui.theme.FrostedRim
import com.example.ui.theme.GlacialCyan
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Full-screen OLED Always-On Display (AOD) watch face matching striders_howl_watchface.png.
 * Keeps screen awake via FLAG_KEEP_SCREEN_ON while active and displays live steps conservation goal,
 * glowing Striders Howl wolf crest, real-time clock, date, and real Android battery percentage.
 */
@Composable
fun AlwaysOnWatchFaceDialog(
    todayMiles: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var batteryPct by remember { mutableIntStateOf(readBatteryPercent(context)) }

    // Keep screen awake while Always-On Trail Display is active
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            batteryPct = readBatteryPercent(context)
            delay(15_000L)
        }
    }

    val estimatedSteps = (todayMiles * 2112.0).roundToInt().coerceAtLeast(0)
    val goalSteps = 15000
    val stepProgress = (estimatedSteps.toFloat() / goalSteps.toFloat()).coerceIn(0.08f, 1f)

    val timeStr = remember(nowMs) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
    }
    val dateStr = remember(nowMs) {
        SimpleDateFormat("EEE MMM d", Locale.getDefault()).format(Date(nowMs)).uppercase(Locale.US)
    }

    val pulseTransition = rememberInfiniteTransition(label = "aod_wolf_pulse")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aod_glow"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss)
                .testTag("always_on_display_overlay")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                // Brushed Titanium Watch Bezel & OLED Face
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(320.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3E454C), Color(0xFF1D2227), Color(0xFF2D333A))
                            )
                        )
                        .border(2.dp, Color(0xFF56606A), CircleShape)
                        .padding(14.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF13222E), Color(0xFF070B10), Color.Black)
                            )
                        )
                ) {
                    // Glowing outer conservation progress arc
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pad = 16.dp.toPx()
                        val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                        val topLeft = Offset(pad, pad)

                        // Track arc
                        drawArc(
                            color = FrostedRim.copy(alpha = 0.45f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Active Glacial Cyan -> Aurora Mint sweep arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(GlacialCyan, AuroraMint, Color(0xFF2563EB), GlacialCyan)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * stepProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Ambient radial glow behind wolf crest
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(GlacialCyan.copy(alpha = glowAlpha), Color.Transparent),
                                center = Offset(size.width / 2f, size.height * 0.48f),
                                radius = size.minDimension * 0.30f
                            ),
                            radius = size.minDimension * 0.30f,
                            center = Offset(size.width / 2f, size.height * 0.48f)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 28.dp, horizontal = 24.dp)
                    ) {
                        // Top Steps Conservation Goal
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%,d / %,d STEPS", estimatedSteps, goalSteps),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ArcticFrost
                            )
                            Text(
                                text = "WOLF CONSERVATION GOAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlacialCyan,
                                letterSpacing = 1.sp
                            )
                        }

                        // Glowing Striders Howl Wolf Emblem
                        Image(
                            painter = painterResource(R.drawable.img_striders_howl_logo),
                            contentDescription = "Always-On Wolf Crest",
                            modifier = Modifier
                                .size(116.dp)
                                .clip(CircleShape)
                                .border(2.dp, GlacialCyan, CircleShape)
                        )

                        // Time, Date, Battery & Paw
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = timeStr,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ArcticFrost
                                )
                                Icon(
                                    imageVector = Icons.Filled.Pets,
                                    contentDescription = "Wolf Paw",
                                    tint = GlacialCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "$dateStr  ·  $batteryPct%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ArcticFrost.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "GLANCEABLE. RUGGED. PREMIUM.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlpineMist,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Subtle exit pill
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF141E28),
                    border = BorderStroke(1.dp, FrostedRim)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit Always-On", tint = GlacialCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Tap anywhere to exit Always-On Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = AlpineMist
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlwaysOnDisplayLaunchCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0E1721),
        border = BorderStroke(1.2.dp, GlacialCyan.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("launch_always_on_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF132533), BasaltObsidian))
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlacialCyan.copy(alpha = 0.16f))
                    .border(1.2.dp, GlacialCyan, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.BrightnessLow,
                    contentDescription = "Always-On Display",
                    tint = GlacialCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ALWAYS-ON TRAIL WATCHFACE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArcticFrost
                )
                Text(
                    text = "OLED glanceable clock, battery & wolf conservation steps arc",
                    style = MaterialTheme.typography.labelSmall,
                    color = AlpineMist
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "OPEN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = GlacialCyan
            )
        }
    }
}

private fun readBatteryPercent(context: Context): Int {
    return try {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt().coerceIn(0, 100) else 84
    } catch (_: Exception) {
        84
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
