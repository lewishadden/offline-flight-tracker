package com.lewishadden.flighttracker.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette: midnight-aviation — deep indigo primary, sky-cyan secondary,
// amber accent for delays/alerts, paired with an almost-black surface.
object Brand {
    val IndigoDeep = Color(0xFF0B1328)
    val Indigo = Color(0xFF1B2447)
    val IndigoHi = Color(0xFF2C3A6B)
    val Sky = Color(0xFF55B9F7)
    val SkyDeep = Color(0xFF1E88E5)
    val Cyan = Color(0xFF4DD0E1)
    val Amber = Color(0xFFFFB86B)
    val Amber2 = Color(0xFFFF9F45)
    val Rose = Color(0xFFFF6B7A)
    val Mint = Color(0xFF4ADE80)
    val Violet = Color(0xFF9C88FF)

    val Surface = Color(0xFF0E1530)
    val SurfaceHi = Color(0xFF151D3A)
    val SurfaceLo = Color(0xFF090E22)
    val OnSurface = Color(0xFFE6ECFF)
    val OnSurfaceDim = Color(0xFFA7B3DA)
    val Outline = Color(0xFF2A3358)

    // Light-mode counterparts. Designed as a coherent companion palette — the
    // same sky-blue and amber accents float on a soft off-white surface so the
    // brand identity carries across modes.
    val LightBackground = Color(0xFFF5F8FF)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceHi = Color(0xFFEEF2FB)
    val LightSurfaceLo = Color(0xFFE3EAF7)
    val LightOnSurface = Color(0xFF0B1328)
    val LightOnSurfaceDim = Color(0xFF4A5478)
    val LightOutline = Color(0xFFC4CFE5)
}
