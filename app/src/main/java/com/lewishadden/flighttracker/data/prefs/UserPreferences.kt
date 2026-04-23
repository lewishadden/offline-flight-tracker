package com.lewishadden.flighttracker.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

/** Snapshot of all user-tunable settings. */
data class UserSettings(
    // Display
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val units: UnitSystem = UnitSystem.IMPERIAL,

    // Networking
    val wifiOnlyRefresh: Boolean = false,

    // Notifications — per-event opt-outs (default everything on)
    val notifyGateChanges: Boolean = true,
    val notifyTerminalChanges: Boolean = true,
    val notifyBaggageClaim: Boolean = true,
    val notifyDelays: Boolean = true,
    val notifyStatusChanges: Boolean = true,
    val notifyTimeChanges: Boolean = true,

    // Boarding reminders (optional, opt-in)
    val boardingRemindersEnabled: Boolean = false,

    // Calendar (optional, opt-in)
    val calendarIntegrationEnabled: Boolean = false,
    val calendarIdForEvents: Long? = null,

    // Live ongoing notification during active flight (subscribed flights only)
    val liveOngoingNotificationEnabled: Boolean = true,

    // API quota safety — default ON; user can disable to keep polling past 90 %.
    val pauseOnQuotaCap: Boolean = true,

    // Search history
    val searchHistory: List<String> = emptyList(),
)

enum class ThemeMode { SYSTEM, DARK, LIGHT }
enum class UnitSystem { IMPERIAL, METRIC }

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    val settings: Flow<UserSettings> = store.data.map { prefs -> prefs.toSettings() }

    // -------- Display --------
    suspend fun setTheme(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }
    suspend fun setUnits(units: UnitSystem) = edit { it[Keys.UNITS] = units.name }

    // -------- Networking --------
    suspend fun setWifiOnly(enabled: Boolean) = edit { it[Keys.WIFI_ONLY] = enabled }

    // -------- Notifications --------
    suspend fun setNotifyGate(v: Boolean) = edit { it[Keys.N_GATE] = v }
    suspend fun setNotifyTerminal(v: Boolean) = edit { it[Keys.N_TERMINAL] = v }
    suspend fun setNotifyBaggage(v: Boolean) = edit { it[Keys.N_BAGGAGE] = v }
    suspend fun setNotifyDelays(v: Boolean) = edit { it[Keys.N_DELAYS] = v }
    suspend fun setNotifyStatus(v: Boolean) = edit { it[Keys.N_STATUS] = v }
    suspend fun setNotifyTimes(v: Boolean) = edit { it[Keys.N_TIMES] = v }

    // -------- Boarding reminders --------
    suspend fun setBoardingReminders(enabled: Boolean) = edit { it[Keys.BOARDING_REMINDERS] = enabled }

    // -------- Calendar --------
    suspend fun setCalendarIntegration(enabled: Boolean) = edit { it[Keys.CAL_INTEGRATION] = enabled }
    suspend fun setCalendarId(id: Long?) = edit {
        if (id == null) it.remove(Keys.CAL_ID) else it[Keys.CAL_ID] = id.toString()
    }

    // -------- Live ongoing notification --------
    suspend fun setLiveOngoingNotification(enabled: Boolean) = edit { it[Keys.LIVE_ONGOING] = enabled }

    // -------- Quota safety --------
    suspend fun setPauseOnQuotaCap(enabled: Boolean) = edit { it[Keys.PAUSE_ON_CAP] = enabled }

    // -------- Search history --------
    /** Adds [query] to the front, dedupes, caps at 8 entries. */
    suspend fun addSearchHistory(query: String) {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return
        edit { prefs ->
            val current = prefs[Keys.HISTORY]?.split('\u001f')?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(q) + current.filterNot { it == q }).take(8)
            prefs[Keys.HISTORY] = updated.joinToString("\u001f")
        }
    }
    suspend fun clearSearchHistory() = edit { it.remove(Keys.HISTORY) }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit(block)
    }

    private fun Preferences.toSettings(): UserSettings = UserSettings(
        theme = (this[Keys.THEME])?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        units = (this[Keys.UNITS])?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.IMPERIAL,
        wifiOnlyRefresh = this[Keys.WIFI_ONLY] ?: false,
        notifyGateChanges = this[Keys.N_GATE] ?: true,
        notifyTerminalChanges = this[Keys.N_TERMINAL] ?: true,
        notifyBaggageClaim = this[Keys.N_BAGGAGE] ?: true,
        notifyDelays = this[Keys.N_DELAYS] ?: true,
        notifyStatusChanges = this[Keys.N_STATUS] ?: true,
        notifyTimeChanges = this[Keys.N_TIMES] ?: true,
        boardingRemindersEnabled = this[Keys.BOARDING_REMINDERS] ?: false,
        calendarIntegrationEnabled = this[Keys.CAL_INTEGRATION] ?: false,
        calendarIdForEvents = this[Keys.CAL_ID]?.toLongOrNull(),
        liveOngoingNotificationEnabled = this[Keys.LIVE_ONGOING] ?: true,
        pauseOnQuotaCap = this[Keys.PAUSE_ON_CAP] ?: true,
        searchHistory = this[Keys.HISTORY]?.split('\u001f')?.filter { it.isNotBlank() } ?: emptyList(),
    )

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val UNITS = stringPreferencesKey("units")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_refresh")
        val N_GATE = booleanPreferencesKey("notify_gate")
        val N_TERMINAL = booleanPreferencesKey("notify_terminal")
        val N_BAGGAGE = booleanPreferencesKey("notify_baggage")
        val N_DELAYS = booleanPreferencesKey("notify_delays")
        val N_STATUS = booleanPreferencesKey("notify_status")
        val N_TIMES = booleanPreferencesKey("notify_times")
        val BOARDING_REMINDERS = booleanPreferencesKey("boarding_reminders")
        val CAL_INTEGRATION = booleanPreferencesKey("cal_integration")
        val CAL_ID = stringPreferencesKey("cal_id")
        val LIVE_ONGOING = booleanPreferencesKey("live_ongoing")
        val PAUSE_ON_CAP = booleanPreferencesKey("pause_on_cap")
        val HISTORY = stringPreferencesKey("search_history")
    }
}
