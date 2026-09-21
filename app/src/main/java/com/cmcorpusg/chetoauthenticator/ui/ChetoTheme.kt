package com.cmcorpusg.chetoauthenticator.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val ChetoBlue = Color(0xFF3857F4)
internal val ChetoViolet = Color(0xFF7453E8)
internal val ChetoInk = Color(0xFF171B2C)
internal val ChetoMuted = Color(0xFF667085)
internal val ChetoCanvas = Color(0xFFF6F7FB)
internal val ChetoLine = Color(0xFFE7E9F2)
internal val ChetoSuccess = Color(0xFF16A36A)

private val LightColors = lightColorScheme(
    primary = ChetoBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8ECFF),
    onPrimaryContainer = Color(0xFF16245C),
    secondary = ChetoViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0EBFF),
    background = ChetoCanvas,
    onBackground = ChetoInk,
    surface = Color.White,
    onSurface = ChetoInk,
    surfaceVariant = Color(0xFFF0F2F8),
    onSurfaceVariant = ChetoMuted,
    outline = Color(0xFFD8DCE8),
    outlineVariant = ChetoLine,
    error = Color(0xFFD92D45)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8B4FF),
    onPrimary = Color(0xFF13215D),
    primaryContainer = Color(0xFF283B91),
    secondary = Color(0xFFC6B7FF),
    background = Color(0xFF10121A),
    onBackground = Color(0xFFF4F5FA),
    surface = Color(0xFF191C27),
    onSurface = Color(0xFFF4F5FA),
    surfaceVariant = Color(0xFF242836),
    onSurfaceVariant = Color(0xFFB7BDCC),
    outline = Color(0xFF444A5C),
    outlineVariant = Color(0xFF2D3241),
    error = Color(0xFFFFB3BA)
)

private val ChetoTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 29.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
internal fun ChetoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = ChetoTypography,
        content = content
    )
}

internal object ChetoSpacing {
    val xxs = 4
    val xs = 8
    val sm = 12
    val md = 16
    val lg = 20
    val xl = 24
    val xxl = 32
}
