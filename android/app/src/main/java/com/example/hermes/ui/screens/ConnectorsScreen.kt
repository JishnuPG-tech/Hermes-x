package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeToggle

@Composable
fun ConnectorsScreen(
    onBack: () -> Unit,
    viewModel: ConnectorsViewModel = viewModel()
) {
    var discoveryEnabled by remember { mutableStateOf(true) }
    val knowledgeSources by viewModel.knowledgeSources.collectAsStateWithLifecycle()
    val directoryServers by viewModel.directoryServers.collectAsStateWithLifecycle()

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
                text = "Connectors",
                style = HermesTypography.headlineMedium.copy(fontSize = 22.sp, color = TextPrimaryWarm),
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = { viewModel.refresh() },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Refresh connectors",
                    tint = TextPrimaryWarm,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Connector Discovery
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1F1E1D))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = null,
                        tint = TextPrimaryWarm,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connector discovery",
                            style = HermesTypography.titleLarge.copy(fontSize = 16.5.sp, color = TextPrimaryWarm)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Hermes will discover and query configured knowledge sources and active MCP tools.",
                            style = HermesTypography.bodyMedium.copy(fontSize = 13.5.sp, color = TextMuted, lineHeight = 18.sp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ClaudeToggle(
                        checked = discoveryEnabled,
                        onCheckedChange = { discoveryEnabled = it }
                    )
                }
            }

            // Card 2: Dynamic Live Connectors & Vault Sources
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1F1E1D))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                ) {
                    val allItems = mutableListOf<ConnectorUiModel>()

                    // Knowledge sources (Notion, Obsidian, Vaults)
                    knowledgeSources.forEach { ks ->
                        allItems.add(
                            ConnectorUiModel(
                                name = ks.name.replaceFirstChar { it.uppercase() },
                                subtitle = "Type: ${ks.type} • Status: ${ks.status}",
                                badge = if (ks.primary) "Primary" else null,
                                isExternal = !ks.primary
                            )
                        )
                    }

                    // Directory MCP servers
                    directoryServers.forEach { ds ->
                        allItems.add(
                            ConnectorUiModel(
                                name = ds.name.ifBlank { ds.id },
                                subtitle = ds.description.ifBlank { "${ds.tools.size} active tools" },
                                badge = if (ds.tools.isNotEmpty()) "${ds.tools.size}" else null,
                                isExternal = false
                            )
                        )
                    }

                    if (allItems.isNotEmpty()) {
                        allItems.forEachIndexed { index, item ->
                            if (index > 0) {
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                            }
                            ConnectorRow(item.name, item.subtitle, item.badge, item.isExternal)
                        }
                    } else {
                        // Clean empty state
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Storage, contentDescription = null, tint = TextMuted, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("No Connectors Configured", style = HermesTypography.titleMedium.copy(color = TextPrimaryWarm))
                                Text("Register MCP servers or Notion/Obsidian in config to bind connectors.", style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 12.sp))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private data class ConnectorUiModel(
    val name: String,
    val subtitle: String,
    val badge: String? = null,
    val isExternal: Boolean = false
)

@Composable
private fun ConnectorRow(
    name: String,
    subtitle: String,
    badge: String? = null,
    isExternal: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E2D2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Storage, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = name,
                    style = HermesTypography.titleLarge.copy(fontSize = 16.sp, color = TextPrimaryWarm)
                )
                Text(
                    text = subtitle,
                    style = HermesTypography.bodyMedium.copy(fontSize = 12.sp, color = TextSubtle)
                )
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF233E60))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    style = HermesTypography.labelSmall.copy(color = AccentBlue, fontSize = 11.sp)
                )
            }
        } else if (isExternal) {
            Icon(
                imageVector = Icons.Outlined.NorthEast,
                contentDescription = "External link",
                tint = TextSubtle,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
