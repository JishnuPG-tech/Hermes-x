package com.example.hermes.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HermesDarkColorScheme = darkColorScheme(
    primary = BrandTerracotta,
    onPrimary = PureWhite,
    primaryContainer = BrandCoral,
    onPrimaryContainer = PureWhite,
    background = CanvasNearBlack,
    onBackground = TextPrimaryWarm,
    surface = SurfaceDarkElevated,
    onSurface = TextPrimaryWarm,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextMuted,
    surfaceContainer = SurfacePill,
    outline = HairlineDivider,
    error = DestructiveRed,
    onError = PureWhite
)

@Composable
fun HermesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HermesDarkColorScheme,
        typography = HermesTypography,
        content = content
    )
}

