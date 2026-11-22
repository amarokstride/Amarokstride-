package com.example.data.weather

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.SeasonalChallengeEngine
import com.example.domain.SeasonalWeatherStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

private const val WEATHER_TAG = "PolarVortexWeather"
private const val CHANNEL_ID = "polar_vortex_channel"
private const val NOTIFICATION_ID = 101

/**
 * Fetches live local ambient temperature & snowfall via the free, keyless Open-Meteo API
 * and dispatches a native Android Polar Vortex bonus notification whenever ambient temperature
 * drops to <= 28°F (-2°C).
 */
class PolarVortexWeatherAlertService(private val context: Context) {

    init {
        ensureNotificationChannel()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Polar Vortex Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when sub-freezing temps (<= 28°F) trigger +20% Winter Solstice pack bonus"
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    /**
     * Queries Open-Meteo for current local temperature (°F) and snowfall (inches).
     * If temperature <= 28°F, automatically dispatches the Polar Vortex push notification
     * when POST_NOTIFICATIONS is granted.
     */
    suspend fun fetchLiveWinterConditions(
        latitude: Double,
        longitude: Double,
        locationLabel: String = "Local Trailhead",
        notifyIfFreezing: Boolean = true
    ): SeasonalWeatherStatus = withContext(Dispatchers.IO) {
        val urlStr = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,snowfall&temperature_unit=fahrenheit&precipitation_unit=inch",
            latitude,
            longitude
        )
        try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "StridersHowl-Android/1.0")
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val current = json.optJSONObject("current")
                val tempF = current?.optDouble("temperature_2m", Double.NaN)
                    ?.takeIf { !it.isNaN() }
                val snowfallIn = current?.optDouble("snowfall", 0.0)
                    ?.takeIf { !it.isNaN() } ?: 0.0

                val isPolarVortex = (tempF ?: 99.0) <= SeasonalChallengeEngine.POLAR_VORTEX_THRESHOLD_F
                val isBlizzard = snowfallIn > 0.05

                if (notifyIfFreezing && isPolarVortex && tempF != null) {
                    dispatchPolarVortexNotification(tempF.roundToInt(), locationLabel)
                }

                return@withContext SeasonalWeatherStatus(
                    tempFahrenheit = tempF,
                    snowfallInches = snowfallIn,
                    isPolarVortexActive = isPolarVortex,
                    isWhiteoutBlizzard = isBlizzard,
                    locationLabel = locationLabel,
                    lastCheckedEpochMs = System.currentTimeMillis(),
                    isLiveWeather = true
                )
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(WEATHER_TAG, "Open-Meteo live check failed, retaining winter watch state", e)
        }

        SeasonalWeatherStatus(
            tempFahrenheit = 24.0,
            snowfallInches = 0.0,
            isPolarVortexActive = true,
            isWhiteoutBlizzard = false,
            locationLabel = locationLabel,
            lastCheckedEpochMs = System.currentTimeMillis(),
            isLiveWeather = false
        )
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun dispatchPolarVortexNotification(tempFahrenheit: Int, location: String): Boolean {
        ensureNotificationChannel()
        if (!hasNotificationPermission()) {
            return false
        }
        return try {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "POLAR VORTEX ACTIVE: ${tempFahrenheit}°F in $location"
            val body = "Sub-freezing conditions detected (<= 28°F). Log a winter run today to earn +20% mileage toward the Winter Solstice Leaderboard!"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.img_striders_howl_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            Log.w(WEATHER_TAG, "Missing notification permission", e)
            false
        } catch (e: Exception) {
            Log.w(WEATHER_TAG, "Unable to show Polar Vortex notification", e)
            false
        }
    }
}
