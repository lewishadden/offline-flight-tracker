package com.lewishadden.flighttracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.ui.common.formatDateTime
import com.lewishadden.flighttracker.ui.common.formatDelay
import com.lewishadden.flighttracker.ui.common.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
    vm: FlightDetailViewModel,
    onPreDownload: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onBack: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val hasOffline by vm.hasOfflineRegion.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.data?.flight?.ident ?: "Flight") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Default.Refresh, null)
                    }
                }
            )
        }
    ) { pad ->
        Box(modifier = Modifier.padding(pad).fillMaxSize()) {
            val flight = ui.data?.flight
            if (flight == null) {
                if (ui.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            } else {
                FlightDetailContent(
                    flight = flight,
                    refreshing = ui.refreshing,
                    hasOffline = hasOffline,
                    onPreDownload = { onPreDownload(vm.faFlightId) },
                    onOpenMap = { onOpenMap(vm.faFlightId) },
                )
            }
        }
    }
}

@Composable
private fun FlightDetailContent(
    flight: Flight,
    refreshing: Boolean,
    hasOffline: Boolean,
    onPreDownload: () -> Unit,
    onOpenMap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())

        StatusCard(flight)
        RouteCard(flight)
        DepartureCard(flight)
        ArrivalCard(flight)
        AircraftCard(flight)

        Spacer(Modifier.height(4.dp))
        if (hasOffline) {
            Button(onClick = onOpenMap, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Map, null)
                Spacer(Modifier.width(8.dp))
                Text("Open flight map")
            }
            OutlinedButton(onClick = onPreDownload, modifier = Modifier.fillMaxWidth()) {
                Text("Re-download offline map")
            }
        } else {
            Button(onClick = onPreDownload, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CloudDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("Download offline map for this flight")
            }
        }
    }
}

@Composable
private fun StatusCard(flight: Flight) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(flight.status ?: "—", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KeyValue("Dep delay", flight.departureDelayMin.formatDelay())
                KeyValue("Arr delay", flight.arrivalDelayMin.formatDelay())
            }
            flight.progressPercent?.let { pct ->
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("$pct% complete", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RouteCard(flight: Flight) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AirportBlock(
                code = flight.origin.iata ?: flight.origin.icao ?: "—",
                city = flight.origin.city,
                time = flight.actualOut ?: flight.estimatedOut ?: flight.scheduledOut,
                zone = flight.origin.timezone,
                label = "From"
            )
            Text("→", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp))
            AirportBlock(
                code = flight.destination.iata ?: flight.destination.icao ?: "—",
                city = flight.destination.city,
                time = flight.actualIn ?: flight.estimatedIn ?: flight.scheduledIn,
                zone = flight.destination.timezone,
                label = "To"
            )
        }
    }
}

@Composable
private fun AirportBlock(code: String, city: String?, time: java.time.Instant?, zone: String?, label: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(code, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        city?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text(time.formatTime(zone), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DepartureCard(flight: Flight) {
    val zone = flight.origin.timezone
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Departure", style = MaterialTheme.typography.titleMedium)
            KeyValue("Terminal", flight.terminalOrigin ?: "—")
            KeyValue("Gate", flight.gateOrigin ?: "—")
            KeyValue("Boarding", flight.estimatedOut.formatDateTime(zone))
            KeyValue("Scheduled off", flight.scheduledOff.formatDateTime(zone))
            KeyValue("Estimated off", flight.estimatedOff.formatDateTime(zone))
            KeyValue("Actual off", flight.actualOff.formatDateTime(zone))
        }
    }
}

@Composable
private fun ArrivalCard(flight: Flight) {
    val zone = flight.destination.timezone
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Arrival", style = MaterialTheme.typography.titleMedium)
            KeyValue("Terminal", flight.terminalDestination ?: "—")
            KeyValue("Gate", flight.gateDestination ?: "—")
            KeyValue("Baggage", flight.baggageClaim ?: "—")
            KeyValue("Scheduled on", flight.scheduledOn.formatDateTime(zone))
            KeyValue("Estimated on", flight.estimatedOn.formatDateTime(zone))
            KeyValue("Actual on", flight.actualOn.formatDateTime(zone))
            KeyValue("Estimated in", flight.estimatedIn.formatDateTime(zone))
        }
    }
}

@Composable
private fun AircraftCard(flight: Flight) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Aircraft & route", style = MaterialTheme.typography.titleMedium)
            KeyValue("Type", flight.aircraftType ?: "—")
            KeyValue("Distance", flight.routeDistanceNm?.let { "$it nm" } ?: "—")
            KeyValue("Filed altitude", flight.filedAltitudeFt100?.let { "FL${"%03d".format(it)}" } ?: "—")
            KeyValue("Filed speed", flight.filedAirspeedKts?.let { "$it kts" } ?: "—")
            KeyValue("ETE", flight.filedEteSec?.let { sec -> "%d:%02d".format(sec / 3600, (sec % 3600) / 60) } ?: "—")
        }
    }
}

@Composable
private fun KeyValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
