import asyncio
import subprocess
import os

VOICE_HINDI = "hi-IN-MadhurNeural"

async def speak_jarvis_hindi(text):
    clean_text = text.replace('"', '').replace("'", "")
    output_file = "/data/data/com.termux/files/home/jarvis-core/brain/speech.mp3"
    
    # Rate: +22% (fast & snappy, slow-motion khatam)
    # Pitch: -5Hz (thodi heavy/deep authoritative Jarvis base)
    cmd = f'edge-tts --voice "{VOICE_HINDI}" --text "{clean_text}" --rate="+22%" --pitch="-5Hz" --write-media "{output_file}"'
    proc = await asyncio.create_subprocess_shell(cmd)
    await proc.communicate()
    
    if os.path.exists(output_file):
        # High clarity audio output
        subprocess.run(f'mpv --no-video --really-quiet "{output_file}"', shell=True)

def speak(text):
    asyncio.run(speak_jarvis_hindi(text))

if __name__ == "__main__":
    test_phrase = "Namaste sir. Main Jarvis hoon. Sabhi systems full speed par active hain. Bataiye, market analysis dekhna hai ya koi app operate karna hai?"
    speak(test_phrase)
