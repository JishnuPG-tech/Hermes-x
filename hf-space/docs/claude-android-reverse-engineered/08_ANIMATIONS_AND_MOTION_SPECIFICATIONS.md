# Claude Android — Complete Animation & Motion Specifications

---

## 1. Motion Philosophy & Timing Curves

Claude Android’s motion system is designed to feel **organic, weightless, and calm**:
* **No flashy bounces**: Avoids aggressive spring overshoots.
* **Physics-based springs**: Uses medium-low stiffness springs for gesture sheets and drag dismissals.
* **Standard Duration Tokens**:
  * Micro-interactions (toggles, icons): `150ms`–`200ms`
  * Sheet & Dialog transitions: `300ms` (`accelerate_decelerate_interpolator`)
  * State breathing loops (thinking/shimmer): `1000ms`–`1875ms`

---

## 2. Chat & AI Generation State Animations

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          CHAT STREAMING & GENERATION ANIMATIONS                        │
├─────────────────────┬──────────────┬──────────────────┬────────────────────────────────┤
│ State / Mode        │ Asset / Code │ Timing / Frames  │ Visual Behavior                │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Thinking / Reason   │ claude_spark │ 8 Frames @ 125ms │ Sequential rotation & breathing│
│                     │ _thinking    │ (1000ms Loop)    │ of the 8-point Starburst rays  │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Text Generation     │ claude_spark │ 8 Frames @ 125ms │ Dynamic stroke expansion and   │
│                     │ _writing     │ (1000ms Loop)    │ pulsation during active tokens │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Idle / Shimmer      │ claude_spark │ 15 Frames @ 125ms│ Subtle light wave traveling    │
│                     │ _shimmer     │ (1875ms Loop)    │ across starburst vertices      │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Stream Smoothing    │ StreamSmooth │ 33ms Tick Timer  │ 30 FPS text token batching with│
│                     │ ingConfig    │ (200ms fade-in)  │ 200ms alpha fade on tail chars │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Thinking Box Toggle │ animateCont  │ tween(250ms,     │ Smooth height expansion and    │
│                     │ entSize()    │ FastOutSlowIn)   │ collapse with caret rotation   │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Code / Artifact Card│ fadeIn +     │ tween(250ms,     │ Slides up 40dp while fading in │
│                     │ slideInVert  │ EaseOutCubic)    │ as tool execution settles      │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Planning Checkmark  │ Vector path  │ tween(200ms,     │ Path trim (0.0 to 1.0) on      │
│                     │ trimPathEnd  │ LinearEasing)    │ automated step completion      │
└─────────────────────┴──────────────┴──────────────────┴────────────────────────────────┘
```

---

## 3. Realtime Voice Call Animations ("Bell Mode")

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              VOICE MODE AUDIO ANIMATIONS                               │
├─────────────────────┬──────────────┬──────────────────┬────────────────────────────────┤
│ Interaction State   │ Interpolator │ Value Scale      │ Animation Behavior             │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Listening Waveform  │ FastOutSlowIn│ 1.0f ➔ 1.28f     │ Starburst radial scale pulses  │
│                     │              │ (Audio RMS Level)│ dynamically with mic decibels  │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Assistant Speaking  │ Smooth Damped│ 1.0f ➔ 1.15f     │ Rhythmic breathing scale       │
│                     │ Sine Wave    │ (750ms Cycle)    │ synchronized with TTS output   │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Word Caption Sync   │ Discrete PTS │ pts_ms Match     │ Active word color shifts from  │
│ (TTSWord Events)    │ Step Trigger │ (Realtime Sync)  │ #B0AEA5 (Muted) to #FAF9F5     │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ Barge-In Cutoff     │ Snap (0ms)   │ Scale ➔ 1.0f     │ Instant waveform snap to idle  │
│                     │ Immediate    │ Alpha ➔ 1.0f     │ and immediate buffer flush     │
└─────────────────────┴──────────────┴──────────────────┴────────────────────────────────┘
```

---

## 4. Navigation & Screen Transition Curves

### 4.1 Modal Bottom Sheet Transition (`res/anim/slide_in_bottom.xml`)
* **Duration**: `300ms`
* **Interpolator**: `android.R.anim.accelerate_decelerate_interpolator`
* **Transform**: `fromYDelta="100%p" toYDelta="0"`
* **Scrim Alpha**: `0.0f` ➔ `0.60f` (`#000000` 60% opacity)

### 4.2 Modal Bottom Sheet Dismissal (`res/anim/slide_out_bottom.xml`)
* **Duration**: `300ms`
* **Interpolator**: `android.R.anim.accelerate_decelerate_interpolator`
* **Transform**: `fromYDelta="0" toYDelta="100%p"`
* **Scrim Alpha**: `0.60f` ➔ `0.0f`

### 4.3 Navigation Drawer Slide
* **Slide Duration**: `280ms` with `FastOutSlowInEasing` curve.
* **Content Fade**: Main screen canvas slightly dims and scales down to `0.98f` behind the drawer.

---

## 5. Micro-Interactions & Controls

1. **Toggle Switch Thumb (`glance_switch_thumb_animated.xml`)**:
   * Thumb translates `20dp` horizontally with slight spring elasticity (`duration = 180ms`).
   * Track color animates from `#2A2927` to `#3898EC`.
2. **Model Selector Selection Morph**:
   * Text color transitions from `#FAF9F5` to `#6A9BCC` over `150ms`.
   * Checkmark icon scales from `0.6f` to `1.0f` with `Spring.DampingRatioMediumBouncy`.
3. **Transient Feedback Chip Dismiss**:
   * Slides downward `24dp` while fading alpha to `0.0f` over `200ms`, collapsing parent slot smoothly.
