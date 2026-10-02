package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MarkUnreadChatAlt
import androidx.compose.material.icons.outlined.Unsubscribe
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeToggle

@Composable
fun NotificationsScreen(
    onBack: () -> Unit
) {
    var chatResponsesEnabled by remember { mutableStateOf(true) }
    var dispatchMessagesEnabled by remember { mutableStateOf(false) }
    var productUpdatesEnabled by remember { mutableStateOf(true) }

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
                text = "Notifications",
                style = HermesTypography.headlineMedium.copy(fontSize = 22.sp, color = TextPrimaryWarm),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1F1E1D))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                // Chat responses
                NotificationRow(
                    title = "Chat responses",
                    icon = Icons.Outlined.ChatBubbleOutline,
                    checked = chatResponsesEnabled,
                    onCheckedChange = { chatResponsesEnabled = it }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                // Dispatch messages
                NotificationRow(
                    title = "Dispatch messages",
                    description = "Get notified when Hermes messages you in Dispatch",
                    icon = Icons.Outlined.MarkUnreadChatAlt,
                    checked = dispatchMessagesEnabled,
                    onCheckedChange = { dispatchMessagesEnabled = it }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                // Product updates
                NotificationRow(
                    title = "Product updates",
                    description = "Get notified about new features, tips, and occasional promotions",
                    icon = Icons.Outlined.Unsubscribe,
                    checked = productUpdatesEnabled,
                    onCheckedChange = { productUpdatesEnabled = it }
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(
    title: String,
    description: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimaryWarm,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = HermesTypography.titleLarge.copy(fontSize = 17.sp, color = TextPrimaryWarm)
            )
            if (!description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = HermesTypography.bodyMedium.copy(fontSize = 14.sp, color = TextMuted, lineHeight = 20.sp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        ClaudeToggle(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
