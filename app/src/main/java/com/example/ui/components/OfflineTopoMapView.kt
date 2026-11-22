package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.maps.OfflineMapTileManager
import com.example.data.model.GeoPointEle
import com.example.ui.theme.MapContourLine
import com.example.ui.theme.MapRouteStroke
import com.example.ui.theme.PinePrimaryLight
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun OfflineTopoMapView(
    tileManager: OfflineMapTileManager,
    centerLat: Double,
    centerLon: Double,
    routePoints: List<GeoPointEle>,
    currentLocation: GeoPointEle?,
    offlineOnlyMode: Boolean,
    modifier: Modifier = Modifier,
    initialZoom: Int = 13,
    interactive: Boolean = true,
    onMapTap: ((GeoPointEle) -> Unit)? = null
) {
    var zoom by remember { mutableIntStateOf(initialZoom.coerceIn(10, 16)) }
    var viewLat by remember(centerLat, centerLon) { mutableDoubleStateOf(centerLat) }
    var viewLon by remember(centerLat, centerLon) { mutableDoubleStateOf(centerLon) }

    val loadedTiles = remember { mutableStateMapOf<String, Bitmap>() }
    val tileSizePx = 256f

    // Determine visible tile window around center
    val centerTileX = OfflineMapTileManager.lonToExactTileX(viewLon, zoom)
    val centerTileY = OfflineMapTileManager.latToExactTileY(viewLat, zoom)
    val minTileX = floor(centerTileX).toInt() - 2
    val maxTileX = floor(centerTileX).toInt() + 2
    val minTileY = floor(centerTileY).toInt() - 2
    val maxTileY = floor(centerTileY).toInt() + 2

    LaunchedEffect(minTileX, maxTileX, minTileY, maxTileY, zoom, offlineOnlyMode) {
        val maxIndex = (1 shl zoom) - 1
        for (tx in minTileX..maxTileX) {
            for (ty in minTileY..maxTileY) {
                if (tx in 0..maxIndex && ty in 0..maxIndex) {
                    val key = "$zoom/$tx/$ty"
                    if (!loadedTiles.containsKey(key)) {
                        val cached = tileManager.getCachedTileSync(zoom, tx, ty)
                        if (cached != null) {
                            loadedTiles[key] = cached
                        } else if (!offlineOnlyMode) {
                            launch {
                                val bmp = tileManager.loadOrFetchTile(zoom, tx, ty, allowNetwork = true)
                                if (bmp != null) {
                                    loadedTiles[key] = bmp
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (onMapTap != null) {
                    Modifier.pointerInput(zoom, onMapTap) {
                        detectTapGestures(
                            onTap = { offset ->
                                val halfW = size.width / 2f
                                val halfH = size.height / 2f
                                val currX = OfflineMapTileManager.lonToExactTileX(viewLon, zoom)
                                val currY = OfflineMapTileManager.latToExactTileY(viewLat, zoom)
                                val tapTileX = currX + ((offset.x - halfW) / tileSizePx)
                                val tapTileY = currY + ((offset.y - halfH) / tileSizePx)
                                val tapLon = OfflineMapTileManager.exactTileXToLon(tapTileX, zoom).coerceIn(-179.9, 179.9)
                                val tapLat = OfflineMapTileManager.exactTileYToLat(tapTileY, zoom).coerceIn(-84.0, 84.0)
                                onMapTap(GeoPointEle(tapLat, tapLon, 450.0))
                            }
                        )
                    }
                } else Modifier
            )
            .then(
                if (interactive) {
                    Modifier.pointerInput(zoom) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val currX = OfflineMapTileManager.lonToExactTileX(viewLon, zoom)
                            val currY = OfflineMapTileManager.latToExactTileY(viewLat, zoom)
                            val newX = currX - (pan.x / tileSizePx)
                            val newY = currY - (pan.y / tileSizePx)
                            viewLon = OfflineMapTileManager.exactTileXToLon(newX, zoom).coerceIn(-179.9, 179.9)
                            viewLat = OfflineMapTileManager.exactTileYToLat(newY, zoom).coerceIn(-84.0, 84.0)
                            if (gestureZoom > 1.25f && zoom < 16) zoom++
                            if (gestureZoom < 0.8f && zoom > 10) zoom--
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val halfW = size.width / 2f
            val halfH = size.height / 2f
            val cTileX = OfflineMapTileManager.lonToExactTileX(viewLon, zoom)
            val cTileY = OfflineMapTileManager.latToExactTileY(viewLat, zoom)

            // 1. Procedural topographic fallback contour grid (ensures clean map look even when offline & uncached)
            drawProceduralTopoBackdrop(cTileX, cTileY)

            // 2. Render cached / downloaded OpenTopoMap / OSM tiles
            val maxIndex = (1 shl zoom) - 1
            for (tx in minTileX..maxTileX) {
                for (ty in minTileY..maxTileY) {
                    if (tx !in 0..maxIndex || ty !in 0..maxIndex) continue
                    val bmp = loadedTiles["$zoom/$tx/$ty"] ?: continue
                    val left = halfW + ((tx - cTileX) * tileSizePx).toFloat()
                    val top = halfH + ((ty - cTileY) * tileSizePx).toFloat()
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(tileSizePx.roundToInt() + 1, tileSizePx.roundToInt() + 1)
                    )
                }
            }

            // 3. Draw recorded route polyline
            if (routePoints.size >= 2) {
                val path = Path()
                routePoints.forEachIndexed { idx, pt ->
                    val px = halfW + ((OfflineMapTileManager.lonToExactTileX(pt.longitude, zoom) - cTileX) * tileSizePx).toFloat()
                    val py = halfH + ((OfflineMapTileManager.latToExactTileY(pt.latitude, zoom) - cTileY) * tileSizePx).toFloat()
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                // Crisp outer halo for contrast over topo tiles
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.85f),
                    style = Stroke(width = 13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = MapRouteStroke,
                    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Start pin (Pine Green) & End pin (Terracotta)
                val first = routePoints.first()
                val startOffset = Offset(
                    halfW + ((OfflineMapTileManager.lonToExactTileX(first.longitude, zoom) - cTileX) * tileSizePx).toFloat(),
                    halfH + ((OfflineMapTileManager.latToExactTileY(first.latitude, zoom) - cTileY) * tileSizePx).toFloat()
                )
                drawCircle(Color.White, radius = 12f, center = startOffset)
                drawCircle(PinePrimaryLight, radius = 8f, center = startOffset)

                val last = routePoints.last()
                val endOffset = Offset(
                    halfW + ((OfflineMapTileManager.lonToExactTileX(last.longitude, zoom) - cTileX) * tileSizePx).toFloat(),
                    halfH + ((OfflineMapTileManager.latToExactTileY(last.latitude, zoom) - cTileY) * tileSizePx).toFloat()
                )
                drawCircle(Color.White, radius = 13f, center = endOffset)
                drawCircle(MapRouteStroke, radius = 9f, center = endOffset)
            }

            // 4. Current live GPS blue/pine beacon
            val loc = currentLocation
            if (loc != null) {
                val lx = halfW + ((OfflineMapTileManager.lonToExactTileX(loc.longitude, zoom) - cTileX) * tileSizePx).toFloat()
                val ly = halfH + ((OfflineMapTileManager.latToExactTileY(loc.latitude, zoom) - cTileY) * tileSizePx).toFloat()
                val center = Offset(lx, ly)
                drawCircle(Color(0x331B75D0), radius = 26f, center = center)
                drawCircle(Color.White, radius = 12f, center = center)
                drawCircle(Color(0xFF1B75D0), radius = 8f, center = center)
            }
        }

        // Top-left status pill
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(50),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (offlineOnlyMode) Icons.Filled.CloudOff else Icons.Filled.Wifi,
                    contentDescription = if (offlineOnlyMode) "Offline map mode" else "Hybrid cached map mode",
                    tint = if (offlineOnlyMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (offlineOnlyMode) "Offline Topo · Z$zoom" else "Topo Map · Z$zoom",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Zoom & recenter controls
        if (interactive) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { if (zoom < 16) zoom++ },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_zoom_in_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom in map")
                }
                FilledTonalIconButton(
                    onClick = { if (zoom > 10) zoom-- },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_zoom_out_button")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom out map")
                }
                FilledTonalIconButton(
                    onClick = {
                        val target = currentLocation ?: routePoints.lastOrNull()
                        if (target != null) {
                            viewLat = target.latitude
                            viewLon = target.longitude
                        } else {
                            viewLat = centerLat
                            viewLon = centerLon
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_recenter_button")
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Recenter map on location")
                }
            }
        }
    }
}

private fun DrawScope.drawProceduralTopoBackdrop(centerTileX: Double, centerTileY: Double) {
    // Subtle survey grid lines
    val gridStep = 64f
    val offsetX = ((centerTileX * 256.0) % gridStep).toFloat()
    val offsetY = ((centerTileY * 256.0) % gridStep).toFloat()

    var x = -offsetX
    while (x < size.width) {
        drawLine(
            color = MapContourLine.copy(alpha = 0.10f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += gridStep
    }
    var y = -offsetY
    while (y < size.height) {
        drawLine(
            color = MapContourLine.copy(alpha = 0.10f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += gridStep
    }

    // Gentle organic topographic contour rings
    val cx = size.width * 0.52f - ((centerTileX * 18.0) % 90.0).toFloat()
    val cy = size.height * 0.46f - ((centerTileY * 18.0) % 90.0).toFloat()
    for (ring in 1..6) {
        val baseRadius = ring * 58f
        val path = Path()
        val segments = 36
        for (i in 0..segments) {
            val angle = (i.toDouble() / segments) * 2.0 * Math.PI
            val wobble = 1.0 + 0.16 * sin(angle * 3.0 + ring) + 0.08 * cos(angle * 2.0 - ring)
            val px = cx + (baseRadius * wobble * cos(angle)).toFloat()
            val py = cy + (baseRadius * 0.72 * wobble * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
            path = path,
            color = MapContourLine.copy(alpha = 0.18f),
            style = Stroke(width = if (ring % 2 == 0) 2.2f else 1.2f)
        )
    }
}
