package com.lewishadden.flighttracker.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lewishadden.flighttracker.FlightTrackerApp
import com.lewishadden.flighttracker.MainActivity
import com.lewishadden.flighttracker.R
import com.lewishadden.flighttracker.data.prefs.UserPreferences
import com.lewishadden.flighttracker.data.repository.FlightRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BoardingReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: FlightRepository,
    private val prefs: UserPreferences,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val faFlightId = inputData.getString(KEY_FA_FLIGHT_ID) ?: return Result.success()
        val offsetMin = inputData.getLong(KEY_OFFSET_MIN, 0L)

        val settings = prefs.settings.first()
        if (!settings.boardingRemindersEnabled) return Result.success()

        val flight = repo.getFlightSnapshot(faFlightId) ?: return Result.success()
        // Skip if cancelled or already pushed back so far the reminder is stale.
        if (flight.cancelled) return Result.success()
        if (flight.actualOut != null) return Result.success()
        if (!hasPostPermission()) return Result.success()

        val origin = flight.origin.iata ?: flight.origin.icao ?: ""
        val dest = flight.destination.iata ?: flight.destination.icao ?: ""
        val gate = flight.gateOrigin?.let { " · Gate $it" } ?: ""
        val terminal = flight.terminalOrigin?.let { " · Terminal $it" } ?: ""
        val title = "${flight.ident}: boarding in ${offsetMin}m"
        val body = "$origin → $dest$terminal$gate"

        val pending = PendingIntent.getActivity(
            applicationContext,
            (faFlightId + "#reminder").hashCode(),
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notif = NotificationCompat.Builder(applicationContext, FlightTrackerApp.UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flight)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(applicationContext)
            .notify((faFlightId + "#reminder#" + offsetMin).hashCode(), notif)

        return Result.success()
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val KEY_FA_FLIGHT_ID = "faFlightId"
        const val KEY_OFFSET_MIN = "offsetMin"
    }
}
