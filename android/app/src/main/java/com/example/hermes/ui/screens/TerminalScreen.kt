package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

@Composable
fun TerminalScreen(
    onBack: () -> Unit
) {
    var commandText by remember { mutableStateOf("") }

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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryWarm)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Live Execution",
                    style = HermesTypography.headlineMedium.copy(fontSize = 20.sp, color = TextPrimaryWarm)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextMuted)
                }
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
                Text(" • /data/jarvis", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 12.sp))
            }

            IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(18.dp))
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
                            Text("HERMES NODE TELEMETRY", style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 11.5.sp, letterSpacing = 1.sp))
                        }
                        Text("US-EAST-2A", style = HermesTypography.labelSmall.copy(color = BrandCoral, fontSize = 11.5.sp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryGauge("CPU", "24%", AccentWarning)
                        TelemetryGauge("RAM", "3.8 / 8GB", BrandCoral)
                        TelemetryGauge("Disk Free", "42 GB", TextPrimaryWarm, "HF Bucket")
                        TelemetryGauge("WebSocket", "18ms", AccentGreen, "Optimal")
                    }
                }
            }

            // macOS Terminal Console Window
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
                            Text("hermes@server-compute...", style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 11.sp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2D2C28))
                                    .clickable {}
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Clear", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 10.sp))
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2D2C28))
                                    .clickable {}
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Rollback", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 10.sp))
                            }
                        }
                    }

                    // Console Output
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("[System Engine v4.19-rc] Session bound via secure TLS handshake.", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 12.sp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("hermes@box:~$ git status -s", style = HermesTypography.labelSmall.copy(color = BrandCoral, fontSize = 12.sp))
                        Text(" M docs/ui-ux/DESIGN_SYSTEM.md", style = HermesTypography.labelSmall.copy(color = AccentWarning, fontSize = 12.sp))
                        Text(" M app/src/main/java/tech/hermes/ui/chat/ChatScreen.kt", style = HermesTypography.labelSmall.copy(color = AccentWarning, fontSize = 12.sp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("hermes@box:~$ ./gradlew testDebugUnitTest --continue", style = HermesTypography.labelSmall.copy(color = BrandCoral, fontSize = 12.sp))
                        Text("> Task :app:testDebugUnitTest", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 12.sp))
                        Text("🤖 [QA Agent] Visual regression test: SCREEN_2 to SCREEN_9 diff = 0.00%", style = HermesTypography.labelSmall.copy(color = AccentBlue, fontSize = 12.sp))
                        Text("🛡 [Security Agent] Vault scan: 0 hardcoded credentials detected.", style = HermesTypography.labelSmall.copy(color = AccentGreen, fontSize = 12.sp))
                        Text("BUILD SUCCESSFUL in 14s", style = HermesTypography.labelSmall.copy(color = AccentGreen, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
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
                        QuickCommandPill("git diff", Icons.Default.Description)
                        QuickCommandPill("git log -n 5", Icons.Default.History)
                        QuickCommandPill("checkpoint c...", Icons.Default.BookmarkBorder)
                    }
                }
            }
        }

        // Bottom Command Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF1E1D1B))
                .border(1.dp, BorderSubtle, RoundedCornerShape(26.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text("$ ", style = HermesTypography.titleLarge.copy(color = BrandCoral, fontSize = 16.sp))
                Text(
                    text = "Run command via Hermes authority",
                    style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 13.5.sp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(BrandTerracotta),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Dispatch", tint = PureWhite, modifier = Modifier.size(18.dp))
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
        Text(label, style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 11.sp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = HermesTypography.titleLarge.copy(color = valueColor, fontSize = 14.sp))
        if (subtext != null) {
            Text(subtext, style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 10.sp))
        }
    }
}

@Composable
private fun QuickCommandPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF1E1D1B))
            .border(1.dp, BorderSubtle, CircleShape)
            .clickable {}
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = HermesTypography.bodyMedium.copy(color = TextPrimaryWarm, fontSize = 12.sp))
    }
}
