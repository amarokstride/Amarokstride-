package com.example.data.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ActivitySource
import com.example.data.model.ActivityType
import com.example.data.model.GeoPointEle
import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "CorosIntegration"
private const val COROS_REDIRECT_URI = "summitpace://coros/callback"
private const val COROS_BASE_URL = "https://open.coros.com/"

@JsonClass(generateAdapter = true)
data class CorosWorkoutDto(
    val labelId: String? = null,
    val name: String? = null,
    val mode: Int? = null, // 100=Run, 102=Trail Run, 103=Hike
    val distance: Double? = null, // meters
    val duration: Long? = null, // seconds
    val ascent: Double? = null, // meters
    val startTime: Long? = null // epoch seconds
)

@JsonClass(generateAdapter = true)
data class CorosWorkoutListResponse(
    val data: List<CorosWorkoutDto>? = null
)

interface CorosApiService {
    @GET("v2/coros/sport/list")
    suspend fun fetchWorkouts(
        @Header("Authorization") bearerToken: String,
        @Query("openId") openId: String,
        @Query("startDate") startDateYyyyMmDd: String,
        @Query("endDate") endDateYyyyMmDd: String
    ): CorosWorkoutListResponse
}

data class ParsedGpxTrack(
    val title: String,
    val activityType: ActivityType,
    val distanceMiles: Double,
    val durationSeconds: Long,
    val elevationGainFeet: Long,
    val startedAtEpochMs: Long,
    val routePoints: List<GeoPointEle>,
    val splitPacesSec: List<Long>,
    val externalId: String
)

class CorosIntegrationManager(
    appContext: Context? = null,
    private val oauthClient: OAuthCloudFunctionClient = OAuthCloudFunctionClient()
) {
    private val tokenStore: OAuthTokenStore? = appContext?.let { OAuthTokenStore(it) }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api: CorosApiService by lazy {
        Retrofit.Builder()
            .baseUrl(COROS_BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CorosApiService::class.java)
    }

    /**
     * Checks only the public COROS_CLIENT_ID and Firebase Cloud Functions availability.
     * COROS_CLIENT_SECRET is stored exclusively server-side in Firebase Cloud Functions.
     */
    fun hasConfiguredOAuthCredentials(): Boolean {
        val clientId = runCatching { BuildConfig.COROS_CLIENT_ID }.getOrDefault("")
        return clientId.isNotBlank() &&
            !clientId.startsWith("YOUR_") &&
            oauthClient.resolveBaseUrl() != null
    }

    fun hasStoredSession(): Boolean {
        val uid = currentUid() ?: return false
        return tokenStore?.load(uid, OAuthProvider.COROS) != null
    }

    fun clearStoredSession(uid: String) {
        if (uid.isBlank()) return
        tokenStore?.clear(uid, OAuthProvider.COROS)
    }

    fun buildOAuthAuthorizeIntent(): Intent? {
        if (!hasConfiguredOAuthCredentials()) return null
        val uri = Uri.parse("${COROS_BASE_URL}oauth2/authorize").buildUpon()
            .appendQueryParameter("client_id", BuildConfig.COROS_CLIENT_ID)
            .appendQueryParameter("redirect_uri", COROS_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", "summitpace_coros")
            .build()
        return Intent(Intent.ACTION_VIEW, uri)
    }

    /**
     * Exchanges the OAuth authorization code via the secure Firebase Cloud Function
     * (`exchangeOAuthToken`), caches the resulting token bundle privately per user,
     * and pulls the last 30 days of COROS workouts.
     */
    suspend fun exchangeAndSyncCorosWorkouts(authCode: String): Result<List<ImportedExternalActivity>> {
        if (!hasConfiguredOAuthCredentials()) {
            return Result.failure(
                IllegalStateException(
                    "COROS_CLIENT_ID is not configured. Deploy the Firebase OAuth Cloud Functions with " +
                        "COROS_CLIENT_SECRET or import COROS .gpx/.tcx files directly."
                )
            )
        }
        return try {
            val tokenBundle = oauthClient.exchangeAuthorizationCode(
                provider = OAuthProvider.COROS,
                code = authCode,
                redirectUri = COROS_REDIRECT_URI
            ).getOrElse { return Result.failure(it) }

            currentUid()?.let { uid -> tokenStore?.save(uid, tokenBundle) }
            fetchWorkoutsWithToken(tokenBundle.accessToken, tokenBundle.openId.orEmpty())
        } catch (e: Exception) {
            Log.e(TAG, "COROS Open API sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Syncs COROS workouts using the cached token bundle, automatically calling the
     * Firebase Cloud Function (`refreshOAuthToken`) whenever the access token is expired.
     */
    suspend fun refreshAndSyncCorosWorkouts(forceRefresh: Boolean = false): Result<List<ImportedExternalActivity>> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Sign in before syncing COROS workouts."))
        val cached = tokenStore?.load(uid, OAuthProvider.COROS)
            ?: return Result.failure(IllegalStateException("Connect COROS first to authorize cloud sync."))

        val activeBundle = if (forceRefresh || cached.isExpiredOrExpiringSoon()) {
            val refreshToken = cached.refreshToken
                ?: return Result.failure(IllegalStateException("COROS session expired. Reconnect COROS to continue."))
            val refreshed = oauthClient.refreshAccessToken(
                provider = OAuthProvider.COROS,
                refreshToken = refreshToken
            ).getOrElse { return Result.failure(it) }
            val merged = refreshed.copy(
                refreshToken = refreshed.refreshToken ?: cached.refreshToken,
                openId = refreshed.openId ?: cached.openId
            )
            tokenStore.save(uid, merged)
            merged
        } else {
            cached
        }

        return try {
            fetchWorkoutsWithToken(activeBundle.accessToken, activeBundle.openId.orEmpty())
        } catch (e: Exception) {
            Log.e(TAG, "COROS workout refresh sync failed", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchWorkoutsWithToken(
        accessToken: String,
        openId: String
    ): Result<List<ImportedExternalActivity>> {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        val nowMs = System.currentTimeMillis()
        val startStr = fmt.format(nowMs - 30L * 86400L * 1000L)
        val endStr = fmt.format(nowMs)

        val response = api.fetchWorkouts(
            bearerToken = "Bearer $accessToken",
            openId = openId,
            startDateYyyyMmDd = startStr,
            endDateYyyyMmDd = endStr
        )
        val activities = response.data.orEmpty().mapNotNull { dto ->
            val distMeters = dto.distance ?: return@mapNotNull null
            val durSec = dto.duration ?: return@mapNotNull null
            if (distMeters < 50.0 || durSec < 30L) return@mapNotNull null
            val type = if (dto.mode == 103) ActivityType.HIKE else ActivityType.RUN
            ImportedExternalActivity(
                title = dto.name?.takeIf { it.isNotBlank() } ?: "COROS ${type.displayName}",
                activityType = type,
                source = ActivitySource.COROS,
                distanceMiles = distMeters / 1609.344,
                durationSeconds = durSec,
                elevationGainFeet = ((dto.ascent ?: 0.0) * 3.28084).roundToLong(),
                startedAtEpochMs = (dto.startTime ?: (nowMs / 1000L)) * 1000L,
                externalId = "coros_${dto.labelId ?: dto.startTime}",
                notes = "Synced directly from COROS Open API"
            )
        }
        return Result.success(activities)
    }

    private fun currentUid(): String? =
        runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()

    fun parseGpxOrTcxFromUri(context: Context, uri: Uri): Result<ParsedGpxTrack> {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("Unable to open selected COROS file."))
            stream.use { input ->
                parseTrackXml(input, uri.lastPathSegment ?: "COROS Track")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing COROS GPX/TCX file", e)
            Result.failure(e)
        }
    }

    private fun parseTrackXml(input: InputStream, fallbackName: String): Result<ParsedGpxTrack> {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(input, null)

        var trackName = fallbackName.substringAfterLast('/').substringBeforeLast('.')
            .replace('_', ' ')
            .ifBlank { "COROS Trail Activity" }
        var detectedType = ActivityType.RUN

        val rawPoints = mutableListOf<TimedPoint>()
        var currentLat: Double? = null
        var currentLon: Double? = null
        var currentEleMeters = 0.0
        var currentTimeMs: Long? = null
        var insidePoint = false
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.lowercase(Locale.US)
                    if (currentTag == "trkpt") {
                        insidePoint = true
                        currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        currentEleMeters = 0.0
                        currentTimeMs = null
                    } else if (currentTag == "trackpoint") {
                        insidePoint = true
                        currentLat = null
                        currentLon = null
                        currentEleMeters = 0.0
                        currentTimeMs = null
                    } else if (currentTag == "activity") {
                        val sport = parser.getAttributeValue(null, "Sport")?.lowercase(Locale.US)
                        if (sport != null && (sport.contains("hik") || sport.contains("walk"))) {
                            detectedType = ActivityType.HIKE
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        when (currentTag) {
                            "name" -> if (!insidePoint) {
                                trackName = text.take(100)
                                if (text.lowercase(Locale.US).contains("hike")) {
                                    detectedType = ActivityType.HIKE
                                }
                            }
                            "type" -> if (!insidePoint && text.lowercase(Locale.US).contains("hik")) {
                                detectedType = ActivityType.HIKE
                            }
                            "latitudedegrees" -> if (insidePoint) currentLat = text.toDoubleOrNull()
                            "longitudedegrees" -> if (insidePoint) currentLon = text.toDoubleOrNull()
                            "ele", "altitudemeters" -> if (insidePoint) {
                                currentEleMeters = text.toDoubleOrNull() ?: currentEleMeters
                            }
                            "time" -> if (insidePoint) {
                                currentTimeMs = parseIso8601(text)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val endTag = parser.name.lowercase(Locale.US)
                    if (endTag == "trkpt" || endTag == "trackpoint") {
                        val lat = currentLat
                        val lon = currentLon
                        if (lat != null && lon != null) {
                            rawPoints += TimedPoint(
                                lat = lat,
                                lon = lon,
                                eleFeet = currentEleMeters * 3.28084,
                                timeMs = currentTimeMs
                            )
                        }
                        insidePoint = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        if (rawPoints.size < 2) {
            return Result.failure(
                IllegalArgumentException("The selected file did not contain valid GPX/TCX GPS track points.")
            )
        }

        var totalMeters = 0.0
        var elevGainFeet = 0.0
        var lastMileMark = 0.0
        var lastMileTimeMs = rawPoints.first().timeMs ?: 0L
        val splits = mutableListOf<Long>()

        for (i in 1 until rawPoints.size) {
            val prev = rawPoints[i - 1]
            val curr = rawPoints[i]
            val segmentMeters = haversineMeters(prev.lat, prev.lon, curr.lat, curr.lon)
            if (segmentMeters < 500.0) { // Filter GPS teleport spikes
                totalMeters += segmentMeters
            }
            val eleDiff = curr.eleFeet - prev.eleFeet
            if (eleDiff > 2.5) {
                elevGainFeet += eleDiff
            }
            val currMiles = totalMeters / 1609.344
            if (currMiles - lastMileMark >= 1.0 && curr.timeMs != null && lastMileTimeMs > 0L) {
                val splitSec = ((curr.timeMs - lastMileTimeMs) / 1000L).coerceIn(180L, 5400L)
                splits += splitSec
                lastMileMark = currMiles
                lastMileTimeMs = curr.timeMs
            }
        }

        val distanceMiles = (totalMeters / 1609.344).coerceAtLeast(0.05)
        val firstTime = rawPoints.firstNotNullOfOrNull { it.timeMs }
        val lastTime = rawPoints.lastOrNull { it.timeMs != null }?.timeMs
        val durationSec = if (firstTime != null && lastTime != null && lastTime > firstTime) {
            ((lastTime - firstTime) / 1000L).coerceAtLeast(60L)
        } else {
            (distanceMiles * 600.0).roundToLong()
        }

        // Auto-classify slow high-elevation tracks as hikes if unspecified
        val avgPaceSec = (durationSec / distanceMiles).roundToLong()
        if (avgPaceSec > 960L && elevGainFeet > 300.0) {
            detectedType = ActivityType.HIKE
        }

        val geoPoints = rawPoints.map { GeoPointEle(it.lat, it.lon, it.eleFeet) }
        val startEpoch = firstTime ?: System.currentTimeMillis()
        val extId = "coros_file_${startEpoch}_${(distanceMiles * 100).roundToLong()}"

        return Result.success(
            ParsedGpxTrack(
                title = trackName,
                activityType = detectedType,
                distanceMiles = distanceMiles,
                durationSeconds = durationSec,
                elevationGainFeet = elevGainFeet.roundToLong().coerceAtLeast(0L),
                startedAtEpochMs = startEpoch,
                routePoints = geoPoints,
                splitPacesSec = splits,
                externalId = extId
            )
        )
    }

    private data class TimedPoint(
        val lat: Double,
        val lon: Double,
        val eleFeet: Double,
        val timeMs: Long?
    )

    private fun parseIso8601(raw: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (p in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return fmt.parse(raw)?.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    companion object {
        fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
