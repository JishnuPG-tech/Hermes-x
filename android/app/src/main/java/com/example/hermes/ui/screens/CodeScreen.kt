package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.ui.components.*

@Composable
fun CodeScreen(
    onOpenDrawer: () -> Unit,
    onLaunchTerminal: () -> Unit
) {
    var selectedMode by remember { mutableStateOf("Auto") }
    var showPermissionDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = TextPrimaryWarm,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Claude Code",
                        style = HermesTypography.headlineMedium.copy(fontSize = 20.sp, color = TextPrimaryWarm)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Terminal Quick Launch Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF242320))
                            .border(1.dp, BorderSubtle, CircleShape)
                            .clickable(onClick = onLaunchTerminal)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnthropicIcon(AnthropicIcons.CommandLine, size = 15.dp, tint = TextPrimaryWarm)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Terminal", style = HermesTypography.labelSmall.copy(color = TextPrimaryWarm, fontSize = 12.sp))
                        }
                    }
                }
            }

            // Context Window Usage Bar (CCR)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Context window",
                        style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 12.sp)
                    )
                    Text(
                        text = "24k / 200k (12%)",
                        style = HermesTypography.labelSmall.copy(color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2926))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.12f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(AccentBlue)
                    )
                }
            }

            // Git Branch Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnthropicIcon(AnthropicIcons.Branch, size = 16.dp, tint = TextMuted)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "feat/docker-isolate-worker",
                    style = HermesTypography.bodyMedium.copy(
                        fontFamily = JetBrainsMono,
                        fontSize = 12.5.sp,
                        color = TextMuted
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1E281F))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Auto-merge ON", style = HermesTypography.labelSmall.copy(color = AccentGreen, fontSize = 11.sp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Prompt Bubble
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1D1B))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "Isolate worker workspaces in Docker container",
                                style = HermesTypography.bodyLarge.copy(fontSize = 15.sp, color = TextPrimaryWarm)
                            )
                        }
                    }
                }

                // Assistant Reasoning
                item {
                    Text(
                        text = "I'll create an isolated Dockerfile for the worker workspace and verify Playwright dependencies.",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 15.sp,
                            color = TextPrimaryWarm,
                            lineHeight = 22.sp
                        )
                    )
                }

                // Interactive Tool Steps
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1B1A18))
                            .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                            .padding(vertical = 8.dp)
                    ) {
                        ToolStepItem(title = "Grep", detail = "isolate|workspace|docker")
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        ToolStepItem(title = "Write", detail = "Dockerfile")
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        ToolStepItem(title = "Bash", detail = "docker build -t hermes-worker .")
                    }
                }

                // Action Approval Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF242017))
                            .border(1.dp, Color(0xFF4A3E20), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = AccentWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Permission Request: bash command",
                                style = HermesTypography.titleMedium.copy(fontSize = 14.sp, color = TextPrimaryWarm, fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$ docker push registry.internal/hermes-worker:latest",
                            style = HermesTypography.bodyMedium.copy(
                                fontFamily = JetBrainsMono,
                                color = TextMuted,
                                fontSize = 12.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Deny", fontSize = 13.sp)
                            }
                            Button(
                                onClick = { },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandCoral),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Approve", color = PureWhite, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Mode Toggle & Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF242320))
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable {
                            selectedMode = if (selectedMode == "Auto") "Plan" else if (selectedMode == "Plan") "Manual" else "Auto"
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Mode: $selectedMode ▾",
                        style = HermesTypography.labelSmall.copy(color = TextPrimaryWarm, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolStepItem(title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = HermesTypography.titleMedium.copy(
                fontFamily = JetBrainsMono,
                fontSize = 15.sp,
                color = TextPrimaryWarm,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = detail,
            style = HermesTypography.bodyMedium.copy(
                fontFamily = JetBrainsMono,
                fontSize = 13.5.sp,
                color = TextSubtle
            )
        )
    }
}
