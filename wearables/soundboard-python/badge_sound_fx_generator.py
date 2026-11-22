#!/usr/bin/env python3
"""
================================================================================
STRIDERS HOWL: AUDIO EFFECTS & BADGE STINGER SYNTHESIZER
================================================================================
Requires: pip install numpy scipy
Generates high-fidelity WAV sound effects for badge unlocks and winter trails.
"""

import os
import numpy as np
from scipy.io import wavfile

SAMPLE_RATE = 44100


def synth_badge_unlock_stinger(duration_sec: float = 4.0) -> np.ndarray:
    """
    Synthesizes a celebratory milestone stinger:
    Ascending crystalline chime arpeggio + swelling pack choir howl + low resonant sub-thud.
    """
    t = np.linspace(0, duration_sec, int(SAMPLE_RATE * duration_sec), endpoint=False)
    audio = np.zeros_like(t)

    # 1. Crystalline Ice Chimes (E Major triad: E5, G#5, B5, E6)
    chime_freqs = [659.25, 830.61, 987.77, 1318.51]
    for i, freq in enumerate(chime_freqs):
        delay = i * 0.12
        t_note = np.maximum(0, t - delay)
        decay = np.exp(-t_note * 4.5)
        chime = (
            np.sin(2 * np.pi * freq * t_note)
            + 0.4 * np.sin(2 * np.pi * freq * 2.0 * t_note)
            + 0.2 * np.sin(2 * np.pi * freq * 3.0 * t_note)
        ) * decay * (t >= delay)
        audio += chime * 0.25

    # 2. Resonant Wolf Choir Howl (Swells after chimes)
    howl_start = 0.4
    t_howl = np.maximum(0, t - howl_start)
    howl_dur = duration_sec - howl_start

    f1 = 340 + 240 * (np.sin(np.pi * (t_howl / howl_dur)) ** 1.8)
    f2 = 420 + 290 * (np.sin(np.pi * (t_howl / howl_dur)) ** 1.8)

    phase1 = 2 * np.pi * np.cumsum(f1) / SAMPLE_RATE
    phase2 = 2 * np.pi * np.cumsum(f2) / SAMPLE_RATE

    howl_env = (
        np.minimum(1.0, t_howl / 0.8)
        * np.minimum(1.0, (howl_dur - t_howl) / 1.5)
        * (t >= howl_start)
    )
    howl_sound = (np.sin(phase1) + 0.6 * np.sin(phase2) + 0.3 * np.sin(2 * phase1)) * howl_env
    audio += howl_sound * 0.65

    # 3. Low Sub-Bass Ground Thud (Impact at t = 0)
    sub_decay = np.exp(-t * 8.0)
    sub_thud = np.sin(2 * np.pi * 55.0 * t) * sub_decay * 0.5
    audio += sub_thud

    return audio


def synth_deep_powder_footfall(duration_sec: float = 1.2) -> np.ndarray:
    """Synthesizes the muffled, crunching sound of snow collapsing underfoot."""
    t = np.linspace(0, duration_sec, int(SAMPLE_RATE * duration_sec), endpoint=False)
    noise = np.random.normal(0, 0.4, len(t))
    envelope = np.exp(-t * 14.0)
    thump = np.sin(2 * np.pi * 70.0 * t) * np.exp(-t * 22.0) * 0.6
    return (noise * envelope * 0.5) + thump


def synth_glacial_ice_crack(duration_sec: float = 2.0) -> np.ndarray:
    """Synthesizes a sharp, echoing snap of frozen lake ice."""
    t = np.linspace(0, duration_sec, int(SAMPLE_RATE * duration_sec), endpoint=False)
    snap = np.random.normal(0, 0.8, len(t)) * np.exp(-t * 40.0)
    chirp_freq = 1800 * np.exp(-t * 8.0) + 120
    chirp = np.sin(2 * np.pi * chirp_freq * t) * np.exp(-t * 3.5) * 0.4
    return snap + chirp


if __name__ == "__main__":
    os.makedirs("soundboard_assets", exist_ok=True)
    effects = {
        "badge_unlock_stinger.wav": synth_badge_unlock_stinger(),
        "snow_powder_footfall.wav": synth_deep_powder_footfall(),
        "glacial_ice_crack.wav": synth_glacial_ice_crack(),
    }
    print("Generating custom audio effects...")
    for filename, wave in effects.items():
        norm_wave = wave / (np.max(np.abs(wave)) + 1e-6)
        int16_wave = (norm_wave * 32767).astype(np.int16)
        out_path = os.path.join("soundboard_assets", filename)
        wavfile.write(out_path, SAMPLE_RATE, int16_wave)
        print(f"[OK] Saved: {out_path}")
