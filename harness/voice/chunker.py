"""
Streaming Sentence & Phrase Chunker
===================================
Splits streamed LLM text tokens into natural clause/sentence boundaries
for low-latency TTS streaming while sanitizing code fences and markdown.
"""
from __future__ import annotations

import re
from typing import Generator, List, Optional


class SentenceChunker:
    """
    Incremental phrase/sentence buffer designed for low-latency voice synthesis.
    Emits natural speech chunks as soon as a complete sentence or clause boundary is detected.
    """
    def __init__(self, min_chars: int = 30, max_chars: int = 180):
        self.min_chars = min_chars
        self.max_chars = max_chars
        self._buffer = ""
        self._in_code_block = False

    def push(self, delta: str) -> List[str]:
        """Adds a new text delta and yields complete speakable phrase chunks."""
        self._buffer += delta
        return self._extract_phrases(force=False)

    def feed(self, delta: str) -> List[str]:
        """Alias for push."""
        return self.push(delta)

    def flush(self) -> List[str]:
        """Flushes remaining text in the buffer when the stream completes."""
        return self._extract_phrases(force=True)

    def _clean_text(self, text: str) -> str:
        """Sanitizes text for clean, natural speech synthesis."""
        # Replace code blocks with concise spoken notice
        text = re.sub(r'```[\s\S]*?```', ' (code block omitted) ', text)
        # Handle unclosed code block starts
        text = re.sub(r'```[a-zA-Z0-9_-]*', ' (code block) ', text)
        # Strip inline code backticks
        text = re.sub(r'`([^`]+)`', r'\1', text)
        # Strip Markdown links: [text](url) -> text
        text = re.sub(r'\[([^\]]+)\]\([^\)]+\)', r'\1', text)
        # Strip bare URLs
        text = re.sub(r'https?://\S+', '', text)
        # Strip markdown formatting
        text = re.sub(r'[#*_~>]+', '', text)
        # Strip XML/HTML tags
        text = re.sub(r'<[^>]+>', '', text)
        # Collapse repeated whitespace
        text = re.sub(r'\s+', ' ', text).strip()
        return text

    def _extract_phrases(self, force: bool = False) -> List[str]:
        phrases: List[str] = []
        
        while self._buffer:
            clean_buf = self._clean_text(self._buffer)
            if not clean_buf and not force:
                break

            # 1. Primary sentence boundaries: . ! ? followed by whitespace or end of buffer
            sentence_match = re.search(r'([.!?])\s+', self._buffer)
            if sentence_match:
                end_idx = sentence_match.end()
                chunk_raw = self._buffer[:end_idx]
                chunk_clean = self._clean_text(chunk_raw)
                if chunk_clean:
                    phrases.append(chunk_clean)
                self._buffer = self._buffer[end_idx:].lstrip()
                continue

            # 2. Secondary clause boundaries: , ; : or newline if buffer exceeds min_chars
            if len(clean_buf) >= self.min_chars:
                clause_match = re.search(r'([,;:\n])\s+', self._buffer)
                if clause_match and clause_match.end() >= self.min_chars:
                    end_idx = clause_match.end()
                    chunk_raw = self._buffer[:end_idx]
                    chunk_clean = self._clean_text(chunk_raw)
                    if chunk_clean:
                        phrases.append(chunk_clean)
                    self._buffer = self._buffer[end_idx:].lstrip()
                    continue

            # 3. Forced flush at end of stream or if buffer exceeds max_chars
            if force or len(self._buffer) >= self.max_chars:
                # Break at last whitespace if possible
                if not force and ' ' in self._buffer:
                    last_space = self._buffer.rfind(' ')
                    chunk_raw = self._buffer[:last_space]
                    self._buffer = self._buffer[last_space:].lstrip()
                else:
                    chunk_raw = self._buffer
                    self._buffer = ""
                
                chunk_clean = self._clean_text(chunk_raw)
                if chunk_clean:
                    phrases.append(chunk_clean)
                break

            break

        return phrases
