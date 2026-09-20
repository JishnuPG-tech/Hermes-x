package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeToggle

@Composable
fun ConnectorsScreen(
    onBack: () -> Unit
) {
    var discoveryEnabled by remember { mutableStateOf(true) }

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
                    imageVector = Icons.Default.ArrowBack,
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
                onClick = { },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add connector",
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
                            text = "Claude will help you find available connectors in your directory.",
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

            // Card 2: Connectors List Group
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1F1E1D))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                ) {
                    ConnectorRow("Anthropic Economic Index", badge = "9")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    ConnectorRow("Context7", badge = "2")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    ConnectorRow("Figma", badge = "37")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    ConnectorRow("Firecrawl", badge = "6")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    ConnectorRow("Hugging Face", badge = "5")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    ConnectorRow("Lovable", badge = "41")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    ConnectorRow("Google Drive", isExternal = true)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    ConnectorRow("Huggingfac", isExternal = true)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    ConnectorRow("Huggingface", isExternal = true)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ConnectorRow(
    name: String,
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
                    .background(PureWhite),
                contentAlignment = Alignment.Center
            ) {
                // Service icon placeholder container
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = name,
                style = HermesTypography.titleLarge.copy(fontSize = 16.sp, color = TextPrimaryWarm)
            )
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF233E60)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    style = HermesTypography.labelSmall.copy(color = AccentBlue, fontSize = 12.sp)
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
