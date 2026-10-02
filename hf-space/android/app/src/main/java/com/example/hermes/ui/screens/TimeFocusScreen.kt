package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
fun TimeFocusScreen(
    onBack: () -> Unit
) {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }

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
                text = "Time & focus",
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
            Text(
                text = "Time and focus",
                style = HermesTypography.titleLarge.copy(fontSize = 19.sp, color = TextPrimaryWarm)
            )

            // Break reminders section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Break reminders",
                    style = HermesTypography.titleMedium.copy(fontSize = 17.sp, color = TextPrimaryWarm)
                )
                Text(
                    text = "Get a nudge to take a break from Hermes. You can snooze or adjust anytime.",
                    style = HermesTypography.bodyMedium.copy(fontSize = 14.5.sp, color = TextMuted, lineHeight = 20.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownSelector(label = "-")
                    DropdownSelector(label = "-")
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

            // Quiet hours section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quiet hours",
                    style = HermesTypography.titleMedium.copy(fontSize = 17.sp, color = TextPrimaryWarm)
                )
                Text(
                    text = "Set time limits for Hermes. You can dismiss or adjust anytime.",
                    style = HermesTypography.bodyMedium.copy(fontSize = 14.5.sp, color = TextMuted, lineHeight = 20.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    days.forEachIndexed { index, day ->
                        val isSelected = selectedDays.contains(index)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PureWhite else Color(0xFF262522))
                                .clickable {
                                    selectedDays = if (isSelected) selectedDays - index else selectedDays + index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = HermesTypography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    color = if (isSelected) PureBlack else TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownSelector(label: String) {
    Row(
        modifier = Modifier
            .width(110.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1F1E1D))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable { }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = HermesTypography.bodyLarge.copy(color = TextMuted, fontSize = 16.sp))
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}
