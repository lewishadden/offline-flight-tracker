package com.lewishadden.flighttracker.notify

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lewishadden.flighttracker.data.db.FlightDao
import com.lewishadden.flighttracker.data.repository.FlightRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the self-rescheduling [FlightPollWorker] chain.
 *
 * WorkManager's periodic minimum is 15 min, so we use a single one-time work unit
 * that re-enqueues itself after each run with an adaptive delay (15 min normally,
 * 5 min when any subscribed flight is in its boarding window).
 */
@Singleton
class FlightSubscriptionScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: FlightDao,
    private val repo: FlightRepository,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    suspend fun subscribe(faFlightId: String) {
        repo.setSubscribed(faFlightId, true)
        scheduleNext(Duration.ofMinutes(FlightPollWorker.TIGHT_INTERVAL_MIN))
    }

    suspend fun unsubscribe(faFlightId: String) {
        repo.setSubscribed(faFlightId, false)
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
        // REPLACE so calling subscribe() resets the clock to the tighter interval
        // instead of waiting out the previously-scheduled 15-min tick.
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "flight_poll_worker"
    }
}
