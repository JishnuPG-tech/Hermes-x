# Hermes Flutter UI

Flutter UI foundation for Hermes-x.

## Included

- Claude-style dark visual language
- Hermes design tokens
- Reusable cards, pills, status chips and setting rows
- Chat/home
- Composer
- Autonomous tasks
- Projects
- Voice surface
- Settings

## Architecture boundary

This package is presentation-first. It must connect to Hermes through a session/event gateway rather than directly to OmniRoute or individual model providers.

## Run

```bash
flutter pub get
flutter run
```

## Production integration still required

- Hermes session transport
- Streaming events
- Real task state
- Voice STT/TTS transport
- Device control
- Memory API
- GitHub integration
- Notion integration
- Authentication
- Permissions
- Offline/reconnect handling
- Functional and visual regression tests
