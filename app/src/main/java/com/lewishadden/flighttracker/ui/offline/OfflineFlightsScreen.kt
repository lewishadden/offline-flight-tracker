package com.lewishadden.flighttracker.ui.offline

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.ui.common.BrandBackground
import com.lewishadden.flighttracker.ui.common.BrandCard
import com.lewishadden.flighttracker.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineFlightsScreen(
    vm: OfflineFlightsViewModel,
    onOpenMap: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    BrandBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Offline flights") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        ) { pad ->
            if (entries.isEmpty()) {
                EmptyState(
                    title = "No offline maps",
                    subtitle = "Download a flight's map from its detail page to have it available without internet.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(entries, key = { it.region.faFlightId }) { entry ->
                        OfflineRow(
                            entry = entry,
                            onOpenMap = { onOpenMap(entry.region.faFlightId) },
                            onOpenDetail = { onOpenDetail(entry.region.faFlightId) },
                            onDelete = { pendingDelete = entry.region.faFlightId },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete offline map?") },
            text = { Text("This removes cached map tiles for this flight. The flight data is kept, and you can re-download later.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun OfflineRow(
    entry: OfflineEntry,
    onOpenMap: () -> Unit,
    onOpenDetail: () -> Unit,
    onDelete: () -> Unit,
) {
    BrandCard(modifier = Modifier
        .fillMaxWidth()
        .clickable { onOpenMap() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.flight?.ident ?: entry.region.faFlightId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val origin = entry.flight?.origin?.iata ?: entry.flight?.origin?.icao
                val dest = entry.flight?.destination?.iata ?: entry.flight?.destination?.icao
                if (origin != null && dest != null) {
                    Text(
                        "$origin → $dest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "%.1f MB · zoom %.0f–%.0f".format(
                        entry.region.sizeBytes / (1024.0 * 1024.0),
                        entry.region.minZoom,
                        entry.region.maxZoom,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onOpenDetail) {
                Icon(Icons.Default.Info, contentDescription = "Open details")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
