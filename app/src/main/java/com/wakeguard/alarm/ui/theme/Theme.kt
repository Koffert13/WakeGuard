package com.wakeguard.alarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Warm nocturnal nightstand ramp (derived from unslop warm dark tokens)
val WakeBg = Color(0xFF151412)
val WakeSurface = Color(0xFF1D1B18)
val WakeSurfaceHigh = Color(0xFF272420)
val WakeBorder = Color(0x14EFE9DE) // 8% warm off-white hairline

val WakeTextPrimary = Color(0xFFEFE9DE)
val WakeTextSecondary = Color(0xFFAAA398)
val WakeTextMuted = Color(0xFF8A847A)

// Single physical-clock accent: ember orange
val WakeAccent = Color(0xFFE8733A)
val WakeAccentDim = Color(0xFF2E1E16)
val WakeOnAccent = Color(0xFF151412)

// Toned semantic colors that sit inside the warm ramp
val WakeEmerald = Color(0xFF4CC38A)
val WakeEmeraldBright = Color(0xFF6DD4A3)
val WakeEmeraldDim = Color(0xFF192921)
val WakeAmber = Color(0xFFD99B38)
val WakeCoral = Color(0xFFE55358)
val WakeCoralDim = Color(0xFF2F1A1B)

private val DarkColorScheme = darkColorScheme(
    primary = WakeAccent,
    onPrimary = WakeOnAccent,
    primaryContainer = WakeAccentDim,
    onPrimaryContainer = WakeTextPrimary,
    secondary = WakeAmber,
    onSecondary = WakeOnAccent,
    tertiary = WakeCoral,
    onTertiary = WakeTextPrimary,
    background = WakeBg,
    onBackground = WakeTextPrimary,
    surface = WakeSurface,
    onSurface = WakeTextPrimary,
    surfaceVariant = WakeSurfaceHigh,
    onSurfaceVariant = WakeTextSecondary,
    outline = WakeBorder,
    error = WakeCoral,
    onError = WakeTextPrimary
)

private val WakeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-1.2).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Medium
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
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
