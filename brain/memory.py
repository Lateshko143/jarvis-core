import json
import os

MEMORY_FILE = os.path.expanduser("~/jarvis-core/brain/memory.json")

def load_memory():
    if not os.path.exists(MEMORY_FILE):
        return {"user_name": "Sir", "portfolio": {}, "notes": []}
    try:
        with open(MEMORY_FILE, "r") as f:
            return json.load(f)
    except Exception:
        return {"user_name": "Sir", "portfolio": {}, "notes": []}

def save_memory(data):
    with open(MEMORY_FILE, "w") as f:
        json.dump(data, f, indent=4)

def update_note(note):
    mem = load_memory()
    mem.setdefault("notes", []).append(note)
    save_memory(mem)

def get_context():
    mem = load_memory()
    return f"User: {mem.get('user_name')}, Active Notes: {len(mem.get('notes', []))}"
