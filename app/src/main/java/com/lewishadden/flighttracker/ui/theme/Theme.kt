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

@Suppress("unused")
private val BrandLightColors = lightColorScheme(
    primary = Brand.SkyDeep,
    secondary = Color(0xFF0277BD),
    tertiary = Brand.Amber2,
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
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Fixed brand theme — ignores system dynamic color to preserve identity.
    val colors = BrandDarkColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Brand.IndigoDeep.toArgb()
                window.navigationBarColor = Brand.IndigoDeep.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = BrandTypography,
        content = content,
    )
}
