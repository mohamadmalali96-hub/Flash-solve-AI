package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FlashSolveDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = CyberNavy,
    primaryContainer = CyberCardElevated,
    onPrimaryContainer = ElectricCyan,
    secondary = NeonEmerald,
    onSecondary = CyberNavy,
    secondaryContainer = Color(0xFF0A2E26),
    onSecondaryContainer = NeonEmerald,
    tertiary = StandbyAmber,
    onTertiary = CyberNavy,
    background = CyberNavy,
    onBackground = TextPrimaryDark,
    surface = CyberSlate,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberCard,
    onSurfaceVariant = TextSecondaryDark,
    error = AlertCoral,
    onError = Color.White
)

private val FlashSolveLightColorScheme = lightColorScheme(
    primary = ElectricBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = EmeraldLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF047857),
    tertiary = StandbyAmber,
    onTertiary = CyberNavy,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextSecondaryLight,
    error = AlertCoral,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Default to the signature dark HUD theme for FlashSolve AI while supporting light mode if needed
    val colorScheme = if (darkTheme) FlashSolveDarkColorScheme else FlashSolveDarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
