package com.example.hermes.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

data class IntegrationField(
    val name: String,
    val label: String,
    val type: String, // "text" or "password"
    val placeholder: String = "",
    val hint: String? = null,
    val required: Boolean = true
)

data class IntegrationSpec(
    val service: String,
    val title: String,
    val description: String,
    val fields: List<IntegrationField>,
    val rawBlock: String
)

object IntegrationCardParser {
    private val INTEGRATION_REGEX = Regex(
        """<hermesIntegration\s+([^>]+)>([\s\S]*?)</hermesIntegration>""",
        RegexOption.IGNORE_CASE
    )
    private val FIELD_REGEX = Regex(
        """<field\s+([^>]+)/>""",
        RegexOption.IGNORE_CASE
    )
    private val ATTR_REGEX = Regex("""([a-zA-Z0-9_-]+)=["']([^"']*)["']""")

    fun parse(text: String): IntegrationSpec? {
        val match = INTEGRATION_REGEX.find(text) ?: return null
        val topAttrsStr = match.groupValues[1]
        val body = match.groupValues[2]

        val topAttrs = ATTR_REGEX.findAll(topAttrsStr).associate {
            it.groupValues[1].lowercase() to it.groupValues[2]
        }

        val service = topAttrs["service"] ?: "generic"
        val title = topAttrs["title"] ?: "Connect Integration"
        val description = topAttrs["description"] ?: "Securely authenticate your service."

        val fields = mutableListOf<IntegrationField>()
        FIELD_REGEX.findAll(body).forEach { fMatch ->
            val fAttrs = ATTR_REGEX.findAll(fMatch.groupValues[1]).associate {
                it.groupValues[1].lowercase() to it.groupValues[2]
            }
            val name = fAttrs["name"] ?: "token"
            val label = fAttrs["label"] ?: name.replaceFirstChar { it.uppercase() }
            val type = fAttrs["type"] ?: "password"
            val placeholder = fAttrs["placeholder"] ?: ""
            val hint = fAttrs["hint"]
            val required = fAttrs["required"]?.toBooleanStrictOrNull() ?: true
            fields.add(IntegrationField(name, label, type, placeholder, hint, required))
        }

        if (fields.isEmpty()) {
            fields.add(
                IntegrationField(
                    name = "token",
                    label = "Personal Access Token",
                    type = "password",
                    placeholder = "ghp_...",
                    hint = "Requires repository permissions"
                )
            )
        }

        return IntegrationSpec(service, title, description, fields, match.value)
    }
}

@Composable
fun HermesIntegrationCard(
    spec: IntegrationSpec,
    onConnect: (service: String, credentials: Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldValues = remember { mutableStateMapOf<String, String>() }
    val passwordVisibility = remember { mutableStateMapOf<String, Boolean>() }
    var isSubmitting by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1D1B),
        border = BorderStroke(1.dp, Color(0xFF33312B)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Service Icon, Title, and Encryption Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2C2A26)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = spec.service,
                            tint = BrandCoral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = spec.title,
                            style = HermesTypography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 16.sp
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFF59E0B))
                            )
                            Text(
                                text = if (isConnected) "Connected & Encrypted" else "AES-256 Vault",
                                style = HermesTypography.labelSmall.copy(
                                    color = if (isConnected) Color(0xFF81C784) else Color(0xFFD4AF37),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // Description
            Text(
                text = spec.description,
                style = HermesTypography.bodySmall.copy(
                    color = Color(0xFFC4C0B6),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            )

            // Dynamic Input Fields
            spec.fields.forEach { field ->
                val currentValue = fieldValues[field.name] ?: ""
                val isPassword = field.type.equals("password", ignoreCase = true)
                val isVisible = passwordVisibility[field.name] ?: false

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = field.label + if (field.required) " *" else "",
                        style = HermesTypography.labelMedium.copy(
                            color = TextPrimaryWarm,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )

                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = {
                            fieldValues[field.name] = it
                            validationError = null
                        },
                        placeholder = {
                            Text(
                                text = field.placeholder,
                                style = HermesTypography.bodySmall.copy(color = TextSubtle, fontSize = 13.sp)
                            )
                        },
                        visualTransformation = if (isPassword && !isVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
                        trailingIcon = if (isPassword) {
                            {
                                IconButton(onClick = { passwordVisibility[field.name] = !isVisible }) {
                                    Icon(
                                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = TextSubtle,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141312),
                            unfocusedContainerColor = Color(0xFF141312),
                            focusedBorderColor = BrandCoral,
                            unfocusedBorderColor = Color(0xFF38352F),
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!field.hint.isNullOrBlank()) {
                        Text(
                            text = field.hint,
                            style = HermesTypography.labelSmall.copy(
                                color = TextSubtle,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }

            // Validation Error if any
            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    style = HermesTypography.bodySmall.copy(
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.5.sp
                    )
                )
            }

            // Save & Connect Button
            AnimatedContent(targetState = isConnected, label = "ConnectButton") { connected ->
                if (connected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B3820))
                            .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Connected",
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connected Successfully",
                            style = HermesTypography.labelMedium.copy(
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            // Validate required fields
                            val missing = spec.fields.filter { it.required && fieldValues[it.name].isNullOrBlank() }
                            if (missing.isNotEmpty()) {
                                validationError = "Please fill in: ${missing.joinToString(", ") { it.label }}"
                                return@Button
                            }

                            isSubmitting = true
                            validationError = null
                            onConnect(spec.service, fieldValues.toMap())
                            isSubmitting = false
                            isConnected = true
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandCoral,
                            contentColor = PureWhite,
                            disabledContainerColor = Color(0xFF4A3525)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save & Connect Securely",
                                style = HermesTypography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
