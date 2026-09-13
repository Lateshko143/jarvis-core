import subprocess
import json
import time
import os
from jarvis_brain import execute_command
from voice import speak

def get_voice_input():
    """Captures speech from Phone Mic or Bluetooth SCO Headset"""
    try:
        cmd = ["termux-speech-to-text"]
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=12)
        text = proc.stdout.strip()
        return text if text else ""
    except subprocess.TimeoutExpired:
        return ""
    except Exception as e:
        print(f"[MIC ERROR]: {e}")
        return ""

def start_voice_assistant():
    speak("Voice command loop active hai sir. Main sun raha hoon.")
    print("\n" + "="*45)
    print("  JARVIS HANDS-FREE VOICE PIPELINE ACTIVE")
    print("  (Phone Mic & Bluetooth Headset Ready)")
    print("="*45 + "\n")

    while True:
        try:
            print("[LISTENING...]")
            user_speech = get_voice_input()

            if user_speech:
                print(f"\n[DETECTED VOICE]: {user_speech}")
                clean_speech = user_speech.lower().strip()

                if any(w in clean_speech for w in ["exit", "sleep", "stop", "khatam"]):
                    speak("Standing down, sir. Good night.")
                    print("[JARVIS STANDBY]")
                    break

                execute_command(user_speech)

            time.sleep(0.3)
        except KeyboardInterrupt:
            speak("Voice systems suspended.")
            break

if __name__ == "__main__":
    start_voice_assistant()
