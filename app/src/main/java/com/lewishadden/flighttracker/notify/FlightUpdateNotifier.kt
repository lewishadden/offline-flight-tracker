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
import com.lewishadden.flighttracker.FlightTrackerApp
import com.lewishadden.flighttracker.MainActivity
import com.lewishadden.flighttracker.R
import com.lewishadden.flighttracker.data.prefs.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightUpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Posts notifications for [changes] respecting the user's per-event prefs
     * in [settings]. Filters out muted event types silently.
     */
    fun notifyChanges(faFlightId: String, changes: List<FlightChange>, settings: UserSettings) {
        if (changes.isEmpty()) return
        if (!hasPostPermission()) return

        val filtered = changes.filter { it.isAllowedBy(settings) }
        if (filtered.isEmpty()) return

        val nm = NotificationManagerCompat.from(context)
        val pending = PendingIntent.getActivity(
            context,
            faFlightId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        filtered.forEach { change ->
            val notif = NotificationCompat.Builder(context, FlightTrackerApp.UPDATES_CHANNEL_ID)
                // Silhouette drawable — Android requires the small icon to be
                // single-colour with alpha. Using the launcher mipmap renders as
                // a white square in the status bar.
                .setSmallIcon(R.drawable.ic_flight)
                .setContentTitle(change.title)
                .setContentText(change.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(change.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            nm.notify(notificationId(faFlightId, change.key), notif)
        }
    }

    private fun FlightChange.isAllowedBy(s: UserSettings): Boolean = when (key) {
        "gateOrigin", "gateDestination" -> s.notifyGateChanges
        "terminalOrigin", "terminalDestination" -> s.notifyTerminalChanges
        "baggageClaim" -> s.notifyBaggageClaim
        "departureDelay", "arrivalDelay" -> s.notifyDelays
        "estimatedOut", "estimatedOff", "estimatedOn", "estimatedIn" -> s.notifyTimeChanges
        "status", "cancelled", "diverted" -> s.notifyStatusChanges
        else -> true
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Unique per (flight, field) so successive changes to the same field replace rather than stack. */
    private fun notificationId(faFlightId: String, key: String): Int =
        (faFlightId + "#" + key).hashCode()
}
