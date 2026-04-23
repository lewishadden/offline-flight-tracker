package com.lewishadden.flighttracker.notify

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lewishadden.flighttracker.data.calendar.CalendarRepository
import com.lewishadden.flighttracker.data.db.FlightDao
import com.lewishadden.flighttracker.data.prefs.UserPreferences
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.isAirborneNow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for everything that should happen when a user subscribes
 * to / unsubscribes from a flight: polling worker, boarding reminders, and the
 * calendar event.
 */
@Singleton
class FlightSubscriptionScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: FlightDao,
    private val repo: FlightRepository,
    private val boardingReminders: BoardingReminderScheduler,
    private val calendar: CalendarRepository,
    private val ongoing: OngoingFlightNotification,
    private val prefs: UserPreferences,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    suspend fun subscribe(faFlightId: String) {
        repo.setSubscribed(faFlightId, true)
        scheduleNext(Duration.ofMinutes(FlightPollWorker.TIGHT_INTERVAL_MIN))
        // Best-effort sync of side effects — none of these should block the toggle.
        repo.getFlightSnapshot(faFlightId)?.let { snap ->
            boardingReminders.schedule(snap)
            calendar.upsertEvent(snap)
            // If the flight is already in the air when we subscribe, show the
            // ongoing notification immediately rather than waiting up to 5 min
            // for the first poll tick to fire it.
            val settings = prefs.settings.first()
            if (settings.liveOngoingNotificationEnabled && snap.isAirborneNow()) {
                ongoing.show(snap)
            }
        }
    }

    suspend fun unsubscribe(faFlightId: String) {
        repo.setSubscribed(faFlightId, false)
        boardingReminders.cancel(faFlightId)
        calendar.deleteEvent(faFlightId)
        ongoing.dismiss(faFlightId)
        if (dao.countSubscribed() == 0) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }

    /** Called by the worker itself after each run, or by [subscribe] on initial registration. */
    fun scheduleNext(delay: Duration) {
        val request = OneTimeWorkRequestBuilder<FlightPollWorker>()
            .setInitialDelay(delay)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.ofMinutes(5),
            )
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "flight_poll_worker"
    }
}
