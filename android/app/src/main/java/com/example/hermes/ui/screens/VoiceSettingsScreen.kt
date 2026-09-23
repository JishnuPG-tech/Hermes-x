package com.example.hermes.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.voice.PersonaDemoPlayer
import kotlinx.coroutines.launch

data class VoicePersonaItem(
    val id: String,
    val name: String,
    val gender: String,
    val characterTag: String,
    val voiceModel: String,
    val description: String,
    val isRecommended: Boolean = false
)

private val VOICE_PERSONAS = listOf(
    VoicePersonaItem(
        id = "Jenny",
        name = "Jenny",
        gender = "Female",
        characterTag = "Sweet & Calm",
        voiceModel = "en-US-JennyNeural",
        description = "Warm, soothing, calm, and sweet conversational voice.",
        isRecommended = true
    ),
    VoicePersonaItem(
        id = "Ava",
        name = "Ava",
        gender = "Female",
        characterTag = "Articulate",
        voiceModel = "en-US-AvaNeural",
        description = "Crisp, poised, modern, and articulate female voice."
    ),
    VoicePersonaItem(
        id = "Aria",
        name = "Aria",
        gender = "Female",
        characterTag = "Expressive",
        voiceModel = "en-US-AriaNeural",
        description = "Bright, lively, expressive, and natural conversational cadence."
    ),
    VoicePersonaItem(
        id = "Chris",
        name = "Chris",
        gender = "Male",
        characterTag = "Sophisticated",
        voiceModel = "en-US-ChristopherNeural",
        description = "Distinguished, cultured, calm, and thoughtful male voice."
    ),
    VoicePersonaItem(
        id = "Guy",
        name = "Guy",
        gender = "Male",
        characterTag = "Casual & Warm",
        voiceModel = "en-US-GuyNeural",
        description = "Engaging, casual, warm, and natural conversational companion."
    ),
    VoicePersonaItem(
        id = "Eric",
        name = "Eric",
        gender = "Male",
        characterTag = "Authoritative",
        voiceModel = "en-US-EricNeural",
        description = "Authoritative, resonant, commanding, and deep tone."
    )
)

@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { com.example.hermes.data.PreferencesManager.getInstance(context) }

    val savedPersona by prefs.voicePersona.collectAsState(initial = "Jenny")
    val savedLanguage by prefs.voiceLanguage.collectAsState(initial = "English (United States)")
    val savedPace by prefs.voicePace.collectAsState(initial = "Normal")
    val savedVoiceMode by prefs.voiceMode.collectAsState(initial = "hugging_voice")

    val selectedIndex = VOICE_PERSONAS.indexOfFirst {
        it.id.equals(savedPersona, ignoreCase = true)
    }.let { if (it >= 0) it else 0 }

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showPaceMenu by remember { mutableStateOf(false) }

    // ── Demo Playback ────────────────────────────────────────────────────────
    var isPlayingDemo by remember { mutableStateOf(false) }

    // Cancel demo when screen leaves composition
    DisposableEffect(Unit) {
        onDispose { PersonaDemoPlayer.cancel() }
    }

    // Auto-play demo whenever persona changes (debounce: only after a swipe settles)
    LaunchedEffect(selectedIndex) {
        isPlayingDemo = true
        val persona = VOICE_PERSONAS[selectedIndex]
        PersonaDemoPlayer.playDemo(context, persona.voiceModel)
        // Show "playing" indicator for ~7 seconds then clear
        kotlinx.coroutines.delay(7_200)
        isPlayingDemo = false
    }

    val languages = listOf(
        "English (United States)",
        "English (United Kingdom)",
        "English (Australia)",
        "French (France)",
        "German (Germany)",
        "Spanish (Spain)",
        "Japanese (Japan)"
    )
    val paces = listOf("Slow", "Normal", "Fast")

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CanvasNearBlack)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Voice",
                    style = HermesTypography.headlineMedium.copy(
                        fontSize = 20.sp,
                        color = TextPrimaryWarm,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        containerColor = CanvasNearBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // ── Carousel top spacing ──────────────────────────────────────────────
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ── Persona Carousel ──────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                                onDragEnd = {
                                    if (totalDrag > 50 && selectedIndex > 0) {
                                        coroutineScope.launch { prefs.setVoicePersona(VOICE_PERSONAS[selectedIndex - 1].id) }
                                    } else if (totalDrag < -50 && selectedIndex < VOICE_PERSONAS.size - 1) {
                                        coroutineScope.launch { prefs.setVoicePersona(VOICE_PERSONAS[selectedIndex + 1].id) }
                                    }
                                }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Persona Cards Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Left peeking card
                        if (selectedIndex > 0) {
                            val leftPersona = VOICE_PERSONAS[selectedIndex - 1]
                            val alpha by animateFloatAsState(
                                targetValue = 0.45f, animationSpec = tween(200), label = "leftAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 16.dp)
                                    .width(110.dp)
                                    .height(145.dp)
                                    .alpha(alpha)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF1C1B19))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                                    .clickable { coroutineScope.launch { prefs.setVoicePersona(leftPersona.id) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = leftPersona.name,
                                        style = HermesTypography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            color = TextSubtle,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = leftPersona.gender.uppercase(),
                                        style = HermesTypography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = TextMuted
                                        )
                                    )
                                }
                            }
                        }

                        // Center active card
                        val activePersona = VOICE_PERSONAS[selectedIndex]
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(195.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFF222120))
                                .border(
                                    1.dp,
                                    if (isPlayingDemo) BrandCoral.copy(alpha = 0.45f) else Color(0xFF3A3835),
                                    RoundedCornerShape(28.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = activePersona.name,
                                    style = HermesTypography.displayLarge.copy(
                                        fontSize = 28.sp,
                                        color = TextPrimaryWarm,
                                        fontFamily = AnthropicSerif,
                                        fontWeight = FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = activePersona.gender,
                                    style = HermesTypography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Normal,
                                        letterSpacing = 0.3.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                // ── Animated equalizer bars (visible while demo plays) ──
                                val waveAlpha by animateFloatAsState(
                                    targetValue = if (isPlayingDemo) 1f else 0f,
                                    animationSpec = tween(400),
                                    label = "waveAlpha"
                                )
                                val infiniteTransition = rememberInfiniteTransition(label = "wave")
                                val barHeights = listOf(
                                    infiniteTransition.animateFloat(
                                        initialValue = 4f, targetValue = 16f,
                                        animationSpec = infiniteRepeatable(tween<Float>(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "b0"
                                    ),
                                    infiniteTransition.animateFloat(
                                        initialValue = 10f, targetValue = 22f,
                                        animationSpec = infiniteRepeatable(tween<Float>(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "b1"
                                    ),
                                    infiniteTransition.animateFloat(
                                        initialValue = 6f, targetValue = 20f,
                                        animationSpec = infiniteRepeatable(tween<Float>(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "b2"
                                    ),
                                    infiniteTransition.animateFloat(
                                        initialValue = 12f, targetValue = 24f,
                                        animationSpec = infiniteRepeatable(tween<Float>(460, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "b3"
                                    ),
                                    infiniteTransition.animateFloat(
                                        initialValue = 4f, targetValue = 14f,
                                        animationSpec = infiniteRepeatable(tween<Float>(340, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "b4"
                                    ),
                                )
                                Row(
                                    modifier = Modifier
                                        .height(26.dp)
                                        .alpha(waveAlpha),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    barHeights.forEach { heightState ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(heightState.value.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(BrandCoral.copy(alpha = 0.85f))
                                        )
                                    }
                                }
                            }
                        }

                        // Right peeking card
                        if (selectedIndex < VOICE_PERSONAS.size - 1) {
                            val rightPersona = VOICE_PERSONAS[selectedIndex + 1]
                            val alpha by animateFloatAsState(
                                targetValue = 0.45f, animationSpec = tween(200), label = "rightAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 16.dp)
                                    .width(110.dp)
                                    .height(145.dp)
                                    .alpha(alpha)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF1C1B19))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                                    .clickable { coroutineScope.launch { prefs.setVoicePersona(rightPersona.id) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = rightPersona.name,
                                        style = HermesTypography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            color = TextSubtle,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rightPersona.gender.uppercase(),
                                        style = HermesTypography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Dot indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VOICE_PERSONAS.forEachIndexed { idx, p ->
                            val isSelected = idx == selectedIndex
                            val dotSize by animateDpAsState(
                                targetValue = if (isSelected) 7.dp else 4.5.dp,
                                animationSpec = tween(200),
                                label = "dotSize"
                            )
                            val dotAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.35f,
                                animationSpec = tween(200),
                                label = "dotAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .alpha(dotAlpha)
                                    .clip(CircleShape)
                                    .background(TextPrimaryWarm)
                                    .clickable { coroutineScope.launch { prefs.setVoicePersona(p.id) } }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Swipe to switch AI voice persona",
                        style = HermesTypography.labelSmall.copy(
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    )
                }
            }


            item { Spacer(modifier = Modifier.height(28.dp)) }


            // ── Settings Section Label ──────────────────────────────────────────
            item {
                Text(
                    text = "SETTINGS",
                    style = HermesTypography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // ── Language + Pace rows inside single card ──────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1F1E1C))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                ) {
                    // Language Row
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageMenu = true }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Language",
                                    style = HermesTypography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        color = TextPrimaryWarm,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = savedLanguage,
                                        style = HermesTypography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            color = TextMuted
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF282724))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            "BETA",
                                            style = HermesTypography.labelSmall.copy(
                                                color = TextMuted,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSubtle,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1D1B))
                                .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(16.dp))
                                .padding(vertical = 6.dp)
                        ) {
                            languages.forEach { lang ->
                                val isSelected = lang == savedLanguage
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = lang,
                                                style = HermesTypography.bodyMedium.copy(
                                                    fontSize = 15.sp,
                                                    color = if (isSelected) TextPrimaryWarm else TextMuted
                                                )
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = BrandCoral,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        coroutineScope.launch { prefs.setVoiceLanguage(lang) }
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                    // Pace Row
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPaceMenu = true }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Speaking pace",
                                style = HermesTypography.bodyLarge.copy(
                                    fontSize = 15.sp,
                                    color = TextPrimaryWarm,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = savedPace,
                                    style = HermesTypography.bodyMedium.copy(
                                        color = TextMuted,
                                        fontSize = 14.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextSubtle,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showPaceMenu,
                            onDismissRequest = { showPaceMenu = false },
                            modifier = Modifier
                                .width(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1D1B))
                                .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(16.dp))
                                .padding(vertical = 6.dp)
                        ) {
                            paces.forEach { p ->
                                val isSelected = p == savedPace
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = p,
                                                style = HermesTypography.bodyMedium.copy(
                                                    fontSize = 15.sp,
                                                    color = if (isSelected) TextPrimaryWarm else TextMuted
                                                )
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = BrandCoral,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        coroutineScope.launch { prefs.setVoicePace(p) }
                                        showPaceMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // ── Engine Mode Section Label ─────────────────────────────────────────
            item {
                Text(
                    text = "ENGINE ARCHITECTURE",
                    style = HermesTypography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // ── Engine Mode Cards ─────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF191816))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                ) {
                    // Option 1: Hugging Voice (Realtime)
                    val isHuggingVoice = savedVoiceMode == "hugging_voice" || savedVoiceMode == "apollo"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { coroutineScope.launch { prefs.setVoiceMode("hugging_voice") } }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Hugging Voice (Realtime)",
                                    modifier = Modifier.weight(1f, fill = false),
                                    style = HermesTypography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        color = if (isHuggingVoice) TextPrimaryWarm else TextMuted,
                                        fontWeight = if (isHuggingVoice) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E3A24))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "RECOMMENDED",
                                        maxLines = 1,
                                        softWrap = false,
                                        style = HermesTypography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Full-duplex conversational voice, sub-50ms instant barge-in, AudioTrack streaming PCM.",
                                style = HermesTypography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                        if (isHuggingVoice) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = BrandCoral,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

                    // Option 2: Classic Voice Engine
                    val isClassic = savedVoiceMode == "classic"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { coroutineScope.launch { prefs.setVoiceMode("classic") } }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Classic Voice Engine",
                                style = HermesTypography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    color = if (isClassic) TextPrimaryWarm else TextMuted,
                                    fontWeight = if (isClassic) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Standard multiplexed turn-based audio with MediaPlayer buffering.",
                                style = HermesTypography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                        if (isClassic) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = BrandCoral,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ── Footer note ──────────────────────────────────────────────────────
            item {
                Text(
                    text = "Voice quality and language availability depend on your device and network conditions.",
                    style = HermesTypography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}
