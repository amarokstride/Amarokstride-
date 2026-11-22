package com.example.data.integrations

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val OAUTH_PROXY_TAG = "OAuthCloudFunctions"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

enum class OAuthProvider(val wireName: String, val redirectUri: String) {
    COROS("coros", "summitpace://coros/callback"),
    STRAVA("strava", "summitpace://strava/callback"),
    GARMIN("garmin", "summitpace://garmin/callback")
}

data class OAuthTokenBundle(
    val provider: OAuthProvider,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtEpochSec: Long? = null,
    val openId: String? = null,
    val scope: String? = null
) {
    fun isExpiredOrExpiringSoon(bufferSeconds: Long = 300L): Boolean {
        val exp = expiresAtEpochSec ?: return false
        val nowSec = System.currentTimeMillis() / 1000L
        return nowSec + bufferSeconds >= exp
    }
}

/**
 * Private per-user token cache stored in app-private SharedPreferences so connected
 * fitness accounts can automatically refresh expired access tokens via Firebase Cloud Functions.
 */
class OAuthTokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "striders_oauth_tokens_v1",
        Context.MODE_PRIVATE
    )

    private fun keyPrefix(uid: String, provider: OAuthProvider): String =
        "${uid}_${provider.wireName}_"

    fun save(uid: String, bundle: OAuthTokenBundle) {
        val prefix = keyPrefix(uid, bundle.provider)
        prefs.edit().apply {
            putString("${prefix}access", bundle.accessToken)
            if (!bundle.refreshToken.isNullOrBlank()) {
                putString("${prefix}refresh", bundle.refreshToken)
            }
            if (bundle.expiresAtEpochSec != null) {
                putLong("${prefix}expires_at", bundle.expiresAtEpochSec)
            }
            if (!bundle.openId.isNullOrBlank()) {
                putString("${prefix}open_id", bundle.openId)
            }
            if (!bundle.scope.isNullOrBlank()) {
                putString("${prefix}scope", bundle.scope)
            }
        }.apply()
    }

    fun load(uid: String, provider: OAuthProvider): OAuthTokenBundle? {
        val prefix = keyPrefix(uid, provider)
        val access = prefs.getString("${prefix}access", null)?.takeIf { it.isNotBlank() } ?: return null
        val refresh = prefs.getString("${prefix}refresh", null)?.takeIf { it.isNotBlank() }
        val exp = if (prefs.contains("${prefix}expires_at")) prefs.getLong("${prefix}expires_at", 0L) else null
        val openId = prefs.getString("${prefix}open_id", null)?.takeIf { it.isNotBlank() }
        val scope = prefs.getString("${prefix}scope", null)?.takeIf { it.isNotBlank() }
        return OAuthTokenBundle(
            provider = provider,
            accessToken = access,
            refreshToken = refresh,
            expiresAtEpochSec = exp?.takeIf { it > 0L },
            openId = openId,
            scope = scope
        )
    }

    fun clear(uid: String, provider: OAuthProvider) {
        val prefix = keyPrefix(uid, provider)
        prefs.edit()
            .remove("${prefix}access")
            .remove("${prefix}refresh")
            .remove("${prefix}expires_at")
            .remove("${prefix}open_id")
            .remove("${prefix}scope")
            .apply()
    }

    fun clearAllForUser(uid: String) {
        if (uid.isBlank()) return
        OAuthProvider.entries.forEach { clear(uid, it) }
    }
}

/**
 * Calls server-side Firebase Cloud Functions (`exchangeOAuthToken` and `refreshOAuthToken`)
 * authenticated with the signed-in user's Firebase Auth ID token so no OAuth client secrets
 * are ever bundled into the Android APK/AAB.
 */
class OAuthCloudFunctionClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {

    fun resolveBaseUrl(): String? {
        val overrideUrl = runCatching { BuildConfig.OAUTH_FUNCTIONS_BASE_URL }
            .getOrDefault("")
            .trim()
            .trimEnd('/')
        if (overrideUrl.startsWith("https://")) {
            return overrideUrl
        }
        val projectId = runCatching { FirebaseApp.getInstance().options.projectId }
            .getOrNull()
            ?.trim()
            .orEmpty()
        if (projectId.isBlank() || projectId == "remixed-project-id") {
            return null
        }
        return "https://us-central1-$projectId.cloudfunctions.net"
    }

    suspend fun exchangeAuthorizationCode(
        provider: OAuthProvider,
        code: String,
        redirectUri: String = provider.redirectUri,
        codeVerifier: String? = null
    ): Result<OAuthTokenBundle> = withContext(Dispatchers.IO) {
        val baseUrl = resolveBaseUrl()
            ?: return@withContext Result.failure(
                IllegalStateException("Firebase Cloud Functions endpoint is not configured for this project.")
            )
        val idToken = fetchFirebaseIdToken()
            ?: return@withContext Result.failure(
                IllegalStateException("Sign in with Google before connecting ${provider.name}.")
            )

        val payload = JSONObject().apply {
            put("provider", provider.wireName)
            put("code", code)
            put("redirectUri", redirectUri)
            if (!codeVerifier.isNullOrBlank()) {
                put("codeVerifier", codeVerifier)
            }
        }
        postToFunction(
            url = "$baseUrl/exchangeOAuthToken",
            idToken = idToken,
            provider = provider,
            payload = payload
        )
    }

    suspend fun refreshAccessToken(
        provider: OAuthProvider,
        refreshToken: String
    ): Result<OAuthTokenBundle> = withContext(Dispatchers.IO) {
        val baseUrl = resolveBaseUrl()
            ?: return@withContext Result.failure(
                IllegalStateException("Firebase Cloud Functions endpoint is not configured for this project.")
            )
        val idToken = fetchFirebaseIdToken()
            ?: return@withContext Result.failure(
                IllegalStateException("Sign in with Google before refreshing ${provider.name}.")
            )

        val payload = JSONObject().apply {
            put("provider", provider.wireName)
            put("refreshToken", refreshToken)
        }
        postToFunction(
            url = "$baseUrl/refreshOAuthToken",
            idToken = idToken,
            provider = provider,
            payload = payload
        )
    }

    private fun postToFunction(
        url: String,
        idToken: String,
        provider: OAuthProvider,
        payload: JSONObject
    ): Result<OAuthTokenBundle> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $idToken")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(rawBody) }.getOrNull()
                if (!response.isSuccessful || json == null) {
                    val serverMsg = json?.optString("message").orEmpty()
                        .ifBlank { "OAuth Cloud Function error (HTTP ${response.code})." }
                    Log.w(OAUTH_PROXY_TAG, "${provider.wireName} OAuth proxy returned HTTP ${response.code}")
                    return Result.failure(IllegalStateException(serverMsg))
                }

                val accessToken = json.optString("access_token").trim()
                if (accessToken.isBlank()) {
                    return Result.failure(
                        IllegalStateException("${provider.name} OAuth proxy did not return an access token.")
                    )
                }

                val nowSec = System.currentTimeMillis() / 1000L
                val expiresAt = when {
                    json.has("expires_at") && !json.isNull("expires_at") -> json.optLong("expires_at", 0L).takeIf { it > 0L }
                    json.has("expires_in") && !json.isNull("expires_in") -> {
                        val ttl = json.optLong("expires_in", 0L)
                        if (ttl > 0L) nowSec + ttl else null
                    }
                    else -> null
                }

                Result.success(
                    OAuthTokenBundle(
                        provider = provider,
                        accessToken = accessToken,
                        refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() && it != "null" },
                        expiresAtEpochSec = expiresAt,
                        openId = json.optString("open_id").takeIf { it.isNotBlank() && it != "null" },
                        scope = json.optString("scope").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(OAUTH_PROXY_TAG, "${provider.wireName} OAuth Cloud Function request failed", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchFirebaseIdToken(): String? {
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() ?: return null
        return runCatching {
            suspendCancellableCoroutine<String?> { cont ->
                user.getIdToken(false)
                    .addOnSuccessListener { result ->
                        if (cont.isActive) cont.resume(result.token)
                    }
                    .addOnFailureListener { err ->
                        if (cont.isActive) cont.resumeWithException(err)
                    }
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}
