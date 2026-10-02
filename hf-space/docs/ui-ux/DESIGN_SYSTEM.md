# Hermes UI/UX Design System

## Design intent

Hermes uses a calm, minimal, editorial interface inspired by the supplied Claude-style references. Hermes capabilities are added without creating a competing visual language.

Rules:
- Near-black primary surfaces
- Warm off-white primary text
- Muted gray secondary text
- Rounded dark cards
- Thin low-contrast borders
- Pill-shaped composer and actions
- Serif display headings
- Sans-serif body/interface text
- Minimal iconography
- Calm motion
- Progressive disclosure
- No decorative gradients
- No dense dashboard chrome

## Tokens

### Colors

```text
background  #141414
surface     #202020
input       #2E2E2E
accent      #5B8CFF
system      #4A7FE8
action      #FFB454
danger      #E5484D
success     #3FC97D
text        #F5F2ED
secondary   #8E8E93
border      #333333
```

### Typography

Display:
- Serif
- Weight 400
- Large sizes 28-40px
- Tight line height

Interface:
- Sans-serif
- Regular to semibold
- 13-18px body range
- 12-13px metadata

### Shape

- Small controls: 10-14px radius
- Cards: 18-22px radius
- Composer: 26-30px radius
- Status pills: 999px
- Voice control: circular

### Spacing

Base unit: 4px.

Preferred scale:
`4, 8, 12, 16, 20, 24, 32, 40, 48`.

## Components

### Navigation
- Bottom navigation on mobile
- Sidebar/drawer on larger screens
- Keep navigation visually quiet
- Current destination is indicated by restrained fill or icon state

### Composer

The composer is a central Hermes control.

Contains:
- Add/attach
- Text input
- Voice input
- Send
- Contextual tools when required

States:
- Empty
- Focused
- Draft
- Sending
- Streaming
- Disabled
- Offline
- Attachment pending

### Message surface

User messages are compact and visually distinct. Hermes responses prioritize readable text and progressive tool/task disclosure.

### HermesCard

Used for grouped information such as:
- Task progress
- Agent activity
- Settings groups
- Integration state
- Project summaries
- System health

### StatusChip

Canonical states:
- Ready
- Listening
- Thinking
- Running
- Delegating
- Waiting
- Needs approval
- Recovering
- Paused
- Failed
- Completed
- Offline
- Reconnecting

### ActionPill

Use for contextual actions such as:
- View plan
- Pause
- Resume
- Approve
- Deny
- Retry
- Open project
- View logs

### SettingRow

Grouped settings should use quiet rows with icon, title, optional subtitle and trailing control/chevron.

## Hermes-specific components

### AutonomousTaskCard
Displays:
- Objective
- Current step
- Progress
- Active workers
- Verification status
- Pause/resume
- Approval state

### AgentCard
Displays:
- Worker role
- Objective
- Status
- Current action
- Workspace
- Output
- Verification

### ApprovalCard
Displays:
- Requested action
- Why approval is required
- Risk level
- Scope
- Allow once
- Allow for task
- Deny

### VoiceSurface
Displays:
- Hermes identity
- Listening/speaking state
- Current transcript
- Barge-in state
- Microphone control
- Connection status

### TerminalSurface
Displays:
- Command
- Running state
- Output
- Exit code
- Working directory
- Stop control
- Copy/open artifact actions

### TraceSurface
Displays sanitized execution events without credentials or sensitive secret material.

## Motion

Motion is functional, not decorative.

- Short transitions
- Subtle streaming indicators
- Gentle task progress
- Clear state changes
- Respect reduced-motion preferences

## Responsive rules

Mobile is the primary interaction target.

Desktop:
- Expands information density
- Uses persistent navigation where useful
- Preserves same visual hierarchy
- Never creates a separate desktop visual language

## Accessibility

Every interactive component must provide:
- Semantic label
- Keyboard/focus support where applicable
- Sufficient contrast
- Minimum touch target
- Screen-reader meaning
- Non-color-only state communication
- Reduced-motion behavior

## Visual quality gate

```text
Implement
 → capture screenshot
 → compare to approved reference/spec
 → identify spacing/type/state mismatch
 → fix
 → repeat
```

Figma is not required. The repository and Notion UI Kit together are the design contract.
