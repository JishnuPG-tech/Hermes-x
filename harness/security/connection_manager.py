"""Connection Manager & Permission Scopes.

Manages external service connections with scoped permissions and capability grants.
"""

from __future__ import annotations

import logging
from typing import Any, Dict, List, Optional

from harness.security.credential_vault import CredentialReference, get_credential_vault

logger = logging.getLogger(__name__)


class ConnectionManager:
    """Manages verified connections to external services."""

    def __init__(self) -> None:
        self.vault = get_credential_vault()

    def get_connection(self, service_type: str) -> Optional[CredentialReference]:
        """Find the primary active credential reference for a service."""
        refs = self.vault.list_references()
        for r in refs:
            if r.service_type == service_type:
                return r
        return None

    def list_connections(self) -> List[Dict[str, Any]]:
        """Return safe list of connected integrations."""
        refs = self.vault.list_references()
        return [
            {
                "credential_id": r.credential_id,
                "service": r.service_type,
                "name": r.display_name,
                "scopes": r.scopes,
                "created_at": r.created_at,
            }
            for r in refs
        ]
