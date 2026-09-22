import os
import glob
from pathlib import Path
from typing import Optional
from hermes_core.tools.registry import registry

@registry.register(
    name="read_file",
    description="Read the contents of a file on the server. Supports text files, scripts, configurations, and markdown.",
    parameters={
        "type": "object",
        "properties": {
            "path": {"type": "string", "description": "Path to the file to read (absolute or relative to current working directory)"},
            "offset_lines": {"type": "integer", "description": "Optional starting line offset (1-indexed)"},
            "limit_lines": {"type": "integer", "description": "Optional maximum number of lines to return (default: 500)"}
        },
        "required": ["path"]
    },
    category="files"
)
async def read_file(path: str, offset_lines: int = 1, limit_lines: int = 500) -> str:
    try:
        p = Path(path).expanduser().resolve()
        if not p.exists():
            return f"[ERROR] File not found: {path}"
        if p.is_dir():
            return f"[ERROR] Path is a directory: {path}. Use list_directory instead."
        
        with open(p, "r", encoding="utf-8", errors="replace") as f:
            lines = f.readlines()
        
        start = max(0, offset_lines - 1)
        end = start + limit_lines
        selected = lines[start:end]
        
        numbered = [f"{i + start + 1}: {line}" for i, line in enumerate(selected)]
        output = "".join(numbered)
        total = len(lines)
        if end < total:
            output += f"\n[File has {total} total lines. Showing lines {start+1}-{min(end, total)}]"
        return output
    except Exception as e:
        return f"[ERROR] Failed to read file {path}: {e}"

@registry.register(
    name="write_file",
    description="Write content to a file on the server. Creates parent directories if they do not exist.",
    parameters={
        "type": "object",
        "properties": {
            "path": {"type": "string", "description": "Path to the file to write"},
            "content": {"type": "string", "description": "The exact text content to write into the file"},
            "overwrite": {"type": "boolean", "description": "Whether to overwrite if file already exists (default: true)"}
        },
        "required": ["path", "content"]
    },
    category="files"
)
async def write_file(path: str, content: str, overwrite: bool = True) -> str:
    try:
        p = Path(path).expanduser().resolve()
        if p.exists() and not overwrite:
            return f"[ERROR] File already exists at {path} and overwrite is False."
        
        p.parent.mkdir(parents=True, exist_ok=True)
        with open(p, "w", encoding="utf-8") as f:
            f.write(content)
        return f"[SUCCESS] File written successfully: {path} ({len(content)} characters)"
    except Exception as e:
        return f"[ERROR] Failed to write file {path}: {e}"

@registry.register(
    name="edit_file",
    description="Replace an exact target text snippet with replacement text inside an existing file.",
    parameters={
        "type": "object",
        "properties": {
            "path": {"type": "string", "description": "Path to the file to edit"},
            "target_text": {"type": "string", "description": "The exact text in the file to find and replace"},
            "replacement_text": {"type": "string", "description": "The new text to replace the target text with"}
        },
        "required": ["path", "target_text", "replacement_text"]
    },
    category="files"
)
async def edit_file(path: str, target_text: str, replacement_text: str) -> str:
    try:
        p = Path(path).expanduser().resolve()
        if not p.exists():
            return f"[ERROR] File not found: {path}"
        with open(p, "r", encoding="utf-8") as f:
            content = f.read()
        if target_text not in content:
            return f"[ERROR] target_text not found in {path}. Make sure whitespace and line endings match exactly."
        
        occurrences = content.count(target_text)
        if occurrences > 1:
            return f"[ERROR] target_text appears {occurrences} times in {path}. Please provide a more unique snippet."
        
        new_content = content.replace(target_text, replacement_text, 1)
        with open(p, "w", encoding="utf-8") as f:
            f.write(new_content)
        return f"[SUCCESS] Successfully replaced target text in {path}."
    except Exception as e:
        return f"[ERROR] Failed to edit file {path}: {e}"

@registry.register(
    name="list_directory",
    description="List files and directories in a directory path.",
    parameters={
        "type": "object",
        "properties": {
            "path": {"type": "string", "description": "Directory path to list. Defaults to current directory."}
        }
    },
    category="files"
)
async def list_directory(path: str = ".") -> str:
    try:
        p = Path(path).expanduser().resolve()
        if not p.exists():
            return f"[ERROR] Directory not found: {path}"
        if not p.is_dir():
            return f"[ERROR] Path is not a directory: {path}"
        
        entries = []
        for item in sorted(p.iterdir()):
            kind = "[DIR]" if item.is_dir() else "[FILE]"
            size = f"({item.stat().st_size} bytes)" if item.is_file() else ""
            entries.append(f"{kind} {item.name} {size}")
        return "\n".join(entries) if entries else "[Directory is empty]"
    except Exception as e:
        return f"[ERROR] Failed to list directory {path}: {e}"
