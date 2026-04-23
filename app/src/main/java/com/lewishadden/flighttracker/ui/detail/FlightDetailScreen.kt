package com.lewishadden.flighttracker.ui.detail

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.ui.common.BrandBackground
import com.lewishadden.flighttracker.ui.common.BrandCard
import com.lewishadden.flighttracker.ui.common.BrandChip
import com.lewishadden.flighttracker.ui.common.KvRow
import com.lewishadden.flighttracker.ui.common.SectionHeader
import com.lewishadden.flighttracker.ui.common.formatDateOnly
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
    val subscribed by vm.subscribed.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        vm.toggleSubscription()
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
                            if (!subscribed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                        photo = ui.photo,
                        onPreDownload = { onPreDownload(vm.faFlightId) },
                        onOpenMap = { onOpenMap(vm.faFlightId) },
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
    photo: AircraftPhoto?,
    onPreDownload: () -> Unit,
    onOpenMap: () -> Unit,
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
        HeroCard(flight)
        DepartureCard(flight)
        ArrivalCard(flight)
        AircraftCard(flight)

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
private fun HeroCard(flight: Flight) {
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
                RouteEnd(
                    code = flight.origin.iata ?: flight.origin.icao ?: "—",
                    city = flight.origin.city,
                    date = flight.actualOut ?: flight.estimatedOut ?: flight.scheduledOut,
                    time = flight.actualOut ?: flight.estimatedOut ?: flight.scheduledOut,
                    tz = flight.origin.timezone,
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
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            code,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
        )
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
private fun AircraftCard(flight: Flight) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader("Aircraft & route")
            KvRow("Type", flight.aircraftType ?: "—")
            KvRow(
                "Distance",
                // AeroAPI returns route distance in nautical miles. Convert to
                // statute miles (the everyday "mile") for display.
                flight.routeDistanceNm?.let { "%,d mi".format((it * 1.15078).toInt()) } ?: "—",
            )
            KvRow(
                "Filed altitude",
                // filedAltitudeFt100 is in hundreds of feet (e.g. 350 = FL350 = 35,000 ft).
                flight.filedAltitudeFt100?.let { "%,d ft".format(it * 100) } ?: "—",
            )
            KvRow(
                "Filed speed",
                // Filed airspeed is in knots; convert to mph.
                flight.filedAirspeedKts?.let { "%,d mph".format((it * 1.15078).toInt()) } ?: "—",
            )
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
