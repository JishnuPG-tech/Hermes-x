"""Hardened Encrypted Credential Vault.

Security Enhancements:
- Eliminates hardcoded fallback master keys.
- Requires HERMES_VAULT_KEY or generates a cryptographically secure 256-bit key
  stored in a restricted keyfile with strict filesystem permissions (0600).
- Enforces authenticated encryption: AES-256-GCM, or PBKDF2 + AES/XOR with HMAC-SHA256
  authentication tag to prevent ciphertext tampering.
- Emits opaque credential_id references to models.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import logging
import os
import sqlite3
import stat
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)

DEFAULT_VAULT_DIR = Path("/data/jarvis/secrets") if Path("/data").exists() else Path("/tmp/jarvis/secrets")
DEFAULT_VAULT_PATH = DEFAULT_VAULT_DIR / "vault.db"
DEFAULT_KEY_PATH = DEFAULT_VAULT_DIR / ".vault_key"


def _get_or_create_master_key(key_path: Optional[Path] = None) -> str:
    """Retrieve master key from environment or secure restricted keyfile. Never uses hardcoded constants."""
    env_key = os.getenv("HERMES_VAULT_KEY") or os.getenv("API_KEY_SECRET")
    if env_key and env_key.strip():
        return env_key.strip()

    kp = key_path or DEFAULT_KEY_PATH
    kp.parent.mkdir(parents=True, exist_ok=True)

    if kp.exists():
        try:
            key_content = kp.read_text(encoding="utf-8").strip()
            if key_content:
                return key_content
        except Exception as e:
            logger.warning("Could not read vault keyfile %s: %s", kp, e)

    # Generate cryptographically secure random 256-bit hex key
    new_key = os.urandom(32).hex()
    try:
        kp.write_text(new_key, encoding="utf-8")
        # Restrict permissions to owner only (chmod 0600 on POSIX)
        if hasattr(os, "chmod"):
            try:
                os.chmod(kp, stat.S_IRUSR | stat.S_IWUSR)
            except Exception:
                pass
        logger.info("Generated new secure machine vault key at %s", kp)
    except Exception as e:
        logger.error("Failed to write secure keyfile %s: %s", kp, e)

    return new_key


def _derive_key(secret: str, salt: bytes) -> bytes:
    """Derive 32-byte key via PBKDF2 HMAC SHA-256."""
    return hashlib.pbkdf2_hmac("sha256", secret.encode("utf-8"), salt, 100_000, 32)


def _encrypt_val(plaintext: str, master_key: str) -> str:
    """Authenticated encryption (AES-256-GCM or HMAC-SHA256 envelope)."""
    salt = os.urandom(16)
    key = _derive_key(master_key, salt)
    pt_bytes = plaintext.encode("utf-8")

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
            "tag": "",  # Included in ciphertext in AESGCM
        }
        return json.dumps(envelope)
    except ImportError:
        # Authenticated stream cipher with HMAC-SHA256
        keystream = hashlib.sha256(key + salt).digest()
        while len(keystream) < len(pt_bytes):
            keystream += hashlib.sha256(keystream + salt).digest()
        ct = bytes(b ^ k for b, k in zip(pt_bytes, keystream))
        mac = hmac.new(key, salt + ct, hashlib.sha256).digest()
        envelope = {
            "mode": "hmac_stream",
            "salt": base64.b64encode(salt).decode("ascii"),
            "nonce": "",
            "ct": base64.b64encode(ct).decode("ascii"),
            "tag": base64.b64encode(mac).decode("ascii"),
        }
        return json.dumps(envelope)


def _decrypt_val(envelope_json: str, master_key: str) -> str:
    """Authenticated decryption with tamper detection."""
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
        # Verify HMAC tag before decrypting
        expected_mac = hmac.new(key, salt + ct, hashlib.sha256).digest()
        actual_mac = base64.b64decode(envelope.get("tag", ""))
        if not hmac.compare_digest(expected_mac, actual_mac):
            raise ValueError("Ciphertext authentication failed: data tampering detected")

        keystream = hashlib.sha256(key + salt).digest()
        while len(keystream) < len(ct):
            keystream += hashlib.sha256(keystream + salt).digest()
        pt = bytes(b ^ k for b, k in zip(ct, keystream))
        return pt.decode("utf-8")


@dataclass
class CredentialReference:
    credential_id: str
    service_type: str
    display_name: str
    scopes: List[str] = field(default_factory=list)
    created_at: float = field(default_factory=time.time)
    expires_at: Optional[float] = None
    metadata: Dict[str, Any] = field(default_factory=dict)


class CredentialVault:
    """Production hardened encrypted credential vault."""

    def __init__(self, db_path: Optional[Path] = None, master_key: Optional[str] = None) -> None:
        self.db_path = db_path or DEFAULT_VAULT_PATH
        self.master_key = master_key or _get_or_create_master_key()
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
        with sqlite3.connect(self.db_path) as conn:
            cur = conn.cursor()
            cur.execute("SELECT credential_id, service_type, display_name, scopes, created_at, expires_at, metadata FROM credentials")
            rows = cur.fetchall()
            return [
                CredentialReference(
                    credential_id=r[0],
                    service_type=r[1],
                    display_name=r[2],
                    scopes=json.loads(r[3]),
                    created_at=r[4],
                    expires_at=r[5],
                    metadata=json.loads(r[6]),
                )
                for r in rows
            ]


_VAULT_INSTANCE: Optional[CredentialVault] = None


def get_credential_vault() -> CredentialVault:
    global _VAULT_INSTANCE
    if _VAULT_INSTANCE is None:
        _VAULT_INSTANCE = CredentialVault()
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
