package com.lewishadden.flighttracker.ui.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.ui.common.BrandBackground
import com.lewishadden.flighttracker.ui.common.BrandCard
import com.lewishadden.flighttracker.ui.common.BrandChip
import com.lewishadden.flighttracker.ui.common.SectionHeader
import com.lewishadden.flighttracker.ui.common.formatDateOnly
import com.lewishadden.flighttracker.ui.common.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: SearchViewModel,
    onOpenFlight: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    BrandBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Flight Tracker",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Offline-first flight status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    label = { Text("Flight number (e.g. BA283, UAL123)") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.loading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                if (state.results.isNotEmpty()) {
                    SectionHeader("Matches")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.results, key = { it.faFlightId }) { f ->
                            FlightRow(f) {
                                vm.prefetch(f.faFlightId)
                                onOpenFlight(f.faFlightId)
                            }
                        }
                    }
                } else if (state.cached.isNotEmpty()) {
                    SectionHeader("Recently viewed · available offline")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.cached, key = { it.faFlightId }) { f ->
                            FlightRow(f) { onOpenFlight(f.faFlightId) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightRow(flight: Flight, onClick: () -> Unit) {
    BrandCard(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Flight,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.padding(end = 10.dp).height(0.dp))
                Text(
                    flight.ident,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp),
                )
                Spacer(Modifier.weight(1f).height(0.dp))
                when {
                    flight.cancelled -> BrandChip("Cancelled", color = MaterialTheme.colorScheme.error)
                    flight.diverted -> BrandChip("Diverted", color = MaterialTheme.colorScheme.tertiary)
                    flight.status != null -> BrandChip(flight.status, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AirportBlock(
                    code = flight.origin.iata ?: flight.origin.icao ?: "—",
                    city = flight.origin.city,
                    date = flight.actualOut ?: flight.estimatedOut ?: flight.scheduledOut,
                    time = flight.actualOut ?: flight.estimatedOut ?: flight.scheduledOut,
                    tz = flight.origin.timezone,
                )
                Spacer(Modifier.weight(1f).height(0.dp))
                Icon(
                    Icons.Default.ArrowRightAlt,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f).height(0.dp))
                AirportBlock(
                    code = flight.destination.iata ?: flight.destination.icao ?: "—",
                    city = flight.destination.city,
                    date = flight.actualIn ?: flight.estimatedIn ?: flight.scheduledIn,
                    time = flight.actualIn ?: flight.estimatedIn ?: flight.scheduledIn,
                    tz = flight.destination.timezone,
                    alignEnd = true,
                )
            }
        }
    }
}

@Composable
private fun AirportBlock(
    code: String,
    city: String?,
    date: java.time.Instant?,
    time: java.time.Instant?,
    tz: String?,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            code,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        city?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            date.formatDateOnly(tz),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            time.formatTime(tz),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
