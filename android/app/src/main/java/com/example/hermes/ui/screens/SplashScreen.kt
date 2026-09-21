package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeStarburst

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
        // Center: Terracotta/Coral Starburst + Serif Hermes Wordmark (Matches Screenshot 1)
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ClaudeStarburst(
                size = 46.dp,
                color = BrandCoral
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Hermes",
                style = HermesTypography.displayLarge.copy(
                    fontSize = 44.sp,
                    color = TextPrimaryWarm,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.5).sp
                )
            )
        }

        // Bottom: APEX Logo Text in bold tracked subtle grey (Matches Screenshot 1)
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
                    letterSpacing = 4.sp
                )
            )
        }
    }
}
