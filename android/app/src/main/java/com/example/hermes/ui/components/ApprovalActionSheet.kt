package com.example.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.data.ApprovalDto
import com.example.hermes.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalActionSheet(
    approval: ApprovalDto,
    onApprove: (String) -> Unit,
    onDeny: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var showDenyReasonInput by remember { mutableStateOf(false) }
    var denyReason by remember { mutableStateOf("") }

    val isHighRisk = approval.risk.contains("HIGH", ignoreCase = true) || approval.risk.contains("CRITICAL", ignoreCase = true)
    val riskColor = if (isHighRisk) Color(0xFFE57373) else Color(0xFFFFB74D)

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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Close icon on left, title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(riskColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = riskColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Action Approval Required",
                            style = HermesTypography.titleMedium.copy(
                                color = TextPrimaryWarm,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        )
                        Text(
                            text = "Human-in-the-Loop Policy Gate",
                            style = HermesTypography.bodySmall.copy(
                                color = TextSubtle,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Risk & Tool Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(riskColor.copy(alpha = 0.2f))
                        .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${approval.risk} RISK",
                        style = HermesTypography.labelSmall.copy(
                            color = riskColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2C2B28))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Tool: ${approval.tool}",
                        style = HermesTypography.labelSmall.copy(
                            color = TextPrimaryWarm,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Command / Parameters Box
            Text(
                text = "Command / Parameters",
                style = HermesTypography.labelMedium.copy(
                    color = TextSubtle,
                    fontSize = 12.5.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1A18))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = approval.command.ifBlank { "Tool action: ${approval.tool}" },
                    style = HermesTypography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFC4C2BA),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }

            // Reason Section if present
            if (!approval.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Agent Justification",
                    style = HermesTypography.labelMedium.copy(
                        color = TextSubtle,
                        fontSize = 12.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = approval.reason,
                    style = HermesTypography.bodyMedium.copy(
                        color = TextPrimaryWarm,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    )
                )
            }

            // Optional Deny Reason input field
            if (showDenyReasonInput) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = denyReason,
                    onValueChange = { denyReason = it },
                    placeholder = { Text("Reason for denial (e.g., destructive command, security concern)", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1F1E1C),
                        unfocusedContainerColor = Color(0xFF1F1E1C),
                        focusedTextColor = TextPrimaryWarm,
                        unfocusedTextColor = TextPrimaryWarm,
                        focusedBorderColor = BrandCoral,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons (Allow Once & Deny)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Deny Button
                Button(
                    onClick = {
                        if (!showDenyReasonInput) {
                            showDenyReasonInput = true
                        } else {
                            onDeny(approval.id, denyReason.ifBlank { "Denied by owner" })
                            onDismiss()
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C2B28),
                        contentColor = Color(0xFFE57373)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = if (showDenyReasonInput) "Confirm Deny" else "Deny",
                        style = HermesTypography.titleMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE57373)
                        )
                    )
                }

                // Approve Button
                Button(
                    onClick = {
                        onApprove(approval.id)
                        onDismiss()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Approve",
                        style = HermesTypography.titleMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PureBlack
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
