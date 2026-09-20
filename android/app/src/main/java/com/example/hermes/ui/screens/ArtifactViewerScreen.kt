package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeCodeBlock
import com.example.hermes.ui.components.ClaudeHomeComposer
import com.example.hermes.ui.components.ClaudeMarkdownView
import com.example.hermes.ui.components.InlineCyanCodePill

@Composable
fun ArtifactViewerScreen(
    artifactTitle: String = "06-android-implementation-design.md",
    artifactType: String = "Document · MD",
    artifactCode: String? = null,
    artifactLanguage: String? = null,
    onBack: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    var followUpText by remember { mutableStateOf("") }
    var versionIndex by remember { mutableStateOf(1) }
    val totalVersions = 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar (Exact Match to media_1789883537967.jpg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = artifactTitle,
                    style = HermesTypography.titleMedium.copy(
                        fontSize = 17.sp,
                        color = TextPrimaryWarm,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextPrimaryWarm,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier
                            .width(220.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1E1D1B))
                            .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                            .padding(vertical = 8.dp)
                    ) {
                        // Exact 6 menu items from Screenshot 11 (android/Animation/11-43-32)
                        DropdownMenuItem(
                            text = { Text("Publish", color = TextPrimaryWarm, fontSize = 15.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Public, null, tint = TextPrimaryWarm, modifier = Modifier.size(20.dp)) },
                            onClick = { showOverflowMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy", color = TextPrimaryWarm, fontSize = 15.sp) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = TextPrimaryWarm, modifier = Modifier.size(20.dp)) },
                            onClick = { showOverflowMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Download", color = TextPrimaryWarm, fontSize = 15.sp) },
                            leadingIcon = { Icon(Icons.Outlined.FileDownload, null, tint = TextPrimaryWarm, modifier = Modifier.size(20.dp)) },
                            onClick = { showOverflowMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Save as PDF", color = TextPrimaryWarm, fontSize = 15.sp) },
                            leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null, tint = TextPrimaryWarm, modifier = Modifier.size(20.dp)) },
                            onClick = { showOverflowMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Share as PDF", color = TextPrimaryWarm, fontSize = 15.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Share, null, tint = TextPrimaryWarm, modifier = Modifier.size(20.dp)) },
                            onClick = { showOverflowMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Print", color = TextPrimaryWarm, fontSize = 15.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Print, null, tint = TextPrimaryWarm, modifier = Modifier.size(20.dp)) },
                            onClick = { showOverflowMenu = false }
                        )
                    }
                }
            }

            val isCode = artifactType.contains("Code", ignoreCase = true) ||
                    artifactType.contains("PY", ignoreCase = true) ||
                    artifactType.contains("JS", ignoreCase = true) ||
                    artifactType.contains("KT", ignoreCase = true) ||
                    (artifactLanguage != null && !artifactLanguage.equals("Markdown", ignoreCase = true) && !artifactLanguage.equals("Text", ignoreCase = true))

            val displayCode = artifactCode ?: ""

            if (isCode) {
                // Code Viewer
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    item {
                        if (displayCode.isNotBlank()) {
                            ClaudeCodeBlock(
                                code = displayCode,
                                language = artifactLanguage ?: "Code",
                                onCopy = {}
                            )
                        } else {
                            Text(
                                text = "No code content available for this artifact.",
                                style = HermesTypography.bodyMedium.copy(color = TextSubtle),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Document / Markdown View
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = artifactTitle,
                            style = HermesTypography.displayLarge.copy(
                                fontSize = 28.sp,
                                lineHeight = 34.sp,
                                fontFamily = AnthropicSerif,
                                color = TextPrimaryWarm,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }

                    item {
                        if (displayCode.isNotBlank()) {
                            ClaudeMarkdownView(content = displayCode)
                        } else {
                            Text(
                                text = "No document content available for this artifact.",
                                style = HermesTypography.bodyMedium.copy(color = TextSubtle),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Claude Follow-Up Composer Bar
            ClaudeHomeComposer(
                text = followUpText,
                onTextChange = { followUpText = it },
                selectedModel = "Sonnet 5",
                modelTier = "Low",
                placeholder = "Reply to Claude...",
                showProBanner = false,
                onModelClick = {},
                onAttachClick = {},
                onVoiceClick = {},
                onSend = { followUpText = "" },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
