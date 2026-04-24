package com.lewishadden.flighttracker.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.lewishadden.flighttracker.data.repository.AircraftPhoto
import com.lewishadden.flighttracker.ui.theme.Brand
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.data.prefs.UnitSystem
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.ui.common.BrandBackground
import com.lewishadden.flighttracker.ui.common.BrandCard
import com.lewishadden.flighttracker.ui.common.BrandChip
import com.lewishadden.flighttracker.ui.common.KvRow
import com.lewishadden.flighttracker.ui.common.SectionHeader
import com.lewishadden.flighttracker.ui.common.formatAirspeed
import com.lewishadden.flighttracker.ui.common.formatAltitude
import com.lewishadden.flighttracker.ui.common.formatDateOnly
import com.lewishadden.flighttracker.ui.common.formatDateTime
import com.lewishadden.flighttracker.ui.common.formatDelay
import com.lewishadden.flighttracker.ui.common.formatDistance
import com.lewishadden.flighttracker.ui.common.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
    vm: FlightDetailViewModel,
    onPreDownload: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onOpenAirport: (String) -> Unit,
    onOpenRouteWeather: (String) -> Unit,
    onBack: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val hasOffline by vm.hasOfflineRegion.collectAsStateWithLifecycle()
    val subscribed by vm.subscribed.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()

    val context = LocalContext.current
    // Only subscribe if the user actually granted POST_NOTIFICATIONS. Without
    // the permission nm.notify() is a silent no-op on Android 13+, so a
    // "subscribed" flight would never deliver updates.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            vm.toggleSubscription()
        } else {
            Toast.makeText(
                context,
                "Enable notifications in system settings to receive flight updates.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    BrandBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            ui.data?.flight?.ident ?: "Flight",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val needsRequest = !subscribed &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            if (needsRequest) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.toggleSubscription()
                            }
                        }) {
                            Icon(
                                if (subscribed) Icons.Default.Notifications
                                else Icons.Default.NotificationsOff,
                                contentDescription = if (subscribed) "Unsubscribe from updates"
                                                     else "Subscribe to updates",
                                tint = if (subscribed) MaterialTheme.colorScheme.tertiary
                                       else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { onOpenRouteWeather(vm.faFlightId) }) {
                            Icon(
                                Icons.Default.WbCloudy,
                                contentDescription = "Route weather",
                            )
                        }
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
            Box(modifier = Modifier.padding(pad).fillMaxSize()) {
                val flight = ui.data?.flight
                if (flight == null) {
                    Column(Modifier.fillMaxWidth()) {
                        if (ui.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
                        ui.error?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    FlightDetailContent(
                        flight = flight,
                        refreshing = ui.refreshing,
                        hasOffline = hasOffline,
                        subscribed = subscribed,
                        units = units,
                        photo = ui.photo,
                        onPreDownload = { onPreDownload(vm.faFlightId) },
                        onOpenMap = { onOpenMap(vm.faFlightId) },
                        onOpenAirport = onOpenAirport,
                        onPrepareForTrip = {
                            vm.prepareForTrip(
                                onReadyForOfflineDownload = { onPreDownload(vm.faFlightId) }
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightDetailContent(
    flight: Flight,
    refreshing: Boolean,
    hasOffline: Boolean,
    subscribed: Boolean,
    units: UnitSystem,
    photo: AircraftPhoto?,
    onPreDownload: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenAirport: (String) -> Unit,
    onPrepareForTrip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())

        if (photo != null) AircraftPhotoCard(photo, flight.registration, flight.aircraftType)
        HeroCard(flight, onOpenAirport)
        DepartureCard(flight)
        ArrivalCard(flight)
        AircraftCard(flight, units)

        Spacer(Modifier.height(2.dp))
        // Trip Mode — one tap to subscribe + download offline map. Hidden once
        // both are already done (the user is already prepared).
        if (!subscribed || !hasOffline) {
            PrimaryButton(
                text = "Prepare for trip",
                icon = Icons.Default.Luggage,
                onClick = onPrepareForTrip,
            )
        }
        if (hasOffline) {
            PrimaryButton(
                text = "Open flight map",
                icon = Icons.Default.Map,
                onClick = onOpenMap,
            )
            OutlinedButton(
                onClick = onPreDownload,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Re-download offline map")
            }
        } else {
            PrimaryButton(
                text = "Download offline map",
                icon = Icons.Default.CloudDownload,
                onClick = onPreDownload,
            )
        }
    }
}

@Composable
private fun HeroCard(flight: Flight, onOpenAirport: (String) -> Unit) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        flight.ident,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    flight.operatorIata?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                when {
                    flight.cancelled -> BrandChip("Cancelled", color = MaterialTheme.colorScheme.error)
                    flight.diverted -> BrandChip("Diverted", color = MaterialTheme.colorScheme.tertiary)
                    flight.status != null -> BrandChip(flight.status, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.Top) {
                // Tap an airport code to dive into the airport screen — uses
                // ICAO (e.g. EGLL) for the API since IATA can be ambiguous;
                // falls back to IATA if no ICAO is cached.
                val originLookup = flight.origin.icao ?: flight.origin.iata
                val destLookup = flight.destination.icao ?: flight.destination.iata
                RouteEnd(
                    code = flight.origin.iata ?: flight.origin.icao ?: "—",
                    city = flight.origin.city,
                    date = flight.actualOut ?: flight.estimatedOut ?: flight.scheduledOut,
                    time = flight.actualOut ?: flight.estimatedOut ?: flight.scheduledOut,
                    tz = flight.origin.timezone,
                    onCodeClick = originLookup?.let { code -> { onOpenAirport(code) } },
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.ArrowRightAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Spacer(Modifier.weight(1f))
                RouteEnd(
                    code = flight.destination.iata ?: flight.destination.icao ?: "—",
                    city = flight.destination.city,
                    date = flight.actualIn ?: flight.estimatedIn ?: flight.scheduledIn,
                    time = flight.actualIn ?: flight.estimatedIn ?: flight.scheduledIn,
                    tz = flight.destination.timezone,
                    alignEnd = true,
                    onCodeClick = destLookup?.let { code -> { onOpenAirport(code) } },
                )
            }

            flight.progressPercent?.let { pct ->
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(
                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$pct% complete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DelayPill("Departure", flight.departureDelayMin)
                DelayPill("Arrival", flight.arrivalDelayMin)
            }
        }
    }
}

@Composable
private fun RouteEnd(
    code: String,
    city: String?,
    date: java.time.Instant?,
    time: java.time.Instant?,
    tz: String?,
    alignEnd: Boolean = false,
    onCodeClick: (() -> Unit)? = null,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        // Airport code — rendered as a chunky button when there's an airport
        // to look up, plain text otherwise. The button styling (filled tint +
        // outline + chevron) makes the affordance unmistakable without
        // shrinking the displaySmall typography that anchors the hero card.
        if (onCodeClick != null) {
            AirportCodeButton(code = code, onClick = onCodeClick)
        } else {
            Text(
                text = code,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        city?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            date.formatDateOnly(tz),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            time.formatTime(tz),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Premium tappable airport-code chip used in the route hero. Displays the
 * code at the same display-size scale as before but wrapped in a tinted
 * pill with an outline + chevron so it reads unambiguously as a button.
 */
@Composable
private fun AirportCodeButton(
    code: String,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = primary.copy(alpha = 0.14f),
        contentColor = primary,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, primary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = primary,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open airport details",
                tint = primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DelayPill(label: String, delayMin: Long?) {
    val onTime = delayMin == null || delayMin == 0L
    val color = when {
        delayMin == null -> MaterialTheme.colorScheme.secondary
        delayMin <= 0L -> MaterialTheme.colorScheme.secondary
        delayMin < 15L -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        BrandChip(
            text = if (onTime) "On time" else delayMin.formatDelay(),
            color = color,
        )
    }
}

@Composable
private fun DepartureCard(flight: Flight) {
    val zone = flight.origin.timezone
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader("Departure")
            KvRow("Terminal", flight.terminalOrigin ?: "—", highlight = flight.terminalOrigin != null)
            KvRow("Gate", flight.gateOrigin ?: "—", highlight = flight.gateOrigin != null)
            KvRow("Boarding", flight.estimatedOut.formatDateTime(zone))
            KvRow("Scheduled off", flight.scheduledOff.formatDateTime(zone))
            KvRow("Estimated off", flight.estimatedOff.formatDateTime(zone))
            KvRow("Actual off", flight.actualOff.formatDateTime(zone))
        }
    }
}

@Composable
private fun ArrivalCard(flight: Flight) {
    val zone = flight.destination.timezone
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader("Arrival")
            KvRow("Terminal", flight.terminalDestination ?: "—", highlight = flight.terminalDestination != null)
            KvRow("Gate", flight.gateDestination ?: "—", highlight = flight.gateDestination != null)
            KvRow("Baggage", flight.baggageClaim ?: "—", highlight = flight.baggageClaim != null)
            KvRow("Scheduled on", flight.scheduledOn.formatDateTime(zone))
            KvRow("Estimated on", flight.estimatedOn.formatDateTime(zone))
            KvRow("Actual on", flight.actualOn.formatDateTime(zone))
            KvRow("Estimated in", flight.estimatedIn.formatDateTime(zone))
        }
    }
}

@Composable
private fun AircraftCard(flight: Flight, units: UnitSystem) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader("Aircraft & route")
            KvRow("Type", flight.aircraftType ?: "—")
            KvRow("Distance", formatDistance(flight.routeDistanceNm, units))
            KvRow("Filed altitude", formatAltitude(flight.filedAltitudeFt100, units))
            KvRow("Filed speed", formatAirspeed(flight.filedAirspeedKts, units))
            KvRow(
                // ETE = Estimated Time Enroute — the planned flight duration
                // per the filed flight plan. Spelled out for clarity.
                "Flight time",
                flight.filedEteSec?.let { sec -> "%d h %02d m".format(sec / 3600, (sec % 3600) / 60) } ?: "—",
            )
        }
    }
}

@Composable
private fun AircraftPhotoCard(
    photo: AircraftPhoto,
    registration: String?,
    aircraftType: String?,
) {
    val url = photo.largeUrl ?: photo.thumbUrl ?: return
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Brand.SurfaceLo),
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "Aircraft photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Attribution strip — Planespotters' free API requires crediting
            // the photographer when displaying their image.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    val title = listOfNotNull(registration, aircraftType).joinToString(" · ")
                    if (title.isNotEmpty()) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    photo.photographer?.let {
                        Text(
                            "Photo: $it · planespotters.net",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            // Dark indigo button with bright Sky text — reads as a premium dark
            // surface with a luminous accent, rather than dark text on a light
            // Sky button (which looked black against the dark-blue page bg).
            containerColor = Color(0xFF1B2447),
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
