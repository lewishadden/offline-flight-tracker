package com.lewishadden.flighttracker.domain

import com.lewishadden.flighttracker.domain.model.Flight

/**
 * Single source of truth for "is this flight in the air right now?"
 *
 * AeroAPI's `actualOff`/`actualOut` timestamps can lag the real wheels-up
 * event by minutes, so we can't rely on them alone — a flight at 50%
 * progress may have neither set yet. Anything that hints at "in flight"
 * counts: cached takeoff times, in-flight progress percent, or the AeroAPI
 * status string.
 */
fun Flight.isAirborneNow(): Boolean {
    if (cancelled) return false
    // Arrived — definitely on the ground.
    if (actualIn != null || actualOn != null) return false

    if (actualOff != null || actualOut != null) return true
    val progress = progressPercent
    if (progress != null && progress in 1..99) return true
    val status = status?.lowercase() ?: return false
    return "en route" in status ||
        "in flight" in status ||
        "airborne" in status ||
        "enroute" in status
}
