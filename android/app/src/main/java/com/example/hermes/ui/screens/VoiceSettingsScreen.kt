package com.example.hermes.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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
    val language = savedLanguage
    val pace = savedPace

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimaryWarm,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Voice settings",
                style = HermesTypography.headlineMedium.copy(
                    fontSize = 24.sp,
                    color = TextPrimaryWarm,
                    fontFamily = AnthropicSerif,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Persona Carousel (Screenshots 3-7)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
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
            // Card Carousel Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Left Peeking Card
                if (selectedIndex > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-135).dp)
                            .size(155.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color(0xFF1C1B19))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(30.dp))
                            .clickable { coroutineScope.launch { prefs.setVoicePersona(personas[selectedIndex - 1]) } }
                            .padding(end = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = personas[selectedIndex - 1].takeLast(3),
                            style = HermesTypography.titleLarge.copy(fontSize = 18.sp, color = TextSubtle)
                        )
                    }
                }

                // Center Active Card
                Box(
                    modifier = Modifier
                        .size(186.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(Color(0xFF22211E))
                        .border(1.dp, Color(0xFF383632), RoundedCornerShape(34.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = personas[selectedIndex],
                        style = HermesTypography.displayLarge.copy(
                            fontSize = 22.sp,
                            color = TextPrimaryWarm,
                            fontFamily = AnthropicSerif,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                // Right Peeking Card
                if (selectedIndex < personas.size - 1) {
                    Box(
                        modifier = Modifier
                            .offset(x = 135.dp)
                            .size(155.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color(0xFF1C1B19))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(30.dp))
                            .clickable { coroutineScope.launch { prefs.setVoicePersona(personas[selectedIndex + 1]) } }
                            .padding(start = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = personas[selectedIndex + 1].take(3),
                            style = HermesTypography.titleLarge.copy(fontSize = 18.sp, color = TextSubtle)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Carousel Pagination Dots (5 Dots with animated size/color)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                personas.forEachIndexed { idx, _ ->
                    val isSelected = idx == selectedIndex
                    val dotSize by animateDpAsState(
                        targetValue = if (isSelected) 7.dp else 5.5.dp,
                        animationSpec = tween(200),
                        label = "dotSize"
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(if (isSelected) TextPrimaryWarm else Color(0xFF383733))
                            .clickable { coroutineScope.launch { prefs.setVoicePersona(personas[idx]) } }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Settings Options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language Row
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1B1B19))
                        .clickable { showLanguageMenu = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF282724))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("BETA", style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(language, style = HermesTypography.bodyLarge.copy(fontSize = 15.sp, color = TextPrimaryWarm))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(20.dp))
                }

                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1D1B))
                        .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                        .padding(vertical = 6.dp)
                ) {
                    languages.forEach { lang ->
                        val isSelected = lang == language
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
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = BrandCoral,
                                            modifier = Modifier.size(18.dp)
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

            // Pace Row
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1B1B19))
                        .clickable { showPaceMenu = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pace", style = HermesTypography.bodyLarge.copy(fontSize = 15.sp, color = TextPrimaryWarm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pace, style = HermesTypography.bodyMedium.copy(color = TextMuted, fontSize = 14.sp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(20.dp))
                    }
                }

                DropdownMenu(
                    expanded = showPaceMenu,
                    onDismissRequest = { showPaceMenu = false },
                    modifier = Modifier
                        .width(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1D1B))
                        .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                        .padding(vertical = 6.dp)
                ) {
                    paces.forEach { p ->
                        val isSelected = p == pace
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
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = BrandCoral,
                                            modifier = Modifier.size(18.dp)
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
}
