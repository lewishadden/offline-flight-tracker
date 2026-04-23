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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightUpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun notifyChanges(faFlightId: String, changes: List<FlightChange>) {
        if (changes.isEmpty()) return
        if (!hasPostPermission()) return

        val nm = NotificationManagerCompat.from(context)
        val pending = PendingIntent.getActivity(
            context,
            faFlightId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        changes.forEach { change ->
            val notif = NotificationCompat.Builder(context, FlightTrackerApp.UPDATES_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
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
