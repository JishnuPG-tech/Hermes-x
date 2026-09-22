"""TTS Manager orchestrator.

Handles:
- Primary / Fallback arbitration (Kokoro primary, Edge-TTS fallback).
- Circuit breaker: CLOSED -> OPEN -> HALF_OPEN.
- Hard fallback deadline (default 4000ms).
- Sub-300ms barge-in cancellation.
- Deduplication via tts_generation_id.
"""

from __future__ import annotations

import asyncio
import logging
import time
from typing import AsyncIterator, Dict, List, Optional, Set

from harness.voice.models import (
    AudioFormat,
    ProviderHealth,
    TTSGenerationRequest,
    TTSGenerationResult,
)
from harness.voice.tts.base import TTSProvider
from harness.voice.tts.edge_tts_provider import EdgeTTSProvider
from harness.voice.tts.kokoro_provider import KokoroProvider

logger = logging.getLogger(__name__)


class CircuitState:
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


class CircuitBreaker:
    """Manages failure threshold and cooldown period for a TTS provider."""

    def __init__(
        self,
        failure_threshold: int = 3,
        cooldown_seconds: float = 30.0,
    ) -> None:
        self.failure_threshold = failure_threshold
        self.cooldown_seconds = cooldown_seconds
        self.state = CircuitState.CLOSED
        self.failure_count = 0
        self.last_failure_time: float = 0.0

    def record_success(self) -> None:
        self.state = CircuitState.CLOSED
        self.failure_count = 0

    def record_failure(self) -> None:
        self.failure_count += 1
        self.last_failure_time = time.monotonic()
        if self.failure_count >= self.failure_threshold:
            self.state = CircuitState.OPEN
            logger.warning(
                "Circuit breaker tripped to OPEN after %d consecutive failures",
                self.failure_count,
            )

    def can_attempt(self) -> bool:
        if self.state == CircuitState.CLOSED:
            return True
        if self.state == CircuitState.OPEN:
            now = time.monotonic()
            if now - self.last_failure_time >= self.cooldown_seconds:
                self.state = CircuitState.HALF_OPEN
                logger.info("Circuit breaker transitioning to HALF_OPEN trial")
                return True
            return False
        if self.state == CircuitState.HALF_OPEN:
            return True
        return False


class TTSManager:
    """Orchestrates TTS synthesis across primary and fallback engines."""

    def __init__(
        self,
        kokoro_voice: str = "am_adam",
        edge_voice: str = "en-US-ChristopherNeural",
        fallback_deadline_seconds: float = 4.0,
        enable_kokoro: bool = True,
    ) -> None:
        self.fallback_deadline = fallback_deadline_seconds
        self._providers: Dict[str, TTSProvider] = {}
        self._circuits: Dict[str, CircuitBreaker] = {}
        self._processed_generations: Set[str] = set()
        self._active_cancel_events: Dict[str, asyncio.Event] = {}

        if enable_kokoro:
            self._providers["kokoro"] = KokoroProvider(default_voice=kokoro_voice)
            self._circuits["kokoro"] = CircuitBreaker(failure_threshold=3, cooldown_seconds=30.0)

        self._providers["edge_tts"] = EdgeTTSProvider(default_voice=edge_voice)
        self._circuits["edge_tts"] = CircuitBreaker(failure_threshold=5, cooldown_seconds=20.0)

        self._initialized = False

    async def initialize(self) -> None:
        """Initialize all registered TTS engines in parallel."""
        if self._initialized:
            return
        tasks = [p.initialize() for p in self._providers.values()]
        await asyncio.gather(*tasks, return_exceptions=True)
        self._initialized = True
        logger.info(
            "TTSManager initialized. Available providers: %s",
            list(self._providers.keys()),
        )

    def cancel_generation(self, session_id: Optional[str] = None) -> None:
        """Immediate barge-in cancellation (<300ms SLA)."""
        if session_id and session_id in self._active_cancel_events:
            self._active_cancel_events[session_id].set()
            logger.info("Barge-in: Signaled cancellation for session %s", session_id)
        else:
            for sid, ev in self._active_cancel_events.items():
                ev.set()
                logger.info("Barge-in: Signaled cancellation for all active sessions")

    def register_session(self, session_id: str) -> asyncio.Event:
        ev = asyncio.Event()
        self._active_cancel_events[session_id] = ev
        return ev

    def unregister_session(self, session_id: str) -> None:
        self._active_cancel_events.pop(session_id, None)

    async def get_health_status(self) -> Dict[str, ProviderHealth]:
        """Query health of all underlying providers."""
        results = {}
        for name, p in self._providers.items():
            health = await p.check_health()
            circuit = self._circuits.get(name)
            if circuit:
                health.circuit_state = circuit.state
                health.consecutive_failures = circuit.failure_count
            results[name] = health
        return results

    async def synthesize(self, request: TTSGenerationRequest) -> TTSGenerationResult:
        """Synthesize audio with primary -> fallback failover and deadline."""
        if not self._initialized:
            await self.initialize()

        # Deduplication check
        if request.tts_generation_id in self._processed_generations:
            logger.info("Deduplication: Dropping duplicate tts_generation_id %s", request.tts_generation_id)
            return TTSGenerationResult(
                request_id=request.request_id,
                audio_bytes=b"",
                format=AudioFormat.MP3,
                sample_rate=24000,
                latency_ms=0.0,
                provider="cache",
                voice_used=request.voice or "none",
                success=True,
            )

        # 1. Try Kokoro if registered, ready, and circuit allows
        kokoro = self._providers.get("kokoro")
        kokoro_circuit = self._circuits.get("kokoro")

        if kokoro and getattr(kokoro, "_is_ready", False) and kokoro_circuit and kokoro_circuit.can_attempt():
            try:
                # Run with fallback deadline
                result = await asyncio.wait_for(
                    kokoro.synthesize(request),
                    timeout=self.fallback_deadline,
                )
                if result.success and len(result.audio_bytes) > 0:
                    kokoro_circuit.record_success()
                    self._processed_generations.add(request.tts_generation_id)
                    return result
                else:
                    kokoro_circuit.record_failure()
                    logger.warning("Kokoro synthesis unsuccessful (%s), triggering EdgeTTS fallback", result.error)
            except asyncio.TimeoutError:
                kokoro_circuit.record_failure()
                logger.warning("Kokoro exceeded %0.1fs deadline, triggering EdgeTTS fallback", self.fallback_deadline)
            except Exception as exc:
                kokoro_circuit.record_failure()
                logger.warning("Kokoro raised error: %s. Triggering EdgeTTS fallback", exc)

        # 2. Fallback to EdgeTTS
        edge = self._providers.get("edge_tts")
        edge_circuit = self._circuits.get("edge_tts")
        if edge and (not edge_circuit or edge_circuit.can_attempt()):
            try:
                result = await edge.synthesize(request)
                if result.success:
                    if edge_circuit:
                        edge_circuit.record_success()
                    self._processed_generations.add(request.tts_generation_id)
                    return result
                else:
                    if edge_circuit:
                        edge_circuit.record_failure()
                    logger.error("EdgeTTS fallback synthesis failed: %s", result.error)
                    return result
            except Exception as exc:
                if edge_circuit:
                    edge_circuit.record_failure()
                logger.error("EdgeTTS synthesis error: %s", exc)
                return TTSGenerationResult(
                    request_id=request.request_id,
                    audio_bytes=b"",
                    format=AudioFormat.MP3,
                    sample_rate=24000,
                    latency_ms=0.0,
                    provider="edge_tts",
                    voice_used=request.voice or "default",
                    success=False,
                    error=str(exc),
                )

        return TTSGenerationResult(
            request_id=request.request_id,
            audio_bytes=b"",
            format=AudioFormat.MP3,
            sample_rate=24000,
            latency_ms=0.0,
            provider="none",
            voice_used="none",
            success=False,
            error="All TTS providers unavailable",
        )

    async def stream_audio_chunks(
        self, request: TTSGenerationRequest
    ) -> AsyncIterator[bytes]:
        """Stream audio chunks with failover support."""
        if not self._initialized:
            await self.initialize()

        kokoro = self._providers.get("kokoro")
        kokoro_circuit = self._circuits.get("kokoro")

        # 1. Try Kokoro stream if ready and circuit permits
        if kokoro and getattr(kokoro, "_is_ready", False) and kokoro_circuit and kokoro_circuit.can_attempt():
            try:
                async def _get_first():
                    async for chunk in kokoro.synthesize_stream(request):
                        return chunk
                    return None

                first_chunk = await asyncio.wait_for(_get_first(), timeout=self.fallback_deadline)
                if first_chunk:
                    kokoro_circuit.record_success()
                    yield first_chunk
                    async for chunk in kokoro.synthesize_stream(request):
                        yield chunk
                    return
                else:
                    kokoro_circuit.record_failure()
            except (asyncio.TimeoutError, Exception) as exc:
                kokoro_circuit.record_failure()
                logger.warning("Kokoro streaming failed (%s), shifting to EdgeTTS stream", exc)

        # 2. Seamless failover to EdgeTTS stream
        edge = self._providers.get("edge_tts")
        edge_circuit = self._circuits.get("edge_tts")
        if edge and (not edge_circuit or edge_circuit.can_attempt()):
            try:
                async for chunk in edge.synthesize_stream(request):
                    if edge_circuit:
                        edge_circuit.record_success()
                    yield chunk
            except Exception as exc:
                if edge_circuit:
                    edge_circuit.record_failure()
                logger.error("EdgeTTS stream failed: %s", exc)
