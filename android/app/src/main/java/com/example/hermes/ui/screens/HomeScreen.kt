package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.ui.components.*

@Composable
fun HomeScreen(
    userName: String = "Jishnu",
    onOpenDrawer: () -> Unit,
    onNavigateChat: (String?) -> Unit,
    onNavigateVoice: () -> Unit,
    onNavigateIncognito: () -> Unit = { onNavigateChat(null) },
    onUpgradeClick: () -> Unit = {}
) {
    var composerText by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("Sonnet 3.7") }
    var modelTier by remember { mutableStateOf("Low") }
    var showModelSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar: Hamburger on left, Ghost/Incognito on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onNavigateIncognito) {
                    ClaudeGhostIcon(tint = TextMuted, size = 25.dp)
                }
            }

            // Center Hero Greeting Section (Anthropic Starburst + Serif greeting)
            val isKeyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = if (isKeyboardVisible) 0.dp else (-20).dp)
                ) {
                    ClaudeStarburst(
                        size = if (isKeyboardVisible) 36.dp else 56.dp,
                        color = BrandCoral
                    )
                    Spacer(modifier = Modifier.height(if (isKeyboardVisible) 8.dp else 20.dp))
                    Text(
                        text = "Back at it, $userName",
                        style = HermesTypography.displayLarge.copy(
                            fontSize = if (isKeyboardVisible) 24.sp else 32.sp,
                            lineHeight = if (isKeyboardVisible) 28.sp else 38.sp,
                            color = TextPrimaryWarm
                        )
                    )
                }
            }

            // Docked Bottom Claude Home Composer
            ClaudeHomeComposer(
                text = composerText,
                onTextChange = { composerText = it },
                selectedModel = selectedModel,
                modelTier = modelTier,
                onModelClick = { showModelSheet = true },
                onAttachClick = { showAddSheet = true },
                onVoiceClick = onNavigateVoice,
                onSend = {
                    if (composerText.isNotBlank()) {
                        val query = composerText
                        composerText = ""
                        onNavigateChat(query)
                    }
                },
                onUpgradeClick = onUpgradeClick,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Model Select Bottom Sheet
        if (showModelSheet) {
            ModelSelectSheet(
                selectedModel = selectedModel,
                onModelSelected = { model ->
                    selectedModel = model
                    modelTier = if (model.contains("Sonnet")) "Low" else if (model.contains("Opus")) "Pro" else "Pro or Max"
                },
                onDismiss = { showModelSheet = false }
            )
        }

        // Add to Chat Bottom Sheet
        if (showAddSheet) {
            AddToChatSheet(
                onDismiss = { showAddSheet = false }
            )
        }
    }
}
