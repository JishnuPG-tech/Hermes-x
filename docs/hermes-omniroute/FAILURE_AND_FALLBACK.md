# Failure and Fallback

## Principle

OmniRoute is replaceable infrastructure. Hermes is the durable authority.

## Failure flow

```text
Hermes model request
       |
       v
OmniRoute
       |
   failure?
    /     \
  no       yes
  |         |
  v         v
stream   retry transient error
             |
             v
       configured fallback
             |
       +-----+-----+
       |           |
     works       fails
       |           |
       v           v
    Hermes     checkpoint
                  |
                  v
             inform user
```

## Required behavior

1. Never discard the active Hermes task because OmniRoute fails.
2. Persist the task checkpoint before long recovery waits.
3. Retry only errors classified as transient or recoverable.
4. Use configured provider/model fallback when available.
5. Preserve the user conversation.
6. Preserve tool outputs already obtained.
7. Resume the task when model inference becomes available again.
8. Tell the user what is blocked when continued reasoning is impossible.

## Voice

If model inference fails during a voice session, Hermes should keep the voice session alive where possible, speak a short status response through the available TTS path, and preserve the session/task state for recovery.

## Never do

- silently restart Hermes state
- delete task checkpoints
- reset user memory
- expose OmniRoute provider credentials
- make OmniRoute the user-facing fallback assistant
