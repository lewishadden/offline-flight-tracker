package com.lewishadden.flighttracker.notify

import android.Manifest
import android.app.NotificationManager
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
import com.lewishadden.flighttracker.domain.model.Flight
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent "live now" notification shown while a subscribed flight is in
 * the air. Updates each poll tick with current status, ETA, and progress %.
 * Backed by its own low-importance channel so it doesn't buzz the device.
 */
@Singleton
class OngoingFlightNotification @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    init { ensureChannel() }

    fun show(flight: Flight) {
        if (!hasPostPermission()) return
        val pending = PendingIntent.getActivity(
            context,
            flight.faFlightId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val origin = flight.origin.iata ?: flight.origin.icao ?: "—"
        val dest = flight.destination.iata ?: flight.destination.icao ?: "—"
        val pct = flight.progressPercent ?: 0
        val arrivalInstant = flight.estimatedIn ?: flight.estimatedOn
            ?: flight.scheduledIn ?: flight.scheduledOn
        val nowMs = System.currentTimeMillis()
        val arrivalMs = arrivalInstant?.toEpochMilli()

        val delayText = flight.arrivalDelayMin?.let { d ->
            when {
                d > 0 -> "+${d}m late"
                d < 0 -> "${d}m early"
                else -> "On time"
            }
        }

        // Stale-but-readable countdown in the body. The chronometer set below
        // is the live-ticking version; this string is what shows in launchers
        // / lock-screens that don't render the chronometer prominently.
        val remainingText = arrivalMs?.let { ms ->
            val msLeft = ms - nowMs
            if (msLeft > 0) formatRemaining(msLeft) else null
        }
        val arrivesAtText = arrivalMs?.let { ms ->
            val zone = runCatching { ZoneId.of(flight.destination.timezone ?: "UTC") }
                .getOrDefault(ZoneId.systemDefault())
            "arr " + DateTimeFormatter.ofPattern("HH:mm")
                .withZone(zone)
                .format(Instant.ofEpochMilli(ms))
        }

        val statusLine = listOfNotNull(
            flight.status,
            delayText,
            remainingText,
            arrivesAtText,
        ).joinToString(" · ")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flight)
            .setContentTitle("${flight.ident} · $origin → $dest")
            .setContentText(statusLine.ifEmpty { "In flight" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusLine.ifEmpty { "In flight" }))
            .setProgress(100, pct.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        // Live-ticking countdown in the notification's "when" slot. The system
        // updates this every second on its own, so the user sees a smooth
        // countdown without us having to repost the notification.
        if (arrivalMs != null && arrivalMs > nowMs) {
            builder.setWhen(arrivalMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder.setShowWhen(false)
        }

        NotificationManagerCompat.from(context)
            .notify(notificationId(flight.faFlightId), builder.build())
    }

    private fun formatRemaining(msLeft: Long): String {
        val totalMin = msLeft / 60_000L
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h == 0L -> "${m}m left"
            m == 0L -> "${h}h left"
            else -> "${h}h ${m}m left"
        }
    }

    fun dismiss(faFlightId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(faFlightId))
    }

    private fun ensureChannel() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "Live flight progress",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Persistent progress card while a subscribed flight is airborne."
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationId(faFlightId: String): Int =
        ("ongoing#" + faFlightId).hashCode()

    companion object {
        const val CHANNEL_ID = "flight_live_progress"
        @Suppress("unused") private val unused = FlightTrackerApp.UPDATES_CHANNEL_ID
    }
}
