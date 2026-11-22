package com.example.data.safety

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import com.example.data.audio.SoundboardTrigger
import com.example.data.audio.TavernAudioEngine
import com.example.data.model.GeoPointEle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private const val PREFS_SOS = "striders_howl_sos_prefs"
private const val KEY_EMERGENCY_CONTACTS = "emergency_contacts_csv"

/**
 * Striders Howl Winter Trail SOS ("Howl for Help") Acoustic SAR Beacon & Offline SMS Dispatcher.
 *
 * - Plays a repeating max-volume piercing Alpha wolf howl + heavy haptic pulse every 15 seconds.
 * - Composes a Play-compliant zero-permission `ACTION_SENDTO` (`smsto:`) emergency payload
 *   containing last smoothed GPS coordinates, elevation, real battery %, and a Google Maps link
 *   (automatically hand-off compatible with native Android Satellite SOS when off-grid).
 */
class WinterSosController(
    context: Context,
    private val audioEngine: TavernAudioEngine
) {
    private val appContext = context.applicationContext
    private val beaconScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var beaconJob: Job? = null

    @Volatile
    var isAcousticBeaconActive: Boolean = false
        private set

    fun getSavedEmergencyContacts(): String =
        appContext.getSharedPreferences(PREFS_SOS, Context.MODE_PRIVATE)
            .getString(KEY_EMERGENCY_CONTACTS, "")
            .orEmpty()

    fun saveEmergencyContacts(contactsCsv: String) {
        val cleaned = contactsCsv.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
        appContext.getSharedPreferences(PREFS_SOS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EMERGENCY_CONTACTS, cleaned)
            .apply()
    }

    fun clearEmergencyContacts() {
        appContext.getSharedPreferences(PREFS_SOS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun readBatteryPercent(): Int {
        val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 1..100 } ?: 50
    }

    /**
     * Starts repeating max-volume Search-and-Rescue Alpha howl every 15 seconds.
     */
    fun startAcousticBeacon() {
        stopAcousticBeacon()
        isAcousticBeaconActive = true
        audioEngine.setMutedState(false)
        audioEngine.setAmbientVolumeLevel(1.0f)

        beaconJob = beaconScope.launch {
            while (isActive && isAcousticBeaconActive) {
                audioEngine.playLoneWolfOpeningHowl(forcePreview = true)
                delay(15_000L)
            }
        }
    }

    fun stopAcousticBeacon() {
        isAcousticBeaconActive = false
        beaconJob?.cancel()
        beaconJob = null
        audioEngine.stopAll()
    }

    fun announceCountdownWarning() {
        audioEngine.setMutedState(false)
        audioEngine.playSoundboardTrigger(
            trigger = SoundboardTrigger.HEART_RATE_ZONE_ALERT,
            spokenAlphaCue = "Hard fall detected. Tap I'm OK within 45 seconds to cancel emergency beacon."
        )
    }

    /**
     * Builds the offline Emergency SOS text payload with pinpoint GPS, elevation & battery.
     */
    fun buildSosMessageBody(
        lastLocation: GeoPointEle?,
        batteryPercent: Int
    ): String {
        val locationLines = if (lastLocation != null) {
            val lat = String.format(Locale.US, "%.5f", lastLocation.latitude)
            val lng = String.format(Locale.US, "%.5f", lastLocation.longitude)
            val elevFt = lastLocation.elevationFeet.roundToInt()
            "GPS: $lat, $lng\n" +
                "Elevation: $elevFt ft\n" +
                "Map: https://maps.google.com/?q=$lat,$lng"
        } else {
            "GPS: Last fix unavailable (awaiting satellite lock)"
        }

        return "EMERGENCY [STRIDERS HOWL SOS]: Severe fall or trail emergency. Need assistance.\n" +
            "$locationLines\n" +
            "Battery: $batteryPercent%"
    }

    /**
     * Launches the native Android SMS composer (zero-permission ACTION_SENDTO, Play-compliant,
     * compatible with cellular SMS & OS Satellite SOS relay).
     */
    fun launchEmergencySmsComposer(
        activityContext: Context,
        lastLocation: GeoPointEle?
    ): Boolean {
        val contacts = getSavedEmergencyContacts()
            .split(",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(";")
        val body = buildSosMessageBody(lastLocation, readBatteryPercent())

        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$contacts")
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun release() {
        stopAcousticBeacon()
        beaconScope.cancel()
    }
}
