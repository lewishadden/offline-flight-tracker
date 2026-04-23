package com.lewishadden.flighttracker.ui.flightmap

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.location.LocationController
import com.lewishadden.flighttracker.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightMapScreen(
    vm: FlightMapViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (permissionGranted) LocationController.start(context)
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) LocationController.start(context)
    }

    DisposableEffect(Unit) {
        onDispose { /* keep service running for background tracking */ }
    }

    // Debounce the back press — MapView.onDestroy() briefly holds the UI thread
    // during the Compose exit transition, so without this guard repeated taps
    // would queue up while the first pop is still in flight.
    var backConsumed by remember { mutableStateOf(false) }
    val handleBack = {
        if (!backConsumed) {
            backConsumed = true
            onBack()
        }
    }

    Scaffold(
        containerColor = Brand.IndigoDeep,
        topBar = {
            TopAppBar(
                title = { Text(state.flight?.ident ?: "Flight map") },
                navigationIcon = {
                    IconButton(onClick = handleBack, enabled = !backConsumed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Brand.IndigoDeep,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                // Solid dark backdrop so any brief GL re-render during rotation
                // blends with the theme instead of showing a lighter surface.
                .background(Brand.IndigoDeep)
        ) {
            if (!permissionGranted) {
                PermissionRequest(onGrant = {
                    launcher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS,
                        )
                    )
                })
            } else {
                MapLibreFlightView(state)
                InfoOverlay(state)
                state.flight?.takeIf { it.diverted }?.let { f ->
                    DivertedBanner(
                        destinationName = f.destination.city
                            ?: f.destination.iata
                            ?: f.destination.icao,
                    )
                }
            }
        }
    }
}

@Composable
private fun DivertedBanner(destinationName: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 12.dp, end = 12.dp)
            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "DIVERTED",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelSmall,
        )
        if (destinationName != null) {
            Text(
                "  →  $destinationName",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PermissionRequest(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Location permission needed to show your position on the cached flight path.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onGrant, modifier = Modifier.padding(top = 16.dp)) {
            Text("Grant permission")
        }
    }
}

@Composable
private fun InfoOverlay(state: FlightMapUiState) {
    val progress = state.progressAlongRouteKm
    val total = state.totalRouteKm
    val pct = if (progress != null && total != null && total > 0) (progress / total).coerceIn(0.0, 1.0) else null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val loc = state.location
            Column {
                Text("GPS", color = Color.White, style = MaterialTheme.typography.labelSmall)
                if (loc != null) {
                    Text(
                        "%.4f, %.4f".format(loc.latitude, loc.longitude),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "alt %.0f m · %.0f kt · ±%.0f m".format(
                            loc.altitude,
                            loc.speed * 1.94384f,
                            loc.accuracy,
                        ),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text("Acquiring…", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column {
                Text("ROUTE", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text(
                    if (progress != null && total != null) "%.0f / %.0f km".format(progress, total) else "—",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    pct?.let { "%.0f%% along path".format(it * 100) } ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
                state.crossTrackKm?.let {
                    Text(
                        "%.1f km off path".format(it),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
