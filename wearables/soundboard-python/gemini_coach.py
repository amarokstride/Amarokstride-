"""
Striders Howl - Gemini Audio Coach & Wolf Pack Soundboard Engine (Companion Generator)
Requires: pip install -r requirements.txt
Set GEMINI_API_KEY in your environment before running.
"""

import os
import sys
import numpy as np
from scipy.io import wavfile
from google import genai
from google.genai import types

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    raise RuntimeError(
        "GEMINI_API_KEY environment variable is required to run the standalone Python audio generator."
    )

client = genai.Client(api_key=GEMINI_API_KEY)


def generate_wolf_howl(duration_s: float = 4.0, sample_rate: int = 24000):
    """Synthesizes an organic, atmospheric wolf howl with pitch glide and vibrato."""
    t = np.linspace(0, duration_s, int(sample_rate * duration_s), endpoint=False)
    freq_env = 320 + 320 * (np.sin(np.pi * t / duration_s) ** 1.5)
    phase = 2 * np.pi * np.cumsum(freq_env) / sample_rate

    howl = (
        np.sin(phase)
        + 0.45 * np.sin(2 * phase)
        + 0.25 * np.sin(3 * phase)
        + 0.15 * np.sin(0.5 * phase)
    )
    vibrato = 1.0 + 0.15 * np.sin(2 * np.pi * 5.5 * t)
    fade_in = np.minimum(1.0, t / 0.8)
    fade_out = np.minimum(1.0, (duration_s - t) / 1.5)
    env = fade_in * fade_out

    wind = np.random.normal(0, 0.05, len(t))
    audio = (howl * vibrato * env * 0.75) + (wind * 0.1)
    audio_int16 = (audio / (np.max(np.abs(audio)) + 1e-6) * 32767).astype(np.int16)
    return sample_rate, audio_int16


def generate_running_strides_snow(
    duration_s: float = 3.0, cadence_spm: int = 178, sample_rate: int = 24000
):
    """Synthesizes rhythmic galloping and snow-crunch strides."""
    total_samples = int(sample_rate * duration_s)
    audio = np.zeros(total_samples)
    stride_samples = int((60.0 / cadence_spm) * sample_rate)

    for start_idx in range(0, total_samples, stride_samples):
        crunch_len = int(0.12 * sample_rate)
        if start_idx + crunch_len < total_samples:
            noise = np.random.normal(0, 0.4, crunch_len)
            decay = np.exp(-np.linspace(0, 6, crunch_len))
            audio[start_idx : start_idx + crunch_len] += noise * decay

    t = np.linspace(0, duration_s, total_samples)
    audio += 0.3 * np.sin(2 * np.pi * (cadence_spm / 60.0) * t)
    audio_int16 = (audio / (np.max(np.abs(audio)) + 1e-6) * 28000).astype(np.int16)
    return sample_rate, audio_int16


SYSTEM_COACH_INSTRUCTION = """
You are 'The Alpha'—the legendary running and endurance coach for Striders Howl.
Your persona is grounded, stoic, athletic, and fiercely motivating, like a seasoned wilderness guide
leading a wolf pack across frozen mountain passes.

Guidance Rules:
- Keep prompts under 25 words. Runners are moving at high speed and need clear, impactful cues.
- Voice Tone: Deep, calm, steady, and resonant.
- Metaphors: Pack resilience, breath control in cold air, striding in rhythm, hunting the horizon.
- Never use robotic language or filler greetings. Deliver the exact tactical audio cue.
"""


def request_gemini_audio_prompt(scenario: str, current_pace: str, heart_rate: int) -> bytes:
    """Requests spoken audio from Gemini 3.1 Flash TTS using the Charon voice."""
    prompt = f"""
    The runner is currently on a trail run.
    - Scenario: {scenario}
    - Current Pace: {current_pace}
    - Current Heart Rate: {heart_rate} bpm

    Provide an instantaneous, 1-to-2 sentence audio coaching cue to motivate or guide them.
    """

    response = client.models.generate_content(
        model="gemini-3.1-flash-tts-preview",
        contents=prompt,
        config=types.GenerateContentConfig(
            system_instruction=SYSTEM_COACH_INSTRUCTION,
            temperature=0.7,
            response_modalities=["AUDIO"],
            speech_config=types.SpeechConfig(
                voice_config=types.VoiceConfig(
                    prebuilt_voice_config=types.PrebuiltVoiceConfig(voice_name="Charon")
                )
            ),
        ),
    )

    for candidate in response.candidates or []:
        for part in candidate.content.parts or []:
            if getattr(part, "inline_data", None):
                return part.inline_data.data
    return b""


def create_soundboard_cue(scenario: str, pace: str, hr: int, output_file: str):
    """Generates a Gemini spoken cue or procedural wolf pack sound stem."""
    print(f"[*] Generating Gemini Alpha audio cue for: {scenario}...")
    voice_bytes = request_gemini_audio_prompt(scenario, pace, hr)

    rate, howl_data = generate_wolf_howl(duration_s=4.5)
    _, stride_data = generate_running_strides_snow(duration_s=4.5, cadence_spm=178)
    pack_ambient = (
        howl_data.astype(np.float32) * 0.6 + stride_data.astype(np.float32) * 0.4
    ).astype(np.int16)

    if voice_bytes:
        with open(output_file, "wb") as f:
            f.write(voice_bytes)
        print(f"[+] Alpha voice cue saved to {output_file}")
    else:
        wavfile.write(output_file, rate, pack_ambient)
        print(f"[!] Fallback procedural wolf stem saved to {output_file}")


if __name__ == "__main__":
    create_soundboard_cue(
        scenario="Milestone unlocked: Half Marathon (13.1 Miles)",
        pace="7:15 /mi",
        hr=162,
        output_file=sys.argv[1] if len(sys.argv) > 1 else "cue_milestone_howl.wav",
    )
