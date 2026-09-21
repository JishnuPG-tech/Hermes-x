package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeToggle

@Composable
fun PrivacyScreen(
    onBack: () -> Unit
) {
    var trainModelsEnabled by remember { mutableStateOf(false) }

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
                text = "Privacy",
                style = HermesTypography.headlineMedium.copy(fontSize = 22.sp, color = TextPrimaryWarm),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Data Privacy Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Data Privacy",
                    style = HermesTypography.titleLarge.copy(fontSize = 19.sp, color = TextPrimaryWarm)
                )
                Text(
                    text = "Apex believes in transparent data practices",
                    style = HermesTypography.bodyLarge.copy(fontSize = 16.5.sp, color = TextPrimaryWarm)
                )
                Row {
                    Text(
                        text = "Keeping your data safe is a priority. Learn how your information is protected when using Apex products, and visit our ",
                        style = HermesTypography.bodyMedium.copy(fontSize = 14.5.sp, color = TextMuted, lineHeight = 21.sp)
                    )
                }
                Row {
                    Text(
                        text = "Privacy Center",
                        style = HermesTypography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            color = Color(0xFF789DCE),
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier.clickable { }
                    )
                    Text(
                        text = " and ",
                        style = HermesTypography.bodyMedium.copy(fontSize = 14.5.sp, color = TextMuted)
                    )
                    Text(
                        text = "Privacy Policy",
                        style = HermesTypography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            color = Color(0xFF789DCE),
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier.clickable { }
                    )
                    Text(
                        text = " for more details.",
                        style = HermesTypography.bodyMedium.copy(fontSize = 14.5.sp, color = TextMuted)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

            // Help improve AI models Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Help improve our AI models",
                        style = HermesTypography.titleLarge.copy(fontSize = 18.sp, color = TextPrimaryWarm)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Allow the use of your chats and coding sessions to train and improve Apex AI models.",
                        style = HermesTypography.bodyMedium.copy(fontSize = 14.5.sp, color = TextMuted, lineHeight = 20.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Learn More",
                        style = HermesTypography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            color = Color(0xFF789DCE),
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier.clickable { }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                ClaudeToggle(
                    checked = trainModelsEnabled,
                    onCheckedChange = { trainModelsEnabled = it }
                )
            }
        }
    }
}
