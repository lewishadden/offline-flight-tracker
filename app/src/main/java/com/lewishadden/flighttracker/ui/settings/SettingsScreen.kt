package com.lewishadden.flighttracker.ui.settings

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.data.prefs.ThemeMode
import com.lewishadden.flighttracker.data.prefs.UnitSystem
import com.lewishadden.flighttracker.data.prefs.UserSettings
import com.lewishadden.flighttracker.ui.common.BrandBackground
import com.lewishadden.flighttracker.ui.common.BrandCard
import com.lewishadden.flighttracker.ui.common.BrandSwitch
import com.lewishadden.flighttracker.ui.common.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.WRITE_CALENDAR] == true &&
            result[Manifest.permission.READ_CALENDAR] == true
        if (granted) {
            vm.refreshCalendarsList()
            vm.setCalendarIntegration(true)
        } else {
            vm.setCalendarIntegration(false)
        }
    }

    BrandBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        ) { pad ->
            Column(
                modifier = Modifier
                    .padding(pad)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                AppearanceCard(s, vm::setTheme, vm::setUnits)
                NetworkCard(s, vm::setWifiOnly)
                NotificationsCard(s, vm)
                LiveProgressCard(s, vm::setLiveOngoingNotification)
                BoardingRemindersCard(s, vm::setBoardingReminders)
                CalendarCard(
                    s = s,
                    calendars = ui.calendars,
                    onToggle = { enabled ->
                        if (enabled) {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.WRITE_CALENDAR,
                                    Manifest.permission.READ_CALENDAR,
                                )
                            )
                        } else {
                            vm.setCalendarIntegration(false)
                        }
                    },
                    onSelectCalendar = vm::setCalendarId,
                )
                QuotaCard(s, vm::setPauseOnQuotaCap)
                BackupCard(
                    onExport = vm::exportWatched,
                    pendingJson = ui.watchedJsonExport,
                    onConsumeExport = {
                        ui.watchedJsonExport?.let { json ->
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                                putExtra(Intent.EXTRA_SUBJECT, "Flight Tracker watched flights")
                            }
                            context.startActivity(Intent.createChooser(send, "Export watched flights"))
                            vm.consumeExport()
                        }
                    },
                )
                AboutCard()
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    s: UserSettings,
    onTheme: (ThemeMode) -> Unit,
    onUnits: (UnitSystem) -> Unit,
) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Appearance")
            Text("Theme", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            ThemeMode.entries.forEach { mode ->
                Row(
                    Modifier.fillMaxWidth().clickable { onTheme(mode) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.theme == mode, onClick = { onTheme(mode) })
                    Text(mode.label, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Units", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            UnitSystem.entries.forEach { unit ->
                Row(
                    Modifier.fillMaxWidth().clickable { onUnits(unit) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.units == unit, onClick = { onUnits(unit) })
                    Text(unit.label, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun NetworkCard(s: UserSettings, onWifiOnly: (Boolean) -> Unit) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Network")
            ToggleRow(
                title = "Refresh on Wi-Fi only",
                description = "Suppresses background polling on cellular. Manual refresh and search still work.",
                checked = s.wifiOnlyRefresh,
                onChange = onWifiOnly,
            )
        }
    }
}

@Composable
private fun NotificationsCard(s: UserSettings, vm: SettingsViewModel) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Notifications")
            Text(
                "Pick which subscribed-flight events should buzz the device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ToggleRow("Gate changes", null, s.notifyGateChanges, vm::setNotifyGate)
            ToggleRow("Terminal changes", null, s.notifyTerminalChanges, vm::setNotifyTerminal)
            ToggleRow("Baggage claim", null, s.notifyBaggageClaim, vm::setNotifyBaggage)
            ToggleRow("Delays", null, s.notifyDelays, vm::setNotifyDelays)
            ToggleRow("Time changes", null, s.notifyTimeChanges, vm::setNotifyTimes)
            ToggleRow("Status (cancelled, diverted, …)", null, s.notifyStatusChanges, vm::setNotifyStatus)
        }
    }
}

@Composable
private fun LiveProgressCard(s: UserSettings, onChange: (Boolean) -> Unit) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Live progress")
            ToggleRow(
                title = "Persistent in-flight notification",
                description = "Shows a low-priority progress card while a subscribed flight is airborne.",
                checked = s.liveOngoingNotificationEnabled,
                onChange = onChange,
            )
        }
    }
}

@Composable
private fun BoardingRemindersCard(s: UserSettings, onChange: (Boolean) -> Unit) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Boarding reminders")
            ToggleRow(
                title = "Enable boarding reminders",
                description = "Notify 60, 30, and 15 minutes before scheduled boarding.",
                checked = s.boardingRemindersEnabled,
                onChange = onChange,
            )
        }
    }
}

@Composable
private fun CalendarCard(
    s: UserSettings,
    calendars: List<com.lewishadden.flighttracker.data.calendar.CalendarOption>,
    onToggle: (Boolean) -> Unit,
    onSelectCalendar: (Long?) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    LaunchedEffect(s.calendarIntegrationEnabled) {
        // Auto-open picker once permission is granted and no calendar selected.
        if (s.calendarIntegrationEnabled && s.calendarIdForEvents == null && calendars.isNotEmpty()) {
            pickerOpen = true
        }
    }

    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Calendar integration")
            ToggleRow(
                title = "Sync subscribed flights to calendar",
                description = "Creates an event when you subscribe and updates it as the flight changes.",
                checked = s.calendarIntegrationEnabled,
                onChange = onToggle,
            )
            if (s.calendarIntegrationEnabled) {
                val selectedName = calendars.firstOrNull { it.id == s.calendarIdForEvents }?.displayName
                Row(
                    Modifier.fillMaxWidth().clickable { pickerOpen = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Target calendar", color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        selectedName ?: "Tap to select",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            title = { Text("Select calendar") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(calendars, key = { it.id }) { cal ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    onSelectCalendar(cal.id)
                                    pickerOpen = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = s.calendarIdForEvents == cal.id,
                                onClick = {
                                    onSelectCalendar(cal.id)
                                    pickerOpen = false
                                },
                            )
                            Column {
                                Text(cal.displayName, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    cal.accountName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerOpen = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun QuotaCard(s: UserSettings, onChange: (Boolean) -> Unit) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("API quota")
            ToggleRow(
                title = "Pause polling near plan cap",
                description = "When AeroAPI usage reaches 90% of your monthly cap, pause subscribed-flight polling. Disable to keep polling past the cap (you'll be billed for overage if enabled on your plan).",
                checked = s.pauseOnQuotaCap,
                onChange = onChange,
            )
        }
    }
}

@Composable
private fun BackupCard(
    onExport: () -> Unit,
    pendingJson: String?,
    onConsumeExport: () -> Unit,
) {
    LaunchedEffect(pendingJson) {
        if (pendingJson != null) onConsumeExport()
    }
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Backup")
            Text(
                "Export your watched flights as JSON. Useful for archiving or sharing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text("Export watched flights")
            }
        }
    }
}

@Composable
private fun AboutCard() {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader("About")
            Text("Flight Tracker", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(
                "Offline-first flight status, built for travelers and spotters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Data: FlightAware AeroAPI · Aircraft photos: planespotters.net",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BrandSwitch(checked = checked, onCheckedChange = onChange)
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System default"
        ThemeMode.DARK -> "Dark"
        ThemeMode.LIGHT -> "Light"
    }

private val UnitSystem.label: String
    get() = when (this) {
        UnitSystem.IMPERIAL -> "Imperial (mi, ft, mph)"
        UnitSystem.METRIC -> "Metric (km, m, kph)"
    }

