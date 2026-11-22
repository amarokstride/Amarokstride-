package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.WolfFrostBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class RunningWolfSpec(
    val yFrac: Float,
    val startOffsetFrac: Float,
    val speedMultiplier: Float,
    val scale: Float,
    val strideCyclesPerLoop: Int,
    val phaseOffset: Float,
    val bodyAlpha: Float,
    val rimAlpha: Float,
    val runsRightToLeft: Boolean = false
)

/**
 * Ambient 60fps Canvas background animation showing a parallax pack of articulated
 * timber wolves bounding across a misty alpine horizon. Designed to sit behind app
 * screens and cards without blocking touch input or reducing text contrast.
 */
@Composable
fun RunningWolvesBackgroundOverlay(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    showHorizonMist: Boolean = true
) {
    val pack = remember {
        listOf(
            // Distant ridge scout wolf
            RunningWolfSpec(
                yFrac = 0.24f,
                startOffsetFrac = 0.12f,
                speedMultiplier = 1f,
                scale = 0.62f,
                strideCyclesPerLoop = 10,
                phaseOffset = 0.4f,
                bodyAlpha = 0.20f,
                rimAlpha = 0.30f,
                runsRightToLeft = false
            ),
            // Mid-horizon pack runner (heading left for dynamic depth)
            RunningWolfSpec(
                yFrac = 0.48f,
                startOffsetFrac = 0.62f,
                speedMultiplier = 1f,
                scale = 0.80f,
                strideCyclesPerLoop = 12,
                phaseOffset = 2.1f,
                bodyAlpha = 0.24f,
                rimAlpha = 0.38f,
                runsRightToLeft = true
            ),
            // Lead Alpha timber wolf bounding across lower-mid trail
            RunningWolfSpec(
                yFrac = 0.72f,
                startOffsetFrac = 0.0f,
                speedMultiplier = 1f,
                scale = 1.05f,
                strideCyclesPerLoop = 14,
                phaseOffset = 0.0f,
                bodyAlpha = 0.32f,
                rimAlpha = 0.52f,
                runsRightToLeft = false
            ),
            // Companion pack runner trailing the Alpha
            RunningWolfSpec(
                yFrac = 0.82f,
                startOffsetFrac = 0.38f,
                speedMultiplier = 1f,
                scale = 0.90f,
                strideCyclesPerLoop = 13,
                phaseOffset = 3.6f,
                bodyAlpha = 0.26f,
                rimAlpha = 0.42f,
                runsRightToLeft = false
            )
        )
    }

    val transition = rememberInfiniteTransition(label = "running_wolves_pack")
    val loopProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wolf_pack_loop"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val clampedIntensity = intensity.coerceIn(0.2f, 1.5f)

        if (showHorizonMist) {
            drawSubtleAlpineMistBands(w, h, clampedIntensity)
        }

        val unitScale = (minOf(w, h) / 420f).coerceIn(0.75f, 1.6f)

        pack.forEach { wolf ->
            val travelSpan = w + 260f * unitScale
            val progress = (wolf.startOffsetFrac + loopProgress * wolf.speedMultiplier) % 1f
            val xPos = if (wolf.runsRightToLeft) {
                w + 130f * unitScale - progress * travelSpan
            } else {
                -130f * unitScale + progress * travelSpan
            }
            val strideAngle = (loopProgress * wolf.strideCyclesPerLoop * 2.0 * PI + wolf.phaseOffset).toFloat()
            val yBase = h * wolf.yFrac
            val bounceY = -sin(strideAngle) * 7.5f * wolf.scale * unitScale

            drawGallopingWolf(
                centerX = xPos,
                centerY = yBase + bounceY,
                scale = wolf.scale * unitScale,
                strideAngle = strideAngle,
                facingLeft = wolf.runsRightToLeft,
                bodyColor = Color(0xFF0A141E).copy(alpha = (wolf.bodyAlpha * clampedIntensity).coerceIn(0f, 0.85f)),
                rimColor = GlacialCyan.copy(alpha = (wolf.rimAlpha * clampedIntensity).coerceIn(0f, 0.90f)),
                mistColor = WolfFrostBlue.copy(alpha = (0.20f * clampedIntensity).coerceIn(0f, 0.6f))
            )
        }
    }
}

private fun DrawScope.drawSubtleAlpineMistBands(
    w: Float,
    h: Float,
    intensity: Float
) {
    // Distant silhouetted ridge line behind the pack
    val ridgePath = Path().apply {
        moveTo(0f, h * 0.78f)
        lineTo(w * 0.16f, h * 0.70f)
        lineTo(w * 0.32f, h * 0.75f)
        lineTo(w * 0.54f, h * 0.67f)
        lineTo(w * 0.76f, h * 0.74f)
        lineTo(w * 0.92f, h * 0.69f)
        lineTo(w, h * 0.73f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = ridgePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                GlacialCyan.copy(alpha = (0.06f * intensity).coerceIn(0f, 0.25f)),
                Color.Transparent
            ),
            startY = h * 0.66f,
            endY = h
        )
    )
}

private fun DrawScope.drawGallopingWolf(
    centerX: Float,
    centerY: Float,
    scale: Float,
    strideAngle: Float,
    facingLeft: Boolean,
    bodyColor: Color,
    rimColor: Color,
    mistColor: Color
) {
    val pitchDegrees = cos(strideAngle) * 6.5f
    val spineArch = sin(strideAngle) * 4.5f
    val tailWave = sin(strideAngle - 1.1f) * 8.0f

    // Subtle snow/frost mist puffs kicked up behind the galloping paws
    for (i in 1..3) {
        val dir = if (facingLeft) 1f else -1f
        val puffPhase = ((strideAngle / (2f * PI.toFloat())) + i * 0.28f) % 1f
        val puffX = centerX + dir * (28f + i * 20f + puffPhase * 16f) * scale
        val puffY = centerY + (19f - puffPhase * 8f) * scale
        val puffRadius = (4.5f + puffPhase * 5.5f) * scale
        drawCircle(
            color = mistColor.copy(alpha = mistColor.alpha * (1f - puffPhase)),
            radius = puffRadius,
            center = Offset(puffX, puffY)
        )
    }

    withTransform({
        translate(left = centerX, top = centerY)
        scale(scaleX = if (facingLeft) -scale else scale, scaleY = scale, pivot = Offset.Zero)
        rotate(degrees = pitchDegrees, pivot = Offset.Zero)
    }) {
        // 1. Far-side articulated legs (drawn behind torso for depth)
        drawArticulatedFrontLeg(
            shoulder = Offset(20f, 4f),
            phase = strideAngle + 0.72f,
            color = bodyColor.copy(alpha = bodyColor.alpha * 0.78f),
            rimColor = rimColor.copy(alpha = rimColor.alpha * 0.55f)
        )
        drawArticulatedHindLeg(
            hip = Offset(-24f, 4f),
            phase = strideAngle + PI.toFloat() + 0.78f,
            color = bodyColor.copy(alpha = bodyColor.alpha * 0.78f),
            rimColor = rimColor.copy(alpha = rimColor.alpha * 0.55f)
        )

        // 2. Flowing bushy wolf tail
        val tailPath = Path().apply {
            moveTo(-30f, -5f)
            cubicTo(
                -44f, -10f + tailWave * 0.5f,
                -56f, -6f + tailWave,
                -63f, 2f + tailWave
            )
            cubicTo(
                -54f, 7f + tailWave * 0.6f,
                -41f, 6f + tailWave * 0.3f,
                -29f, 2f
            )
            close()
        }
        drawPath(path = tailPath, color = bodyColor)
        drawPath(path = tailPath, color = rimColor.copy(alpha = rimColor.alpha * 0.75f), style = Stroke(width = 1.4f))

        // 3. Athletic wolf torso, ruffed chest, neck, pointed ears & snout
        val torsoPath = Path().apply {
            // Start at rump
            moveTo(-31f, -5f)
            // Arched dynamic spine to shoulders
            cubicTo(-14f, -14f - spineArch, 6f, -15f - spineArch * 0.6f, 22f, -12f)
            // Muscular neck rising into head
            lineTo(33f, -21f)
            // Back ear
            lineTo(34f, -31f)
            lineTo(39f, -23f)
            // Front ear
            lineTo(43f, -30f)
            lineTo(45f, -20f)
            // Brow & noble wolf snout
            lineTo(56f, -15f)
            lineTo(55f, -11f)
            // Jaw & throat ruff
            lineTo(44f, -9f)
            cubicTo(39f, -3f, 34f, 3f, 28f, 9f)
            // Deep chest to tucked athletic flank
            cubicTo(16f, 13f, 2f, 9f + spineArch * 0.4f, -12f, 7f + spineArch * 0.5f)
            cubicTo(-22f, 6f, -29f, 7f, -32f, 1f)
            close()
        }
        drawPath(path = torsoPath, color = bodyColor)
        drawPath(
            path = torsoPath,
            color = rimColor,
            style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Subtle glowing wolf eye glint
        drawCircle(
            color = rimColor.copy(alpha = (rimColor.alpha * 1.35f).coerceAtMost(0.95f)),
            radius = 1.5f,
            center = Offset(46.5f, -16.5f)
        )

        // 4. Near-side articulated legs (drawn in foreground)
        drawArticulatedFrontLeg(
            shoulder = Offset(22f, 6f),
            phase = strideAngle,
            color = bodyColor,
            rimColor = rimColor
        )
        drawArticulatedHindLeg(
            hip = Offset(-22f, 5f),
            phase = strideAngle + PI.toFloat(),
            color = bodyColor,
            rimColor = rimColor
        )
    }
}

private fun DrawScope.drawArticulatedFrontLeg(
    shoulder: Offset,
    phase: Float,
    color: Color,
    rimColor: Color
) {
    val upperAngle = sin(phase) * 0.85f + 0.12f
    val kneeBend = (cos(phase) * 0.65f + 0.45f).coerceIn(0.05f, 1.25f)

    val upperLen = 14f
    val lowerLen = 14f

    val knee = Offset(
        x = shoulder.x + sin(upperAngle) * upperLen,
        y = shoulder.y + cos(upperAngle) * upperLen
    )
    val lowerAngle = upperAngle - kneeBend
    val paw = Offset(
        x = knee.x + sin(lowerAngle) * lowerLen,
        y = knee.y + cos(lowerAngle) * lowerLen
    )

    drawLine(color = color, start = shoulder, end = knee, strokeWidth = 5.5f, cap = StrokeCap.Round)
    drawLine(color = color, start = knee, end = paw, strokeWidth = 3.8f, cap = StrokeCap.Round)
    drawLine(color = rimColor, start = shoulder, end = knee, strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color = rimColor, start = knee, end = paw, strokeWidth = 1.1f, cap = StrokeCap.Round)
}

private fun DrawScope.drawArticulatedHindLeg(
    hip: Offset,
    phase: Float,
    color: Color,
    rimColor: Color
) {
    val thighAngle = sin(phase) * 0.90f - 0.20f
    val hockBend = (cos(phase) * 0.55f + 0.50f).coerceIn(0.05f, 1.20f)

    val thighLen = 15f
    val shinLen = 15f

    val hock = Offset(
        x = hip.x + sin(thighAngle) * thighLen,
        y = hip.y + cos(thighAngle) * thighLen
    )
    val shinAngle = thighAngle + hockBend
    val paw = Offset(
        x = hock.x + sin(shinAngle) * shinLen,
        y = hock.y + cos(shinAngle) * shinLen
    )

    drawLine(color = color, start = hip, end = hock, strokeWidth = 6.2f, cap = StrokeCap.Round)
    drawLine(color = color, start = hock, end = paw, strokeWidth = 3.8f, cap = StrokeCap.Round)
    drawLine(color = rimColor, start = hip, end = hock, strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color = rimColor, start = hock, end = paw, strokeWidth = 1.1f, cap = StrokeCap.Round)
}
