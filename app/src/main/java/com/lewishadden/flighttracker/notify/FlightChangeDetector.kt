package com.lewishadden.flighttracker.notify

import com.lewishadden.flighttracker.domain.model.Flight
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FlightChange(
    val key: String,
    val title: String,
    val message: String,
)

object FlightChangeDetector {

    /**
     * Produces a list of user-facing changes between [previous] and [current].
     * Empty if nothing material changed. If [previous] is null (first observation),
     * no changes are emitted — we only announce actual transitions.
     */
    fun diff(previous: Flight?, current: Flight): List<FlightChange> {
        if (previous == null) return emptyList()
        val changes = mutableListOf<FlightChange>()

        change(previous.gateOrigin, current.gateOrigin)?.let { (from, to) ->
            changes += FlightChange(
                key = "gateOrigin",
                title = "${current.ident}: departure gate",
                message = formatAssignOrChange("Gate", from, to),
            )
        }
        change(previous.terminalOrigin, current.terminalOrigin)?.let { (from, to) ->
            changes += FlightChange(
                key = "terminalOrigin",
                title = "${current.ident}: departure terminal",
                message = formatAssignOrChange("Terminal", from, to),
            )
        }
        change(previous.gateDestination, current.gateDestination)?.let { (from, to) ->
            changes += FlightChange(
                key = "gateDestination",
                title = "${current.ident}: arrival gate",
                message = formatAssignOrChange("Arrival gate", from, to),
            )
        }
        change(previous.terminalDestination, current.terminalDestination)?.let { (from, to) ->
            changes += FlightChange(
                key = "terminalDestination",
                title = "${current.ident}: arrival terminal",
                message = formatAssignOrChange("Arrival terminal", from, to),
            )
        }
        change(previous.baggageClaim, current.baggageClaim)?.let { (from, to) ->
            changes += FlightChange(
                key = "baggageClaim",
                title = "${current.ident}: baggage claim",
                message = formatAssignOrChange("Baggage claim", from, to),
            )
        }

        timeChange(previous.estimatedOut, current.estimatedOut)?.let { (_, to) ->
            changes += FlightChange(
                key = "estimatedOut",
                title = "${current.ident}: boarding/off-gate update",
                message = "Estimated off-gate now ${formatClock(to, current.origin.timezone)}.",
            )
        }
        timeChange(previous.estimatedOff, current.estimatedOff)?.let { (_, to) ->
            changes += FlightChange(
                key = "estimatedOff",
                title = "${current.ident}: takeoff update",
                message = "Estimated takeoff now ${formatClock(to, current.origin.timezone)}.",
            )
        }
        timeChange(previous.estimatedOn, current.estimatedOn)?.let { (_, to) ->
            changes += FlightChange(
                key = "estimatedOn",
                title = "${current.ident}: arrival update",
                message = "Estimated touchdown now ${formatClock(to, current.destination.timezone)}.",
            )
        }
        timeChange(previous.estimatedIn, current.estimatedIn)?.let { (_, to) ->
            changes += FlightChange(
                key = "estimatedIn",
                title = "${current.ident}: arrival update",
                message = "Estimated in-gate now ${formatClock(to, current.destination.timezone)}.",
            )
        }

        delayChange(previous.departureDelayMin, current.departureDelayMin)?.let { to ->
            changes += FlightChange(
                key = "departureDelay",
                title = "${current.ident}: departure delay",
                message = if (to > 0) "Departure delayed by ${formatMinutes(to)}."
                          else "Departure delay cleared.",
            )
        }
        delayChange(previous.arrivalDelayMin, current.arrivalDelayMin)?.let { to ->
            changes += FlightChange(
                key = "arrivalDelay",
                title = "${current.ident}: arrival delay",
                message = if (to > 0) "Arrival delayed by ${formatMinutes(to)}."
                          else "Arrival delay cleared.",
            )
        }

        if (!previous.cancelled && current.cancelled) {
            changes += FlightChange(
                key = "cancelled",
                title = "${current.ident}: CANCELLED",
                message = "Flight has been cancelled.",
            )
        }
        if (!previous.diverted && current.diverted) {
            changes += FlightChange(
                key = "diverted",
                title = "${current.ident}: DIVERTED",
                message = "Flight has been diverted.",
            )
        }

        change(previous.status, current.status)?.let { (_, to) ->
            if (to != null) {
                changes += FlightChange(
                    key = "status",
                    title = "${current.ident}: status",
                    message = to,
                )
            }
        }

        return changes
    }

    private fun change(a: String?, b: String?): Pair<String?, String?>? =
        if (a.orEmpty() != b.orEmpty()) a to b else null

    /** Only emit a time change if it moved by at least 1 minute. */
    private fun timeChange(a: Instant?, b: Instant?): Pair<Instant?, Instant>? {
        if (b == null) return null
        if (a == null) return null to b
        val deltaMs = kotlin.math.abs(a.toEpochMilli() - b.toEpochMilli())
        return if (deltaMs >= 60_000L) a to b else null
    }

    /** Ignore noise under 1 min. */
    private fun delayChange(a: Long?, b: Long?): Long? {
        val aMin = a ?: 0L
        val bMin = b ?: 0L
        return if (kotlin.math.abs(aMin - bMin) >= 1L) bMin else null
    }

    private fun formatAssignOrChange(label: String, from: String?, to: String?): String = when {
        from.isNullOrBlank() && !to.isNullOrBlank() -> "$label $to assigned."
        !from.isNullOrBlank() && to.isNullOrBlank() -> "$label cleared (was $from)."
        else -> "$label changed from $from to $to."
    }

    private fun formatClock(instant: Instant, timezone: String?): String {
        val zone = runCatching { ZoneId.of(timezone) }.getOrNull() ?: ZoneId.systemDefault()
        return DateTimeFormatter.ofPattern("HH:mm 'local'").withZone(zone).format(instant)
    }

    private fun formatMinutes(total: Long): String {
        val h = total / 60L
        val m = total % 60L
        return when {
            h == 0L -> "${m}m"
            m == 0L -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}
