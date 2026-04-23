package com.lewishadden.flighttracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.calendar.CalendarOption
import com.lewishadden.flighttracker.data.calendar.CalendarRepository
import com.lewishadden.flighttracker.data.prefs.ThemeMode
import com.lewishadden.flighttracker.data.prefs.UnitSystem
import com.lewishadden.flighttracker.data.prefs.UserPreferences
import com.lewishadden.flighttracker.data.prefs.UserSettings
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.isAirborneNow
import com.lewishadden.flighttracker.notify.BoardingReminderScheduler
import com.lewishadden.flighttracker.notify.OngoingFlightNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val calendars: List<CalendarOption> = emptyList(),
    val watchedJsonExport: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val calendar: CalendarRepository,
    private val repo: FlightRepository,
    private val ongoing: OngoingFlightNotification,
    private val boardingReminders: BoardingReminderScheduler,
) : ViewModel() {

    val settings: StateFlow<UserSettings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    fun setTheme(mode: ThemeMode) = launch { prefs.setTheme(mode) }
    fun setUnits(units: UnitSystem) = launch { prefs.setUnits(units) }

    fun setWifiOnly(enabled: Boolean) = launch { prefs.setWifiOnly(enabled) }
    fun setNotifyGate(v: Boolean) = launch { prefs.setNotifyGate(v) }
    fun setNotifyTerminal(v: Boolean) = launch { prefs.setNotifyTerminal(v) }
    fun setNotifyBaggage(v: Boolean) = launch { prefs.setNotifyBaggage(v) }
    fun setNotifyDelays(v: Boolean) = launch { prefs.setNotifyDelays(v) }
    fun setNotifyStatus(v: Boolean) = launch { prefs.setNotifyStatus(v) }
    fun setNotifyTimes(v: Boolean) = launch { prefs.setNotifyTimes(v) }
    fun setBoardingReminders(enabled: Boolean) = launch {
        prefs.setBoardingReminders(enabled)
        // Apply immediately rather than waiting up to 15 min for the next poll
        // tick. Toggling off cancels any in-flight WorkManager reminders;
        // toggling on schedules them for every currently-subscribed flight.
        val subscribed = repo.getSubscribedFlights()
        if (enabled) {
            subscribed.forEach { boardingReminders.schedule(it) }
        } else {
            subscribed.forEach { boardingReminders.cancel(it.faFlightId) }
        }
    }
    fun setLiveOngoingNotification(enabled: Boolean) = launch {
        prefs.setLiveOngoingNotification(enabled)
        // Apply immediately rather than waiting for the next poll tick. Without
        // this the toggle reads as broken — flipping it off leaves the existing
        // notification on screen for up to 15 minutes.
        val subscribed = repo.getSubscribedFlights()
        if (enabled) {
            subscribed.filter { it.isAirborneNow() }.forEach { ongoing.show(it) }
        } else {
            subscribed.forEach { ongoing.dismiss(it.faFlightId) }
        }
    }
    fun setPauseOnQuotaCap(enabled: Boolean) = launch { prefs.setPauseOnQuotaCap(enabled) }

    fun setCalendarIntegration(enabled: Boolean) = launch {
        prefs.setCalendarIntegration(enabled)
        if (enabled) refreshCalendarsList()
    }
    fun setCalendarId(id: Long?) = launch { prefs.setCalendarId(id) }

    fun refreshCalendarsList() = launch {
        _ui.value = _ui.value.copy(calendars = calendar.listCalendars())
    }

    /** Build a JSON snapshot of subscribed flights for backup / share. */
    fun exportWatched() = launch {
        val flights = repo.getSubscribedFlights()
        val json = buildString {
            append("[")
            flights.forEachIndexed { i, f ->
                if (i > 0) append(",")
                append("{")
                append("\"ident\":\"${f.ident}\",")
                append("\"faFlightId\":\"${f.faFlightId}\",")
                append("\"origin\":\"${f.origin.iata ?: f.origin.icao ?: ""}\",")
                append("\"destination\":\"${f.destination.iata ?: f.destination.icao ?: ""}\",")
                append("\"scheduledOut\":\"${f.scheduledOut?.toString().orEmpty()}\"")
                append("}")
            }
            append("]")
        }
        _ui.value = _ui.value.copy(watchedJsonExport = json)
    }

    fun consumeExport() {
        _ui.value = _ui.value.copy(watchedJsonExport = null)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
