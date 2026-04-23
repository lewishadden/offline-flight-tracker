package com.lewishadden.flighttracker.ui.airport

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.domain.model.AirportSummary
import com.lewishadden.flighttracker.domain.model.AirportWeather
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.ui.common.BrandBackground
import com.lewishadden.flighttracker.ui.common.BrandCard
import com.lewishadden.flighttracker.ui.common.BrandChip
import com.lewishadden.flighttracker.ui.common.EmptyState
import com.lewishadden.flighttracker.ui.common.SectionHeader
import com.lewishadden.flighttracker.ui.common.formatDateOnly
import com.lewishadden.flighttracker.ui.common.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirportScreen(
    vm: AirportViewModel,
    onBack: () -> Unit,
    onOpenFlight: (String) -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    BrandBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                ui.info?.name ?: vm.airportCode,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            )
                            ui.info?.let { info ->
                                val codes = listOfNotNull(info.codeIata, info.codeIcao).joinToString(" · ")
                                if (codes.isNotEmpty()) {
                                    Text(
                                        codes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = vm::refresh) {
                            Icon(Icons.Default.Refresh, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        ) { pad ->
            when {
                ui.loading && ui.info == null -> Box(
                    Modifier.fillMaxSize().padding(pad),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                ui.error != null && ui.info == null -> EmptyState(
                    title = "Airport unavailable",
                    subtitle = ui.error ?: "",
                )

                else -> AirportContent(
                    ui = ui,
                    modifier = Modifier.padding(pad),
                    onOpenFlight = onOpenFlight,
                )
            }
        }
    }
}

@Composable
private fun AirportContent(
    ui: AirportUiState,
    modifier: Modifier = Modifier,
    onOpenFlight: (String) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        ui.info?.let { item { HeroCard(it) } }
        // Weather is best-effort — shown when AeroAPI returns a METAR.
        // Otherwise render a small placeholder so the section's absence is
        // explained rather than silently missing.
        if (ui.weather != null) {
            item { WeatherCard(ui.weather) }
        } else if (!ui.loading) {
            item { WeatherUnavailableCard() }
        }

        item {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Departures (${ui.departures.size})") },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Arrivals (${ui.arrivals.size})") },
                )
            }
        }

        val list = if (tab == 0) ui.departures else ui.arrivals
        if (list.isEmpty()) {
            item {
                Text(
                    if (tab == 0) "No upcoming departures" else "No upcoming arrivals",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            items(list, key = { "${it.faFlightId}#$tab" }) { f ->
                AirportFlightRow(
                    flight = f,
                    showOriginCode = tab == 1,  // arrivals show where they came from
                    onClick = { onOpenFlight(f.faFlightId) },
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HeroCard(info: AirportSummary) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                info.name ?: info.code,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val sub = listOfNotNull(info.city, info.countryCode).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                info.codeIata?.let { BrandChip(it, color = MaterialTheme.colorScheme.primary) }
                info.codeIcao?.let { BrandChip(it, color = MaterialTheme.colorScheme.secondary) }
                info.elevationFt?.let { BrandChip("${it.format()} ft", color = MaterialTheme.colorScheme.tertiary) }
            }
            info.timezone?.let {
                Text(
                    "Local time zone: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeatherCard(weather: AirportWeather) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader("Weather")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weather.temperatureC?.let {
                    Text(
                        "${it}° C",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                weather.conditions?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            weather.wind?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            weather.visibility?.let {
                Text(
                    "Visibility · $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            weather.clouds?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            weather.pressure?.let { p ->
                Text(
                    "Pressure $p ${weather.pressureUnits ?: ""}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            weather.raw?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeatherUnavailableCard() {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader("Weather")
            Text(
                "No current METAR available for this airport.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Some smaller airports don't publish weather, and the FlightAware Personal tier doesn't include weather endpoints — see Settings → API usage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AirportFlightRow(
    flight: Flight,
    showOriginCode: Boolean,
    onClick: () -> Unit,
) {
    val otherCode = if (showOriginCode) {
        flight.origin.iata ?: flight.origin.icao ?: "—"
    } else {
        flight.destination.iata ?: flight.destination.icao ?: "—"
    }
    val time = flight.actualOut ?: flight.estimatedOut ?: flight.scheduledOut
    val tz = flight.origin.timezone

    BrandCard(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    flight.ident,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowRightAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        otherCode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    time.formatTime(tz),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    time.formatDateOnly(tz),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    flight.cancelled -> Spacer(Modifier.height(2.dp))
                        .also { /* render chip below */ }
                    else -> { /* nothing */ }
                }
                if (flight.cancelled) {
                    BrandChip("Cancelled", color = MaterialTheme.colorScheme.error)
                } else if (flight.gateOrigin != null && !showOriginCode) {
                    Text(
                        "Gate ${flight.gateOrigin}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun Int.format(): String = "%,d".format(this)
