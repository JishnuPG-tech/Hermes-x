"""Uniform TTS Provider Protocol & Interface Definitions.

Defines the abstract contract all TTS engines (Kokoro, Edge-TTS, etc.) implement.
"""

from __future__ import annotations

from typing import AsyncIterator, List, Protocol
from harness.voice.models import AudioFormat, ProviderHealth, TTSGenerationRequest, TTSGenerationResult


class TTSProvider(Protocol):
    """Uniform contract for all speech synthesis providers."""

    @property
    def name(self) -> str:
        """Provider identifier string (e.g. 'kokoro', 'edge_tts')."""
        ...

    @property
    def is_local(self) -> bool:
        """Whether this provider executes entirely locally without network calls."""
        ...

    @property
    def supported_formats(self) -> List[AudioFormat]:
        """List of audio formats this provider can produce."""
        ...

    async def initialize(self) -> bool:
        """Prepare engine, load models/warm up connections."""
        ...

    async def check_health(self) -> ProviderHealth:
        """Probe and return the current operational status of the engine."""
        ...

    async def synthesize(self, request: TTSGenerationRequest) -> TTSGenerationResult:
        """Synthesize text into a single complete audio buffer."""
        ...

    async def synthesize_stream(
        self, request: TTSGenerationRequest
    ) -> AsyncIterator[bytes]:
        """Stream synthesized audio chunks as they become available."""
        ...
