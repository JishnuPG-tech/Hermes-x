package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        // Center: Centered Serif Hermes Wordmark (No logo icon)
        Text(
            text = "Hermes",
            style = HermesTypography.displayLarge.copy(
                fontSize = 44.sp,
                color = TextPrimaryWarm,
                fontFamily = AnthropicSerif,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            ),
            modifier = Modifier.align(Alignment.Center)
        )

        // Bottom: APEX Logo Text in bold tracked subtle grey
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "APEX",
                style = HermesTypography.labelMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSubtle,
                    letterSpacing = 4.sp,
                    fontFamily = AnthropicSans
                )
            )
        }
    }
}
