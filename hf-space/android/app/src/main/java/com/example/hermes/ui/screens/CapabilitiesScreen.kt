package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeToggle
import kotlinx.coroutines.launch

import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun CapabilitiesScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { com.example.hermes.data.PreferencesManager.getInstance(context) }

    val webSearchEnabled by prefs.capWebSearch.collectAsState(initial = true)
    val inlineVisualizationsEnabled by prefs.capInlineViz.collectAsState(initial = true)
    val codeExecutionEnabled by prefs.capCodeExec.collectAsState(initial = true)
    val switchModelsEnabled by prefs.capSwitchModels.collectAsState(initial = true)
    val generateMemoryEnabled by prefs.capGenMemory.collectAsState(initial = true)
    val sensitiveMemoryEnabled by prefs.capSensitiveMem.collectAsState(initial = false)
    val selectedToolAccess by prefs.capToolAccess.collectAsState(initial = "Auto")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        // Top Navigation Header
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimaryWarm,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Capabilities",
                style = HermesTypography.headlineMedium.copy(fontSize = 22.sp, color = TextPrimaryWarm),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Scrollable Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Group 1: Capabilities
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1F1E1D))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                // Web search
                CapabilityItem(
                    title = "Web search",
                    description = "Hermes will automatically search the web when it determines it needs current information",
                    icon = Icons.Outlined.Language,
                    checked = webSearchEnabled,
                    onCheckedChange = { coroutineScope.launch { prefs.setCapWebSearch(it) } }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                // Artifacts (Required)
                CapabilityItem(
                    title = "Artifacts",
                    description = "Required by code execution",
                    icon = Icons.Outlined.Description,
                    checked = true,
                    enabled = false,
                    onCheckedChange = {}
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                // Inline visualizations (BETA)
                CapabilityItem(
                    title = "Inline visualizations",
                    description = "Allow Hermes to generate interactive visualizations, charts, and diagrams directly in the conversation.",
                    icon = Icons.Outlined.BarChart,
                    badge = "BETA",
                    checked = inlineVisualizationsEnabled,
                    onCheckedChange = { coroutineScope.launch { prefs.setCapInlineViz(it) } }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                // Code execution and file creation
                CapabilityItem(
                    title = "Code execution and file creation",
                    description = "Allow Hermes to execute code and create and edit docs, spreadsheets, presentations, PDFs, and data reports.",
                    icon = Icons.Outlined.Code,
                    checked = codeExecutionEnabled,
                    onCheckedChange = { coroutineScope.launch { prefs.setCapCodeExec(it) } }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                // Switch models
                CapabilityItem(
                    title = "Switch models when a message is flagged",
                    description = "When safety measures flag a message, automatically switch to a different model to keep chatting. When off, your chat will pause instead.",
                    icon = Icons.Outlined.SwapVert,
                    checked = switchModelsEnabled,
                    onCheckedChange = { coroutineScope.launch { prefs.setCapSwitchModels(it) } }
                )
            }

            // Group 2: Memory
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Memory",
                    style = HermesTypography.titleMedium.copy(fontSize = 17.sp, color = TextSubtle),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1F1E1D))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                ) {
                    // Generate memory
                    CapabilityItem(
                        title = "Generate memory from chats",
                        description = "Allow Hermes to generate memory from your chats.",
                        checked = generateMemoryEnabled,
                        onCheckedChange = { coroutineScope.launch { prefs.setCapGenMemory(it) } }
                    )

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                    // Sensitive topics
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include sensitive topics in memory",
                                    style = HermesTypography.titleLarge.copy(fontSize = 16.5.sp, color = TextPrimaryWarm)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Allow Hermes to save details about sensitive topics like health conditions or religious beliefs to memory. ",
                                    style = HermesTypography.bodyMedium.copy(fontSize = 13.5.sp, color = TextMuted, lineHeight = 18.sp)
                                )
                                Text(
                                    text = "Learn more.",
                                    style = HermesTypography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        color = BrandCoral,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    modifier = Modifier.clickable { }
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            ClaudeToggle(
                                checked = sensitiveMemoryEnabled,
                                onCheckedChange = { coroutineScope.launch { prefs.setCapSensitiveMem(it) } }
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                    // Memory files
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Memory files",
                                style = HermesTypography.titleLarge.copy(fontSize = 16.5.sp, color = TextPrimaryWarm)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "View and manage what Hermes remembers.",
                                style = HermesTypography.bodyMedium.copy(fontSize = 13.5.sp, color = TextMuted)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Group 3: Tool access
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tool access",
                    style = HermesTypography.titleMedium.copy(fontSize = 17.sp, color = TextSubtle),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1F1E1D))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                ) {
                    // Auto
                    ToolAccessOptionRow(
                        title = "Auto",
                        description = "Hermes chooses for you",
                        isSelected = selectedToolAccess == "Auto",
                        onClick = { coroutineScope.launch { prefs.setCapToolAccess("Auto") } }
                    )

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                    // On demand
                    ToolAccessOptionRow(
                        title = "On demand",
                        description = "Load when needed. More messages, lower accuracy",
                        isSelected = selectedToolAccess == "On demand",
                        onClick = { coroutineScope.launch { prefs.setCapToolAccess("On demand") } }
                    )

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                    // Always available
                    ToolAccessOptionRow(
                        title = "Always available",
                        description = "Ready from start. Fewer messages, better accuracy",
                        isSelected = selectedToolAccess == "Always available",
                        onClick = { coroutineScope.launch { prefs.setCapToolAccess("Always available") } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CapabilityItem(
    title: String,
    description: String,
    icon: ImageVector? = null,
    badge: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (enabled) TextPrimaryWarm else TextSubtle
    val descColor = if (enabled) TextMuted else TextSubtle

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) TextPrimaryWarm else TextSubtle,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = HermesTypography.titleLarge.copy(fontSize = 16.5.sp, color = textColor),
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF262522))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 10.sp),
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = HermesTypography.bodyMedium.copy(fontSize = 13.5.sp, color = descColor, lineHeight = 18.sp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        ClaudeToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun ToolAccessOptionRow(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = HermesTypography.titleLarge.copy(
                    fontSize = 16.5.sp,
                    color = if (isSelected) AccentBlue else TextPrimaryWarm
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = HermesTypography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    color = if (isSelected) AccentBlue else TextMuted
                )
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
