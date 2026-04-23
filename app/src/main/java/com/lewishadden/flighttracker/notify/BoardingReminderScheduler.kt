package com.lewishadden.flighttracker.notify

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lewishadden.flighttracker.domain.model.Flight
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules pre-departure boarding reminder notifications. Uses
 * one-time WorkManager requests anchored to scheduled-out − {60, 30, 15} min.
 *
 * Re-scheduling is idempotent thanks to per-(flight, offset) unique work names.
 * If [Flight.estimatedOut] shifts later we replace; if it shifts earlier the
 * already-scheduled reminder may fire slightly early — acceptable trade-off
 * vs. firing late.
 */
@Singleton
class BoardingReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Reminder offsets, in minutes before boarding. */
    private val offsets = longArrayOf(60, 30, 15)

    fun schedule(flight: Flight) {
        val target = flight.estimatedOut ?: flight.scheduledOut ?: return
        val now = Instant.now()
        offsets.forEach { offsetMin ->
            val fireAt = target.minusSeconds(offsetMin * 60)
            if (fireAt.isBefore(now)) return@forEach   // already past — skip
            val delayMs = Duration.between(now, fireAt).toMillis()
            val data: Data = workDataOf(
                BoardingReminderWorker.KEY_FA_FLIGHT_ID to flight.faFlightId,
                BoardingReminderWorker.KEY_OFFSET_MIN to offsetMin,
            )
            val req = OneTimeWorkRequestBuilder<BoardingReminderWorker>()
                .setInitialDelay(Duration.ofMillis(delayMs))
                .setInputData(data)
                .build()
            workManager.enqueueUniqueWork(
                workName(flight.faFlightId, offsetMin),
                ExistingWorkPolicy.REPLACE,
                req,
            )
        }
    }

    fun cancel(faFlightId: String) {
        offsets.forEach { offsetMin ->
            workManager.cancelUniqueWork(workName(faFlightId, offsetMin))
        }
    }

    private fun workName(faFlightId: String, offsetMin: Long) =
        "boarding_reminder#$faFlightId#$offsetMin"
}
