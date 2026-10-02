# Chat Streaming, Adaptive Reasoning & Thinking Animations: Complete Reverse Engineering Report

**Target Package**: `com.anthropic.claude` (`Claude.apk` v`1.260828.0`)  
**Scope**: In-depth analysis of Chat Streaming, Adaptive Thinking blocks, Tool Execution Cards, Deep Research Steppers, and 30 FPS Stream Smoothing.

---

## 1. Adaptive Execution Paths: Fast Path vs Extended Reasoning

In the Claude Android client, animations are **dynamically adaptive** based on the model's response mode:

```
                                  USER QUERY DISPATCHED
                                            │
                                            ▼
                           POST /chat_conversations/{id}/completion2
                                            │
               ┌────────────────────────────┴────────────────────────────┐
               │                                                         │
               ▼                                                         ▼
     [ FAST / DIRECT PATH ]                                    [ EXTENDED REASONING PATH ]
   (Simple queries & greetings)                              (Coding, Math, Research, Tool use)
               │                                                         │
               ├─ NO ThinkingBlock received                              ├─ 1. ContentBlockStart: ThinkingBlock
               ├─ NO thinking box rendered                               ├─ 2. Renders Inline Thinking Box
               ├─ Zero animation lag                                     ├─ 3. Plays 8-Frame Rotating Spark
               ├─ Tokens streamed instantly at 30 FPS                    ├─ 4. Streams thinking_delta tokens
               └─ Direct text bubble emission                            ├─ 5. ContentBlockStop: Finalizes Duration
                                                                         └─ 6. ContentBlockStart: TextBlock / ToolUse
```

---

## 2. The 3 Starburst Animated Vector Drawables

The APK embeds 3 dedicated multi-frame frame-by-frame animation sets (`res/drawable/claude_spark_animated_*.xml`):

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                             STARBURST ANIMATION SPECIFICATION                          │
├─────────────────────┬──────────────┬──────────────────┬────────────────────────────────┤
│ Animation Type      │ Resource XML │ Frame Count & Rate Visual Behavior                │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ 1. Thinking Spark   │ claude_spark │ 8 Frames @ 125ms │ Rhythmic rotation and breathing│
│    (Reasoning)      │ _thinking    │ (1000ms loop)    │ of the 8 terracotta-coral rays │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ 2. Writing Spark    │ claude_spark │ 8 Frames @ 125ms │ Dynamic stroke expansion and   │
│    (Active Token)   │ _writing     │ (1000ms loop)    │ contraction during text stream │
├─────────────────────┼──────────────┼──────────────────┼────────────────────────────────┤
│ 3. Ambient Shimmer  │ claude_spark │ 15 Frames @ 125ms│ Subtle light wave traveling    │
│    (Idle / Ready)   │ _shimmer     │ (1875ms cycle)   │ across starburst ray vertices  │
└─────────────────────┴──────────────┴──────────────────┴────────────────────────────────┘
```

### 2.1 Thinking Spark Frame Animation (`claude_spark_animated_thinking.xml`)
* **XML Structure**:
```xml
<animation-list xmlns:android="http://schemas.android.com/apk/res/android" android:oneshot="false">
    <item android:duration="125" android:drawable="@drawable/claude_spark_thinking1"/>
    <item android:duration="125" android:drawable="@drawable/claude_spark_thinking2"/>
    <item android:duration="125" android:drawable="@drawable/claude_spark_thinking3"/>
    <item android:duration="125" android:drawable="@drawable/claude_spark_thinking4"/>
    <item android:duration="125" android:drawable="@drawable/claude_spark_thinking5"/>
    <item android:duration="125" android:drawable="@drawable/claude_spark_thinking6"/>
    <item android:duration="125" android:drawable="@drawable/claude_spark_thinking7"/>
    <item android:duration="125" android:drawable="@drawable/claude_spark_thinking8"/>
</animation-list>
```
* **Rotational Symmetry**: Each frame rotates the 8-point radiant starburst by `45° / 8 = 5.625°` while simultaneously scaling the ray thickness by `±10%`, creating an organic spinning/thinking feel.

---

## 3. Extended Thinking Box Anatomy & Micro-Animations

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. COLLAPSED STATE (ACTIVE STREAMING)                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  (✴️) Thinking…                                                ⌄ │  │ ◄── 8-Frame Rotating Spark (#D97757)
│  └──────────────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────────────┤
│ 2. COLLAPSED STATE (COMPLETED)                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  💡 Thought for 4 seconds                                      ⌄ │  │ ◄── Static Lightbulb / Elapsed Time
│  └──────────────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────────────┤
│ 3. EXPANDED STATE (TAP TO REVEAL)                                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  💡 Thought for 4 seconds                                      ⌃ │  │ ◄── Chevron Rotates 180°
│  │  ──────────────────────────────────────────────────────────────  │  │ ◄── Hairline Divider (#302F2C)
│  │  The user is requesting an optimization of the SQLite queries.   │  │
│  │  First, examine table indexes on 'chat_messages' table.          │  │ ◄── JetBrains Mono 12.5sp (#7B7974)
│  │  Need to verify if WAL mode is active...                         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.1 Jetpack Compose Implementation Architecture

```kotlin
@Composable
fun ExtendedThinkingBox(
    thinkingText: String,
    isThinking: Boolean = false,
    durationSeconds: Int = 4,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "ChevronRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1F1E1C))
            .border(1.dp, Color(0xFF282724), RoundedCornerShape(14.dp))
            .animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isThinking) {
                    ClaudeSparkThinkingAnimation(size = 18.dp, tint = BrandCoral)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thinking…",
                        style = HermesTypography.bodyMedium.copy(
                            color = BrandCoral,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thought for $durationSeconds seconds",
                        style = HermesTypography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(rotationZ = chevronRotation)
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF302F2C))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = thinkingText,
                    style = HermesTypography.bodyMedium.copy(
                        fontFamily = JetBrainsMono,
                        color = TextSubtle,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}
```

---

## 4. Deep Research & Stepper Progress Animations

When Claude executes multi-stage autonomous tasks or deep research (`ResearchStatusResponse`):

```
┌────────────────────────────────────────────────────────────────────────┐
│ 🕒 Drafting an Android animation design doc...                       › │
└────────────────────────────────────────────────────────────────────────┘
```

### 4.1 State Flow (`ResearchStatus`)
1. **`STARTING`**: Initializing task context.
2. **`PLANNING`**: Formulating subtask DAG.
3. **`SEARCHING`**: Querying web/database sources (`total_sources`, `top_source_domains`).
4. **`INITIATING_AGENTS`**: Spawning worker subagents.
5. **`CREATING_ARTIFACT`**: Compiling code/document artifact.
6. **`COMPLETED`**: Final result verified.

### 4.2 Pulsing Active Dot Animation
In the stepper modal sheet (`ExecutionSummarySheet`):
* The active step displays a pulsing dot oscillating alpha from `0.6f` to `1.0f` over `800ms` (`infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse)`).

---

## 5. Tool Execution Cards & Vector Checkmark Draw-In

When tools are executed inline:
1. **Running State**: Displays tool name (`"Web search"`, `"bash"`, `"Write"`) + `ClaudeSparkWritingAnimation` + amber/coral status chip (`"Running"`).
2. **Completion State**: Status switches to `"Completed"` in green (`#17A34A`), and the checkmark icon draws itself using vector path trimming:

```kotlin
val checkmarkProgress by animateFloatAsState(
    targetValue = if (isCompleted) 1.0f else 0.0f,
    animationSpec = tween(durationMillis = 200, easing = LinearEasing),
    label = "CheckmarkPathTrim"
)
```

---

## 6. 30 FPS Stream Smoothing Engine (`StreamSmoothingConfig`)

To prevent jerky UI layout recalculations during bursty token deliveries, Claude Android implements a hardware-timed token buffer:

```
Incoming SSE Token Packets (Bursty: 0 to 120 tokens/sec)
                      │
                      ▼
┌────────────────────────────────────────────────────────┐
│            CONCURRENT SMOOTHING RING BUFFER            │
└─────────────────────┬──────────────────────────────────┘
                      │ 33ms Tick Timer (30 FPS Choreographer)
                      ▼
┌────────────────────────────────────────────────────────┐
│         CHARACTER MICRO-BATCH EMITTER (3 chars/tick)   │
│         • smoother_tick_interval_ms: 33ms              │
│         • fade_in_duration_ms: 200ms                   │
│         • min_markdown_group_size_chars: 800           │
└─────────────────────┬──────────────────────────────────┘
                      │
                      ▼
             Jetpack Compose Text
```

---

## 7. Complete Summary Matrix: Animation Types

| Animation Scope | Component / State | Asset / Implementation | Timing / Spec |
|---|---|---|---|
| **Reasoning** | ThinkingBlock active | `claude_spark_animated_thinking` | 8 frames @ 125ms (1000ms loop) |
| **Writing** | Token generation active | `claude_spark_animated_writing` | 8 frames @ 125ms (1000ms loop) |
| **Idle** | Ambient Starburst | `claude_spark_animated_shimmer` | 15 frames @ 125ms (1875ms cycle) |
| **Smoothing** | Stream text rendering | `StreamSmoothingEngine` | 33ms tick / 30 FPS Choreographer |
| **Expansion** | Thinking box toggle | `animateContentSize` + `graphicsLayer` | 250ms `FastOutSlowInEasing`, 180° rot |
| **Tool Complete**| Checkmark draw-in | `trimPathEnd` (0f ➔ 1f) | 200ms `LinearEasing` |
| **Voice Audio** | Starburst breathing | Mic RMS scale (1.0f ➔ 1.28f) | Realtime spring-damped audio wave |
| **Voice Caption**| Synchronized words | `TTSWord` (`pts_ms`) | Realtime color shift (#B0AEA5 ➔ #FAF9F5)|
| **Sheets** | Modal Bottom Sheets | `res/anim/slide_in_bottom.xml` | 300ms `accelerate_decelerate_interpolator`|
