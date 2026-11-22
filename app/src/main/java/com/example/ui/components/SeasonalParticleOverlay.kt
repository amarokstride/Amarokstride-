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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.data.model.SeasonTheme
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class ParticleSeed(
    val xFrac: Float,
    val yOffset: Float,
    val speed: Float,
    val size: Float,
    val phase: Float,
    val colorIndex: Int
)

@Composable
fun SeasonalParticleOverlay(
    seasonTheme: SeasonTheme,
    modifier: Modifier = Modifier
) {
    val activeSeason = seasonTheme.resolveEffectiveSeason()
    val seeds = remember(activeSeason) {
        val rng = Random(activeSeason.ordinal * 9173 + 42)
        val count = if (activeSeason == SeasonTheme.SUMMER) 10 else 24
        List(count) {
            ParticleSeed(
                xFrac = rng.nextFloat(),
                yOffset = rng.nextFloat(),
                speed = 0.55f + rng.nextFloat() * 0.85f,
                size = 5f + rng.nextFloat() * 8f,
                phase = rng.nextFloat() * 6.28f,
                colorIndex = rng.nextInt(3)
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "seasonal_particles")
    val tick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_tick"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        when (activeSeason) {
            SeasonTheme.WINTER -> {
                seeds.forEach { p ->
                    val y = ((p.yOffset + tick * p.speed) % 1f) * h
                    val drift = sin((tick * 2f * PI + p.phase).toDouble()).toFloat() * 24f
                    val x = (p.xFrac * w + drift + w) % w
                    val color = if (p.colorIndex == 0) Color(0x99D9F4FF) else Color(0x77FFFFFF)
                    drawCircle(color = color, radius = p.size * 0.55f, center = Offset(x, y))
                }
            }

            SeasonTheme.FALL -> {
                val leafColors = listOf(Color(0x99D46427), Color(0x99C78F28), Color(0x889E3B1D))
                seeds.forEach { p ->
                    val y = ((p.yOffset + tick * p.speed) % 1f) * h
                    val sway = sin((tick * 3f * PI + p.phase).toDouble()).toFloat() * 38f
                    val x = (p.xFrac * w + sway + w) % w
                    val angle = (tick * 360f * p.speed + p.phase * 40f) % 360f
                    rotate(degrees = angle, pivot = Offset(x, y)) {
                        drawOval(
                            color = leafColors[p.colorIndex],
                            topLeft = Offset(x - p.size * 0.8f, y - p.size * 0.4f),
                            size = Size(p.size * 1.6f, p.size * 0.85f)
                        )
                    }
                }
            }

            SeasonTheme.SPRING -> {
                val petalColors = listOf(Color(0x88F8BBD0), Color(0x88FFF0F5), Color(0x88F48FB1))
                seeds.forEach { p ->
                    val y = ((p.yOffset + tick * p.speed * 0.8f) % 1f) * h
                    val sway = sin((tick * 2.5f * PI + p.phase).toDouble()).toFloat() * 30f
                    val x = (p.xFrac * w + sway + w) % w
                    val angle = (tick * 240f + p.phase * 50f) % 360f
                    rotate(degrees = angle, pivot = Offset(x, y)) {
                        drawOval(
                            color = petalColors[p.colorIndex],
                            topLeft = Offset(x - p.size * 0.65f, y - p.size * 0.45f),
                            size = Size(p.size * 1.3f, p.size * 0.9f)
                        )
                    }
                }
            }

            SeasonTheme.SUMMER, SeasonTheme.AUTO -> {
                // Sweeping golden summer wind currents
                seeds.forEach { p ->
                    val xStart = ((p.xFrac + tick * p.speed * 1.3f) % 1.3f - 0.15f) * w
                    val yBase = (0.12f + p.yOffset * 0.78f) * h
                    val ribbonLen = 90f + p.size * 12f
                    val path = Path().apply {
                        moveTo(xStart, yBase)
                        cubicTo(
                            xStart + ribbonLen * 0.35f,
                            yBase - 16f,
                            xStart + ribbonLen * 0.7f,
                            yBase + 16f,
                            xStart + ribbonLen,
                            yBase - 6f
                        )
                    }
                    val color = if (p.colorIndex == 0) Color(0x45FFE082) else Color(0x388BD8F0)
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2.6f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
