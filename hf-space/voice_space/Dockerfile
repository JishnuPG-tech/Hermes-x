FROM python:3.10-slim

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    MODEL_CACHE_DIR=/app/models \
    PORT=7860

WORKDIR /app

# Install system dependencies (ffmpeg for transcoding, curl, piper-tts dependencies)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    curl \
    git \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install Python requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Create models directory and bake models into image for instant zero-latency cold-start
RUN mkdir -p /app/models/whisper /app/models/vad /app/models/piper
COPY download_models.py .
RUN python download_models.py

# Copy application source
COPY . .

# Hugging Face default user (UID 1000)
RUN useradd -m -u 1000 user && \
    chown -R user:user /app
USER user

EXPOSE 7860

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7860"]
