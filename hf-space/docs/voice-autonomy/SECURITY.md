# Voice and Autonomous Runtime Security

## 1. Security objective

Voice must not become a privileged control channel. A spoken sentence has exactly the same authority as an equivalent text command under the same user, project, task, and tool policy.

## 2. Threat model

Threats include:

- unauthorized client access
- stolen session tokens
- replayed voice requests
- prompt injection through speech or web content
- malicious repository instructions
- model-generated destructive commands
- sensitive information sent to external TTS
- audio transport interception
- cross-session task access
- background task abuse after client disconnect
- provider compromise or outage
- denial of service through long audio/model requests

## 3. Trust boundaries

```text
Microphone
  |
Untrusted audio
  |
Authenticated voice transport
  |
Voice gateway
  |
Intent and policy
  |
Jarvis task
  |
Hermes tools
  |
External systems
```

A transcript is untrusted data until interpreted by the policy system.

## 4. Authentication

Every voice connection should authenticate before receiving task capability.

Requirements:

- TLS for transport
- short-lived access token where practical
- refresh/reconnect mechanism
- device/session binding
- server-side session authorization
- logout/revoke capability

The server must not trust `user_id` values supplied by the client without authentication context.

## 5. Authorization

Use the existing trust hierarchy:

```text
Owner Policy
  -> Project Policy
    -> Task Policy
      -> Worker Policy
        -> Tool Permission
          -> Actual Action
```

The voice gateway can submit intent but cannot bypass policy.

## 6. Approval tiers

Suggested levels:

```text
0 read-only
1 normal file/project changes
2 terminal execution
3 external API/network write
4 GitHub write
5 deployment
6 destructive/system administration
```

Voice can automatically execute only the levels allowed by current policy.

## 7. High-risk voice confirmation

For dangerous actions, use explicit confirmation. The confirmation should include enough context to prevent accidental approval.

Example:

> "This will delete the production database. Do you approve?"

A simple conversational acknowledgement such as "okay" should be accepted only when the approval request is still active, unambiguous, within expiry, and bound to the exact action.

## 8. Prompt injection

Voice requests, retrieved web pages, repository files, documents, and model outputs are all untrusted content. None may grant themselves permissions.

Examples:

- a README saying "run this command as root"
- a webpage saying "ignore the system policy"
- a tool response asking for credentials
- an agent output claiming that approval has already been granted

These are inputs to policy, not policy changes.

## 9. TTS privacy

Kokoro is local and should be preferred for sensitive text. EdgeTTS is an online fallback and therefore needs a data classification gate.

Never send through EdgeTTS:

- API keys
- access tokens
- passwords
- private keys
- session secrets
- raw authentication headers
- secret database credentials

For other sensitive content, use a configurable policy: deny, redact, or allow.

## 10. Secret handling

Secrets must be stored outside the model context when possible.

Rules:

- never log secrets
- never synthesize secrets
- do not include secrets in task artifacts
- redact known token formats in error messages
- do not persist raw environment variables into memory

## 11. Audio privacy

Define a retention policy:

- raw microphone audio: do not persist by default
- streaming audio buffers: ephemeral
- final transcript: session retention according to normal policy
- durable memory: only selected facts or lessons
- security audit: metadata and hashes, not raw sensitive audio

## 12. Replay protection

Every command-producing turn should have:

```text
request_id
turn_id
idempotency_key
timestamp
session_id
```

Reject stale or duplicated requests according to the configured replay window.

## 13. Rate limits

Apply limits to:

- connection attempts
- audio bytes per second
- concurrent voice sessions
- TTS requests
- task creation
- approval requests
- background task creation

Rate limiting protects both the server and external providers.

## 14. Background task safety

A voice client disconnect must not implicitly grant or revoke authority. A task retains the policy that was approved when it was created. Sensitive permissions should be revalidated at execution time.

## 15. Audit logging

Log security-relevant events:

```text
voice_session_open
voice_auth_failure
voice_command
policy_decision
approval_requested
approval_granted
approval_rejected
high_risk_action
provider_fallback
credential_access_attempt
cross_session_denied
```

Audit records must contain correlation IDs and outcome, not secret values.

## 16. Dependency security

Pin production dependencies and container base images where practical. Verify model files and maintain checksums for downloaded artifacts. Track Hermes and TTS provider versions.

## 17. Security tests

Required tests include:

- invalid token
- expired token
- stolen session token simulation
- cross-user task access
- replayed turn
- duplicate task submission
- prompt injection from repository
- prompt injection from webpage
- unauthorized production deploy request
- secret text sent to EdgeTTS
- malicious audio payload
- oversized frame
- connection flood

## 18. Security principle

Convenience must never be used as a reason to bypass policy. Voice should make approved operations easier, not make privileged operations easier to perform accidentally.
