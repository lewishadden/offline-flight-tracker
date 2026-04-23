package com.lewishadden.flighttracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lewishadden.flighttracker.data.prefs.ThemeMode

private val BrandDarkColors = darkColorScheme(
    primary = Brand.Sky,
    onPrimary = Brand.IndigoDeep,
    primaryContainer = Brand.IndigoHi,
    onPrimaryContainer = Brand.OnSurface,
    secondary = Brand.Cyan,
    onSecondary = Brand.IndigoDeep,
    secondaryContainer = Brand.Indigo,
    onSecondaryContainer = Brand.Cyan,
    tertiary = Brand.Amber,
    onTertiary = Brand.IndigoDeep,
    tertiaryContainer = Color(0xFF3A2A12),
    onTertiaryContainer = Brand.Amber,
    error = Brand.Rose,
    onError = Color.White,
    background = Brand.IndigoDeep,
    onBackground = Brand.OnSurface,
    surface = Brand.Surface,
    onSurface = Brand.OnSurface,
    surfaceVariant = Brand.SurfaceHi,
    onSurfaceVariant = Brand.OnSurfaceDim,
    surfaceContainer = Brand.SurfaceHi,
    surfaceContainerHigh = Color(0xFF1B244A),
    surfaceContainerHighest = Color(0xFF22305C),
    surfaceContainerLow = Brand.SurfaceLo,
    surfaceContainerLowest = Color(0xFF060A1A),
    outline = Brand.Outline,
    outlineVariant = Color(0xFF1F2849),
)

private val BrandLightColors = lightColorScheme(
    primary = Brand.SkyDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E9FF),
    onPrimaryContainer = Brand.IndigoDeep,
    secondary = Color(0xFF0277BD),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEBF6),
    onSecondaryContainer = Brand.IndigoDeep,
    tertiary = Brand.Amber2,
    onTertiary = Brand.IndigoDeep,
    tertiaryContainer = Color(0xFFFFE3C4),
    onTertiaryContainer = Color(0xFF3A2A12),
    error = Color(0xFFC62828),
    onError = Color.White,
    background = Brand.LightBackground,
    onBackground = Brand.LightOnSurface,
    surface = Brand.LightSurface,
    onSurface = Brand.LightOnSurface,
    surfaceVariant = Brand.LightSurfaceHi,
    onSurfaceVariant = Brand.LightOnSurfaceDim,
    surfaceContainer = Brand.LightSurfaceHi,
    surfaceContainerHigh = Brand.LightSurfaceLo,
    surfaceContainerHighest = Color(0xFFD7DFEF),
    surfaceContainerLow = Color(0xFFF0F4FB),
    surfaceContainerLowest = Color.White,
    outline = Brand.LightOutline,
    outlineVariant = Color(0xFFDEE5F2),
)

private val BrandTypography = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
)

@Composable
fun FlightTrackerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val colors = if (useDark) BrandDarkColors else BrandLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colors.background.toArgb()
                window.navigationBarColor = colors.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = BrandTypography,
        content = content,
    )
}
