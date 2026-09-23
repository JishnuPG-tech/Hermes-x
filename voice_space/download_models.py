"""Pre-download ASR and VAD models at Docker image build time.
Baking models into the image guarantees zero model download delay on cold starts.
"""

import os
import urllib.request
from faster_whisper import WhisperModel

MODEL_DIR = os.environ.get("MODEL_CACHE_DIR", "/app/models")
os.makedirs(f"{MODEL_DIR}/whisper", exist_ok=True)
os.makedirs(f"{MODEL_DIR}/vad", exist_ok=True)
os.makedirs(f"{MODEL_DIR}/piper", exist_ok=True)

def download_vad():
    vad_url = "https://raw.githubusercontent.com/snakers4/silero-vad/master/src/silero_vad/data/silero_vad.onnx"
    vad_dest = os.path.join(MODEL_DIR, "vad", "silero_vad.onnx")
    if not os.path.exists(vad_dest):
        print(f"Downloading Silero VAD ONNX to {vad_dest}...")
        urllib.request.urlretrieve(vad_url, vad_dest)
        print("Silero VAD downloaded successfully.")
    else:
        print("Silero VAD already cached.")

def download_whisper():
    print("Downloading and caching faster-whisper tiny.en int8 checkpoint...")
    WhisperModel(
        "tiny.en",
        device="cpu",
        compute_type="int8",
        download_root=os.path.join(MODEL_DIR, "whisper"),
    )
    print("faster-whisper tiny.en cached successfully.")

def download_piper_voice():
    # Piper English voice: en_US-lessac-medium
    voice_onnx = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/lessac/medium/en_US-lessac-medium.onnx"
    voice_json = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/lessac/medium/en_US-lessac-medium.onnx.json"
    
    onnx_dest = os.path.join(MODEL_DIR, "piper", "en_US-lessac-medium.onnx")
    json_dest = os.path.join(MODEL_DIR, "piper", "en_US-lessac-medium.onnx.json")
    
    if not os.path.exists(onnx_dest):
        print(f"Downloading Piper ONNX voice model to {onnx_dest}...")
        try:
            urllib.request.urlretrieve(voice_onnx, onnx_dest)
            urllib.request.urlretrieve(voice_json, json_dest)
            print("Piper voice model cached successfully.")
        except Exception as e:
            print(f"Warning: Failed to download Piper voice during build ({e}). Will fallback to edge-tts if unavailable.")

if __name__ == "__main__":
    download_vad()
    download_whisper()
    download_piper_voice()
    print("All voice models baked into image successfully.")
