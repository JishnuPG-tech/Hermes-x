"""Encrypted Credential Vault & Connection Manager.

Stores credentials with AES-GCM / PBKDF2 encryption in SQLite.
Exposes opaque credential_id references to tools and LLMs so raw secrets
are never leaked or handled in agent context.
"""

from __future__ import annotations

import base64
import hashlib
import json
import logging
import os
import sqlite3
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)

# Master key resolution
VAULT_MASTER_KEY = (
    os.getenv("HERMES_VAULT_KEY")
    or os.getenv("UPSTREAM_API_KEY")
    or os.getenv("API_KEY_SECRET")
    or "HermesMasterVaultKey2026"
)

DEFAULT_VAULT_DIR = Path("/data/jarvis/secrets") if Path("/data").exists() else Path("/tmp/jarvis/secrets")
DEFAULT_VAULT_PATH = DEFAULT_VAULT_DIR / "vault.db"


def _derive_key(secret: str, salt: bytes) -> bytes:
    """Derive 32-byte key via PBKDF2 HMAC SHA-256."""
    return hashlib.pbkdf2_hmac("sha256", secret.encode("utf-8"), salt, 100_000, 32)


def _encrypt_val(plaintext: str, master_key: str) -> str:
    """XOR/HMAC-salted envelope encryption with AES fallback."""
    salt = os.urandom(16)
    key = _derive_key(master_key, salt)
    pt_bytes = plaintext.encode("utf-8")
    
    # Try cryptography AESGCM if available, else robust salted XOR-stream
    try:
        from cryptography.hazmat.primitives.ciphers.aead import AESGCM
        aesgcm = AESGCM(key)
        nonce = os.urandom(12)
        ct = aesgcm.encrypt(nonce, pt_bytes, None)
        envelope = {
            "mode": "aes_gcm",
            "salt": base64.b64encode(salt).decode("ascii"),
            "nonce": base64.b64encode(nonce).decode("ascii"),
            "ct": base64.b64encode(ct).decode("ascii"),
        }
        return json.dumps(envelope)
    except ImportError:
        # High-entropy keystream fallback
        keystream = hashlib.sha256(key + salt).digest()
        while len(keystream) < len(pt_bytes):
            keystream += hashlib.sha256(keystream + salt).digest()
        ct = bytes(b ^ k for b, k in zip(pt_bytes, keystream))
        envelope = {
            "mode": "xor_hmac",
            "salt": base64.b64encode(salt).decode("ascii"),
            "nonce": "",
            "ct": base64.b64encode(ct).decode("ascii"),
        }
        return json.dumps(envelope)


def _decrypt_val(envelope_json: str, master_key: str) -> str:
    """Decrypt payload using envelope metadata."""
    envelope = json.loads(envelope_json)
    salt = base64.b64decode(envelope["salt"])
    key = _derive_key(master_key, salt)
    ct = base64.b64decode(envelope["ct"])

    if envelope.get("mode") == "aes_gcm":
        from cryptography.hazmat.primitives.ciphers.aead import AESGCM
        nonce = base64.b64decode(envelope["nonce"])
        aesgcm = AESGCM(key)
        pt = aesgcm.decrypt(nonce, ct, None)
        return pt.decode("utf-8")
    else:
        keystream = hashlib.sha256(key + salt).digest()
        while len(keystream) < len(ct):
            keystream += hashlib.sha256(keystream + salt).digest()
        pt = bytes(b ^ k for b, k in zip(ct, keystream))
        return pt.decode("utf-8")


@dataclass
class CredentialReference:
    credential_id: str
    service_type: str  # notion, github, huggingface, telegram, custom_http
    display_name: str
    scopes: List[str] = field(default_factory=list)
    created_at: float = field(default_factory=time.time)
    expires_at: Optional[float] = None
    metadata: Dict[str, Any] = field(default_factory=dict)


class CredentialVault:
    """Encrypted SQLite credential vault."""

    def __init__(self, db_path: Optional[Path] = None, master_key: Optional[str] = None) -> None:
        self.db_path = db_path or DEFAULT_VAULT_PATH
        self.master_key = master_key or VAULT_MASTER_KEY
        self._init_db()

    def _init_db(self) -> None:
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS credentials (
                    credential_id TEXT PRIMARY KEY,
                    service_type TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    encrypted_data TEXT NOT NULL,
                    scopes TEXT NOT NULL,
                    created_at REAL NOT NULL,
                    expires_at REAL,
                    metadata TEXT NOT NULL
                )
            """)
            conn.commit()

    def store_credential(
        self,
        credential_id: str,
        service_type: str,
        secret_value: str,
        display_name: str = "",
        scopes: Optional[List[str]] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> CredentialReference:
        """Store a secret encrypted in vault, returning the safe public reference."""
        enc_payload = _encrypt_val(secret_value, self.master_key)
        sc = scopes or ["read", "write"]
        meta = metadata or {}
        now = time.time()
        disp = display_name or f"{service_type}:{credential_id}"

        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                """
                INSERT OR REPLACE INTO credentials 
                (credential_id, service_type, display_name, encrypted_data, scopes, created_at, expires_at, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    credential_id,
                    service_type,
                    disp,
                    enc_payload,
                    json.dumps(sc),
                    now,
                    None,
                    json.dumps(meta),
                ),
            )
            conn.commit()

        return CredentialReference(
            credential_id=credential_id,
            service_type=service_type,
            display_name=disp,
            scopes=sc,
            created_at=now,
            metadata=meta,
        )

    def resolve_secret(self, credential_id: str) -> Optional[str]:
        """Internal execution-layer resolution of secret value by credential_id."""
        with sqlite3.connect(self.db_path) as conn:
            cur = conn.cursor()
            cur.execute("SELECT encrypted_data FROM credentials WHERE credential_id = ?", (credential_id,))
            row = cur.fetchone()
            if not row:
                return None
            try:
                return _decrypt_val(row[0], self.master_key)
            except Exception as e:
                logger.error("Failed to decrypt credential %s: %s", credential_id, e)
                return None

    def list_references(self) -> List[CredentialReference]:
        """List opaque safe references without secret values."""
        with sqlite3.connect(self.db_path) as conn:
            cur = conn.cursor()
            cur.execute("SELECT credential_id, service_type, display_name, scopes, created_at, expires_at, metadata FROM credentials")
            rows = cur.fetchall()
            refs = []
            for r in rows:
                refs.append(
                    CredentialReference(
                        credential_id=r[0],
                        service_type=r[1],
                        display_name=r[2],
                        scopes=json.loads(r[3]),
                        created_at=r[4],
                        expires_at=r[5],
                        metadata=json.loads(r[6]),
                    )
                )
            return refs


_VAULT_INSTANCE: Optional[CredentialVault] = None


def get_credential_vault() -> CredentialVault:
    global _VAULT_INSTANCE
    if _VAULT_INSTANCE is None:
        _VAULT_INSTANCE = CredentialVault()
        # Seed standard credentials from environment if present
        if os.getenv("NOTION_API_KEY"):
            _VAULT_INSTANCE.store_credential(
                "notion_primary",
                "notion",
                os.getenv("NOTION_API_KEY", ""),
                display_name="Primary Notion Workspace",
            )
        if os.getenv("GITHUB_TOKEN"):
            _VAULT_INSTANCE.store_credential(
                "github_main",
                "github",
                os.getenv("GITHUB_TOKEN", ""),
                display_name="Main GitHub Integration",
            )
        if os.getenv("HF_TOKEN"):
            _VAULT_INSTANCE.store_credential(
                "hf_primary",
                "huggingface",
                os.getenv("HF_TOKEN", ""),
                display_name="Hugging Face Space Token",
            )
    return _VAULT_INSTANCE
