package com.lewishadden.flighttracker.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.app.ActivityCompat
import com.lewishadden.flighttracker.data.prefs.UserPreferences
import com.lewishadden.flighttracker.domain.model.Flight
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarOption(
    val id: Long,
    val displayName: String,
    val accountName: String,
)

/**
 * Two-way sync between subscribed flights and a user-selected device calendar.
 *
 * On `subscribe`, an event titled by the flight ident is inserted; the row id is
 * stored in DataStore keyed by faFlightId so subsequent updates can target it.
 * On flight refresh (gate change, time change, …) the same event is updated.
 * On unsubscribe, the event is deleted.
 *
 * Honors the user-selected calendar id stored in [UserPreferences]. If none
 * has been picked yet (or the picked calendar is gone), falls back to the
 * device's primary calendar.
 */
@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
) {

    fun hasWritePermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    suspend fun listCalendars(): List<CalendarOption> = withContext(Dispatchers.IO) {
        if (!hasWritePermission()) return@withContext emptyList()
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            ),
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null,
        ) ?: return@withContext emptyList()

        val out = mutableListOf<CalendarOption>()
        cursor.use {
            while (it.moveToNext()) {
                out += CalendarOption(
                    id = it.getLong(0),
                    displayName = it.getString(1) ?: "Calendar",
                    accountName = it.getString(2) ?: "",
                )
            }
        }
        out
    }

    /** Insert or update the calendar event for [flight]. Returns the row id, or null on failure. */
    suspend fun upsertEvent(flight: Flight): Long? = withContext(Dispatchers.IO) {
        val settings = prefs.settings.first()
        if (!settings.calendarIntegrationEnabled || !hasWritePermission()) return@withContext null
        val calendarId = settings.calendarIdForEvents ?: defaultCalendarId() ?: return@withContext null

        val start = flight.estimatedOut ?: flight.scheduledOut ?: return@withContext null
        val end = flight.estimatedIn ?: flight.scheduledIn ?: start.plusSeconds(2 * 3600L)

        val origin = flight.origin.iata ?: flight.origin.icao ?: "?"
        val dest = flight.destination.iata ?: flight.destination.icao ?: "?"
        val title = "${flight.ident}  $origin → $dest"
        val location = listOfNotNull(
            flight.origin.name,
            flight.terminalOrigin?.let { "Terminal $it" },
            flight.gateOrigin?.let { "Gate $it" },
        ).joinToString(" · ")
        val descr = buildString {
            append("Status: ${flight.status ?: "Scheduled"}\n")
            flight.aircraftType?.let { append("Aircraft: $it\n") }
            flight.gateOrigin?.let { append("Gate: $it\n") }
            flight.terminalOrigin?.let { append("Terminal: $it\n") }
            flight.baggageClaim?.let { append("Baggage: $it\n") }
            append("\nManaged by Flight Tracker.")
        }

        val tz = TimeZone.getDefault().id
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.DESCRIPTION, descr)
            put(CalendarContract.Events.DTSTART, start.toEpochMilli())
            put(CalendarContract.Events.DTEND, end.toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, tz)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        val existing = lookupExistingEventId(flight.faFlightId)
        if (existing != null) {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existing)
            val rows = context.contentResolver.update(uri, values, null, null)
            if (rows > 0) existing else null
        } else {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val id = uri?.let { ContentUris.parseId(it) }
            if (id != null) rememberEventId(flight.faFlightId, id)
            id
        }
    }

    suspend fun deleteEvent(faFlightId: String) = withContext(Dispatchers.IO) {
        if (!hasWritePermission()) return@withContext
        val id = lookupExistingEventId(faFlightId) ?: return@withContext
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
        context.contentResolver.delete(uri, null, null)
        forgetEventId(faFlightId)
    }

    /** Pick the first owned writable calendar — typically the user's primary. */
    private fun defaultCalendarId(): Long? {
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.IS_PRIMARY} = 1 OR " +
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} = ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_OWNER.toString()),
            null,
        ) ?: return null
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    // Per-flight event id is stored in app-private SharedPreferences so this
    // file stays self-contained (no extra DataStore key wiring required).
    private val eventIdPrefs by lazy {
        context.getSharedPreferences("calendar_event_ids", Context.MODE_PRIVATE)
    }
    private fun lookupExistingEventId(faFlightId: String): Long? =
        eventIdPrefs.getLong(faFlightId, -1L).takeIf { it != -1L }
    private fun rememberEventId(faFlightId: String, id: Long) {
        eventIdPrefs.edit().putLong(faFlightId, id).apply()
    }
    private fun forgetEventId(faFlightId: String) {
        eventIdPrefs.edit().remove(faFlightId).apply()
    }
}
