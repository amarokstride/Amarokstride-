package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.GlacialCyan
import kotlinx.coroutines.delay

/**
 * Cinematic Striders Howl launch splash screen with pulsing glacial cyan wolf crest,
 * synced atmospheric lone wolf opening howl + 3-stage haptics, and smooth auto-transition.
 */
@Composable
fun StridersSplashScreen(
    onTriggerOpeningHowl: () -> Unit,
    onFinished: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "striders_splash_pulse")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.22f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crest_glow"
    )
    val crestScale by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crest_scale"
    )

    LaunchedEffect(Unit) {
        onTriggerOpeningHowl()
        delay(3800L)
        onFinished()
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onFinished
            )
            .testTag("striders_splash_screen")
    ) {
        // Subtle frosty radial ambient aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlacialCyan.copy(alpha = 0.16f * glowAlpha + 0.06f),
                        Color(0xFF4FACFE).copy(alpha = 0.05f * glowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.48f
                ),
                radius = size.minDimension * 0.48f,
                center = center
            )
        }

        // Pulsing Wolf Crest & Brand Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(164.dp)
                    .scale(crestScale)
            ) {
                // Outer animated glacial halo ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GlacialCyan.copy(alpha = 0.48f * glowAlpha),
                                GlacialCyan.copy(alpha = 0.14f * glowAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.5f
                        )
                    )
                }

                Image(
                    painter = painterResource(R.drawable.img_striders_howl_logo),
                    contentDescription = "Striders Howl Wolf Crest",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            color = GlacialCyan.copy(alpha = 0.55f + 0.45f * glowAlpha),
                            shape = CircleShape
                        )
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "STRIDERS HOWL",
                color = ArcticFrost,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.5.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "RUN WITH THE PACK",
                color = GlacialCyan.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.2.sp
            )
        }

        // Wildlife Conservation Initiative Footer Tag
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "WILD WOLF CONSERVATION INITIATIVE",
                color = ArcticFrost.copy(alpha = 0.40f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.8.sp
            )
        }
    }
}
