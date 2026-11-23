package com.wakeguard.alarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val WakeBg = Color(0xFF0B1016)
val WakeSurface = Color(0xFF131B24)
val WakeSurfaceHigh = Color(0xFF1C2735)
val WakeEmerald = Color(0xFF34D399)
val WakeEmeraldBright = Color(0xFF6EE7B7)
val WakeEmeraldDim = Color(0xFF0D3B2E)
val WakeAmber = Color(0xFFFBBF24)
val WakeCoral = Color(0xFFFB7185)
val WakeCoralDim = Color(0xFF3F131E)
val WakeTextPrimary = Color(0xFFF1F5F9)
val WakeTextSecondary = Color(0xFF94A3B8)
val WakeBorder = Color(0xFF253447)

private val DarkColorScheme = darkColorScheme(
    primary = WakeEmerald,
    onPrimary = Color(0xFF042219),
    primaryContainer = WakeEmeraldDim,
    onPrimaryContainer = WakeEmeraldBright,
    secondary = WakeAmber,
    onSecondary = Color(0xFF261A00),
    tertiary = WakeCoral,
    onTertiary = Color(0xFF2B0610),
    background = WakeBg,
    onBackground = WakeTextPrimary,
    surface = WakeSurface,
    onSurface = WakeTextPrimary,
    surfaceVariant = WakeSurfaceHigh,
    onSurfaceVariant = WakeTextSecondary,
    outline = WakeBorder,
    error = WakeCoral,
    onError = Color(0xFF2B0610)
)

private val WakeTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 56.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5).sp
    ),
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
)

@Composable
fun WakeGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = WakeTypography,
        content = content
    )
}
