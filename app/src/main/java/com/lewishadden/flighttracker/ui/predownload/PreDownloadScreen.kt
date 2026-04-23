package com.lewishadden.flighttracker.ui.predownload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreDownloadScreen(
    vm: PreDownloadViewModel,
    onFinished: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onFinished(vm.faFlightId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-download map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val f = state.flight
            Text(
                text = f?.let { "${it.ident}  ·  ${it.origin.iata ?: "?"} → ${it.destination.iata ?: "?"}" } ?: "Loading flight…",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "This will cache map tiles along your flight's projected route at zoom 3–9. " +
                    "Use Wi-Fi — size depends on route distance (typically 50–300 MB).",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (state.downloading || state.percent > 0f) {
                LinearProgressIndicator(
                    progress = { state.percent },
                    modifier = Modifier.fillMaxWidth(),
                )
                val mb = state.bytes / (1024f * 1024f)
                Text(
                    "%.0f%%  ·  %.1f MB  ·  %d / %d tiles".format(
                        state.percent * 100f, mb, state.tilesDone, state.tilesTotal
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }

            if (state.done) {
                Text("Complete — map available offline.", color = MaterialTheme.colorScheme.primary)
            }

            if (state.downloading) {
                OutlinedButton(onClick = vm::cancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            } else if (!state.done) {
                Button(
                    onClick = vm::startDownload,
                    enabled = state.flight != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start download") }
            } else {
                Button(onClick = { onFinished(vm.faFlightId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open map")
                }
            }
        }
    }
}
