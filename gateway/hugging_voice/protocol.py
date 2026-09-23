"""Hugging Voice Wire Protocol Definitions.

Follows the official HuggingFace speech-to-speech / Hugging Voice
reverse-engineered OpenAI Realtime WebSocket protocol specifications.
"""

from __future__ import annotations
import uuid
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class ToolParameters(BaseModel):
    type: str = "object"
    properties: Dict[str, Any] = Field(default_factory=dict)
    required: Optional[List[str]] = None


class RealtimeTool(BaseModel):
    type: str = "function"
    name: str
    description: str
    parameters: ToolParameters


class TurnDetection(BaseModel):
    type: str = "server_vad"
    threshold: float = 0.5
    prefix_padding_ms: int = 300
    silence_duration_ms: int = 500


class RealtimeSessionConfig(BaseModel):
    id: str = Field(default_factory=lambda: f"sess_{uuid.uuid4().hex[:12]}")
    model: str = "hermes-agent"
    modalities: List[str] = Field(default_factory=lambda: ["audio", "text"])
    instructions: str = (
        "You are Hermes, speaking in an exceptionally calm, warm, articulate, and sweet tone. "
        "Keep your answers concise, direct, and conversational (1 to 2 sentences maximum). "
        "Do NOT output markdown tables, markdown headers, or code blocks; speak clearly in natural prose. "
        "When the user asks you to perform actions, execute terminal commands, check system status, "
        "or inspect code, call the 'hermes_execute' tool to run it through your autonomous execution layer, "
        "and then explain what you did."
    )
    voice: str = "en-US-JennyNeural"
    input_audio_format: str = "pcm16"
    output_audio_format: str = "pcm16"
    input_audio_transcription: Optional[Dict[str, Any]] = Field(default_factory=lambda: {"model": "whisper-1"})
    turn_detection: Optional[TurnDetection] = Field(default_factory=TurnDetection)
    tools: List[RealtimeTool] = Field(default_factory=list)
    tool_choice: str = "auto"
    temperature: float = 0.6


HERMES_EXECUTE_TOOL = RealtimeTool(
    name="hermes_execute",
    description="Execute an autonomous action, shell command, code inspection, or workspace task via Hermes Agent.",
    parameters=ToolParameters(
        type="object",
        properties={
            "objective": {
                "type": "string",
                "description": "The specific objective or command to execute via Hermes Agent."
            },
            "task_type": {
                "type": "string",
                "description": "Category of the task: 'code', 'research', 'terminal', 'file_operation', 'git', or 'general'."
            },
            "background": {
                "type": "boolean",
                "description": "Whether to schedule as a long-running background task (default: false)."
            }
        },
        required=["objective"]
    )
)


def create_session_created_event(session: RealtimeSessionConfig) -> dict:
    return {
        "type": "session.created",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "session": session.model_dump(),
    }


def create_session_updated_event(session: RealtimeSessionConfig) -> dict:
    return {
        "type": "session.updated",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "session": session.model_dump(),
    }


def create_speech_started_event(audio_start_ms: int = 0) -> dict:
    return {
        "type": "input_audio_buffer.speech_started",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "audio_start_ms": audio_start_ms,
        "item_id": f"item_{uuid.uuid4().hex[:8]}",
    }


def create_speech_stopped_event(audio_end_ms: int = 0) -> dict:
    return {
        "type": "input_audio_buffer.speech_stopped",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "audio_end_ms": audio_end_ms,
        "item_id": f"item_{uuid.uuid4().hex[:8]}",
    }


def create_transcript_completed_event(transcript: str) -> dict:
    return {
        "type": "conversation.item.input_audio_transcription.completed",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "transcript": transcript,
    }


def create_response_created_event(response_id: str) -> dict:
    return {
        "type": "response.created",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "response": {
            "id": response_id,
            "status": "in_progress",
        }
    }


def create_output_audio_transcript_delta_event(response_id: str, delta: str) -> dict:
    return {
        "type": "response.output_audio_transcript.delta",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "response_id": response_id,
        "delta": delta,
    }


def create_output_audio_delta_event(response_id: str, b64_pcm: str) -> dict:
    return {
        "type": "response.output_audio.delta",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "response_id": response_id,
        "delta": b64_pcm,
    }


def create_output_audio_done_event(response_id: str) -> dict:
    return {
        "type": "response.output_audio.done",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "response_id": response_id,
    }


def create_output_audio_transcript_done_event(response_id: str, transcript: str) -> dict:
    return {
        "type": "response.output_audio_transcript.done",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "response_id": response_id,
        "transcript": transcript,
    }


def create_function_call_event(response_id: str, call_id: str, name: str, arguments: str) -> dict:
    return {
        "type": "response.function_call_arguments.done",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "response_id": response_id,
        "call_id": call_id,
        "name": name,
        "arguments": arguments,
    }


def create_response_done_event(response_id: str, status: str = "completed") -> dict:
    return {
        "type": "response.done",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "response": {
            "id": response_id,
            "status": status,
        }
    }


def create_error_event(message: str, code: str = "internal_error") -> dict:
    return {
        "type": "error",
        "event_id": f"evt_{uuid.uuid4().hex[:10]}",
        "error": {
            "type": "invalid_request_error",
            "code": code,
            "message": message,
        }
    }
