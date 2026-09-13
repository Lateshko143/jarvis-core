import sys
import os
import re
from intel import get_market_summary, get_live_news, web_search
from voice import speak

def execute_command(user_text):
    if not user_text:
        return
    text = user_text.lower().strip()
    print(f"\n[USER COMMAND]: {text}")

    # 1. Share Market & Stock Analysis (Natural Detection)
    market_words = ["market", "share", "nifty", "sensex", "stock", "bazar", "bazaar", "paiso ka", "nifty50"]
    if any(w in text for w in market_words):
        speak("Market ka live status check kar raha hoon sir.")
        summary = get_market_summary()
        speak(f"Sir, {summary}")
        return

    # 2. Navigation / Home Action
    home_words = ["home", "homepage", "screen", "piche", "bahar", "band karo"]
    if any(w in text for w in home_words) and ("go" in text or "jao" in text or "aao" in text or "home" == text):
        speak("Home screen par jaa raha hoon.")
        os.system("am start -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null")
        return

    # 3. Live News & Global Situation (Desi + English understanding)
    news_words = ["news", "khabar", "samachar", "kya chal raha", "taaza", "aaj kya hua", "updates"]
    if any(w in text for w in news_words):
        speak("Live news feed scan kar raha hoon sir.")
        news_data = get_live_news()
        speak(news_data)
        return

    # 4. App Launching Command
    if "open" in text or "kholo" in text or "chalao" in text:
        clean_target = re.sub(r'(open|kholo|chalao|app|application)', '', text).strip()
        if clean_target:
            speak(f"{clean_target} open kar raha hoon sir.")
            os.system(f"am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $(pm list packages | grep {clean_target} | head -n 1 | cut -d: -f2) 2>/dev/null")
            return

    # 5. Smart Internet Search & General Q&A
    speak("Data analyze kar raha hoon sir, ek second.")
    clean_query = re.sub(r'(batao|kya hai|kaun hai|search|karo|bataiye)', '', text).strip()
    query_to_run = clean_query if clean_query else text
    ans = web_search(query_to_run)
    speak(f"Sir, {ans}")

if __name__ == "__main__":
    test_input = " ".join(sys.argv[1:]) if len(sys.argv) > 1 else "aaj bazaar kaisa hai"
    execute_command(test_input)
