package com.example.hermes.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.hermes.R

// Exact Anthropic Font Families Extracted Directly from Claude.apk
val AnthropicSerif = FontFamily(
    Font(R.font.anthropic_serif, FontWeight.Normal),
    Font(R.font.anthropic_serif, FontWeight.Medium),
    Font(R.font.anthropic_serif, FontWeight.SemiBold),
    Font(R.font.anthropic_serif, FontWeight.Bold),
    Font(R.font.anthropic_serif_italic, FontWeight.Normal, FontStyle.Italic)
)

val AnthropicSans = FontFamily(
    Font(R.font.anthropic_sans, FontWeight.Normal),
    Font(R.font.anthropic_sans, FontWeight.Medium),
    Font(R.font.anthropic_sans, FontWeight.SemiBold),
    Font(R.font.anthropic_sans, FontWeight.Bold),
    Font(R.font.anthropic_sans_italic, FontWeight.Normal, FontStyle.Italic)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val SerifFontFamily = AnthropicSerif
val SansFontFamily = AnthropicSans

val HermesTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SerifFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        color = PureWhite
    ),
    displayMedium = TextStyle(
        fontFamily = SerifFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = PureWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = SerifFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = PureWhite
    ),
    titleLarge = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = PureWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = PureWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = PureWhite
    ),
    labelSmall = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = PureWhite
    )
)
