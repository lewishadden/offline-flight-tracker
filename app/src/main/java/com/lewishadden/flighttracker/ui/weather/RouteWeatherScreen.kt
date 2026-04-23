package com.lewishadden.flighttracker.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import com.lewishadden.flighttracker.data.repository.PointWeather
import com.lewishadden.flighttracker.domain.model.Airport
import com.lewishadden.flighttracker.domain.model.AirportWeather
import com.lewishadden.flighttracker.ui.common.BrandBackground
import com.lewishadden.flighttracker.ui.common.BrandCard
import com.lewishadden.flighttracker.ui.common.BrandChip
import com.lewishadden.flighttracker.ui.common.EmptyState
import com.lewishadden.flighttracker.ui.common.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteWeatherScreen(
    vm: RouteWeatherViewModel,
    onBack: () -> Unit,
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
                                ui.flight?.ident?.let { "Weather · $it" } ?: "Route weather",
                                fontWeight = FontWeight.Bold,
                            )
                            ui.flight?.let { f ->
                                val origin = f.origin.iata ?: f.origin.icao
                                val dest = f.destination.iata ?: f.destination.icao
                                if (origin != null && dest != null) {
                                    Text(
                                        "$origin → $dest",
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
                ui.loading -> Box(
                    Modifier.fillMaxSize().padding(pad),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                ui.error != null -> EmptyState(
                    title = "Weather unavailable",
                    subtitle = ui.error ?: "",
                )

                else -> Column(
                    modifier = Modifier
                        .padding(pad)
                        .padding(horizontal = 16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Spacer(Modifier.height(4.dp))
                    AirportWeatherCard(
                        label = "Origin",
                        airport = ui.origin,
                        weather = ui.originWeather,
                    )
                    AirportWeatherCard(
                        label = "Destination",
                        airport = ui.destination,
                        weather = ui.destinationWeather,
                    )
                    if (ui.enroute.isNotEmpty()) {
                        EnRouteCard(points = ui.enroute)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AirportWeatherCard(
    label: String,
    airport: Airport?,
    weather: AirportWeather?,
) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader(label)
            val code = airport?.iata ?: airport?.icao
            val city = airport?.city
            val header = listOfNotNull(code, city).joinToString(" · ")
            if (header.isNotEmpty()) {
                Text(
                    header,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (weather == null) {
                Text(
                    "No current report available for this airport.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
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
private fun EnRouteCard(points: List<EnRoutePoint>) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("En route · surface conditions")
            Text(
                "Surface weather sampled along the great-circle path. Doesn't reflect cruise altitude — airliners fly well above most weather.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            points.forEach { p ->
                EnRouteRow(p)
            }
        }
    }
}

@Composable
private fun EnRouteRow(point: EnRoutePoint) {
    val w: PointWeather? = point.weather
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                point.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            w?.temperatureC?.let {
                BrandChip("${it}° C", color = MaterialTheme.colorScheme.primary)
            }
        }
        if (w == null) {
            Text(
                "Unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        w.conditions?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        w.wind?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            w.cloudCoverPct?.let {
                Text(
                    "Cloud $it%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            w.visibilityKm?.let {
                Text(
                    "Vis ${"%.0f".format(it)} km",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
