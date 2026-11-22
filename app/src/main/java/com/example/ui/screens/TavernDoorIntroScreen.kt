package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.RunningWolvesBackgroundOverlay
import com.example.ui.theme.SummitGold
import com.example.ui.theme.WolfFrostBlue
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun TavernDoorIntroScreen(
    onPlayDoorCreak: () -> Unit,
    onDoorOpened: () -> Unit
) {
    var isOpening by remember { mutableStateOf(false) }

    val openProgress by animateFloatAsState(
        targetValue = if (isOpening) 1f else 0f,
        animationSpec = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
        label = "tavern_door_swing"
    )

    LaunchedEffect(isOpening) {
        if (isOpening) {
            onPlayDoorCreak()
            delay(1180L)
            onDoorOpened()
        }
    }

    val triggerOpen = {
        if (!isOpening) {
            isOpening = true
        }
    }

    val doorBitmap: ImageBitmap = ImageBitmap.imageResource(
        id = R.drawable.img_tavern_wolf_door_v2_1795317219861
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF4B2B14), Color(0xFF1A1412), Color(0xFF0E141B))
                )
            )
            .clickable(enabled = !isOpening, onClick = triggerOpen)
    ) {
        // Warm tavern hearth glow revealed as the oak wolf doors swing open
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .alpha(openProgress.coerceIn(0f, 1f))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_tavern_quest_banner_1795304781052),
                contentDescription = "Warm old wolf tavern interior",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66121920))
            )
        }

        // Left & right carved oak wolf double doors swinging outward in 3D perspective.
        // Uses exact source-rect pixel slicing on the symmetrical door bitmap so the
        // center seam lines up seamlessly and the howling wolf's head is NEVER cut off.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val screenWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val screenHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

            val bitmapW = doorBitmap.width
            val bitmapH = doorBitmap.height
            val screenAspect = screenWidthPx / screenHeightPx
            val imageAspect = bitmapW.toFloat() / bitmapH.toFloat()

            // Exact full-screen ContentScale.Crop source rect on the door bitmap
            val srcRect = remember(bitmapW, bitmapH, screenWidthPx, screenHeightPx) {
                if (imageAspect > screenAspect) {
                    val cropH = bitmapH
                    val cropW = (bitmapH * screenAspect).roundToInt().coerceIn(2, bitmapW)
                    val cropX = ((bitmapW - cropW) / 2).coerceAtLeast(0)
                    intArrayOf(cropX, 0, cropW, cropH)
                } else {
                    val cropW = bitmapW
                    val cropH = (bitmapW / screenAspect).roundToInt().coerceIn(2, bitmapH)
                    // Slightly favor the upper-center where the wolf's head and moon sit
                    val cropY = ((bitmapH - cropH) * 0.44f).roundToInt().coerceIn(0, (bitmapH - cropH).coerceAtLeast(0))
                    intArrayOf(0, cropY, cropW, cropH)
                }
            }

            val cropX = srcRect[0]
            val cropY = srcRect[1]
            val cropW = srcRect[2]
            val cropH = srcRect[3]
            val leftCropW = cropW / 2
            val rightCropW = cropW - leftCropW

            Row(modifier = Modifier.fillMaxSize()) {
                // Left door leaf: exact pixels [cropX .. cropX + leftCropW)
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            rotationY = -openProgress * 82f
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            cameraDistance = 16f * density.density
                        }
                ) {
                    val dstW = size.width.roundToInt().coerceAtLeast(1)
                    val dstH = size.height.roundToInt().coerceAtLeast(1)
                    drawImage(
                        image = doorBitmap,
                        srcOffset = IntOffset(cropX, cropY),
                        srcSize = IntSize(leftCropW.coerceAtLeast(1), cropH.coerceAtLeast(1)),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(dstW, dstH)
                    )
                }

                // Right door leaf: exact pixels [cropX + leftCropW .. cropX + cropW)
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            rotationY = openProgress * 82f
                            transformOrigin = TransformOrigin(1f, 0.5f)
                            cameraDistance = 16f * density.density
                        }
                ) {
                    val dstW = size.width.roundToInt().coerceAtLeast(1)
                    val dstH = size.height.roundToInt().coerceAtLeast(1)
                    drawImage(
                        image = doorBitmap,
                        srcOffset = IntOffset(cropX + leftCropW, cropY),
                        srcSize = IntSize(rightCropW.coerceAtLeast(1), cropH.coerceAtLeast(1)),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(dstW, dstH)
                    )
                }
            }
        }

        // Animated pack of spirit wolves running across the misty night backdrop
        RunningWolvesBackgroundOverlay(
            intensity = 0.72f * (1f - openProgress * 0.5f),
            showHorizonMist = false,
            modifier = Modifier.fillMaxSize()
        )

        // Welcome banner & Enter CTA overlaid cleanly at top & bottom, leaving the carved wolf clear
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .alpha(1f - openProgress)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xB40E161E),
                        0.24f to Color.Transparent,
                        0.70f to Color.Transparent,
                        1.0f to Color(0xE00E161E)
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(2.dp, WolfFrostBlue),
                    color = Color(0xFF18242E),
                    modifier = Modifier.size(84.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_striders_howl_logo),
                        contentDescription = "Striders Howl wolf crest logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = Color(0xCC211812),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, SummitGold.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = "WELCOME, STRIDER",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF9E5BD),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Striders Howl",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFFF6F2E9),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Step inside the old wolf tavern to claim your quests, train with your pack, and howl at the summit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD5E2EA),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = triggerOpen,
                    enabled = !isOpening,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF23394A),
                        contentColor = Color(0xFFE6F8FF)
                    ),
                    border = BorderStroke(1.5.dp, WolfFrostBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("enter_tavern_door_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MeetingRoom,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = if (isOpening) "Opening Tavern Doors..." else "Open the Wolf Tavern Door",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
