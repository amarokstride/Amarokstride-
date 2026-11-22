package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.DistanceUnit
import com.example.data.model.PackCrest
import com.example.data.model.PackRankingEntry
import com.example.data.model.SummitFormatters
import com.example.ui.theme.AmberGaze
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.AuroraMint
import com.example.ui.theme.BasaltObsidian
import com.example.ui.theme.FrostedRim
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.MidnightSlate
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DEVELOPER_SUPPORT_EMAIL = "amarokstride@gmail.com"

enum class WolfFeatureKind(val label: String, val icon: ImageVector, val accent: Color) {
    PACE_SPEED("Pace & Speed", Icons.Filled.Speed, GlacialCyan),
    TRAIL_MAP("Trail Map", Icons.Filled.Landscape, AuroraMint),
    HEART_ENDURANCE("Endurance", Icons.Filled.Favorite, GlacialCyan),
    AUDIO_HOWL("Audio Howl", Icons.Filled.GraphicEq, Color(0xFF9DF8FF)),
    PACK_PARTY("Wolf Pack", Icons.Filled.Groups, AuroraMint)
}

@Composable
fun WolfFeatureTileIcon(
    kind: WolfFeatureKind,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MidnightSlate,
        border = BorderStroke(1.5.dp, kind.accent.copy(alpha = 0.72f)),
        modifier = modifier.size(size)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(
                Brush.radialGradient(
                    colors = listOf(kind.accent.copy(alpha = 0.24f), BasaltObsidian)
                )
            )
        ) {
            Icon(
                imageVector = kind.icon,
                contentDescription = kind.label,
                tint = kind.accent,
                modifier = Modifier.size(size * 0.54f)
            )
        }
    }
}

enum class CrestShape { HEXAGON, CIRCLE, SHIELD }

data class WolfCrestStyle(
    val shape: CrestShape,
    val primaryRim: Color,
    val secondaryGlow: Color,
    val topTag: String,
    val icon: ImageVector
)

fun resolveWolfCrestStyle(badgeKey: String): WolfCrestStyle = when {
    badgeKey.contains("100") || badgeKey.contains("centurion") || badgeKey.contains("apex") -> WolfCrestStyle(
        shape = CrestShape.SHIELD,
        primaryRim = AmberGaze,
        secondaryGlow = GlacialCyan,
        topTag = "100M",
        icon = Icons.Filled.WorkspacePremium
    )
    badgeKey.contains("50k") || badgeKey.contains("fifty") || badgeKey.contains("ironhearth") -> WolfCrestStyle(
        shape = CrestShape.HEXAGON,
        primaryRim = Color(0xFFD59B62),
        secondaryGlow = GlacialCyan,
        topTag = "50K",
        icon = Icons.Filled.Landscape
    )
    badgeKey.contains("marathon_26") -> WolfCrestStyle(
        shape = CrestShape.SHIELD,
        primaryRim = AuroraMint,
        secondaryGlow = ArcticFrost,
        topTag = "26.2",
        icon = Icons.Filled.NightsStay
    )
    badgeKey.contains("half_marathon") || badgeKey.contains("warden") -> WolfCrestStyle(
        shape = CrestShape.HEXAGON,
        primaryRim = Color(0xFFD8E9F5),
        secondaryGlow = GlacialCyan,
        topTag = "13.1",
        icon = Icons.Filled.MilitaryTech
    )
    badgeKey.contains("10k") || badgeKey.contains("ten_leagues") -> WolfCrestStyle(
        shape = CrestShape.CIRCLE,
        primaryRim = AuroraMint,
        secondaryGlow = Color(0xFFC9F5FF),
        topTag = "10K",
        icon = Icons.Filled.Pets
    )
    badgeKey.contains("5k") || badgeKey.contains("first_frost") || badgeKey.contains("horizon") -> WolfCrestStyle(
        shape = CrestShape.HEXAGON,
        primaryRim = GlacialCyan,
        secondaryGlow = Color(0xFFBCEFFF),
        topTag = "5K",
        icon = Icons.Filled.Pets
    )
    badgeKey.contains("sub_7") || badgeKey.contains("windrunner") || badgeKey.contains("pace") -> WolfCrestStyle(
        shape = CrestShape.CIRCLE,
        primaryRim = AmberGaze,
        secondaryGlow = GlacialCyan,
        topTag = "PB",
        icon = Icons.Filled.Bolt
    )
    badgeKey.contains("dragonspine") || badgeKey.contains("endurance") || badgeKey.contains("climber") -> WolfCrestStyle(
        shape = CrestShape.HEXAGON,
        primaryRim = GlacialCyan,
        secondaryGlow = AmberGaze,
        topTag = "PEAK",
        icon = Icons.Filled.Explore
    )
    badgeKey.contains("weekly") || badgeKey.contains("eternal") -> WolfCrestStyle(
        shape = CrestShape.HEXAGON,
        primaryRim = GlacialCyan,
        secondaryGlow = AmberGaze,
        topTag = "PACK",
        icon = Icons.Filled.Timer
    )
    else -> WolfCrestStyle(
        shape = CrestShape.CIRCLE,
        primaryRim = GlacialCyan,
        secondaryGlow = AuroraMint,
        topTag = "HOWL",
        icon = Icons.Filled.Pets
    )
}

@Composable
fun WolfAchievementCrest(
    badgeKey: String,
    unlocked: Boolean = true,
    size: Dp = 74.dp,
    modifier: Modifier = Modifier
) {
    val style = resolveWolfCrestStyle(badgeKey)
    val rim = if (unlocked) style.primaryRim else FrostedRim
    val glow = if (unlocked) style.secondaryGlow else AlpineMist.copy(alpha = 0.45f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = (this.size.minDimension / 2f) - 4f

            if (unlocked) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(rim.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r * 1.15f
                    ),
                    radius = r * 1.15f,
                    center = Offset(cx, cy)
                )
            }

            when (style.shape) {
                CrestShape.CIRCLE -> {
                    drawCircle(color = MidnightSlate, radius = r, center = Offset(cx, cy))
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(rim, glow, rim)),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = if (unlocked) 6f else 3f)
                    )
                    drawCircle(
                        color = rim.copy(alpha = 0.35f),
                        radius = r - 7f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                }

                CrestShape.HEXAGON -> {
                    val path = Path()
                    for (i in 0 until 6) {
                        val angle = (PI / 3.0 * i - PI / 2.0)
                        val px = cx + (r * cos(angle)).toFloat()
                        val py = cy + (r * sin(angle)).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, color = MidnightSlate)
                    drawPath(
                        path,
                        brush = Brush.linearGradient(listOf(rim, glow, rim)),
                        style = Stroke(width = if (unlocked) 6f else 3f)
                    )
                }

                CrestShape.SHIELD -> {
                    val path = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r * 0.88f, cy - r * 0.62f)
                        lineTo(cx + r * 0.80f, cy + r * 0.25f)
                        quadraticTo(cx + r * 0.50f, cy + r * 0.78f, cx, cy + r)
                        quadraticTo(cx - r * 0.50f, cy + r * 0.78f, cx - r * 0.80f, cy + r * 0.25f)
                        lineTo(cx - r * 0.88f, cy - r * 0.62f)
                        close()
                    }
                    drawPath(path, color = MidnightSlate)
                    drawPath(
                        path,
                        brush = Brush.verticalGradient(listOf(rim, glow, rim)),
                        style = Stroke(width = if (unlocked) 6f else 3f)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Text(
                text = style.topTag,
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) rim else AlpineMist,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Icon(
                imageVector = if (unlocked) style.icon else Icons.Filled.Lock,
                contentDescription = style.topTag,
                tint = if (unlocked) glow else AlpineMist.copy(alpha = 0.55f),
                modifier = Modifier.size(size * 0.34f)
            )
        }
    }
}

@Composable
fun ProtectWildPackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MidnightSlate,
        border = BorderStroke(
            width = 1.8.dp,
            brush = Brush.horizontalGradient(listOf(GlacialCyan, AuroraMint))
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF172C3A), Color(0xFF102430))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = "Wolf paw conservation icon",
                tint = GlacialCyan,
                modifier = Modifier.size(34.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PROTECT THE WILD PACK",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArcticFrost
                )
                Text(
                    text = "Donate to Wolf Conservation & Habitat Preservation",
                    style = MaterialTheme.typography.labelSmall,
                    color = AlpineMist
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GlacialCyan
            ) {
                Text(
                    text = "DONATE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = BasaltObsidian,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun FuelDeveloperButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MidnightSlate,
        border = BorderStroke(
            width = 1.8.dp,
            brush = Brush.horizontalGradient(listOf(AmberGaze, GlacialCyan))
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF282217), Color(0xFF13222E))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Support developer icon",
                tint = AmberGaze,
                modifier = Modifier.size(30.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FUEL THE DEVELOPER",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArcticFrost
                )
                Text(
                    text = "Support Indie App Development & Trail Features",
                    style = MaterialTheme.typography.labelSmall,
                    color = AlpineMist
                )
            }
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = GlacialCyan,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Contact Developer frosted-glass button matching the Striders Howl Resting & Active Pressed specs:
 * - Resting state: Frosted obsidian glass with subtle Frosted Rim border & glowing cyan paw+envelope icon
 * - Pressed state: Animates to radiant Glacial Cyan (#00F2FE) perimeter glow & elevated shadow halo
 */
@Composable
fun ContactDeveloperButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isPressed) GlacialCyan else FrostedRim.copy(alpha = 0.85f),
        animationSpec = tween(160),
        label = "contactDevBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isPressed) 2.2.dp else 1.2.dp,
        animationSpec = tween(160),
        label = "contactDevBorderWidth"
    )
    val shadowGlow by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 4.dp,
        animationSpec = tween(160),
        label = "contactDevGlow"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .shadow(
                elevation = shadowGlow,
                shape = RoundedCornerShape(18.dp),
                ambientColor = if (isPressed) GlacialCyan else Color.Black,
                spotColor = if (isPressed) GlacialCyan else Color.Black
            )
            .testTag("contact_developer_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        if (isPressed) {
                            listOf(Color(0xFF1F3847), Color(0xFF14242F))
                        } else {
                            listOf(Color(0xFF28323D), Color(0xFF1B232D))
                        }
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            WolfPawEnvelopeEmblem(
                isGlowing = isPressed,
                modifier = Modifier.size(56.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Contact Developer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ArcticFrost
                )
                Text(
                    text = "Report trail bugs, request features, or send feedback",
                    style = MaterialTheme.typography.bodySmall,
                    color = AlpineMist
                )
            }
        }
    }
}

@Composable
private fun WolfPawEnvelopeEmblem(
    isGlowing: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ambient cyan frost aura behind the wolf paw
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    GlacialCyan.copy(alpha = if (isGlowing) 0.55f else 0.34f),
                    AuroraMint.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(w * 0.42f, h * 0.54f),
                radius = w * 0.58f
            ),
            radius = w * 0.58f,
            center = Offset(w * 0.42f, h * 0.54f)
        )

        // Envelope outline on right side
        val envLeft = w * 0.42f
        val envTop = h * 0.34f
        val envW = w * 0.52f
        val envH = h * 0.48f
        val strokeBrush = Brush.linearGradient(listOf(GlacialCyan, Color(0xFFBDFBFF), AuroraMint))

        drawRoundRect(
            brush = strokeBrush,
            topLeft = Offset(envLeft, envTop),
            size = Size(envW, envH),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Envelope V-flap
        val flapPath = Path().apply {
            moveTo(envLeft + envW * 0.08f, envTop + envH * 0.10f)
            lineTo(envLeft + envW * 0.52f, envTop + envH * 0.54f)
            lineTo(envLeft + envW * 0.94f, envTop + envH * 0.10f)
        }
        drawPath(
            path = flapPath,
            brush = strokeBrush,
            style = Stroke(width = w * 0.05f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Frosty glowing Wolf Paw (4 toe pads + claws + main heel pad)
        val pawBrush = Brush.verticalGradient(listOf(Color(0xFFE8FDFF), GlacialCyan, AuroraMint))

        // Main palm pad
        val palmPath = Path().apply {
            moveTo(w * 0.36f, h * 0.52f)
            cubicTo(w * 0.22f, h * 0.56f, w * 0.14f, h * 0.72f, w * 0.18f, h * 0.80f)
            cubicTo(w * 0.24f, h * 0.86f, w * 0.32f, h * 0.79f, w * 0.36f, h * 0.79f)
            cubicTo(w * 0.40f, h * 0.79f, w * 0.48f, h * 0.86f, w * 0.54f, h * 0.80f)
            cubicTo(w * 0.58f, h * 0.72f, w * 0.50f, h * 0.56f, w * 0.36f, h * 0.52f)
            close()
        }
        drawPath(palmPath, brush = pawBrush)

        // 4 toe beans
        val toes = listOf(
            Triple(0.14f, 0.50f, 0.075f),
            Triple(0.28f, 0.35f, 0.085f),
            Triple(0.45f, 0.35f, 0.085f),
            Triple(0.58f, 0.50f, 0.075f)
        )
        toes.forEach { (tx, ty, tr) ->
            drawOval(
                brush = pawBrush,
                topLeft = Offset(w * (tx - tr * 0.85f), h * (ty - tr * 1.15f)),
                size = Size(w * tr * 1.7f, h * tr * 2.3f)
            )
            // Tiny glowing wolf claw tip above each toe
            val clawPath = Path().apply {
                moveTo(w * tx, h * (ty - tr * 1.25f))
                lineTo(w * (tx - 0.025f), h * (ty - tr * 1.25f))
                lineTo(w * tx, h * (ty - tr * 2.25f))
                lineTo(w * (tx + 0.025f), h * (ty - tr * 1.25f))
                close()
            }
            drawPath(clawPath, color = Color(0xFFDDFCFF))
        }
    }
}

@Composable
fun ContactDeveloperDialog(
    onDismiss: () -> Unit,
    onFeedbackSent: (String) -> Unit
) {
    val context = LocalContext.current
    val categories = listOf("Trail Bug", "Feature Request", "Feedback")
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var messageText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MidnightSlate,
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(listOf(GlacialCyan, FrostedRim))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF172634), BasaltObsidian))
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WolfPawEnvelopeEmblem(isGlowing = true, modifier = Modifier.size(44.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contact Developer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = ArcticFrost
                        )
                        Text(
                            text = "Striders Howl Trail Support",
                            style = MaterialTheme.typography.labelMedium,
                            color = GlacialCyan
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Describe the trail bug, feature idea, or feedback") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlacialCyan,
                        unfocusedBorderColor = FrostedRim,
                        focusedLabelColor = GlacialCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_dev_message_input")
                )

                Button(
                    onClick = {
                        val subject = "[Striders Howl] $selectedCategory"
                        val body = buildString {
                            appendLine(messageText.trim().ifBlank { "Hi Striders Howl developer," })
                            appendLine()
                            appendLine("---")
                            appendLine("App: Striders Howl")
                            appendLine("Category: $selectedCategory")
                            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
                        }
                        val mailUri = Uri.parse(
                            "mailto:$DEVELOPER_SUPPORT_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
                        )
                        val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send to Striders Howl Developer"))
                            onFeedbackSent("Opened email composer for Striders Howl support.")
                        } catch (_: Exception) {
                            copyEmailToClipboard(context, DEVELOPER_SUPPORT_EMAIL)
                            onFeedbackSent("No email app found — copied $DEVELOPER_SUPPORT_EMAIL to clipboard.")
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlacialCyan,
                        contentColor = BasaltObsidian
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("contact_dev_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send to Developer", fontWeight = FontWeight.ExtraBold)
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            copyEmailToClipboard(context, DEVELOPER_SUPPORT_EMAIL)
                            onFeedbackSent("Support email copied: $DEVELOPER_SUPPORT_EMAIL")
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = AuroraMint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Email", color = AuroraMint)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = AlpineMist)
                    }
                }
            }
        }
    }
}

private fun copyEmailToClipboard(context: Context, email: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("Striders Howl Support Email", email))
}

/**
 * CREATE PACK frosted glass social card matching Striders Howl Social Component Sheet:
 * Glowing Glacial Cyan border, crowned howling wolf emblem, and cyan chevron.
 */
@Composable
fun CreatePackSocialCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val glowElev by animateDpAsState(
        targetValue = if (isPressed) 20.dp else 10.dp,
        animationSpec = tween(150),
        label = "createPackGlow"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 2.dp,
            brush = Brush.horizontalGradient(listOf(GlacialCyan, Color(0xFF7AF9FF), AuroraMint))
        ),
        modifier = modifier
            .shadow(
                elevation = glowElev,
                shape = RoundedCornerShape(18.dp),
                ambientColor = GlacialCyan,
                spotColor = GlacialCyan
            )
            .testTag("create_pack_social_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF152A36), Color(0xFF101D28), Color(0xFF142A37))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            CrownedHowlingWolfEmblem(modifier = Modifier.size(54.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CREATE PACK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArcticFrost
                )
                Text(
                    text = "Form a new running pack and lead the trail",
                    style = MaterialTheme.typography.bodySmall,
                    color = AlpineMist
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Create Pack",
                tint = GlacialCyan,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * JOIN PACK frosted slate social card matching Striders Howl Social Component Sheet:
 * Silver-frost rimmed glass, dual striding wolves silhouette emblem.
 */
@Composable
fun JoinPackSocialCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) GlacialCyan else Color(0xFF5D738A),
        animationSpec = tween(150),
        label = "joinPackBorder"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.6.dp,
            brush = Brush.verticalGradient(listOf(Color(0xFF9AB4CC), borderColor, Color(0xFF7B98B2)))
        ),
        modifier = modifier.testTag("join_pack_social_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF283747), Color(0xFF192431), Color(0xFF21303F))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            DualStridingWolvesEmblem(modifier = Modifier.size(56.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "JOIN PACK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArcticFrost
                )
                Text(
                    text = "Enter an invite code or find local runners",
                    style = MaterialTheme.typography.bodySmall,
                    color = AlpineMist
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Join Pack",
                tint = Color(0xFFA7C6DE),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun CrownedHowlingWolfEmblem(modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "howling_wolf_motion")
    val howlWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "howl_wave"
    )
    val headLift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "howl_head_lift"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cyanGlow = Brush.linearGradient(listOf(Color(0xFFB5FBFF), GlacialCyan, AuroraMint))
        val liftY = headLift * (h * 0.025f)

        // Expanding sonic howl ripples from the snout
        val rippleOrigin = Offset(w * 0.80f, h * 0.24f + liftY)
        drawCircle(
            color = GlacialCyan.copy(alpha = (1f - howlWave) * 0.55f),
            radius = w * (0.08f + howlWave * 0.34f),
            center = rippleOrigin,
            style = Stroke(width = w * 0.032f)
        )
        val wave2 = (howlWave + 0.5f) % 1f
        drawCircle(
            color = AuroraMint.copy(alpha = (1f - wave2) * 0.45f),
            radius = w * (0.08f + wave2 * 0.34f),
            center = rippleOrigin,
            style = Stroke(width = w * 0.026f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GlacialCyan.copy(alpha = 0.26f + 0.10f * ((headLift + 1f) * 0.5f)), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.55f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.5f, h * 0.55f)
        )

        // Small glowing Alpha Crown bobbing with howl
        val crownPath = Path().apply {
            moveTo(w * 0.16f, h * 0.34f + liftY * 1.4f)
            lineTo(w * 0.14f, h * 0.18f + liftY * 1.4f)
            lineTo(w * 0.26f, h * 0.25f + liftY * 1.4f)
            lineTo(w * 0.32f, h * 0.12f + liftY * 1.4f)
            lineTo(w * 0.40f, h * 0.24f + liftY * 1.4f)
            lineTo(w * 0.50f, h * 0.16f + liftY * 1.4f)
            lineTo(w * 0.46f, h * 0.33f + liftY * 1.4f)
            close()
        }
        drawPath(crownPath, brush = cyanGlow)

        // Howling wolf profile line art in motion
        val wolfPath = Path().apply {
            moveTo(w * 0.16f, h * 0.72f)
            quadraticTo(w * 0.30f, h * 0.56f, w * 0.26f, h * 0.42f + liftY)
            lineTo(w * 0.38f, h * 0.42f + liftY)
            lineTo(w * 0.52f, h * 0.26f + liftY)
            lineTo(w * 0.56f, h * 0.36f + liftY)
            lineTo(w * 0.78f, h * 0.20f + liftY * 1.3f)
            lineTo(w * 0.85f, h * 0.28f + liftY * 1.3f)
            lineTo(w * 0.74f, h * 0.41f + liftY)
            lineTo(w * 0.82f, h * 0.47f + liftY)
            lineTo(w * 0.68f, h * 0.58f)
            quadraticTo(w * 0.72f, h * 0.74f, w * 0.56f, h * 0.90f)
            quadraticTo(w * 0.46f, h * 0.76f, w * 0.36f, h * 0.78f)
        }
        drawPath(
            path = wolfPath,
            brush = cyanGlow,
            style = Stroke(width = w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun DualStridingWolvesEmblem(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "striding_wolves_motion")
    val gallopPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 820, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gallop_phase"
    )
    val streakShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stride_streaks"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strideFront = sin(gallopPhase.toDouble()).toFloat() * (w * 0.09f)
        val strideHind = sin((gallopPhase + PI.toFloat()).toDouble()).toFloat() * (w * 0.09f)
        val bodyBounce = cos(gallopPhase.toDouble()).toFloat() * (h * 0.032f)
        val backBounce = cos((gallopPhase + 1.2f).toDouble()).toFloat() * (h * 0.028f)
        val tailFlick = sin(gallopPhase.toDouble()).toFloat() * (h * 0.045f)

        // Kinetic snow speed lines streaming behind running wolves
        for (i in 0..2) {
            val p = (streakShift + i * 0.33f) % 1f
            val y = h * (0.28f + i * 0.26f)
            val xStart = w * (0.45f - p * 0.42f)
            drawLine(
                color = GlacialCyan.copy(alpha = (1f - p) * 0.45f),
                start = Offset(xStart, y),
                end = Offset(xStart - w * 0.16f, y),
                strokeWidth = w * 0.028f,
                cap = StrokeCap.Round
            )
        }

        // Background shadow wolf (galloping offset phase)
        val backWolf = Path().apply {
            moveTo(w * 0.08f, h * 0.44f + backBounce - tailFlick * 0.6f)
            lineTo(w * 0.32f, h * 0.38f + backBounce)
            lineTo(w * 0.52f, h * 0.24f + backBounce)
            lineTo(w * 0.56f, h * 0.16f + backBounce)
            lineTo(w * 0.62f, h * 0.25f + backBounce)
            lineTo(w * 0.76f, h * 0.30f + backBounce)
            lineTo(w * 0.66f, h * 0.40f + backBounce)
            lineTo(w * 0.54f, h * 0.48f + backBounce)
            lineTo(w * 0.24f, h * 0.52f + backBounce)
            close()
        }
        drawPath(backWolf, color = Color(0xFF8EC5E8))

        // Foreground striding white wolf with animated galloping legs
        val frontWolf = Path().apply {
            moveTo(w * 0.05f, h * 0.60f + bodyBounce + tailFlick)
            lineTo(w * 0.26f, h * 0.52f + bodyBounce)
            lineTo(w * 0.56f, h * 0.46f + bodyBounce)
            lineTo(w * 0.72f, h * 0.36f + bodyBounce)
            lineTo(w * 0.76f, h * 0.28f + bodyBounce)
            lineTo(w * 0.82f, h * 0.39f + bodyBounce)
            lineTo(w * 0.96f, h * 0.46f + bodyBounce)
            lineTo(w * 0.84f, h * 0.54f + bodyBounce)
            lineTo(w * 0.74f, h * 0.60f + bodyBounce)
            lineTo(w * 0.84f + strideFront, h * 0.80f)
            lineTo(w * 0.74f + strideFront, h * 0.83f)
            lineTo(w * 0.62f, h * 0.66f + bodyBounce)
            lineTo(w * 0.36f, h * 0.68f + bodyBounce)
            lineTo(w * 0.24f + strideHind, h * 0.84f)
            lineTo(w * 0.15f + strideHind, h * 0.80f)
            lineTo(w * 0.24f, h * 0.62f + bodyBounce)
            close()
        }
        drawPath(frontWolf, color = ArcticFrost)
    }
}

/**
 * Dual Garmin & COROS live telemetry watch companion showcase matching striders_howl_garmin_coros_screens.png.
 */
@Composable
fun WatchCompanionPreviewRow(
    paceLabel: String,
    distanceLabel: String,
    garminConnected: Boolean,
    corosConnected: Boolean,
    onTapGarmin: () -> Unit,
    onTapCoros: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Garmin Watch Face Card
        Surface(
            onClick = onTapGarmin,
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0E1721),
            border = BorderStroke(1.2.dp, if (garminConnected) GlacialCyan else FrostedRim),
            modifier = Modifier
                .weight(1f)
                .testTag("watch_preview_garmin")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A1118))
                        .border(4.dp, Color(0xFF283544), CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF142E3D), BasaltObsidian))
                        )
                        .border(1.2.dp, GlacialCyan.copy(alpha = 0.6f), CircleShape)
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        val w = size.width
                        val h = size.height
                        // Curved glowing GPS route polyline on right side of watch face
                        val route = Path().apply {
                            moveTo(w * 0.56f, h * 0.78f)
                            quadraticTo(w * 0.86f, h * 0.68f, w * 0.76f, h * 0.50f)
                            quadraticTo(w * 0.92f, h * 0.36f, w * 0.78f, h * 0.24f)
                        }
                        drawPath(
                            path = route,
                            brush = Brush.verticalGradient(listOf(GlacialCyan, AuroraMint)),
                            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                        )
                        // Segmented HR Zone bar
                        val segColors = listOf(Color(0xFF2E5A6B), AuroraMint, GlacialCyan, AmberGaze, Color(0xFF334150))
                        segColors.forEachIndexed { idx, col ->
                            drawRoundRect(
                                color = col,
                                topLeft = Offset(w * (0.18f + idx * 0.13f), h * 0.49f),
                                size = Size(w * 0.10f, h * 0.055f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "STRIDERS HOWL",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlacialCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = paceLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ArcticFrost
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "DIST $distanceLabel",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA7F8FF)
                        )
                        Icon(
                            imageVector = Icons.Filled.Pets,
                            contentDescription = "Garmin Trail Paw",
                            tint = GlacialCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = if (garminConnected) "GARMIN · SYNCED" else "GARMIN · TAP TO LINK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (garminConnected) GlacialCyan else AlpineMist
                )
            }
        }

        // COROS Watch Face Card
        Surface(
            onClick = onTapCoros,
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0E1721),
            border = BorderStroke(1.2.dp, if (corosConnected) GlacialCyan else FrostedRim),
            modifier = Modifier
                .weight(1f)
                .testTag("watch_preview_coros")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A2530))
                        .border(4.dp, Color(0xFF425464), CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF152736), BasaltObsidian))
                        )
                        .border(1.2.dp, AuroraMint.copy(alpha = 0.65f), CircleShape)
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        val w = size.width
                        val h = size.height
                        // Elevation Profile Polygon
                        val elevPath = Path().apply {
                            moveTo(w * 0.14f, h * 0.50f)
                            lineTo(w * 0.28f, h * 0.44f)
                            lineTo(w * 0.42f, h * 0.36f)
                            lineTo(w * 0.56f, h * 0.30f)
                            lineTo(w * 0.68f, h * 0.26f)
                            lineTo(w * 0.86f, h * 0.48f)
                            lineTo(w * 0.86f, h * 0.52f)
                            lineTo(w * 0.14f, h * 0.52f)
                            close()
                        }
                        drawPath(
                            path = elevPath,
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF4CE89D).copy(alpha = 0.72f), AuroraMint.copy(alpha = 0.28f))
                            )
                        )
                        // Stamina Arc Gauge
                        drawArc(
                            color = FrostedRim,
                            startAngle = 205f,
                            sweepAngle = 130f,
                            useCenter = false,
                            topLeft = Offset(w * 0.14f, h * 0.56f),
                            size = Size(w * 0.72f, h * 0.50f),
                            style = Stroke(width = 7f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.horizontalGradient(listOf(GlacialCyan, Color(0xFF56F4B8))),
                            startAngle = 205f,
                            sweepAngle = 92f,
                            useCenter = false,
                            topLeft = Offset(w * 0.14f, h * 0.56f),
                            size = Size(w * 0.72f, h * 0.50f),
                            style = Stroke(width = 7f, cap = StrokeCap.Round)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .size(104.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "STRIDERS HOWL",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArcticFrost,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "STAMINA",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlacialCyan,
                            fontWeight = FontWeight.Bold
                        )
                        DualStridingWolvesEmblem(modifier = Modifier.size(28.dp))
                    }
                }
                Text(
                    text = if (corosConnected) "COROS · SYNCED" else "COROS · TAP TO LINK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (corosConnected) GlacialCyan else AlpineMist
                )
            }
        }
    }
}

/**
 * Create Your Pack frosted modal matching striders_howl_create_pack_modal.png:
 * Pack Name, Pack Crest carousel, Collective Weekly Distance slider (250 Miles default),
 * Public Pack / Invite Code Only toggles, and glowing Glacial Cyan "Launch Pack >" button.
 */
@Composable
fun CreatePackModalDialog(
    unit: DistanceUnit,
    onDismiss: () -> Unit,
    onLaunchPack: (packName: String, crest: PackCrest, weeklyGoalMiles: Double, isPublic: Boolean) -> Unit
) {
    val crests = PackCrest.entries
    var packName by remember { mutableStateOf("Timberline Striders") }
    var selectedCrestIdx by remember { mutableIntStateOf(0) }
    var weeklyGoalMiles by remember { mutableFloatStateOf(250f) }
    var isPublicPack by remember { mutableStateOf(false) }

    val selectedCrest = crests[selectedCrestIdx]
    val prevCrest = crests[(selectedCrestIdx - 1 + crests.size) % crests.size]
    val nextCrest = crests[(selectedCrestIdx + 1) % crests.size]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MidnightSlate,
            border = BorderStroke(1.5.dp, Brush.verticalGradient(listOf(GlacialCyan.copy(alpha = 0.8f), FrostedRim))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A2836), Color(0xFF121C27), BasaltObsidian)
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Top grabber pill
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(FrostedRim)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Create Your Pack",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF9DF7FF)
                    )
                    Text(
                        text = "Rally runners and lead the winter trails",
                        style = MaterialTheme.typography.bodySmall,
                        color = AlpineMist
                    )
                }

                // Pack Name input
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pack Name",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ArcticFrost
                    )
                    OutlinedTextField(
                        value = packName,
                        onValueChange = { packName = it.take(60) },
                        placeholder = { Text("Timberline Striders") },
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit pack name", tint = GlacialCyan)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlacialCyan,
                            unfocusedBorderColor = GlacialCyan.copy(alpha = 0.65f),
                            focusedContainerColor = Color(0xFF152936),
                            unfocusedContainerColor = Color(0xFF132430),
                            focusedTextColor = ArcticFrost,
                            unfocusedTextColor = ArcticFrost
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_pack_name_input")
                    )
                }

                // Select Pack Crest carousel
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Select Pack Crest · ${selectedCrest.displayName}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ArcticFrost
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { selectedCrestIdx = (selectedCrestIdx - 1 + crests.size) % crests.size },
                            modifier = Modifier.testTag("pack_crest_prev")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous crest", tint = AlpineMist)
                        }

                        PackCrestAvatarBubble(
                            crest = prevCrest,
                            isSelected = false,
                            onClick = { selectedCrestIdx = (selectedCrestIdx - 1 + crests.size) % crests.size }
                        )
                        PackCrestAvatarBubble(
                            crest = selectedCrest,
                            isSelected = true,
                            onClick = {}
                        )
                        PackCrestAvatarBubble(
                            crest = nextCrest,
                            isSelected = false,
                            onClick = { selectedCrestIdx = (selectedCrestIdx + 1) % crests.size }
                        )

                        IconButton(
                            onClick = { selectedCrestIdx = (selectedCrestIdx + 1) % crests.size },
                            modifier = Modifier.testTag("pack_crest_next")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next crest", tint = AlpineMist)
                        }
                    }
                }

                // Pack Goal box (Collective Weekly Distance)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pack Goal",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ArcticFrost
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF121E2A),
                        border = BorderStroke(1.dp, FrostedRim),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Collective Weekly Distance",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlpineMist
                            )
                            val goalDisplay = if (unit == DistanceUnit.KILOMETERS) {
                                "${(weeklyGoalMiles * 1.60934).roundToInt()} km"
                            } else {
                                "${weeklyGoalMiles.roundToInt()} Miles"
                            }
                            Text(
                                text = goalDisplay,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF8CF7FF)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { weeklyGoalMiles = (weeklyGoalMiles - 25f).coerceAtLeast(25f) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(FrostedRim.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease goal", tint = GlacialCyan, modifier = Modifier.size(16.dp))
                                }
                                Slider(
                                    value = weeklyGoalMiles,
                                    onValueChange = { weeklyGoalMiles = (it / 5f).roundToInt() * 5f },
                                    valueRange = 25f..500f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFD6FEFF),
                                        activeTrackColor = GlacialCyan,
                                        inactiveTrackColor = FrostedRim
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { weeklyGoalMiles = (weeklyGoalMiles + 25f).coerceAtMost(500f) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(FrostedRim.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase goal", tint = GlacialCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("100", style = MaterialTheme.typography.labelSmall, color = AlpineMist)
                                Text("200", style = MaterialTheme.typography.labelSmall, color = AlpineMist)
                                Text("300", style = MaterialTheme.typography.labelSmall, color = AlpineMist)
                            }
                        }
                    }
                }

                // Privacy switches
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Privacy",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ArcticFrost
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Public Pack", style = MaterialTheme.typography.bodyMedium, color = ArcticFrost)
                        Switch(
                            checked = isPublicPack,
                            onCheckedChange = { isPublicPack = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ArcticFrost,
                                checkedTrackColor = GlacialCyan
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Invite Code Only", style = MaterialTheme.typography.bodyMedium, color = ArcticFrost)
                            Text(
                                text = "Only users with the unique code can join your pack.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlpineMist
                            )
                        }
                        Switch(
                            checked = !isPublicPack,
                            onCheckedChange = { isPublicPack = !it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ArcticFrost,
                                checkedTrackColor = GlacialCyan
                            )
                        )
                    }
                }

                // Glowing Glacial Cyan "Launch Pack >" pill button
                Button(
                    onClick = {
                        onLaunchPack(
                            packName.trim().ifBlank { "Timberline Striders" },
                            selectedCrest,
                            weeklyGoalMiles.toDouble(),
                            isPublicPack
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlacialCyan,
                        contentColor = BasaltObsidian
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(14.dp, RoundedCornerShape(26.dp), ambientColor = GlacialCyan, spotColor = GlacialCyan)
                        .testTag("launch_pack_button")
                ) {
                    Text(
                        text = "Launch Pack",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AlpineMist)
                }
            }
        }
    }
}

@Composable
private fun PackCrestAvatarBubble(
    crest: PackCrest,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val size = if (isSelected) 76.dp else 52.dp
    val accent = Color(crest.accentHex)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        accent.copy(alpha = if (isSelected) 0.36f else 0.14f),
                        MidnightSlate
                    )
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.2.dp,
                color = if (isSelected) GlacialCyan else FrostedRim,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(R.drawable.img_striders_howl_logo),
            contentDescription = crest.displayName,
            alpha = if (isSelected) 1f else 0.65f,
            modifier = Modifier
                .size(size - 10.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun JoinPackModalDialog(
    rankedPacks: List<PackRankingEntry>,
    unit: DistanceUnit,
    onDismiss: () -> Unit,
    onJoinPackCode: (String) -> Unit
) {
    var codeInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MidnightSlate,
            border = BorderStroke(1.5.dp, Brush.verticalGradient(listOf(AuroraMint, FrostedRim))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(Color(0xFF192634), BasaltObsidian)))
                    .padding(20.dp)
            ) {
                Text(
                    text = "Join a Wolf Pack",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArcticFrost
                )
                Text(
                    text = "Enter your partner's invite code or pick an active trail pack below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AlpineMist
                )

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase(Locale.US) },
                    label = { Text("Pack Invite Code (e.g. WOLF-4821)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlacialCyan,
                        unfocusedBorderColor = FrostedRim
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_pack_modal_code_input")
                )

                Button(
                    onClick = {
                        onJoinPackCode(codeInput)
                        onDismiss()
                    },
                    enabled = codeInput.trim().length >= 6,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlacialCyan,
                        contentColor = BasaltObsidian
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("join_pack_modal_submit")
                ) {
                    Text("Join with Invite Code", fontWeight = FontWeight.ExtraBold)
                }

                if (rankedPacks.isNotEmpty()) {
                    Text(
                        text = "Active Trail Packs",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AuroraMint
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        rankedPacks.take(5).forEach { pack ->
                            Surface(
                                onClick = {
                                    onJoinPackCode(pack.partyCode)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF152330),
                                border = BorderStroke(1.dp, FrostedRim),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pack.packName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ArcticFrost,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Code ${pack.partyCode} · ${pack.memberCount} member(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AlpineMist
                                        )
                                    }
                                    Text(
                                        text = SummitFormatters.formatDistanceShort(pack.combinedMiles, unit),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = GlacialCyan
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun DonationThankYouDialog(
    causeLabel: String,
    onDismiss: () -> Unit,
    onOpenSupportLink: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MidnightSlate,
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(listOf(GlacialCyan, FrostedRim))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF122836),
                                BasaltObsidian,
                                Color(0xFF102430)
                            )
                        )
                    )
                    .padding(horizontal = 22.dp, vertical = 24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF39E6B8).copy(alpha = 0.42f),
                                    GlacialCyan.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_striders_howl_logo),
                        contentDescription = "Aurora Wolf Guardian",
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .border(2.dp, GlacialCyan, CircleShape)
                    )
                }

                Text(
                    text = "Thank You,\nPack Member!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArcticFrost,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your contribution helps protect wild wolf habitats and fuels future trails.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AlpineMist,
                    textAlign = TextAlign.Center
                )

                // Wilderness Guardian circular crest medallion
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF132C3C),
                    border = BorderStroke(2.dp, GlacialCyan),
                    modifier = Modifier.size(104.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pets,
                            contentDescription = "Wilderness Guardian Paw",
                            tint = GlacialCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Wilderness\nGuardian",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ArcticFrost,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = causeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = AuroraMint,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        onOpenSupportLink()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlacialCyan,
                        contentColor = BasaltObsidian
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Continue & Return to Run",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Return to Run",
                        color = AlpineMist
                    )
                }
            }
        }
    }
}
