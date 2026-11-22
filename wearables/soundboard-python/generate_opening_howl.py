#!/usr/bin/env python3
"""
Generates 'lone_wolf_opening_howl.wav' for app startup.
Requires: pip install numpy scipy
"""

import numpy as np
from scipy.io import wavfile


def generate_lone_wolf_opening_howl(
    filename: str = "lone_wolf_opening_howl.wav",
    sample_rate: int = 44100,
    duration: float = 4.2,
):
    t = np.linspace(0, duration, int(sample_rate * duration), endpoint=False)

    # 1. Frequency trajectory: 240 Hz chest register -> 580 Hz solitary peak -> cold dissolve
    freq_envelope = 240.0 + 340.0 * (np.sin(np.pi * (t / duration) ** 1.3) ** 1.8)
    phase = 2.0 * np.pi * np.cumsum(freq_envelope) / sample_rate

    howl = (
        0.60 * np.sin(phase)
        + 0.28 * np.sin(2.0 * phase)
        + 0.12 * np.sin(3.0 * phase)
        + 0.18 * np.sin(0.5 * phase)
    )

    vibrato = 1.0 + 0.08 * np.sin(2.0 * np.pi * 5.2 * t)
    howl = howl * vibrato

    fade_in = np.minimum(1.0, t / 0.8)
    fade_out = np.minimum(1.0, (duration - t) / 1.6)
    howl_env = fade_in * fade_out

    wind = np.random.normal(0, 0.03, len(t))
    wind_env = np.sin(np.pi * t / duration)

    audio = (howl * howl_env * 0.85) + (wind * wind_env * 0.15)
    audio_norm = audio / (np.max(np.abs(audio)) + 1e-6)
    audio_int16 = (audio_norm * 32767).astype(np.int16)

    wavfile.write(filename, sample_rate, audio_int16)
    print(f"[+] Generated lone wolf launch howl: {filename}")


if __name__ == "__main__":
    generate_lone_wolf_opening_howl()
