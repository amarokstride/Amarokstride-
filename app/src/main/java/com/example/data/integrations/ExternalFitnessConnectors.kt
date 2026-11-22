package com.example.data.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ActivitySource
import com.example.data.model.ActivityType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val EXT_TAG = "ExternalConnectors"
private const val STRAVA_REDIRECT_URI = "summitpace://strava/callback"
private const val GARMIN_REDIRECT_URI = "summitpace://garmin/callback"

private interface StravaApiService {
    @GET("api/v3/athlete/activities")
    suspend fun listAthleteActivities(
        @Header("Authorization") bearer: String,
        @Query("per_page") perPage: Int = 20
    ): List<Map<String, Any>>
}

class ExternalFitnessConnectors(
    appContext: Context? = null,
    private val oauthClient: OAuthCloudFunctionClient = OAuthCloudFunctionClient()
) {
    private val tokenStore: OAuthTokenStore? = appContext?.let { OAuthTokenStore(it) }

    @Volatile
    private var pendingGarminCodeVerifier: String? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val stravaApi: StravaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.strava.com/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(StravaApiService::class.java)
    }

    /**
     * Checks only the public STRAVA_CLIENT_ID and Firebase Cloud Functions endpoint.
     * STRAVA_CLIENT_SECRET is stored exclusively server-side in Firebase Cloud Functions.
     */
    fun hasStravaCredentials(): Boolean {
        val id = runCatching { BuildConfig.STRAVA_CLIENT_ID }.getOrDefault("")
        return id.isNotBlank() &&
            !id.startsWith("YOUR_") &&
            oauthClient.resolveBaseUrl() != null
    }

    /**
     * Checks only the public GARMIN_CLIENT_ID and Firebase Cloud Functions endpoint.
     * GARMIN_CLIENT_SECRET is stored exclusively server-side in Firebase Cloud Functions.
     */
    fun hasGarminCredentials(): Boolean {
        val id = runCatching { BuildConfig.GARMIN_CLIENT_ID }.getOrDefault("")
        return id.isNotBlank() &&
            !id.startsWith("YOUR_") &&
            oauthClient.resolveBaseUrl() != null
    }

    fun hasStoredStravaSession(): Boolean {
        val uid = currentUid() ?: return false
        return tokenStore?.load(uid, OAuthProvider.STRAVA) != null
    }

    fun hasStoredGarminSession(): Boolean {
        val uid = currentUid() ?: return false
        return tokenStore?.load(uid, OAuthProvider.GARMIN) != null
    }

    fun clearStoredSessions(uid: String) {
        pendingGarminCodeVerifier = null
        if (uid.isBlank()) return
        tokenStore?.clearAllForUser(uid)
    }

    fun buildStravaAuthorizeUri(): Uri? {
        if (!hasStravaCredentials()) return null
        val id = BuildConfig.STRAVA_CLIENT_ID
        return Uri.parse("https://www.strava.com/oauth/mobile/authorize").buildUpon()
            .appendQueryParameter("client_id", id)
            .appendQueryParameter("redirect_uri", STRAVA_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "activity:read_all")
            .build()
    }

    /**
     * Builds a Garmin OAuth 2.0 PKCE authorization URI using only the public GARMIN_CLIENT_ID.
     * Token exchange happens server-side in Firebase Cloud Functions using GARMIN_CLIENT_SECRET.
     */
    fun buildGarminAuthorizeUri(): Uri? {
        if (!hasGarminCredentials()) return null
        val clientId = BuildConfig.GARMIN_CLIENT_ID
        val verifier = generatePkceCodeVerifier()
        pendingGarminCodeVerifier = verifier
        val challenge = sha256Base64Url(verifier)
        return Uri.parse("https://connect.garmin.com/oauth2Confirm").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", GARMIN_REDIRECT_URI)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    /**
     * Exchanges the Strava authorization code through Firebase Cloud Functions (`exchangeOAuthToken`),
     * stores the tokens privately for automatic refresh, and pulls recent athlete activities.
     */
    suspend fun exchangeAndSyncStrava(authCode: String): Result<List<ImportedExternalActivity>> =
        withContext(Dispatchers.IO) {
            if (!hasStravaCredentials()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "STRAVA_CLIENT_ID is not configured. Deploy the Firebase OAuth Cloud Functions with " +
                            "STRAVA_CLIENT_SECRET or import Strava GPX files directly."
                    )
                )
            }
            try {
                val tokenBundle = oauthClient.exchangeAuthorizationCode(
                    provider = OAuthProvider.STRAVA,
                    code = authCode,
                    redirectUri = STRAVA_REDIRECT_URI
                ).getOrElse { return@withContext Result.failure(it) }

                currentUid()?.let { uid -> tokenStore?.save(uid, tokenBundle) }
                fetchStravaActivities(tokenBundle.accessToken)
            } catch (e: Exception) {
                Log.e(EXT_TAG, "Strava sync error", e)
                Result.failure(e)
            }
        }

    /**
     * Refreshes an expired Strava access token through Firebase Cloud Functions (`refreshOAuthToken`)
     * and pulls recent athlete activities.
     */
    suspend fun refreshAndSyncStrava(forceRefresh: Boolean = false): Result<List<ImportedExternalActivity>> =
        withContext(Dispatchers.IO) {
            val uid = currentUid()
                ?: return@withContext Result.failure(IllegalStateException("Sign in before syncing Strava."))
            val cached = tokenStore?.load(uid, OAuthProvider.STRAVA)
                ?: return@withContext Result.failure(IllegalStateException("Connect Strava first to authorize cloud sync."))

            val activeBundle = if (forceRefresh || cached.isExpiredOrExpiringSoon()) {
                val refreshToken = cached.refreshToken
                    ?: return@withContext Result.failure(IllegalStateException("Strava session expired. Reconnect Strava."))
                val refreshed = oauthClient.refreshAccessToken(
                    provider = OAuthProvider.STRAVA,
                    refreshToken = refreshToken
                ).getOrElse { return@withContext Result.failure(it) }
                val merged = refreshed.copy(
                    refreshToken = refreshed.refreshToken ?: cached.refreshToken
                )
                tokenStore.save(uid, merged)
                merged
            } else {
                cached
            }

            try {
                fetchStravaActivities(activeBundle.accessToken)
            } catch (e: Exception) {
                Log.e(EXT_TAG, "Strava refresh sync error", e)
                Result.failure(e)
            }
        }

    /**
     * Exchanges the Garmin OAuth 2.0 authorization code + PKCE verifier through Firebase Cloud Functions
     * (`exchangeOAuthToken`) and stores the resulting session privately per user.
     */
    suspend fun exchangeGarminAuthorizationCode(authCode: String): Result<OAuthTokenBundle> =
        withContext(Dispatchers.IO) {
            if (!hasGarminCredentials()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "GARMIN_CLIENT_ID is not configured. Deploy the Firebase OAuth Cloud Functions with " +
                            "GARMIN_CLIENT_SECRET or use Garmin GPX/TCX import."
                    )
                )
            }
            val verifier = pendingGarminCodeVerifier
            pendingGarminCodeVerifier = null
            val result = oauthClient.exchangeAuthorizationCode(
                provider = OAuthProvider.GARMIN,
                code = authCode,
                redirectUri = GARMIN_REDIRECT_URI,
                codeVerifier = verifier
            )
            result.onSuccess { bundle ->
                currentUid()?.let { uid -> tokenStore?.save(uid, bundle) }
            }
            result
        }

    /**
     * Refreshes an expired Garmin OAuth 2.0 session via Firebase Cloud Functions (`refreshOAuthToken`).
     */
    suspend fun refreshGarminToken(): Result<OAuthTokenBundle> = withContext(Dispatchers.IO) {
        val uid = currentUid()
            ?: return@withContext Result.failure(IllegalStateException("Sign in before refreshing Garmin."))
        val cached = tokenStore?.load(uid, OAuthProvider.GARMIN)
            ?: return@withContext Result.failure(IllegalStateException("Connect Garmin first to authorize cloud sync."))
        val refreshToken = cached.refreshToken
            ?: return@withContext Result.failure(IllegalStateException("Garmin session expired. Reconnect Garmin."))

        val result = oauthClient.refreshAccessToken(
            provider = OAuthProvider.GARMIN,
            refreshToken = refreshToken
        )
        result.map { refreshed ->
            val merged = refreshed.copy(refreshToken = refreshed.refreshToken ?: cached.refreshToken)
            tokenStore.save(uid, merged)
            merged
        }
    }

    private suspend fun fetchStravaActivities(accessToken: String): Result<List<ImportedExternalActivity>> {
        val rawList = stravaApi.listAthleteActivities("Bearer $accessToken")
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val imported = rawList.mapNotNull { item ->
            val id = item["id"]?.toString() ?: return@mapNotNull null
            val name = item["name"]?.toString().orEmpty().ifBlank { "Strava Quest" }
            val sportType = item["sport_type"]?.toString() ?: item["type"]?.toString().orEmpty()
            val type = when {
                sportType.contains("Hike", true) || sportType.contains("Walk", true) -> ActivityType.HIKE
                sportType.contains("Ride", true) && sportType.contains("Horse", true) -> ActivityType.EQUESTRIAN
                else -> ActivityType.RUN
            }
            val distMeters = (item["distance"] as? Number)?.toDouble() ?: 0.0
            val movingSec = (item["moving_time"] as? Number)?.toLong() ?: 0L
            if (distMeters < 50.0 || movingSec < 30L) return@mapNotNull null
            val elevMeters = (item["total_elevation_gain"] as? Number)?.toDouble() ?: 0.0
            val startIso = item["start_date"]?.toString().orEmpty()
            val startMs = runCatching { iso.parse(startIso)?.time }.getOrNull() ?: System.currentTimeMillis()
            ImportedExternalActivity(
                externalId = "strava_$id",
                title = name.take(100),
                activityType = type,
                source = ActivitySource.STRAVA,
                distanceMiles = (distMeters / 1609.344).coerceIn(0.05, 250.0),
                durationSeconds = movingSec.coerceIn(60L, 604800L),
                elevationGainFeet = (elevMeters * 3.28084).toLong().coerceIn(0L, 50000L),
                startedAtEpochMs = startMs,
                notes = "Synced from Strava ($sportType)"
            )
        }
        return Result.success(imported)
    }

    private fun currentUid(): String? =
        runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()

    private fun generatePkceCodeVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun sha256Base64Url(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Fetches real historical weather conditions for a completed route from Open-Meteo Archive API
     * (free, public, no API key required).
     */
    suspend fun fetchHistoricalWeatherSummary(
        latitude: Double,
        longitude: Double,
        epochMs: Long
    ): String = withContext(Dispatchers.IO) {
        try {
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = dateFmt.format(Date(epochMs))
            val url = String.format(
                Locale.US,
                "https://archive-api.open-meteo.com/v1/archive?latitude=%.4f&longitude=%.4f&start_date=%s&end_date=%s&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,windspeed_10m_max&temperature_unit=fahrenheit&windspeed_unit=mph&timezone=auto",
                latitude,
                longitude,
                dateStr,
                dateStr
            )
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext "Weather archive unavailable (HTTP ${resp.code})"
                val body = resp.body?.string().orEmpty()
                val daily = JSONObject(body).optJSONObject("daily")
                    ?: return@withContext "Clear seasonal trail conditions"
                val maxTemp = daily.optJSONArray("temperature_2m_max")?.optDouble(0, Double.NaN) ?: Double.NaN
                val minTemp = daily.optJSONArray("temperature_2m_min")?.optDouble(0, Double.NaN) ?: Double.NaN
                val precip = daily.optJSONArray("precipitation_sum")?.optDouble(0, 0.0) ?: 0.0
                val wind = daily.optJSONArray("windspeed_10m_max")?.optDouble(0, 0.0) ?: 0.0
                if (maxTemp.isNaN()) {
                    "Mild trail conditions recorded on $dateStr"
                } else {
                    val rainNote = if (precip > 0.2) String.format(Locale.US, ", %.1fmm rain", precip) else ", dry"
                    String.format(
                        Locale.US,
                        "%s · High %.0f°F / Low %.0f°F · Wind %.0f mph%s",
                        dateStr,
                        maxTemp,
                        minTemp,
                        wind,
                        rainNote
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(EXT_TAG, "Open-Meteo weather fetch skipped: ${e.message}")
            "Offline weather estimate — check morning temperatures before starting"
        }
    }

    companion object {
        fun openGoogleMapsRouteSearch(
            context: Context,
            query: String,
            lat: Double? = null,
            lon: Double? = null
        ) {
            val encodedQuery = Uri.encode(query)
            val geoUri = if (lat != null && lon != null) {
                Uri.parse(String.format(Locale.US, "geo:%.5f,%.5f?q=%s", lat, lon, encodedQuery))
            } else {
                Uri.parse("geo:0,0?q=$encodedQuery")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val fallbackWeb = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(mapIntent)
            } catch (_: Exception) {
                context.startActivity(fallbackWeb)
            }
        }

        fun openYouTubeMusic(context: Context) {
            val pm = context.packageManager
            val ytMusic = pm.getLaunchIntentForPackage("com.google.android.apps.youtube.music")
                ?: pm.getLaunchIntentForPackage("com.google.android.youtube")
            if (ytMusic != null) {
                ytMusic.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(ytMusic)
            } else {
                openUrl(context, "https://music.youtube.com/")
            }
        }

        fun openLibroAudiobooks(context: Context) {
            val pm = context.packageManager
            val libro = pm.getLaunchIntentForPackage("fm.libro.librofm")
            if (libro != null) {
                libro.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(libro)
            } else {
                openUrl(context, "https://libro.fm/")
            }
        }

        fun openPathboundBridge(context: Context, questSummary: String? = null) {
            if (!questSummary.isNullOrBlank()) {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Striders Howl · Pathbound Sync")
                    putExtra(Intent.EXTRA_TEXT, questSummary)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(share, "Share Quest with Pathbound")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } else {
                openUrl(context, "https://play.google.com/store/search?q=Pathbound&c=apps")
            }
        }

        fun openWolfConservationDonation(context: Context) {
            openUrl(context, "https://wolf.org/donate/")
        }

        fun openDeveloperSupportDonation(context: Context) {
            openUrl(context, "https://ko-fi.com/")
        }

        fun openUrl(context: Context, url: String) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
