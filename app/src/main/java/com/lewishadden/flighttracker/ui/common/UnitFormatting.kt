package com.lewishadden.flighttracker.ui.common

import com.lewishadden.flighttracker.data.prefs.UnitSystem

/**
 * User-unit-aware formatters for the values returned by AeroAPI in nautical /
 * imperial units. Centralised so flipping the units toggle in Settings instantly
 * changes every screen that uses these helpers.
 */

// nautical miles → statute miles (1.15078) or kilometres (1.852)
fun formatDistance(nauticalMiles: Int?, units: UnitSystem): String {
    if (nauticalMiles == null) return "—"
    return when (units) {
        UnitSystem.IMPERIAL -> "%,d mi".format((nauticalMiles * 1.15078).toInt())
        UnitSystem.METRIC -> "%,d km".format((nauticalMiles * 1.852).toInt())
    }
}

// hundreds-of-feet (e.g. 350 = FL350 = 35 000 ft) → ft or m
fun formatAltitude(altitudeFt100: Int?, units: UnitSystem): String {
    if (altitudeFt100 == null) return "—"
    val ft = altitudeFt100 * 100
    return when (units) {
        UnitSystem.IMPERIAL -> "%,d ft".format(ft)
        UnitSystem.METRIC -> "%,d m".format((ft * 0.3048).toInt())
    }
}

// knots → mph or kph
fun formatAirspeed(knots: Int?, units: UnitSystem): String {
    if (knots == null) return "—"
    return when (units) {
        UnitSystem.IMPERIAL -> "%,d mph".format((knots * 1.15078).toInt())
        UnitSystem.METRIC -> "%,d kph".format((knots * 1.852).toInt())
    }
}
