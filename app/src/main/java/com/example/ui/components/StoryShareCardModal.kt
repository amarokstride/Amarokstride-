package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.GeoPointEle
import com.example.ui.theme.AlpineMist
import com.example.ui.theme.ArcticFrost
import com.example.ui.theme.GlacialCyan
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

data class StorySharePayload(
    val distanceStr: String,
    val subtitleStr: String = "WINTER TRAIL RUN",
    val durationStr: String,
    val paceStr: String,
    val elevationStr: String,
    val badgeTitle: String = "SUMMIT ALPHA",
    val routePoints: List<GeoPointEle> = emptyList(),
    val athleteName: String = "Wolf Strider"
)

/**
 * Full-screen 9:16 Runner Story & Social Share Card Generator (`striders_howl_story_share_card.png`).
 * Renders live preview with frosted winter mountain backdrop, glowing cyan GPS route polyline,
 * bold runner metrics, earned milestone crest, and exports a crisp 1080x1920 PNG via FileProvider
 * for Instagram Stories, Strava, and messaging apps (zero storage permissions).
 */
@Composable
fun StoryShareCardModal(
    payload: StorySharePayload,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060A14))
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .testTag("story_share_card_modal")
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "9:16 STORY SHARE CARD",
                    color = ArcticFrost,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.6.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("story_share_close_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Story Share Card", tint = ArcticFrost)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 9:16 Story Card Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0F1D))
                    .border(1.5.dp, GlacialCyan.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            ) {
                // Atmospheric frosted winter mountain backdrop
                Image(
                    painter = painterResource(R.drawable.img_trail_hero_1795302667119),
                    contentDescription = "Winter Mountain Peaks Backdrop",
                    contentScale = ContentScale.Crop,
                    alpha = 0.24f,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xCC060C18),
                                    Color(0x990A1424),
                                    Color(0xEE060A14)
                                )
                            )
                        )
                )

                // Glowing Cyan GPS Route Polyline + Ridge Silhouette Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Frosted alpine ridge backdrop silhouette
                    val ridge = Path().apply {
                        moveTo(0f, h * 0.72f)
                        lineTo(w * 0.22f, h * 0.58f)
                        lineTo(w * 0.44f, h * 0.66f)
                        lineTo(w * 0.70f, h * 0.52f)
                        lineTo(w, h * 0.65f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = ridge,
                        brush = Brush.verticalGradient(
                            colors = listOf(GlacialCyan.copy(alpha = 0.14f), Color.Transparent),
                            startY = h * 0.52f,
                            endY = h
                        )
                    )

                    // Glowing GPS Trail Polyline (from real route points or fallback winter ridge path)
                    val routePath = Path()
                    if (payload.routePoints.size >= 2) {
                        val minLat = payload.routePoints.minOf { it.latitude }
                        val maxLat = payload.routePoints.maxOf { it.latitude }
                        val minLon = payload.routePoints.minOf { it.longitude }
                        val maxLon = payload.routePoints.maxOf { it.longitude }
                        val latSpan = max(maxLat - minLat, 0.0005)
                        val lonSpan = max(maxLon - minLon, 0.0005)

                        payload.routePoints.forEachIndexed { idx, pt ->
                            val nx = ((pt.longitude - minLon) / lonSpan).toFloat()
                            val ny = 1f - ((pt.latitude - minLat) / latSpan).toFloat()
                            val px = w * 0.14f + nx * (w * 0.72f)
                            val py = h * 0.50f + ny * (h * 0.20f)
                            if (idx == 0) routePath.moveTo(px, py) else routePath.lineTo(px, py)
                        }
                    } else {
                        routePath.moveTo(w * 0.14f, h * 0.67f)
                        routePath.cubicTo(w * 0.30f, h * 0.56f, w * 0.48f, h * 0.64f, w * 0.62f, h * 0.54f)
                        routePath.quadraticTo(w * 0.76f, h * 0.49f, w * 0.86f, h * 0.59f)
                    }

                    // Outer neon glow
                    drawPath(
                        path = routePath,
                        color = GlacialCyan.copy(alpha = 0.28f),
                        style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Inner crisp cyan vector line
                    drawPath(
                        path = routePath,
                        color = GlacialCyan,
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Foreground Story Typography & Metrics
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 28.dp)
                ) {
                    // Brand Watermark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_striders_howl_logo),
                            contentDescription = "Striders Howl Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .border(1.2.dp, GlacialCyan, CircleShape)
                        )
                        Text(
                            text = "STRIDERS HOWL",
                            color = ArcticFrost,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.0.sp
                        )
                    }

                    // Hero Distance + 3-Column Telemetry Grid
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = payload.distanceStr,
                            color = ArcticFrost,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = payload.subtitleStr,
                            color = GlacialCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.0.sp
                        )
                        Spacer(Modifier.height(22.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StoryStatCol("TIME", payload.durationStr)
                            StoryStatCol("AVG PACE", payload.paceStr)
                            StoryStatCol("ELEV GAIN", payload.elevationStr)
                        }
                    }

                    // Bottom Earned Milestone Crest + Hashtags
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GlacialCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, GlacialCyan.copy(alpha = 0.55f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MilitaryTech,
                                    contentDescription = null,
                                    tint = GlacialCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = payload.badgeTitle.uppercase(),
                                    color = ArcticFrost,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.0.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Pets, contentDescription = null, tint = GlacialCyan.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                            Text(
                                text = "#RunWithThePack · #WildWolfConservation",
                                color = ArcticFrost.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                                letterSpacing = 1.0.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Export 1080x1920 PNG & Share CTA
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    StoryCardShareExporter.exportAndShare(context, payload)
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlacialCyan,
                    contentColor = Color(0xFF0A0F1D)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("story_share_export_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SHARE TO INSTAGRAM & STRAVA",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
private fun StoryStatCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AlpineMist, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = ArcticFrost, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

/**
 * Renders a crisp 1080x1920 (9:16) PNG Runner Story Card to cacheDir/shared_milestones/
 * and launches Android's zero-permission FileProvider share sheet.
 */
object StoryCardShareExporter {
    private const val TAG = "StoryCardShareExporter"

    fun exportAndShare(context: Context, payload: StorySharePayload): Result<Unit> {
        return runCatching {
            val bitmap = renderStoryBitmap(context, payload)
            val dir = File(context.cacheDir, "shared_milestones").apply { if (!exists()) mkdirs() }
            val file = File(dir, "striders_howl_story.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareText = "Ran ${payload.distanceStr} (${payload.durationStr} · ${payload.paceStr} · ${payload.elevationStr}) with the pack. #StridersHowl #RunWithThePack #WildWolfConservation"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Striders Howl Story Card").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.onFailure { err ->
            Log.e(TAG, "Failed to export story card", err)
        }
    }

    private fun renderStoryBitmap(context: Context, payload: StorySharePayload): Bitmap {
        val w = 1080
        val h = 1920
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)

        // Deep Midnight Navy gradient backdrop
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(
                    AndroidColor.parseColor("#070C18"),
                    AndroidColor.parseColor("#0E1A2D"),
                    AndroidColor.parseColor("#060A14")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Subtle winter mountain hero overlay
        val heroBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_trail_hero_1795302667119)
        if (heroBmp != null) {
            val alphaPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = 55 }
            canvas.drawBitmap(heroBmp, null, Rect(0, 0, w, h), alphaPaint)
        }

        // Frosted cyan outer frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#4400E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRoundRect(RectF(36f, 36f, w - 36f, h - 36f), 48f, 48f, framePaint)

        // Glowing GPS Route Polyline
        val routePath = AndroidPath()
        if (payload.routePoints.size >= 2) {
            val minLat = payload.routePoints.minOf { it.latitude }
            val maxLat = payload.routePoints.maxOf { it.latitude }
            val minLon = payload.routePoints.minOf { it.longitude }
            val maxLon = payload.routePoints.maxOf { it.longitude }
            val latSpan = max(maxLat - minLat, 0.0005)
            val lonSpan = max(maxLon - minLon, 0.0005)
            payload.routePoints.forEachIndexed { idx, pt ->
                val nx = ((pt.longitude - minLon) / lonSpan).toFloat()
                val ny = 1f - ((pt.latitude - minLat) / latSpan).toFloat()
                val px = w * 0.14f + nx * (w * 0.72f)
                val py = h * 0.56f + ny * (h * 0.18f)
                if (idx == 0) routePath.moveTo(px, py) else routePath.lineTo(px, py)
            }
        } else {
            routePath.moveTo(w * 0.14f, h * 0.70f)
            routePath.cubicTo(w * 0.30f, h * 0.58f, w * 0.50f, h * 0.67f, w * 0.65f, h * 0.57f)
            routePath.quadTo(w * 0.78f, h * 0.52f, w * 0.86f, h * 0.62f)
        }
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#4400E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 28f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 9f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(routePath, glowPaint)
        canvas.drawPath(routePath, linePaint)

        // Brand Watermark Header
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 44f
            letterSpacing = 0.22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("STRIDERS HOWL", w / 2f, 190f, brandPaint)

        // Hero Distance
        val heroDistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 152f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(payload.distanceStr, w / 2f, 670f, heroDistPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#00E5FF")
            textSize = 40f
            letterSpacing = 0.18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(payload.subtitleStr, w / 2f, 750f, subPaint)

        // 3-Column Metrics Row
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#8FA8C2")
            textSize = 30f
            letterSpacing = 0.1f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cols = listOf(
            Triple("TIME", payload.durationStr, w * 0.20f),
            Triple("AVG PACE", payload.paceStr, w * 0.50f),
            Triple("ELEV GAIN", payload.elevationStr, w * 0.80f)
        )
        cols.forEach { (lbl, v, cx) ->
            canvas.drawText(lbl, cx, 895f, labelPaint)
            canvas.drawText(v, cx, 960f, valPaint)
        }

        // Milestone Badge Pill
        val pillRect = RectF(170f, 1560f, w - 170f, 1665f)
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.parseColor("#2400E5FF") }
        val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#8800E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(pillRect, 52f, 52f, pillBg)
        canvas.drawRoundRect(pillRect, 52f, 52f, pillBorder)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 34f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(payload.badgeTitle.uppercase(), w / 2f, 1626f, badgePaint)

        // Hashtag Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#99FFFFFF")
            textSize = 30f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("#RunWithThePack • #WildWolfConservation", w / 2f, 1750f, footerPaint)
        canvas.drawText(payload.athleteName, w / 2f, 1810f, labelPaint)

        return bmp
    }
}
