package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.WolfCoat
import com.example.ui.theme.AuroraMint
import com.example.ui.theme.BasaltObsidian
import com.example.ui.theme.GlacialCyan
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Kinetic animated wolf crest avatar in continuous motion:
 * rotating glacial aurora rim, drifting arctic wind streaks, rhythmic stride bob,
 * ear-perk motion, frost breath wisps, and glowing pulsing wolf eyes with natural blink.
 */
@Composable
fun AnimatedWolfAvatar(
    coat: WolfCoat,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "wolf_avatar_${coat.name}")
    val stridePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wolf_stride_phase"
    )
    val rimAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wolf_rim_angle"
    )
    val windShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wolf_wind_shift"
    )
    val blinkCycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wolf_blink"
    )

    val bodyColor = Color(coat.bodyHex)
    val darkColor = Color(coat.darkHex)
    val frostColor = Color(coat.frostHex)
    val eyeColor = Color(coat.eyeHex)
    val isBlinking = blinkCycle in 0.90f..0.96f

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = (w.coerceAtMost(h) / 2f) - 2f
        val wave = sin(stridePhase)
        val bobY = wave * (h * 0.032f)
        val earPerk = cos(stridePhase) * (h * 0.036f)
        val leanX = sin(stridePhase * 0.5f) * (w * 0.022f)

        // Outer obsidian frost medallion
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(darkColor.copy(alpha = 0.95f), BasaltObsidian)
            ),
            radius = r,
            center = Offset(cx, cy)
        )

        // Drifting arctic wind motion streaks behind wolf
        for (i in 0..2) {
            val streakProgress = (windShift + i * 0.33f) % 1f
            val sy = cy - r * 0.36f + (i * r * 0.34f)
            val startX = cx + r * 0.72f - (streakProgress * r * 1.45f)
            val streakLen = r * 0.42f
            drawLine(
                color = GlacialCyan.copy(alpha = (1f - streakProgress) * 0.42f),
                start = Offset(startX.coerceIn(cx - r * 0.85f, cx + r * 0.85f), sy),
                end = Offset((startX + streakLen).coerceIn(cx - r * 0.85f, cx + r * 0.85f), sy),
                strokeWidth = w * 0.03f,
                cap = StrokeCap.Round
            )
        }

        // Rotating aurora glacial rim
        rotate(degrees = rimAngle, pivot = Offset(cx, cy)) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        GlacialCyan,
                        eyeColor,
                        AuroraMint.copy(alpha = 0.45f),
                        GlacialCyan
                    )
                ),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = w * 0.06f)
            )
        }

        // Left & right animated wolf ears
        val leftEar = Path().apply {
            moveTo(cx - r * 0.48f + leanX, cy - r * 0.14f + bobY)
            lineTo(cx - r * 0.58f + leanX, cy - r * 0.74f - earPerk)
            lineTo(cx - r * 0.14f + leanX, cy - r * 0.36f + bobY)
            close()
        }
        val rightEar = Path().apply {
            moveTo(cx + r * 0.48f + leanX, cy - r * 0.14f + bobY)
            lineTo(cx + r * 0.58f + leanX, cy - r * 0.74f + earPerk)
            lineTo(cx + r * 0.14f + leanX, cy - r * 0.36f + bobY)
            close()
        }
        drawPath(leftEar, color = darkColor)
        drawPath(rightEar, color = darkColor)

        // Wolf head silhouette
        val head = Path().apply {
            moveTo(cx + leanX, cy - r * 0.44f + bobY)
            lineTo(cx + r * 0.52f + leanX, cy - r * 0.08f + bobY)
            lineTo(cx + r * 0.34f + leanX, cy + r * 0.34f + bobY)
            lineTo(cx + leanX, cy + r * 0.54f + bobY)
            lineTo(cx - r * 0.34f + leanX, cy + r * 0.34f + bobY)
            lineTo(cx - r * 0.52f + leanX, cy - r * 0.08f + bobY)
            close()
        }
        drawPath(head, color = bodyColor)

        // Snout & cheek frost mask
        val snout = Path().apply {
            moveTo(cx + leanX, cy + r * 0.02f + bobY)
            lineTo(cx + r * 0.28f + leanX, cy + r * 0.30f + bobY)
            lineTo(cx + leanX, cy + r * 0.52f + bobY)
            lineTo(cx - r * 0.28f + leanX, cy + r * 0.30f + bobY)
            close()
        }
        drawPath(snout, color = frostColor)

        // Nose tip
        drawCircle(
            color = darkColor,
            radius = r * 0.09f,
            center = Offset(cx + leanX, cy + r * 0.34f + bobY)
        )

        // Rising frosted breath puff
        val breathAlpha = ((1f - windShift) * 0.55f).coerceIn(0f, 0.55f)
        drawCircle(
            color = GlacialCyan.copy(alpha = breathAlpha),
            radius = r * (0.05f + windShift * 0.08f),
            center = Offset(
                cx + leanX + r * (0.12f + windShift * 0.24f),
                cy + r * 0.44f - windShift * r * 0.16f
            )
        )

        // Glowing wolf eyes (blink periodically + pulse glow)
        val eyePulse = 0.85f + 0.15f * ((wave + 1f) * 0.5f)
        if (isBlinking) {
            drawLine(
                color = eyeColor,
                start = Offset(cx - r * 0.28f + leanX, cy - r * 0.02f + bobY),
                end = Offset(cx - r * 0.10f + leanX, cy + r * 0.02f + bobY),
                strokeWidth = w * 0.045f
            )
            drawLine(
                color = eyeColor,
                start = Offset(cx + r * 0.10f + leanX, cy + r * 0.02f + bobY),
                end = Offset(cx + r * 0.28f + leanX, cy - r * 0.02f + bobY),
                strokeWidth = w * 0.045f
            )
        } else {
            drawCircle(
                color = eyeColor.copy(alpha = 0.35f * eyePulse),
                radius = r * 0.14f,
                center = Offset(cx - r * 0.20f + leanX, cy - r * 0.01f + bobY)
            )
            drawCircle(
                color = eyeColor.copy(alpha = 0.35f * eyePulse),
                radius = r * 0.14f,
                center = Offset(cx + r * 0.20f + leanX, cy - r * 0.01f + bobY)
            )
            drawCircle(
                color = eyeColor,
                radius = r * 0.085f,
                center = Offset(cx - r * 0.20f + leanX, cy - r * 0.01f + bobY)
            )
            drawCircle(
                color = eyeColor,
                radius = r * 0.085f,
                center = Offset(cx + r * 0.20f + leanX, cy - r * 0.01f + bobY)
            )
        }
    }
}
