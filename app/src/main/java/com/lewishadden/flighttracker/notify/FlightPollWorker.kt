package com.lewishadden.flighttracker.notify

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.model.Flight
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.Instant

@HiltWorker
class FlightPollWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: FlightRepository,
    private val notifier: FlightUpdateNotifier,
    private val scheduler: FlightSubscriptionScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val subscribed = runCatching { repo.getSubscribedFlights() }.getOrNull().orEmpty()
        if (subscribed.isEmpty()) return Result.success()

        var earliestNextRunMinutes = DEFAULT_INTERVAL_MIN

        for (flight in subscribed) {
            runCatching {
                val refresh = repo.refreshFlightWithDiff(flight.faFlightId)
                val changes = FlightChangeDetector.diff(refresh.previous, refresh.current)
                notifier.notifyChanges(refresh.current.faFlightId, changes)

                if (isTerminal(refresh.current)) {
                    repo.setSubscribed(refresh.current.faFlightId, false)
                } else {
                    val interval = intervalForFlight(refresh.current)
                    if (interval < earliestNextRunMinutes) earliestNextRunMinutes = interval
                }
            }.onFailure {
                // Transient failures (network, rate limits) — keep the subscription and try again next tick.
            }
        }

        val stillSubscribed = runCatching { repo.getSubscribedFlights() }.getOrNull().orEmpty()
        if (stillSubscribed.isNotEmpty()) {
            scheduler.scheduleNext(Duration.ofMinutes(earliestNextRunMinutes))
        }
        return Result.success()
    }

    private fun intervalForFlight(flight: Flight): Long {
        val reference = flight.estimatedOut ?: flight.scheduledOut ?: return DEFAULT_INTERVAL_MIN
        val minutesUntilDeparture = Duration.between(Instant.now(), reference).toMinutes()
        // Tight polling window: from 3 hours before scheduled departure through 1 hour after.
        return if (minutesUntilDeparture in -60L..180L) TIGHT_INTERVAL_MIN else DEFAULT_INTERVAL_MIN
    }

    private fun isTerminal(flight: Flight): Boolean {
        if (flight.cancelled) return true
        // Flight has arrived at the gate — no further gate/delay updates expected.
        if (flight.actualIn != null) return true
        return false
    }

    companion object {
        const val DEFAULT_INTERVAL_MIN = 15L
        const val TIGHT_INTERVAL_MIN = 5L
    }
}
