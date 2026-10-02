# Claude Android — Security, Vault & Authentication Architecture

---

## 1. Authentication Architecture & Supported Identity Providers

The Claude Android application provides 4 independent, zero-trust authentication pathways:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                AUTHENTICATION PATHWAYS                                 │
├───────────────────────┬──────────────────────────────────┬─────────────────────────────┤
│ Auth Method           │ Request Payload                  │ Verification Endpoint       │
├───────────────────────┼──────────────────────────────────┼─────────────────────────────┤
│ 1. Google One-Tap     │ VerifyGoogleMobileRequest        │ POST /api/login/google      │
│    (Native Android)   │ (id_token, server_client_id)     │                             │
├───────────────────────┼──────────────────────────────────┼─────────────────────────────┤
│ 2. Email Magic Link   │ SendMagicLinkRequest             │ POST /api/login/send_link   │
│                       │ VerifyMagicLinkRequest           │ POST /api/login/verify_code │
├───────────────────────┼──────────────────────────────────┼─────────────────────────────┤
│ 3. Enterprise SSO     │ SAML / Okta / Azure AD           │ Redirect: /sso-callback/..  │
│    (Workspaces)       │ (sso_url, state_token)           │                             │
├───────────────────────┼──────────────────────────────────┼─────────────────────────────┤
│ 4. Hardware Integrity │ ClientAttestation                │ POST /api/auth/trusted_..   │
│    (Play Integrity)   │ (play_integrity_token)           │                             │
└───────────────────────┴──────────────────────────────────┴─────────────────────────────┘
```

---

## 2. Magic Link Authentication Payload Schemas

### 2.1 Send Magic Link (`SendMagicLinkRequest`)
```json
// POST /api/login/send_magic_link
{
  "email_address": "user@example.com",
  "recaptcha_token": "xxx",
  "recaptcha_site_key": "xxx",
  "utc_offset": -240,
  "source": "claude",
  "client": "android",
  "login_intent": "sign_in"
}
```

### 2.2 Verify Magic Link Code (`VerifyMagicLinkRequest`)
```json
// POST /api/login/verify_magic_link
{
  "email_address": "user@example.com",
  "verification_code": "123456",
  "device_id": "dev_android_uuid_789"
}
```

---

## 3. Trusted Devices & Hardware Attestation

To safeguard against modified APK environments and API abuse, Claude Android performs continuous device attestation:

```
┌─────────────────────────────────────────────────────────────────┐
│                      PLAY INTEGRITY SERVICE                     │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Attestation Token (JWS)
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT ATTESTATION PAYLOAD                   │
│  • package_name: com.anthropic.claude                           │
│  • sha256_cert_digest: [...]                                   │
│  • play_integrity_token: "ey..."                                │
└───────────────────────────────┬─────────────────────────────────┘
                                │ POST /auth/trusted_devices
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                  ANTHROPIC TRUST BACKPLANE                      │
│  • Validates hardware-backed keystore                           │
│  • Issues scoped device JWT for WSS Voice & CCR connections     │
└─────────────────────────────────────────────────────────────────┘
```

* **Device Rotation**: Uses `POST /auth/trusted_devices/{device_id}/rotate_reattest` every 30 days or on security policy escalation.

---

## 4. Keystore Vault & Credential Protection

1. **Hardware-Backed Cryptography**:
   * All session tokens (`sessionKey`), GitHub OAuth tokens, and MCP credentials are encrypted using **AES-256-GCM** via Android `MasterKey` inside the device's hardware Secure Element / StrongBox (`AndroidKeyStore`).
2. **Zero-Secret UI Rule**:
   * Client ViewModels never expose secret values (e.g. GitHub tokens or API keys) to the Compose tree. Only public metadata (`"1 connected"`, `"5 tools"`) is held in memory.
3. **Data Loss & Secret Leak Prevention (`ScanSecretsRequest`)**:
   * Before a user can publicly share a session or code artifact, the client invokes `POST /api/organizations/{org}/code/shares/scan_secrets` to ensure no passwords, AWS keys, or tokens are leaked in diffs or code snippets.

---

## 5. Health Connect & Granular OS Permissions

The APK includes full integration with Android Health Connect (`androidx.health.connect`):
* Managed through `PermissionsRationaleActivity` (`androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`).
* Supports 25+ granular health permission scopes (e.g. `READ_STEPS`, `READ_HEART_RATE`, `READ_SLEEP`, `READ_VO2_MAX`) with per-category privacy toggles and explicit user consent state (`fu5.java`: `POST /accounts/me/consents/check`).
