package com.example.data.safety

import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

enum class HeadphoneHardwareAction {
    SINGLE_TAP_VOICE_CUE,
    DOUBLE_TAP_ALPHA_HOWL,
    LONG_PRESS_SOS,
    VOLUME_UP_GROWL_SENSITIVITY,
    VOLUME_DOWN_GROWL_SENSITIVITY
}

/**
 * Native Android hardware key & Bluetooth bone-conduction headset (Shokz / media hook)
 * remapper for gloved winter trail running.
 *
 * Mapping:
 * - Earbud Play/Pause / HeadsetHook 1x tap -> Voice Telemetry Callout
 * - Earbud Play/Pause / HeadsetHook 2x tap -> Acoustic Pack Howl
 * - Earbud button hold >= 1.8s OR Volume Down hold >= 2.5s -> Winter SOS 45s Countdown
 * - Volume Up / Down short tap -> Adjust Steep-Climb Growl threshold sensitivity
 */
object HardwareKeyController {

    const val DOUBLE_TAP_WINDOW_MS = 350L
    const val HEADSET_LONG_PRESS_MS = 1800L
    const val VOLUME_SOS_LONG_PRESS_MS = 2500L

    @Volatile
    var isGloveHardwareModeEnabled: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _events = MutableSharedFlow<HeadphoneHardwareAction>(extraBufferCapacity = 16)
    val events: SharedFlow<HeadphoneHardwareAction> = _events.asSharedFlow()

    private var headsetDownMs: Long? = null
    private var volumeDownHoldMs: Long? = null
    private var tapCount = 0
    private var tapWindowJob: Job? = null

    fun isHeadsetMediaKey(keyCode: Int): Boolean = keyCode in setOf(
        KeyEvent.KEYCODE_HEADSETHOOK,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_MEDIA_NEXT
    )

    fun handleAndroidKeyEvent(event: KeyEvent): Boolean {
        if (!isGloveHardwareModeEnabled) return false
        val nowMs = System.currentTimeMillis()
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> onKeyDown(event.keyCode, event.repeatCount, nowMs)
            KeyEvent.ACTION_UP -> onKeyUp(event.keyCode, nowMs)
            else -> false
        }
    }

    internal fun onKeyDown(keyCode: Int, repeatCount: Int, nowMs: Long): Boolean {
        if (isHeadsetMediaKey(keyCode)) {
            if (headsetDownMs == null) headsetDownMs = nowMs
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (repeatCount == 0) {
                _events.tryEmit(HeadphoneHardwareAction.VOLUME_UP_GROWL_SENSITIVITY)
            }
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (volumeDownHoldMs == null) {
                volumeDownHoldMs = nowMs
            } else if (nowMs - (volumeDownHoldMs ?: nowMs) >= VOLUME_SOS_LONG_PRESS_MS) {
                volumeDownHoldMs = Long.MAX_VALUE // consumed as SOS
                _events.tryEmit(HeadphoneHardwareAction.LONG_PRESS_SOS)
            }
            return true
        }
        return false
    }

    internal fun onKeyUp(keyCode: Int, nowMs: Long): Boolean {
        if (isHeadsetMediaKey(keyCode)) {
            val down = headsetDownMs ?: return false
            headsetDownMs = null
            val heldMs = nowMs - down
            if (heldMs >= HEADSET_LONG_PRESS_MS) {
                tapCount = 0
                tapWindowJob?.cancel()
                _events.tryEmit(HeadphoneHardwareAction.LONG_PRESS_SOS)
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                _events.tryEmit(HeadphoneHardwareAction.DOUBLE_TAP_ALPHA_HOWL)
                return true
            }
            registerHeadsetTap()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val down = volumeDownHoldMs
            volumeDownHoldMs = null
            if (down != null && down != Long.MAX_VALUE) {
                val heldMs = nowMs - down
                if (heldMs >= VOLUME_SOS_LONG_PRESS_MS) {
                    _events.tryEmit(HeadphoneHardwareAction.LONG_PRESS_SOS)
                } else {
                    _events.tryEmit(HeadphoneHardwareAction.VOLUME_DOWN_GROWL_SENSITIVITY)
                }
            }
            return true
        }
        return false
    }

    private fun registerHeadsetTap() {
        tapCount += 1
        tapWindowJob?.cancel()
        tapWindowJob = scope.launch {
            delay(DOUBLE_TAP_WINDOW_MS)
            val action = if (tapCount >= 2) {
                HeadphoneHardwareAction.DOUBLE_TAP_ALPHA_HOWL
            } else {
                HeadphoneHardwareAction.SINGLE_TAP_VOICE_CUE
            }
            tapCount = 0
            _events.tryEmit(action)
        }
    }

    fun emitManualSimulation(action: HeadphoneHardwareAction) {
        _events.tryEmit(action)
    }

    fun resetState() {
        headsetDownMs = null
        volumeDownHoldMs = null
        tapCount = 0
        tapWindowJob?.cancel()
    }
}
