package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.theme.*

@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    viewModel: TerminalViewModel = viewModel()
) {
    var commandText by remember { mutableStateOf("") }
    val hostStatus by viewModel.hostStatus.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimaryWarm)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Live Execution",
                    style = HermesTypography.headlineMedium.copy(fontSize = 20.sp, color = TextPrimaryWarm)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AvatarSalmon),
                    contentAlignment = Alignment.Center
                ) {
                    Text("J", style = HermesTypography.titleLarge.copy(fontSize = 15.sp, color = PureWhite))
                }
            }
        }

        // Subheader Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1E241E))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Online", style = HermesTypography.labelSmall.copy(color = AccentGreen, fontSize = 12.sp))
                Text(
                    " • ${hostStatus?.storage_root ?: "/data"}",
                    style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 12.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Node Telemetry Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1B19))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HERMES HOST TELEMETRY", style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 11.5.sp, letterSpacing = 1.sp))
                        }
                        Text(
                            if (hostStatus?.writable == true) "RW MOUNTED" else "RO / DISCONNECTED",
                            style = HermesTypography.labelSmall.copy(
                                color = if (hostStatus?.writable == true) AccentGreen else AccentWarning,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val freeGb = hostStatus?.disk_free_gb ?: 0.0
                        val totalGb = hostStatus?.disk_total_gb ?: 0.0
                        val activeWorkspaces = hostStatus?.active_workspaces_count ?: 0
                        val activeProjects = hostStatus?.active_projects_count ?: 0

                        TelemetryGauge("Status", hostStatus?.status ?: "Ready", AccentGreen)
                        TelemetryGauge("Disk Free", "${freeGb.toInt()} GB", BrandCoral, "of ${totalGb.toInt()} GB")
                        TelemetryGauge("Workspaces", "$activeWorkspaces", TextPrimaryWarm, "Active")
                        TelemetryGauge("Projects", "$activeProjects", AccentBlue, "Registered")
                    }
                }
            }

            // macOS / Linux Terminal Console Window
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF161514))
                        .border(1.dp, Color(0xFF2E2D2A), RoundedCornerShape(18.dp))
                ) {
                    // Terminal Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF22211E))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE2726E)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEAB308)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("hermes@hf-space:~$", style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 11.sp))
                        }
                    }

                    // Console Output
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        terminalLogs.takeLast(40).forEach { line ->
                            val color = when {
                                line.startsWith("$") -> BrandCoral
                                line.startsWith("[Connected") -> AccentGreen
                                line.startsWith("[PTY Error") -> AccentWarning
                                line.contains("SUCCESS", true) -> AccentGreen
                                else -> TextPrimaryWarm
                            }
                            Text(line, style = HermesTypography.labelSmall.copy(color = color, fontSize = 11.5.sp))
                        }
                    }
                }
            }

            // Quick Commands
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("QUICK COMMANDS", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 11.sp, letterSpacing = 1.sp))
                        Text("Tap to dispatch", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 11.sp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickCommandPill("git status -s", Icons.Default.Description) {
                            viewModel.sendCommand("git status -s")
                        }
                        QuickCommandPill("git log -n 5", Icons.Default.History) {
                            viewModel.sendCommand("git log -n 5")
                        }
                        QuickCommandPill("ls -la", Icons.Default.Folder) {
                            viewModel.sendCommand("ls -la")
                        }
                    }
                }
            }
        }

        // Bottom Command Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF191816),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    placeholder = {
                        Text("Type terminal command...", style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 13.sp))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandCoral,
                        unfocusedBorderColor = Color(0xFF2E2D2A),
                        focusedContainerColor = Color(0xFF141312),
                        unfocusedContainerColor = Color(0xFF141312),
                        focusedTextColor = TextPrimaryWarm,
                        unfocusedTextColor = TextPrimaryWarm
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (commandText.isNotBlank()) {
                            viewModel.sendCommand(commandText)
                            commandText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (commandText.isNotBlank()) BrandCoral else Color(0xFF262522))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Send",
                        tint = if (commandText.isNotBlank()) PureWhite else TextSubtle
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryGauge(
    label: String,
    value: String,
    valueColor: Color,
    subtext: String? = null
) {
    Column {
        Text(label, style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 10.5.sp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = HermesTypography.titleLarge.copy(color = valueColor, fontSize = 15.sp))
        if (subtext != null) {
            Text(subtext, style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 9.5.sp))
        }
    }
}

@Composable
private fun QuickCommandPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF23221F))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = HermesTypography.labelSmall.copy(color = TextPrimaryWarm, fontSize = 11.5.sp))
    }
}
