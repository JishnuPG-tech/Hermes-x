package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ArtifactShapesIcon

data class ArtifactGridItem(
    val id: String,
    val title: String,
    val type: String,
    val language: String?,
    val codePreview: String,
    val fullCode: String?,
    val date: String
)

@Composable
fun ArtifactsScreen(
    onOpenDrawer: () -> Unit,
    onOpenArtifact: (String, String, String?, String?) -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val liveArtifacts by chatViewModel.allArtifacts.collectAsStateWithLifecycle()
    val artifactsList = remember(liveArtifacts) {
        liveArtifacts.map { item ->
            val preview = (item.code ?: "").take(200)
            ArtifactGridItem(
                id = item.id,
                title = item.title,
                type = item.type,
                language = item.language,
                codePreview = preview,
                fullCode = item.code,
                date = "Recent"
            )
        }
    }

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
            // Top App Bar: Hamburger Icon on left (Image 1 & Image 5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Serif Large "Artifacts" Header (Image 1 & Image 5)
            Text(
                text = "Artifacts",
                style = HermesTypography.displayLarge.copy(
                    fontSize = 32.sp,
                    color = TextPrimaryWarm,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (artifactsList.isEmpty()) {
                // Clean Claude Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        ArtifactShapesIcon(size = 48.dp, tint = TextMuted)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No artifacts yet",
                            style = HermesTypography.titleMedium.copy(
                                fontSize = 18.sp,
                                color = TextPrimaryWarm,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generated code blocks, documents, and designs will appear here.",
                            style = HermesTypography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                color = TextSubtle,
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp
                            )
                        )
                    }
                }
            } else {
                // 2-Column Grid Cards (Image 1 & Image 5)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(artifactsList, key = { it.id }) { item ->
                        ArtifactCardGridTile(
                            item = item,
                            onClick = { onOpenArtifact(item.title, item.type, item.fullCode, item.language) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtifactCardGridTile(
    item: ArtifactGridItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Outer Card Container with Code Preview Box inside (Image 1 & Image 5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1D1B))
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(10.dp)
        ) {
            // Inner Miniature Code Preview Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161514))
                    .border(1.dp, Color(0xFF262523), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = item.codePreview,
                    style = HermesTypography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = TextPrimaryWarm
                    ),
                    overflow = TextOverflow.Clip
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filename Title
        Text(
            text = item.title,
            style = HermesTypography.titleMedium.copy(
                fontSize = 15.sp,
                color = TextPrimaryWarm,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Date Subtitle
        Text(
            text = item.date,
            style = HermesTypography.bodyMedium.copy(
                fontSize = 12.5.sp,
                color = TextSubtle
            )
        )
    }
}
