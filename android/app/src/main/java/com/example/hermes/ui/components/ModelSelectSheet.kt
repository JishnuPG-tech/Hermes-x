package com.example.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.data.HermesDataRepository
import com.example.hermes.theme.*

data class ModelOption(
    val name: String,
    val description: String,
    val badge: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectSheet(
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val serverModels by HermesDataRepository.instance.availableModels.collectAsState()
    val models = if (serverModels.isNotEmpty()) {
        serverModels.map { m ->
            ModelOption(
                name = m.display_name ?: m.name,
                description = m.description?.text ?: "Hermes AI model",
                badge = if (m.id.contains("pro", true) || m.id.contains("max", true) || m.id.contains("sonnet", true)) "Pro" else null
            )
        }
    } else {
        listOf(
            ModelOption("Sonnet 3.7", "Most efficient for everyday tasks", "Pro"),
            ModelOption("Haiku 3.5", "Fastest for quick answers"),
            ModelOption("Hermes Smart", "Balanced intelligence and deep reasoning"),
            ModelOption("Hermes Coding Pro", "Specialized for advanced code and tasks", "Pro"),
            ModelOption("Hermes Turbo", "Ultra-low latency inference")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141413),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF383632))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Header (12-01-06: ✕ on left, bold Sans "Select model" in center)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Select model",
                    style = HermesTypography.titleLarge.copy(
                        fontSize = 18.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryWarm
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Single Continuous Rounded Card (12-01-06)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1F1E1C))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                models.forEachIndexed { index, model ->
                    val isSelected = model.name.startsWith(selectedModel) || selectedModel.startsWith(model.name)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onModelSelected(model.name)
                                onDismiss()
                            }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = model.name,
                                    style = HermesTypography.titleLarge.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) AccentBlue else TextPrimaryWarm
                                    )
                                )

                                if (model.badge != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF1D2C3F))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = model.badge,
                                            style = HermesTypography.labelSmall.copy(
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF75B0F8),
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = model.description,
                                style = HermesTypography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    color = if (isSelected) AccentBlue.copy(alpha = 0.85f) else Color(0xFF8E8B82)
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

                    if (index < models.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(BorderSubtle)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
