package com.example.hermes.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import kotlinx.coroutines.launch

@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { com.example.hermes.data.PreferencesManager.getInstance(context) }

    val personas = listOf("Airy", "Mellow", "Glassy", "Rounded", "Brass")
    val savedPersona by prefs.voicePersona.collectAsState(initial = "Rounded")
    val savedLanguage by prefs.voiceLanguage.collectAsState(initial = "English (United Kingdom)")
    val savedPace by prefs.voicePace.collectAsState(initial = "Normal")

    val selectedIndex = personas.indexOf(savedPersona).let { if (it >= 0) it else 3 }

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showPaceMenu by remember { mutableStateOf(false) }

    val languages = listOf(
        "English (United Kingdom)",
        "English (United States)",
        "English (Australia)",
        "French (France)",
        "German (Germany)",
        "Spanish (Spain)",
        "Japanese (Japan)"
    )
    val paces = listOf("Slow", "Normal", "Fast")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
        }

        // ── Persona Section Label ─────────────────────────────────────────────
        item {
            Text(
                text = "VOICE PERSONA",
                style = HermesTypography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

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
                                    coroutineScope.launch { prefs.setVoicePersona(personas[selectedIndex - 1]) }
                                } else if (totalDrag < -50 && selectedIndex < personas.size - 1) {
                                    coroutineScope.launch { prefs.setVoicePersona(personas[selectedIndex + 1]) }
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
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Left peeking card
                    if (selectedIndex > 0) {
                        val alpha by animateFloatAsState(
                            targetValue = 0.45f, animationSpec = tween(200), label = "leftAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp)
                                .width(110.dp)
                                .height(140.dp)
                                .alpha(alpha)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1C1B19))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                                .clickable { coroutineScope.launch { prefs.setVoicePersona(personas[selectedIndex - 1]) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = personas[selectedIndex - 1],
                                style = HermesTypography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    color = TextSubtle,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }

                    // Center active card
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFF222120))
                            .border(1.dp, Color(0xFF3A3835), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = personas[selectedIndex],
                                style = HermesTypography.displayLarge.copy(
                                    fontSize = 24.sp,
                                    color = TextPrimaryWarm,
                                    fontFamily = AnthropicSerif,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(BrandCoral.copy(alpha = 0.15f))
                                    .border(1.dp, BrandCoral.copy(alpha = 0.35f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    style = HermesTypography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = BrandCoral,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // Right peeking card
                    if (selectedIndex < personas.size - 1) {
                        val alpha by animateFloatAsState(
                            targetValue = 0.45f, animationSpec = tween(200), label = "rightAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                                .width(110.dp)
                                .height(140.dp)
                                .alpha(alpha)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1C1B19))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                                .clickable { coroutineScope.launch { prefs.setVoicePersona(personas[selectedIndex + 1]) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = personas[selectedIndex + 1],
                                style = HermesTypography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    color = TextSubtle,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    personas.forEachIndexed { idx, _ ->
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
                                .clickable { coroutineScope.launch { prefs.setVoicePersona(personas[idx]) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                // Swipe hint
                Text(
                    text = "Swipe to change voice",
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
