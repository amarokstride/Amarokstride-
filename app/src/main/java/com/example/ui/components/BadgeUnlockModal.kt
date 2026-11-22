package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.StoryBadgeType
import com.example.domain.StorySurvivalBadge
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.SummitGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun StoryBadgeType.iconVector(): ImageVector = when (this) {
    StoryBadgeType.MIDNIGHT_PATROL -> Icons.Filled.NightsStay
    StoryBadgeType.RIDGE_HOWLER -> Icons.Filled.Terrain
    StoryBadgeType.QUARRY_PURSUIT -> Icons.Filled.Bolt
    StoryBadgeType.DEEP_FREEZE_ZERO -> Icons.Filled.AcUnit
    StoryBadgeType.ALPHA_GUARDIAN -> Icons.Filled.Shield
    StoryBadgeType.CORRIDOR_PIONEER -> Icons.Filled.Explore
}

/**
 * Custom Norse-runic metallic emblem modeled after `striders_howl_custom_story_badges.png`.
 */
@Composable
fun RunicStoryBadgeMedallion(
    type: StoryBadgeType,
    unlocked: Boolean,
    size: Dp = 74.dp,
    modifier: Modifier = Modifier
) {
    val primaryAccent = if (type.isGoldEmblem) SummitGold else GlacialCyan
    val outerRimColor = if (unlocked) primaryAccent else AlpineMist.copy(alpha = 0.35f)
    val innerGlow = if (unlocked) primaryAccent.copy(alpha = 0.32f) else Color.Transparent

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .alpha(if (unlocked) 1f else 0.55f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = this.size.minDimension * 0.46f

            // Ambient radial glow behind unlocked medallion
            if (unlocked) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(innerGlow, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r * 1.15f
                    ),
                    radius = r * 1.15f,
                    center = Offset(cx, cy)
                )
            }

            // Dark metallic core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF172536), Color(0xFF0A0F1D)),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )

            // Outer runic ring band
            drawCircle(
                color = outerRimColor,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 3.5f)
            )
            drawCircle(
                color = outerRimColor.copy(alpha = 0.55f),
                radius = r * 0.78f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.8f)
            )

            // 16 carved Norse rune ticks around the metallic bevel
            val tickCount = 16
            for (i in 0 until tickCount) {
                val angle = (2.0 * PI * i / tickCount).toFloat()
                val innerR = r * 0.82f
                val outerR = r * 0.96f
                drawLine(
                    color = outerRimColor.copy(alpha = if (i % 2 == 0) 0.9f else 0.45f),
                    start = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
                    end = Offset(cx + cos(angle) * outerR, cy + sin(angle) * outerR),
                    strokeWidth = if (i % 2 == 0) 2.2f else 1.4f
                )
            }
        }

        Icon(
            imageVector = type.iconVector(),
            contentDescription = type.title,
            tint = if (unlocked) primaryAccent else AlpineMist,
            modifier = Modifier.size(size * 0.42f)
        )
    }
}

/**
 * 2-row x 3-column Custom Story & Survival Trophy Room grid matching `striders_howl_custom_story_badges.png`.
 */
@Composable
fun StorySurvivalBadgeGrid(
    badges: List<StorySurvivalBadge>,
    onBadgeClick: (StorySurvivalBadge) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        badges.chunked(3).forEach { rowBadges ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowBadges.forEach { badge ->
                    val accent = if (badge.type.isGoldEmblem) SummitGold else GlacialCyan
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = if (badge.unlocked) 0.06f else 0.025f),
                        border = BorderStroke(
                            1.dp,
                            if (badge.unlocked) accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onBadgeClick(badge) }
                            .testTag("story_badge_${badge.type.name.lowercase()}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            RunicStoryBadgeMedallion(
                                type = badge.type,
                                unlocked = badge.unlocked,
                                size = 64.dp
                            )
                            Text(
                                text = badge.type.title,
                                color = if (badge.unlocked) ArcticFrost else AlpineMist,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                text = if (badge.unlocked) "UNLOCKED" else badge.progressLabel,
                                color = if (badge.unlocked) accent else AlpineMist.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Celebratory Badge Unlock Modal with elastic spring scale animation, glowing runic medallion,
 * procedural crystalline chime + wolf choir audio stinger, and Pack Simulation Reward banner.
 */
@Composable
fun BadgeUnlockModal(
    badge: StorySurvivalBadge,
    onPlayStinger: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var revealed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.35f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badge_elastic_scale"
    )

    LaunchedEffect(badge.type) {
        revealed = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onPlayStinger()
    }

    val accent = if (badge.type.isGoldEmblem) SummitGold else GlacialCyan

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 24.dp)
                .testTag("badge_unlock_modal")
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF0A0F1D).copy(alpha = 0.97f),
                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(26.dp)
                ) {
                    Text(
                        text = if (badge.unlocked) "NEW BADGE UNLOCKED" else "STORY BADGE PREVIEW",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.4.sp
                    )
                    Spacer(Modifier.height(18.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(scale)
                            .clip(CircleShape)
                    ) {
                        RunicStoryBadgeMedallion(
                            type = badge.type,
                            unlocked = true,
                            size = 120.dp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = badge.type.title,
                        color = ArcticFrost,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = badge.type.conditionSummary,
                        color = AlpineMist,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    // Pack Simulation Reward Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.Pets, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            Text(
                                text = badge.type.packRewardDescription,
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color(0xFF0A0F1D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("badge_claim_button")
                    ) {
                        Text(
                            text = if (badge.unlocked) "CLAIM & ADD TO TROPHY ROOM" else "CLOSE PREVIEW",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        }
    }
}
