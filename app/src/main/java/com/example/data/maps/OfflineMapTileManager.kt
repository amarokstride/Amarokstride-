package com.example.data.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

private const val TAG = "OfflineMapTileManager"

data class OfflineMapRegion(
    val id: String,
    val name: String,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val tileCount: Int,
    val sizeBytes: Long,
    val downloadedAtMs: Long
)

data class PresetTrailRegion(
    val id: String,
    val name: String,
    val subtitle: String,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double = 3.5
)

class OfflineMapTileManager(context: Context) {

    private val tilesRootDir = File(context.applicationContext.filesDir, "offline_topo_tiles").apply {
        if (!exists()) mkdirs()
    }
    private val manifestFile = File(tilesRootDir, "regions_manifest.json")

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val memoryCache = object : LruCache<String, Bitmap>(72) {}

    val presetRegions: List<PresetTrailRegion> = listOf(
        PresetTrailRegion(
            id = "marin_headlands",
            name = "Marin Headlands Ridge",
            subtitle = "Coastal singletrack & hills (CA)",
            centerLat = 37.8324,
            centerLon = -122.4995
        ),
        PresetTrailRegion(
            id = "boulder_flatirons",
            name = "Boulder Flatirons Loop",
            subtitle = "Rocky Mountain foothills (CO)",
            centerLat = 39.9985,
            centerLon = -105.2845
        ),
        PresetTrailRegion(
            id = "yosemite_valley",
            name = "Yosemite Valley Mist Trail",
            subtitle = "Granite cliffs & waterfalls (CA)",
            centerLat = 37.7325,
            centerLon = -119.5582
        ),
        PresetTrailRegion(
            id = "forest_park_pdx",
            name = "Forest Park Wildwood",
            subtitle = "Lush evergreen running trails (OR)",
            centerLat = 45.5361,
            centerLon = -122.7420
        )
    )

    private fun tileFile(z: Int, x: Int, y: Int): File =
        File(tilesRootDir, "$z/$x/$y.png")

    fun getCachedTileSync(z: Int, x: Int, y: Int): Bitmap? {
        val key = "$z/$x/$y"
        memoryCache.get(key)?.let { return it }
        val file = tileFile(z, x, y)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)?.also { bmp ->
                memoryCache.put(key, bmp)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadOrFetchTile(z: Int, x: Int, y: Int, allowNetwork: Boolean = true): Bitmap? =
        withContext(Dispatchers.IO) {
            getCachedTileSync(z, x, y)?.let { return@withContext it }
            if (!allowNetwork) return@withContext null

            val maxTile = (1 shl z)
            if (x < 0 || y < 0 || x >= maxTile || y >= maxTile) return@withContext null

            // Primary OpenTopoMap with OpenStreetMap fallback
            val urls = listOf(
                "https://tile.opentopomap.org/$z/$x/$y.png",
                "https://tile.openstreetmap.org/$z/$x/$y.png"
            )
            for (url in urls) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "SummitPaceAndroid/1.0 (trail-tracker)")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bmp != null) {
                                    val dest = tileFile(z, x, y)
                                    dest.parentFile?.mkdirs()
                                    dest.writeBytes(bytes)
                                    memoryCache.put("$z/$x/$y", bmp)
                                    return@withContext bmp
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Tile fetch fallback for $z/$x/$y: ${e.message}")
                }
            }
            null
        }

    suspend fun downloadOfflineRegion(
        regionId: String,
        name: String,
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double = 3.0,
        minZoom: Int = 12,
        maxZoom: Int = 14,
        onProgress: (downloaded: Int, total: Int) -> Unit
    ): Result<OfflineMapRegion> = withContext(Dispatchers.IO) {
        try {
            val latDelta = radiusKm / 111.0
            val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(centerLat)).coerceAtLeast(0.2))

            val coords = mutableListOf<Triple<Int, Int, Int>>()
            for (z in minZoom..maxZoom) {
                val minX = lonToTileX(centerLon - lonDelta, z)
                val maxX = lonToTileX(centerLon + lonDelta, z)
                val minY = latToTileY(centerLat + latDelta, z)
                val maxY = latToTileY(centerLat - latDelta, z)
                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        coords += Triple(z, x, y)
                    }
                }
            }

            // Cap to polite tile budget per region download
            val boundedCoords = coords.take(80)
            var downloaded = 0
            var totalBytes = 0L

            for ((z, x, y) in boundedCoords) {
                val existing = tileFile(z, x, y)
                if (existing.exists()) {
                    totalBytes += existing.length()
                } else {
                    loadOrFetchTile(z, x, y, allowNetwork = true)
                    if (existing.exists()) {
                        totalBytes += existing.length()
                    }
                }
                downloaded++
                onProgress(downloaded, boundedCoords.size)
            }

            val region = OfflineMapRegion(
                id = regionId,
                name = name,
                centerLat = centerLat,
                centerLon = centerLon,
                radiusKm = radiusKm,
                minZoom = minZoom,
                maxZoom = maxZoom,
                tileCount = boundedCoords.size,
                sizeBytes = totalBytes,
                downloadedAtMs = System.currentTimeMillis()
            )
            saveRegionToManifest(region)
            Result.success(region)
        } catch (e: Exception) {
            Log.e(TAG, "Offline region download failed", e)
            Result.failure(e)
        }
    }

    suspend fun getSavedRegions(): List<OfflineMapRegion> = withContext(Dispatchers.IO) {
        if (!manifestFile.exists()) return@withContext emptyList()
        try {
            val arr = JSONArray(manifestFile.readText())
            (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                OfflineMapRegion(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    centerLat = obj.getDouble("centerLat"),
                    centerLon = obj.getDouble("centerLon"),
                    radiusKm = obj.optDouble("radiusKm", 3.0),
                    minZoom = obj.optInt("minZoom", 12),
                    maxZoom = obj.optInt("maxZoom", 14),
                    tileCount = obj.optInt("tileCount", 0),
                    sizeBytes = obj.optLong("sizeBytes", 0L),
                    downloadedAtMs = obj.optLong("downloadedAtMs", 0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun deleteRegion(regionId: String): List<OfflineMapRegion> = withContext(Dispatchers.IO) {
        val current = getSavedRegions().filterNot { it.id == regionId }
        writeManifest(current)
        current
    }

    suspend fun clearAllCachedTiles(): Unit = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        tilesRootDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun saveRegionToManifest(region: OfflineMapRegion) {
        val current = if (manifestFile.exists()) {
            try {
                val arr = JSONArray(manifestFile.readText())
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    OfflineMapRegion(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        centerLat = o.getDouble("centerLat"),
                        centerLon = o.getDouble("centerLon"),
                        radiusKm = o.optDouble("radiusKm", 3.0),
                        minZoom = o.optInt("minZoom", 12),
                        maxZoom = o.optInt("maxZoom", 14),
                        tileCount = o.optInt("tileCount", 0),
                        sizeBytes = o.optLong("sizeBytes", 0L),
                        downloadedAtMs = o.optLong("downloadedAtMs", 0L)
                    )
                }.filterNot { it.id == region.id }
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        writeManifest(listOf(region) + current)
    }

    private fun writeManifest(regions: List<OfflineMapRegion>) {
        val arr = JSONArray()
        regions.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("centerLat", r.centerLat)
                    put("centerLon", r.centerLon)
                    put("radiusKm", r.radiusKm)
                    put("minZoom", r.minZoom)
                    put("maxZoom", r.maxZoom)
                    put("tileCount", r.tileCount)
                    put("sizeBytes", r.sizeBytes)
                    put("downloadedAtMs", r.downloadedAtMs)
                }
            )
        }
        manifestFile.writeText(arr.toString())
    }

    companion object {
        fun lonToTileX(lon: Double, zoom: Int): Int {
            val n = 2.0.pow(zoom)
            return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, (1 shl zoom) - 1)
        }

        fun latToTileY(lat: Double, zoom: Int): Int {
            val clampedLat = lat.coerceIn(-85.0511, 85.0511)
            val latRad = Math.toRadians(clampedLat)
            val n = 2.0.pow(zoom)
            return floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n)
                .toInt()
                .coerceIn(0, (1 shl zoom) - 1)
        }

        fun lonToExactTileX(lon: Double, zoom: Int): Double =
            (lon + 180.0) / 360.0 * 2.0.pow(zoom)

        fun latToExactTileY(lat: Double, zoom: Int): Double {
            val clampedLat = lat.coerceIn(-85.0511, 85.0511)
            val latRad = Math.toRadians(clampedLat)
            return (1.0 - asinh(tan(latRad)) / PI) / 2.0 * 2.0.pow(zoom)
        }

        fun exactTileXToLon(tileX: Double, zoom: Int): Double =
            tileX / 2.0.pow(zoom) * 360.0 - 180.0

        fun exactTileYToLat(tileY: Double, zoom: Int): Double {
            val n = PI - 2.0 * PI * tileY / 2.0.pow(zoom)
            return Math.toDegrees(atan(sinh(n)))
        }
    }
}
