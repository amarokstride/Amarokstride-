package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.SummitGold
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class DonorTier(val label: String) {
    GUARDIAN("Wild Wolf Guardian"),
    ALPHA_SPONSOR("Alpha Wolf Sponsor"),
    APEX_PHILANTHROPIST("Apex Philanthropist")
}

data class ConservationDonorBadge(
    val id: String = "badge_pack_protector",
    val title: String = "PACK PROTECTOR",
    val subtitle: String = "Wild Wolf Conservation Guardian",
    val description: String = "Directly funded wild wolf recovery, winter corridor telemetry, or endangered wolf sponsorship.",
    val tier: DonorTier = DonorTier.GUARDIAN,
    val dateAwardedMs: Long = 0L,
    val organizationName: String = "",
    val isEquippedAsProfileFlair: Boolean = true
)

/**
 * Custom electric-cyan and alpine-gold frosted Pack Protector medallion matching
 * striders_howl_conservation_donor_badge.png: double glowing braided rim, luminous cyan
 * heart crest, howling wolf silhouette, and glowing paw emblem.
 */
@Composable
fun PackProtectorMedallionCanvas(
    size: Dp = 136.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "pack_protector_glow")
    val pulse by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "donor_halo_pulse"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.45f

        // Outer electric cyan aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    GlacialCyan.copy(alpha = 0.42f * pulse),
                    SummitGold.copy(alpha = 0.16f * pulse),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r * 1.15f
            ),
            radius = r * 1.15f,
            center = Offset(cx, cy)
        )

        // Deep sapphire medallion core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF10344A), Color(0xFF0A1424)),
                center = Offset(cx, cy),
                radius = r
            ),
            radius = r,
            center = Offset(cx, cy)
        )

        // Braided outer electric cyan & alpine gold double rings
        drawCircle(
            brush = Brush.sweepGradient(listOf(GlacialCyan, SummitGold, GlacialCyan, Color(0xFF88F6FF), GlacialCyan)),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.11f)
        )
        drawCircle(
            color = SummitGold,
            radius = r * 0.80f,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.055f)
        )

        // Braided notches around the ring
        for (i in 0 until 24) {
            val angle = (i * 15.0) * PI / 180.0
            val p1 = Offset(cx + cos(angle).toFloat() * (r * 0.85f), cy + sin(angle).toFloat() * (r * 0.85f))
            val p2 = Offset(cx + cos(angle + 0.12).toFloat() * (r * 0.96f), cy + sin(angle + 0.12).toFloat() * (r * 0.96f))
            drawLine(
                color = if (i % 2 == 0) SummitGold.copy(alpha = 0.85f) else GlacialCyan.copy(alpha = 0.85f),
                start = p1,
                end = p2,
                strokeWidth = 2.2f
            )
        }

        // Glowing heart cradling the wolf
        val heartPath = Path().apply {
            moveTo(cx, cy + r * 0.44f)
            cubicTo(cx - r * 0.54f, cy + r * 0.10f, cx - r * 0.56f, cy - r * 0.42f, cx, cy - r * 0.14f)
            cubicTo(cx + r * 0.56f, cy - r * 0.42f, cx + r * 0.54f, cy + r * 0.10f, cx, cy + r * 0.44f)
            close()
        }
        drawPath(
            path = heartPath,
            color = GlacialCyan.copy(alpha = 0.22f * pulse)
        )
        drawPath(
            path = heartPath,
            color = GlacialCyan,
            style = Stroke(width = r * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Stylized howling wolf profile inside the heart
        val wolfPath = Path().apply {
            moveTo(cx - r * 0.28f, cy + r * 0.30f)
            lineTo(cx - r * 0.22f, cy - r * 0.02f)
            lineTo(cx - r * 0.08f, cy - r * 0.20f)
            lineTo(cx - r * 0.04f, cy - r * 0.34f) // Ear
            lineTo(cx + r * 0.05f, cy - r * 0.22f)
            lineTo(cx + r * 0.24f, cy - r * 0.34f) // Howling snout up
            lineTo(cx + r * 0.28f, cy - r * 0.24f)
            lineTo(cx + r * 0.14f, cy - r * 0.08f)
            lineTo(cx + r * 0.24f, cy + r * 0.26f)
            close()
        }
        drawPath(wolfPath, color = Color(0xFF07131E))
        drawPath(
            wolfPath,
            color = ArcticFrost,
            style = Stroke(width = r * 0.04f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Glowing cyan wolf paw print centered on chest
        drawCircle(color = GlacialCyan, radius = r * 0.10f, center = Offset(cx - r * 0.01f, cy + r * 0.19f))
        val toeOffsets = listOf(
            Offset(-0.12f, 0.07f),
            Offset(-0.04f, 0.02f),
            Offset(0.05f, 0.03f),
            Offset(0.11f, 0.09f)
        )
        toeOffsets.forEach { (dx, dy) ->
            drawCircle(
                color = GlacialCyan,
                radius = r * 0.042f,
                center = Offset(cx + r * dx, cy + r * dy)
            )
        }
    }
}

/**
 * Compact Golden & Cyan Wolf Paw flair rendered next to Pack Protector runners on leaderboards.
 */
@Composable
fun GoldenDonorPawFlair(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SummitGold.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, SummitGold.copy(alpha = 0.7f)),
        modifier = modifier.testTag("golden_donor_paw_flair")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = "Pack Protector Donor Flair",
                tint = SummitGold,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "PROTECTOR",
                color = SummitGold,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp
            )
        }
    }
}

/**
 * Celebration modal awarded when a runner supports a verified wolf conservation portal.
 */
@Composable
fun DonorBadgeAwardModal(
    organizationName: String,
    onEquipBadge: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(organizationName) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(250L)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(250L)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF70A0F1D),
            border = BorderStroke(1.5.dp, GlacialCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("donor_badge_award_modal")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlacialCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "SPECIAL ACHIEVEMENT UNLOCKED",
                        color = GlacialCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.6.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                PackProtectorMedallionCanvas(size = 138.dp)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PACK PROTECTOR",
                        color = ArcticFrost,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Benefactor of $organizationName",
                        color = GlacialCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = SummitGold, modifier = Modifier.size(16.dp))
                            Text(
                                text = "PERMANENT IN-APP PERKS",
                                color = SummitGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.0.sp
                            )
                        }
                        Text(
                            text = "• Golden Wolf Paw flair on all leaderboards\n• +5% Territory Morale in The Living Pack Chronicles\n• Exclusive 'Choir of the Ancients' howl stinger",
                            color = ArcticFrost.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEquipBadge()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlacialCyan,
                        contentColor = Color(0xFF0A0F1D)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("equip_pack_protector_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "EQUIP FLAIR & CLAIM PERKS",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.1.sp
                    )
                }
            }
        }
    }
}
