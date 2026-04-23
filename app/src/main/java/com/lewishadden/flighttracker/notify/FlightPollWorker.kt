package com.lewishadden.flighttracker.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lewishadden.flighttracker.FlightTrackerApp
import com.lewishadden.flighttracker.MainActivity
import com.lewishadden.flighttracker.R
import com.lewishadden.flighttracker.data.calendar.CalendarRepository
import com.lewishadden.flighttracker.data.prefs.UserPreferences
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.isAirborneNow
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.util.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant

@HiltWorker
class FlightPollWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: FlightRepository,
    private val notifier: FlightUpdateNotifier,
    private val ongoing: OngoingFlightNotification,
    private val scheduler: FlightSubscriptionScheduler,
    private val boardingReminders: BoardingReminderScheduler,
    private val calendar: CalendarRepository,
    private val prefs: UserPreferences,
    private val networkMonitor: NetworkMonitor,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = prefs.settings.first()
        val subscribed = runCatching { repo.getSubscribedFlights() }.getOrNull().orEmpty()
        if (subscribed.isEmpty()) return Result.success()

        // Wi-Fi-only: defer this tick if we're on a metered connection.
        val net = networkMonitor.snapshot()
        if (settings.wifiOnlyRefresh && !net.unmetered) {
            scheduler.scheduleNext(Duration.ofMinutes(DEFAULT_INTERVAL_MIN))
            return Result.success()
        }

        // API quota safety — pause polling near plan cap if the user opted in.
        if (settings.pauseOnQuotaCap) {
            val usage = runCatching { repo.getAccountUsage() }.getOrNull()
            val cap = usage?.planCap
            val total = usage?.totalCost
            if (cap != null && cap > 0.0 && total != null && total / cap >= QUOTA_PAUSE_THRESHOLD) {
                postQuotaPausedNotification(total, cap)
                // Try again after an hour — if the user disables the setting
                // or the billing period rolls over, polling resumes.
                scheduler.scheduleNext(Duration.ofHours(1))
                return Result.success()
            }
        }

        var earliestNextRunMinutes = DEFAULT_INTERVAL_MIN

        for (flight in subscribed) {
            runCatching {
                val refresh = repo.refreshFlightWithDiff(flight.faFlightId)
                val changes = FlightChangeDetector.diff(refresh.previous, refresh.current)
                notifier.notifyChanges(refresh.current.faFlightId, changes, settings)

                // Live ongoing notification — show/refresh while airborne, dismiss otherwise.
                if (settings.liveOngoingNotificationEnabled && refresh.current.isAirborneNow()) {
                    ongoing.show(refresh.current)
                } else {
                    ongoing.dismiss(refresh.current.faFlightId)
                }

                if (isTerminal(refresh.current)) {
                    repo.setSubscribed(refresh.current.faFlightId, false)
                    ongoing.dismiss(refresh.current.faFlightId)
                    boardingReminders.cancel(refresh.current.faFlightId)
                    if (settings.calendarIntegrationEnabled) {
                        // Keep the historical event in the calendar — just stop touching it.
                    }
                } else {
                    val interval = intervalForFlight(refresh.current)
                    if (interval < earliestNextRunMinutes) earliestNextRunMinutes = interval
                    // Reschedule reminders if scheduledOut shifted; idempotent.
                    if (settings.boardingRemindersEnabled) {
                        boardingReminders.schedule(refresh.current)
                    }
                    // Keep the calendar event aligned with the latest gate/time.
                    if (settings.calendarIntegrationEnabled) {
                        calendar.upsertEvent(refresh.current)
                    }
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

    private fun postQuotaPausedNotification(total: Double, cap: Double) {
        val pending = PendingIntent.getActivity(
            applicationContext,
            QUOTA_NOTIFICATION_ID,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val pct = ((total / cap) * 100).toInt()
        val notif = NotificationCompat.Builder(applicationContext, FlightTrackerApp.UPDATES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Flight updates paused")
            .setContentText("AeroAPI usage at $pct% of plan cap. Resume in Settings.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Flight subscriptions are paused to keep this billing period free. " +
                        "Open Settings → API quota to resume polling."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(QUOTA_NOTIFICATION_ID, notif)
    }

    private fun intervalForFlight(flight: Flight): Long {
        val reference = flight.estimatedOut ?: flight.scheduledOut ?: return DEFAULT_INTERVAL_MIN
        val minutesUntilDeparture = Duration.between(Instant.now(), reference).toMinutes()
        return if (minutesUntilDeparture in -60L..180L) TIGHT_INTERVAL_MIN else DEFAULT_INTERVAL_MIN
    }

    private fun isTerminal(flight: Flight): Boolean {
        if (flight.cancelled) return true
        if (flight.actualIn != null) return true
        return false
    }

    companion object {
        const val DEFAULT_INTERVAL_MIN = 15L
        const val TIGHT_INTERVAL_MIN = 5L
        private const val QUOTA_PAUSE_THRESHOLD = 0.90
        private const val QUOTA_NOTIFICATION_ID = 99001
    }
}
