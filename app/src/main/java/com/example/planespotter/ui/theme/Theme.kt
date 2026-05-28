package com.example.planespotter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SkyBackground  = Color(0xFF06090F)
val SkyBlue        = Color(0xFF5BB3FF)
val SkyPurple      = Color(0xFF9B7DFF)
val SkyRed         = Color(0xFFFF5B7A)
val SkyPink        = Color(0xFFC064C8)
val SkyAccent      = Color(0xFF1A2030)
val SkySurface     = Color(0xFF0D1520)
val SkyDivider     = Color(0x22FFFFFF)

data class SkyPalette(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val blue: Color,
    val purple: Color,
    val red: Color,
    val divider: Color,
    val primaryText: Color,
    val secondaryText: Color
)

val DarkSkyPalette = SkyPalette(
    background = SkyBackground,
    surface = SkySurface,
    accent = SkyAccent,
    blue = SkyBlue,
    purple = SkyPurple,
    red = SkyRed,
    divider = SkyDivider,
    primaryText = Color.White,
    secondaryText = Color.White.copy(alpha = 0.55f)
)

val LightSkyPalette = SkyPalette(
    background = Color(0xFFF4F7FB),
    surface = Color(0xFFFFFFFF),
    accent = Color(0xFFE8EEF6),
    blue = Color(0xFF1769AA),
    purple = Color(0xFF6E56CF),
    red = Color(0xFFD9304F),
    divider = Color(0x1A0C1726),
    primaryText = Color(0xFF0C1726),
    secondaryText = Color(0x990C1726)
)

val LocalSkyPalette = compositionLocalOf { DarkSkyPalette }

private val DarkColors = darkColorScheme(
    primary = SkyBlue,
    secondary = SkyPurple,
    tertiary = SkyRed,
    background = SkyBackground,
    surface = SkySurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val SkyTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.W700, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.W600, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 15.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.W400, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.W400, fontSize = 13.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.W600, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.W500, fontSize = 10.sp, letterSpacing = 0.4.sp),
)

@Composable
fun PlaneSpotterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = SkyTypography,
        content = content
    )
}
