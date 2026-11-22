package com.example.data.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.domain.PaceTriggerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

private const val AUDIO_TAG = "TavernAudioEngine"
private const val PREFS_AUDIO = "striders_howl_audio_prefs"
private const val KEY_LAUNCH_SOUND_ENABLED = "launch_sound_enabled"

enum class SoundboardTrigger(
    val title: String,
    val shortLabel: String,
    val fxDescription: String,
    val scenarioPrompt: String
) {
    ALPHA_HOWL(
        title = "Milestone Unlocked",
        shortLabel = "Alpha Howl",
        fxDescription = "Ascending alpha howl + arctic wind",
        scenarioPrompt = "Milestone unlocked on the trail"
    ),
    STRIDE_CADENCE(
        title = "Pace Drop Alert",
        shortLabel = "178 SPM Stride",
        fxDescription = "Rhythmic 178 SPM snow-crunch gallop",
        scenarioPrompt = "Runner pace dropped below split target; restore brisk wolf stride cadence"
    ),
    HEART_RATE_ZONE_ALERT(
        title = "HR Zone 5 Threshold",
        shortLabel = "HR Zone 5",
        fxDescription = "Resonant chest growl + heartbeat thud",
        scenarioPrompt = "Heart rate spiked into Zone 5 threshold; steady breath before the next ascent"
    ),
    PACK_LEADER_OVERTAKE(
        title = "Pack Overtake",
        shortLabel = "Pack Overtake",
        fxDescription = "Glacial chime + double wolf bark",
        scenarioPrompt = "Overtaking a pack member ahead on the leaderboard toward Alpha tier"
    ),
    CONSERVATION_MILESTONE(
        title = "Conservation Goal",
        shortLabel = "Wild Pack Choir",
        fxDescription = "Harmonized pack choir howl across ridges",
        scenarioPrompt = "Trail miles just advanced the Wild Wolf Habitat conservation corridor goal"
    ),
    STEEP_CLIMB_GROWL(
        title = "Steep Climb Ascent",
        shortLabel = "Climb Growl",
        fxDescription = "Low resonant chest growl scaled to incline grade",
        scenarioPrompt = "Runner entered a steep uphill incline; dig into the climb, drive the knees, and sync breath"
    )
}

enum class ClimbAudioMode(val label: String) {
    PERIODIC_PULSE("Periodic Pulse"),
    CONTINUOUS_RUMBLE("Ambient Rumble")
}

class TavernAudioEngine(context: Context) {

    private val appContext = context.applicationContext
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var tts: TextToSpeech? = null
    @Volatile
    private var ttsReady = false
    private var lastPaceCueTimeMs = 0L

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var ambientVolume: Float = 0.65f

    @Volatile
    private var isDucked: Boolean = false

    private var activeTrack: AudioTrack? = null
    private var activeFxJob: Job? = null
    private var activeFocusRequest: AudioFocusRequest? = null
    private var climbLoopTrack: AudioTrack? = null
    @Volatile
    private var isClimbLoopPlaying: Boolean = false
    @Volatile
    private var currentClimbLoopVolume: Float = 0f

    init {
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    // Deep, calm, resonant "Alpha" vocal tuning
                    tts?.setPitch(0.78f)
                    tts?.setSpeechRate(0.94f)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            applyDucking(duck = true)
                        }

                        override fun onDone(utteranceId: String?) {
                            applyDucking(duck = false)
                            abandonTransientAudioFocus()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            applyDucking(duck = false)
                            abandonTransientAudioFocus()
                        }
                    })
                    ttsReady = true
                }
            }
        } catch (e: Exception) {
            Log.w(AUDIO_TAG, "TTS initialization skipped: ${e.message}")
        }
    }

    fun isLaunchSoundEnabled(): Boolean =
        appContext.getSharedPreferences(PREFS_AUDIO, Context.MODE_PRIVATE)
            .getBoolean(KEY_LAUNCH_SOUND_ENABLED, true)

    fun setLaunchSoundEnabled(enabled: Boolean) {
        appContext.getSharedPreferences(PREFS_AUDIO, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LAUNCH_SOUND_ENABLED, enabled)
            .apply()
    }

    fun setMutedState(muted: Boolean) {
        isMuted = muted
        if (muted) stopAll()
    }

    fun setAmbientVolumeLevel(volume: Float) {
        ambientVolume = volume.coerceIn(0f, 1f)
        val effective = if (isDucked) ambientVolume * 0.2f else ambientVolume
        try {
            activeTrack?.setVolume(effective)
        } catch (_: Exception) {
        }
    }

    fun speakCoachCue(message: String, force: Boolean = false) {
        if (isMuted) return
        val now = System.currentTimeMillis()
        if (!force && now - lastPaceCueTimeMs < 45_000L) return
        lastPaceCueTimeMs = now
        if (!ttsReady) return
        requestTransientDuckFocus()
        applyDucking(duck = true)
        try {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "striders_howl_cue_$now")
        } catch (e: Exception) {
            applyDucking(duck = false)
            Log.w(AUDIO_TAG, "TTS speak failed: ${e.message}")
        }
    }

    /**
     * Plays a procedural Striders Howl soundboard FX trigger with automatic
     * 20% volume ducking when an Alpha Gemini voice cue is spoken over it,
     * plus tactile multi-pulse haptic feedback.
     */
    fun playSoundboardTrigger(
        trigger: SoundboardTrigger,
        spokenAlphaCue: String? = null
    ) {
        if (isMuted) return
        triggerHapticFeedback(trigger)
        requestTransientDuckFocus()

        val duckForVoice = !spokenAlphaCue.isNullOrBlank()
        applyDucking(duck = duckForVoice)

        activeFxJob?.cancel()
        activeFxJob = audioScope.launch {
            val sampleRate = 24000
            val pcm = when (trigger) {
                SoundboardTrigger.ALPHA_HOWL -> synthesizeAlphaWolfHowl(durationSec = 3.8, sampleRate = sampleRate)
                SoundboardTrigger.STRIDE_CADENCE -> synthesizeSnowStrideGallop(durationSec = 3.2, cadenceSpm = 178, sampleRate = sampleRate)
                SoundboardTrigger.HEART_RATE_ZONE_ALERT -> synthesizeHeartRateGrowlAlert(durationSec = 2.6, sampleRate = sampleRate)
                SoundboardTrigger.PACK_LEADER_OVERTAKE -> synthesizePackOvertakeChimeBark(durationSec = 2.4, sampleRate = sampleRate)
                SoundboardTrigger.CONSERVATION_MILESTONE -> synthesizeConservationPackChoir(durationSec = 4.2, sampleRate = sampleRate)
                SoundboardTrigger.STEEP_CLIMB_GROWL -> synthesizeSteepClimbGrowl(durationSec = 3.5, gradePercent = 9.0, sampleRate = sampleRate)
            }
            playPcm(pcm, sampleRate, ducked = duckForVoice)
            if (!duckForVoice) {
                delay(200L)
                applyDucking(duck = false)
                abandonTransientAudioFocus()
            }
        }

        if (!spokenAlphaCue.isNullOrBlank()) {
            speakCoachCue(spokenAlphaCue, force = true)
        }
    }

    fun stopAll() {
        activeFxJob?.cancel()
        stopContinuousClimbGrowl()
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (_: Exception) {
        }
        activeTrack = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        applyDucking(duck = false)
        abandonTransientAudioFocus()
    }

    private fun applyDucking(duck: Boolean) {
        isDucked = duck
        val gain = if (duck) ambientVolume * 0.2f else ambientVolume
        try {
            activeTrack?.setVolume(gain.coerceIn(0f, 1f))
            if (isClimbLoopPlaying) {
                val loopGain = (currentClimbLoopVolume * gain).coerceIn(0f, 1f)
                climbLoopTrack?.setVolume(loopGain)
            }
        } catch (_: Exception) {
        }
    }

    private fun requestTransientDuckFocus() {
        val manager = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                activeFocusRequest = req
                manager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                manager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun abandonTransientAudioFocus() {
        val manager = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activeFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
                activeFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(null)
            }
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerHapticFeedback(trigger: SoundboardTrigger) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (trigger) {
                    SoundboardTrigger.ALPHA_HOWL,
                    SoundboardTrigger.CONSERVATION_MILESTONE -> VibrationEffect.createWaveform(
                        longArrayOf(0, 140, 80, 220, 80, 380),
                        intArrayOf(0, 120, 0, 210, 0, 255),
                        -1
                    )
                    SoundboardTrigger.HEART_RATE_ZONE_ALERT -> VibrationEffect.createWaveform(
                        longArrayOf(0, 180, 100, 180),
                        intArrayOf(0, 255, 0, 255),
                        -1
                    )
                    SoundboardTrigger.STEEP_CLIMB_GROWL -> VibrationEffect.createWaveform(
                        longArrayOf(0, 320, 90, 240),
                        intArrayOf(0, 240, 0, 190),
                        -1
                    )
                    else -> VibrationEffect.createOneShot(160L, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(180L)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Synthesizes a heavy weathered oak door + iron hinge creak (~1.1s)
     * when opening the carved wolf tavern entrance doors.
     */
    fun playDoorHingeCreak() {
        if (isMuted) return
        audioScope.launch {
            val sampleRate = 22050
            val durationSec = 1.15
            val numSamples = (sampleRate * durationSec).toInt()
            val pcm = ShortArray(numSamples)
            var phase1 = 0.0
            var phase2 = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = t / durationSec
                val baseFreq = 165.0 + 260.0 * sin(progress * PI * 0.85) + 45.0 * sin(t * 28.0)
                val pulseEnv = 0.45 + 0.55 * sin(t * 24.0 + sin(t * 9.0) * 2.0).let { it * it }
                val fadeIn = (t / 0.12).coerceIn(0.0, 1.0)
                val fadeOut = ((durationSec - t) / 0.22).coerceIn(0.0, 1.0)
                val env = fadeIn * fadeOut * pulseEnv

                phase1 += 2.0 * PI * baseFreq / sampleRate
                phase2 += 2.0 * PI * (baseFreq * 2.42) / sampleRate

                val raw = (0.62 * sin(phase1) + 0.28 * sin(phase2) + 0.10 * sin(phase1 * 3.1)) * env
                val clamped = (raw * 0.55).coerceIn(-0.95, 0.95)
                pcm[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(pcm, sampleRate, ducked = false)
        }
    }

    /**
     * Synthesizes an expressive alpha wolf howl played when completing a quest or milestone.
     */
    fun playWolfHowl() {
        playSoundboardTrigger(SoundboardTrigger.ALPHA_HOWL, spokenAlphaCue = null)
    }

    /**
     * Atmospheric lone wolf howl on app launch (240 Hz chest register -> 580 Hz solitary peak
     * dissolving into winter wind) with synced 3-stage tactile haptics (0ms, 600ms, 1400ms).
     * Honors the user's `launch_sound_enabled` preference.
     */
    @SuppressLint("MissingPermission")
    fun playLoneWolfOpeningHowl(forcePreview: Boolean = false) {
        if (!forcePreview && (isMuted || !isLaunchSoundEnabled())) return
        requestTransientDuckFocus()

        // Synced 3-stage tactile sensation: light breath -> medium rise -> deep resonant peak
        audioScope.launch {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(45L, 85))
                        delay(600L)
                        vibrator.vibrate(VibrationEffect.createOneShot(85L, 165))
                        delay(800L)
                        vibrator.vibrate(VibrationEffect.createOneShot(180L, 255))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 45, 555, 85, 715, 180), -1)
                    }
                }
            } catch (_: Exception) {
            }
        }

        activeFxJob?.cancel()
        activeFxJob = audioScope.launch {
            val sampleRate = 24000
            val durationSec = 4.2
            val total = (sampleRate * durationSec).toInt()
            val buf = DoubleArray(total)
            val rng = Random(240580L)
            var phase = 0.0
            var maxAmp = 1e-6

            for (i in 0 until total) {
                val t = i.toDouble() / sampleRate
                val normT = (t / durationSec).coerceIn(0.0, 1.0)
                val freq = 240.0 + 340.0 * sin(PI * normT.pow(1.3)).coerceAtLeast(0.0).pow(1.8)
                phase += 2.0 * PI * freq / sampleRate

                val howl = 0.60 * sin(phase) +
                    0.28 * sin(2.0 * phase) +
                    0.12 * sin(3.0 * phase) +
                    0.18 * sin(0.5 * phase)
                val vibrato = 1.0 + 0.08 * sin(2.0 * PI * 5.2 * t)
                val fadeIn = (t / 0.8).coerceIn(0.0, 1.0)
                val fadeOut = ((durationSec - t) / 1.6).coerceIn(0.0, 1.0)
                val wind = rng.nextGaussian() * 0.03 * sin(PI * normT)

                val sample = (howl * vibrato * fadeIn * fadeOut * 0.85) + (wind * 0.15)
                buf[i] = sample
                maxAmp = maxOf(maxAmp, abs(sample))
            }
            val pcm = normalizeToPcm16(buf, maxAmp, 0.88)
            playPcm(pcm, sampleRate, ducked = false)
            abandonTransientAudioFocus()
        }
    }

    /**
     * Plays a grade-scaled guttural winter wolf climb growl.
     * In Continuous Rumble mode, scales volume/pitch dynamically from 5% to 15%+ slope.
     */
    fun playSteepClimbGrowl(gradePercent: Double, continuousRumble: Boolean) {
        if (isMuted) return
        if (!continuousRumble) {
            triggerHapticFeedback(SoundboardTrigger.STEEP_CLIMB_GROWL)
        }
        requestTransientDuckFocus()
        activeFxJob?.cancel()
        activeFxJob = audioScope.launch {
            val sampleRate = 24000
            val duration = if (continuousRumble) 3.8 else 3.4
            val pcm = synthesizeSteepClimbGrowl(
                durationSec = duration,
                gradePercent = gradePercent,
                sampleRate = sampleRate
            )
            // Scale subtle rumble gain with slope (5% -> soft, 15%+ -> full intensity)
            val intensity = if (continuousRumble) {
                (((gradePercent - 5.0) / 10.0).coerceIn(0.25, 1.0)).toFloat() * 0.65f
            } else {
                1.0f
            }
            val savedVol = ambientVolume
            val tempVol = (savedVol * intensity).coerceIn(0.12f, 1f)
            playPcm(pcm, sampleRate, ducked = false, overrideGain = tempVol)
            abandonTransientAudioFocus()
        }
    }

    /**
     * Dynamically scales a continuous low-latency looping wolf growl based on live trail gradient.
     * Uses quadratic ease-in slope mapping (silent below minGrade, 100% at >= 15%) and
     * Exponential Moving Average (0.7 / 0.3) volume smoothing to avoid sudden audio pops.
     * Returns the smoothed normalized growl intensity (0.0..1.0) for UI telemetry.
     */
    fun updateContinuousClimbGrowl(
        gradePercent: Double,
        minGrade: Double = 4.0
    ): Float {
        if (isMuted) {
            stopContinuousClimbGrowl()
            return 0f
        }
        val targetVol = PaceTriggerService.calculateGrowlVolume(
            gradePercent = gradePercent,
            minGrade = minGrade,
            maxGrade = 15.0
        ).toFloat()

        if (targetVol > 0.05f && !isClimbLoopPlaying) {
            startClimbLoopTrack(initialTarget = targetVol)
        } else if (targetVol <= 0.05f && isClimbLoopPlaying) {
            stopContinuousClimbGrowl()
            return 0f
        }

        if (isClimbLoopPlaying) {
            currentClimbLoopVolume = (currentClimbLoopVolume * 0.7f) + (targetVol * 0.3f)
            val duckFactor = if (isDucked) 0.2f else 1.0f
            val finalGain = (currentClimbLoopVolume * ambientVolume * duckFactor).coerceIn(0f, 1f)
            try {
                climbLoopTrack?.setVolume(finalGain)
            } catch (_: Exception) {
            }
            return currentClimbLoopVolume
        }
        return 0f
    }

    private fun startClimbLoopTrack(initialTarget: Float) {
        isClimbLoopPlaying = true
        currentClimbLoopVolume = (initialTarget * 0.3f).coerceIn(0.05f, 1f)
        audioScope.launch {
            try {
                val sampleRate = 24000
                val loopSamples = synthesizeSteepClimbGrowl(
                    durationSec = 2.8,
                    gradePercent = 10.5,
                    sampleRate = sampleRate
                )
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(loopSamples.size * 2)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(loopSamples, 0, loopSamples.size)
                track.setLoopPoints(0, loopSamples.size, -1)
                val duckFactor = if (isDucked) 0.2f else 1.0f
                track.setVolume((currentClimbLoopVolume * ambientVolume * duckFactor).coerceIn(0f, 1f))
                climbLoopTrack = track
                if (isClimbLoopPlaying) {
                    track.play()
                } else {
                    track.release()
                }
            } catch (e: Exception) {
                isClimbLoopPlaying = false
                Log.w(AUDIO_TAG, "Continuous climb growl loop failed: ${e.message}")
            }
        }
    }

    fun stopContinuousClimbGrowl() {
        isClimbLoopPlaying = false
        currentClimbLoopVolume = 0f
        try {
            climbLoopTrack?.setVolume(0f)
            climbLoopTrack?.pause()
            climbLoopTrack?.stop()
            climbLoopTrack?.release()
        } catch (_: Exception) {
        }
        climbLoopTrack = null
    }

    // --- Procedural Soundboard Synthesizers ---

    private fun synthesizeAlphaWolfHowl(durationSec: Double, sampleRate: Int): ShortArray {
        val total = (sampleRate * durationSec).toInt()
        val buf = DoubleArray(total)
        val rng = Random(42L)
        var phase = 0.0
        var maxAmp = 1e-6

        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate
            // Pitch glides 320 Hz -> 640 Hz -> 280 Hz with expressive natural vibrato
            val contour = sin(PI * t / durationSec).coerceAtLeast(0.0).pow(1.5)
            val freq = 310.0 + 330.0 * contour
            phase += 2.0 * PI * freq / sampleRate

            val harmonics = sin(phase) +
                0.45 * sin(2.0 * phase) +
                0.25 * sin(3.0 * phase) +
                0.15 * sin(0.5 * phase)
            val vibrato = 1.0 + 0.14 * sin(2.0 * PI * 5.5 * t)
            val fadeIn = (t / 0.65).coerceIn(0.0, 1.0)
            val fadeOut = ((durationSec - t) / 1.35).coerceIn(0.0, 1.0)
            val wind = rng.nextGaussian() * 0.045

            val sample = (harmonics * vibrato * fadeIn * fadeOut * 0.72) + wind
            buf[i] = sample
            maxAmp = maxOf(maxAmp, abs(sample))
        }
        return normalizeToPcm16(buf, maxAmp, 0.85)
    }

    private fun synthesizeSnowStrideGallop(durationSec: Double, cadenceSpm: Int, sampleRate: Int): ShortArray {
        val total = (sampleRate * durationSec).toInt()
        val buf = DoubleArray(total)
        val rng = Random(178L)
        val strideSamples = ((60.0 / cadenceSpm) * sampleRate).toInt().coerceAtLeast(1)
        val crunchLen = (0.12 * sampleRate).toInt()

        var startIdx = 0
        while (startIdx < total) {
            for (k in 0 until crunchLen) {
                val idx = startIdx + k
                if (idx >= total) break
                val decay = exp(-6.0 * k / crunchLen.toDouble())
                val pawThump = 0.42 * sin(2.0 * PI * 74.0 * (k.toDouble() / sampleRate)) * decay
                val snowCrunch = rng.nextGaussian() * 0.38 * decay
                buf[idx] += pawThump + snowCrunch
            }
            startIdx += strideSamples
        }

        var maxAmp = 1e-6
        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate
            buf[i] += 0.16 * sin(2.0 * PI * (cadenceSpm / 60.0) * t)
            maxAmp = maxOf(maxAmp, abs(buf[i]))
        }
        return normalizeToPcm16(buf, maxAmp, 0.80)
    }

    private fun synthesizeHeartRateGrowlAlert(durationSec: Double, sampleRate: Int): ShortArray {
        val total = (sampleRate * durationSec).toInt()
        val buf = DoubleArray(total)
        val rng = Random(505L)
        var growlPhase = 0.0
        var maxAmp = 1e-6

        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate
            val growlFreq = 92.0 + 14.0 * sin(2.0 * PI * 3.5 * t)
            growlPhase += 2.0 * PI * growlFreq / sampleRate
            val rasp = (sin(growlPhase) + 0.55 * sin(2.0 * growlPhase) + 0.25 * rng.nextGaussian()) * 0.48

            // Double heartbeat thud (lub-dub) repeating at ~1.2 Hz
            val beatCycle = t % 0.85
            val lub = exp(-42.0 * (beatCycle - 0.12).let { it * it }) * sin(2.0 * PI * 58.0 * beatCycle)
            val dub = exp(-50.0 * (beatCycle - 0.28).let { it * it }) * sin(2.0 * PI * 48.0 * beatCycle)

            val fadeIn = (t / 0.2).coerceIn(0.0, 1.0)
            val fadeOut = ((durationSec - t) / 0.45).coerceIn(0.0, 1.0)
            val sample = (rasp * 0.55 + (lub + dub) * 0.9) * fadeIn * fadeOut
            buf[i] = sample
            maxAmp = maxOf(maxAmp, abs(sample))
        }
        return normalizeToPcm16(buf, maxAmp, 0.85)
    }

    private fun synthesizePackOvertakeChimeBark(durationSec: Double, sampleRate: Int): ShortArray {
        val total = (sampleRate * durationSec).toInt()
        val buf = DoubleArray(total)
        val rng = Random(909L)
        var maxAmp = 1e-6

        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate
            // Glacial crystalline overtake chime (A5 -> E6)
            val chime1 = if (t < 0.7) exp(-5.0 * t) * sin(2.0 * PI * 880.0 * t) else 0.0
            val chime2 = if (t >= 0.22) exp(-3.5 * (t - 0.22)) * sin(2.0 * PI * 1318.5 * (t - 0.22)) else 0.0

            // Energetic double wolf bark at 0.85s and 1.25s
            var bark = 0.0
            for (barkStart in doubleArrayOf(0.85, 1.28)) {
                val dt = t - barkStart
                if (dt in 0.0..0.24) {
                    val barkFreq = 440.0 - 180.0 * (dt / 0.24)
                    val env = sin(PI * dt / 0.24)
                    bark += (sin(2.0 * PI * barkFreq * dt) + 0.35 * rng.nextGaussian()) * env * 0.65
                }
            }

            val sample = chime1 * 0.45 + chime2 * 0.50 + bark
            buf[i] = sample
            maxAmp = maxOf(maxAmp, abs(sample))
        }
        return normalizeToPcm16(buf, maxAmp, 0.84)
    }

    private fun synthesizeConservationPackChoir(durationSec: Double, sampleRate: Int): ShortArray {
        val total = (sampleRate * durationSec).toInt()
        val buf = DoubleArray(total)
        val rng = Random(777L)
        var p1 = 0.0
        var p2 = 0.0
        var p3 = 0.0
        var maxAmp = 1e-6

        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate
            val c1 = sin(PI * t / durationSec).coerceAtLeast(0.0).pow(1.4)
            val c2 = sin(PI * ((t - 0.35).coerceAtLeast(0.0)) / durationSec).coerceAtLeast(0.0).pow(1.6)
            val c3 = sin(PI * ((t - 0.65).coerceAtLeast(0.0)) / durationSec).coerceAtLeast(0.0).pow(1.3)

            // Three harmonized wolves across a fifth + octave interval
            val f1 = 290.0 + 250.0 * c1
            val f2 = 435.0 + 290.0 * c2
            val f3 = 580.0 + 220.0 * c3
            p1 += 2.0 * PI * f1 / sampleRate
            p2 += 2.0 * PI * f2 / sampleRate
            p3 += 2.0 * PI * f3 / sampleRate

            val w1 = (sin(p1) + 0.35 * sin(2.0 * p1)) * c1
            val w2 = (sin(p2) + 0.28 * sin(2.0 * p2)) * c2 * 0.72
            val w3 = (sin(p3) + 0.20 * sin(2.0 * p3)) * c3 * 0.50
            val wind = rng.nextGaussian() * 0.035

            val fadeIn = (t / 0.7).coerceIn(0.0, 1.0)
            val fadeOut = ((durationSec - t) / 1.4).coerceIn(0.0, 1.0)
            val sample = ((w1 + w2 + w3) * 0.45 * fadeIn * fadeOut) + wind
            buf[i] = sample
            maxAmp = maxOf(maxAmp, abs(sample))
        }
        return normalizeToPcm16(buf, maxAmp, 0.85)
    }

    private fun synthesizeSteepClimbGrowl(
        durationSec: Double,
        gradePercent: Double,
        sampleRate: Int
    ): ShortArray {
        val total = (sampleRate * durationSec).toInt()
        val buf = DoubleArray(total)
        val rng = Random(715L)
        val slopeScale = ((gradePercent - 5.0) / 10.0).coerceIn(0.0, 1.0)
        // Deeper, grittier chest register (68-105 Hz) that intensifies with steeper slopes
        val baseFreq = 68.0 + 34.0 * slopeScale
        var phase = 0.0
        var subPhase = 0.0
        var maxAmp = 1e-6

        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate
            val freq = baseFreq + 9.0 * sin(2.0 * PI * 2.4 * t) + 4.0 * sin(2.0 * PI * 19.0 * t)
            phase += 2.0 * PI * freq / sampleRate
            subPhase += 2.0 * PI * (freq * 0.5) / sampleRate

            val laryngealRasp = (sin(phase) + 0.62 * sin(2.0 * phase) + 0.38 * sin(3.0 * phase) + 0.45 * sin(subPhase))
            val throatGrit = rng.nextGaussian() * (0.22 + 0.16 * slopeScale) * (0.55 + 0.45 * sin(2.0 * PI * 14.0 * t))
            val fadeIn = (t / 0.45).coerceIn(0.0, 1.0)
            val fadeOut = ((durationSec - t) / 0.85).coerceIn(0.0, 1.0)
            val breathSwell = 0.72 + 0.28 * sin(PI * t / durationSec)

            val sample = (laryngealRasp * 0.58 + throatGrit) * fadeIn * fadeOut * breathSwell
            buf[i] = sample
            maxAmp = maxOf(maxAmp, abs(sample))
        }
        return normalizeToPcm16(buf, maxAmp, 0.86)
    }

    /**
     * Plays the celebratory Badge Unlock Stinger:
     * Ascending E-Major crystalline ice chimes (E5, G#5, B5, E6) + swelling Alpha/Beta wolf choir
     * howl + 55 Hz sub-bass impact thud with a 3-beat synced celebratory haptic cadence.
     */
    fun playBadgeUnlockStinger() {
        if (isMuted) return
        requestTransientDuckFocus()
        activeFxJob?.cancel()
        activeFxJob = audioScope.launch {
            launch {
                val timings = longArrayOf(0L, 140L, 110L, 110L, 140L, 240L)
                val amps = intArrayOf(0, 230, 0, 175, 0, 255)
                vibratePattern(timings, amps)
            }
            val sampleRate = 24000
            val pcm = synthesizeBadgeUnlockStinger(durationSec = 4.0, sampleRate = sampleRate)
            playPcm(pcm, sampleRate, ducked = false)
            abandonTransientAudioFocus()
        }
    }

    /**
     * Synthesizes & plays muffled crunching deep snow powder footfall.
     */
    fun playDeepPowderFootfall() {
        if (isMuted) return
        activeFxJob?.cancel()
        activeFxJob = audioScope.launch {
            val sampleRate = 24000
            val total = (sampleRate * 1.2).toInt()
            val buf = DoubleArray(total)
            val rng = Random(404L)
            var maxAmp = 1e-6
            for (i in 0 until total) {
                val t = i.toDouble() / sampleRate
                val noise = rng.nextGaussian() * 0.4 * exp(-t * 14.0) * 0.5
                val thump = sin(2.0 * PI * 70.0 * t) * exp(-t * 22.0) * 0.6
                val s = noise + thump
                buf[i] = s
                maxAmp = maxOf(maxAmp, abs(s))
            }
            playPcm(normalizeToPcm16(buf, maxAmp, 0.78), sampleRate, ducked = false)
        }
    }

    /**
     * Synthesizes & plays sharp echoing glacial lake ice crack.
     */
    fun playGlacialIceCrack() {
        if (isMuted) return
        activeFxJob?.cancel()
        activeFxJob = audioScope.launch {
            val sampleRate = 24000
            val total = (sampleRate * 2.0).toInt()
            val buf = DoubleArray(total)
            val rng = Random(909L)
            var maxAmp = 1e-6
            for (i in 0 until total) {
                val t = i.toDouble() / sampleRate
                val snap = rng.nextGaussian() * 0.8 * exp(-t * 40.0)
                val chirpFreq = 1800.0 * exp(-t * 8.0) + 120.0
                val chirp = sin(2.0 * PI * chirpFreq * t) * exp(-t * 3.5) * 0.4
                val s = snap + chirp
                buf[i] = s
                maxAmp = maxOf(maxAmp, abs(s))
            }
            playPcm(normalizeToPcm16(buf, maxAmp, 0.82), sampleRate, ducked = false)
        }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(250L)
            }
        } catch (_: Exception) {
        }
    }

    private fun synthesizeBadgeUnlockStinger(durationSec: Double, sampleRate: Int): ShortArray {
        val total = (sampleRate * durationSec).toInt()
        val buf = DoubleArray(total)
        val chimeFreqs = doubleArrayOf(659.25, 830.61, 987.77, 1318.51)
        val howlStart = 0.4
        val howlDur = (durationSec - howlStart).coerceAtLeast(0.5)
        var phase1 = 0.0
        var phase2 = 0.0
        var maxAmp = 1e-6

        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0

            // 1. Ascending crystalline ice chimes (E5, G#5, B5, E6)
            for (c in chimeFreqs.indices) {
                val delay = c * 0.12
                if (t >= delay) {
                    val tn = t - delay
                    val decay = exp(-tn * 4.5)
                    val f = chimeFreqs[c]
                    val chime = (sin(2.0 * PI * f * tn) +
                        0.4 * sin(2.0 * PI * f * 2.0 * tn) +
                        0.2 * sin(2.0 * PI * f * 3.0 * tn)) * decay
                    sample += chime * 0.25
                }
            }

            // 2. Dual-voice resonant Wolf Choir Howl
            if (t >= howlStart) {
                val th = t - howlStart
                val arch = sin(PI * (th / howlDur).coerceIn(0.0, 1.0)).coerceAtLeast(0.0).pow(1.8)
                val f1 = 340.0 + 240.0 * arch
                val f2 = 420.0 + 290.0 * arch
                phase1 += 2.0 * PI * f1 / sampleRate
                phase2 += 2.0 * PI * f2 / sampleRate
                val env = minOf(1.0, th / 0.8) * minOf(1.0, (howlDur - th) / 1.5).coerceAtLeast(0.0)
                val howl = (sin(phase1) + 0.6 * sin(phase2) + 0.3 * sin(2.0 * phase1)) * env
                sample += howl * 0.65
            }

            // 3. Low 55 Hz sub-bass impact thud
            sample += sin(2.0 * PI * 55.0 * t) * exp(-t * 8.0) * 0.5

            buf[i] = sample
            maxAmp = maxOf(maxAmp, abs(sample))
        }
        return normalizeToPcm16(buf, maxAmp, 0.88)
    }

    private fun normalizeToPcm16(samples: DoubleArray, maxAmp: Double, peakScale: Double): ShortArray {
        val pcm = ShortArray(samples.size)
        val scale = (peakScale / maxAmp.coerceAtLeast(1e-6)) * Short.MAX_VALUE
        for (i in samples.indices) {
            pcm[i] = (samples[i] * scale).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    private fun playPcm(
        samples: ShortArray,
        sampleRate: Int,
        ducked: Boolean,
        overrideGain: Float? = null
    ) {
        var track: AudioTrack? = null
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(samples.size * 2)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            activeTrack = track
            val baseVol = overrideGain ?: ambientVolume
            val vol = if (ducked) baseVol * 0.2f else baseVol
            track.setVolume(vol.coerceIn(0f, 1f))
            track.write(samples, 0, samples.size)
            track.play()
            val sleepMs = (samples.size * 1000L / sampleRate) + 120L
            Thread.sleep(sleepMs)
        } catch (_: InterruptedException) {
            // Cancelled cleanly by stopAll() or new trigger
        } catch (e: Exception) {
            Log.w(AUDIO_TAG, "Synthesized playback skipped: ${e.message}")
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {
            }
            if (activeTrack === track) activeTrack = null
        }
    }

    fun release() {
        stopAll()
        audioScope.cancel()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
