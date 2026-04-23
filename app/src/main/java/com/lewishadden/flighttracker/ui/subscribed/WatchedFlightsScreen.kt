package com.lewishadden.flighttracker.ui.subscribed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.ui.common.BrandBackground
import com.lewishadden.flighttracker.ui.common.BrandCard
import com.lewishadden.flighttracker.ui.common.BrandChip
import com.lewishadden.flighttracker.ui.common.EmptyState
import com.lewishadden.flighttracker.ui.common.formatDateOnly
import com.lewishadden.flighttracker.ui.common.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchedFlightsScreen(
    vm: WatchedFlightsViewModel,
    onOpenFlight: (String) -> Unit,
) {
    val watched by vm.watched.collectAsStateWithLifecycle()

    BrandBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Watched flights") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        ) { pad ->
            if (watched.isEmpty()) {
                EmptyState(
                    title = "No watched flights",
                    subtitle = "Tap the bell on any flight to get notifications about gates, delays and status changes.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(watched, key = { it.faFlightId }) { f ->
                        WatchedRow(
                            flight = f,
                            onClick = { onOpenFlight(f.faFlightId) },
                            onUnsubscribe = { vm.unsubscribe(f.faFlightId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchedRow(flight: Flight, onClick: () -> Unit, onUnsubscribe: () -> Unit) {
    BrandCard(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Flight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        flight.ident,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(8.dp))
                    when {
                        flight.cancelled -> BrandChip("Cancelled", color = MaterialTheme.colorScheme.error)
                        flight.diverted -> BrandChip("Diverted", color = MaterialTheme.colorScheme.tertiary)
                        flight.status != null -> BrandChip(flight.status, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Text(
                    "${flight.origin.iata ?: flight.origin.icao ?: "?"} → " +
                        "${flight.destination.iata ?: flight.destination.icao ?: "?"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val dep = flight.actualOut ?: flight.estimatedOut ?: flight.scheduledOut
                Text(
                    "${dep.formatDateOnly(flight.origin.timezone)} · ${dep.formatTime(flight.origin.timezone)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onUnsubscribe) {
                Icon(Icons.Default.Close, contentDescription = "Unsubscribe")
            }
        }
    }
}
