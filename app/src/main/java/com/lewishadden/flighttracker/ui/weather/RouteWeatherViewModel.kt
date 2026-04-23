package com.lewishadden.flighttracker.ui.weather

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.data.repository.PointWeather
import com.lewishadden.flighttracker.data.repository.WeatherRepository
import com.lewishadden.flighttracker.domain.model.Airport
import com.lewishadden.flighttracker.domain.model.AirportWeather
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.util.Geo
import com.lewishadden.flighttracker.util.LatLon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnRoutePoint(
    val label: String,
    val fractionAlongPath: Double,
    val weather: PointWeather?,
)

data class RouteWeatherUiState(
    val loading: Boolean = true,
    val flight: Flight? = null,
    val origin: Airport? = null,
    val destination: Airport? = null,
    val originWeather: AirportWeather? = null,
    val destinationWeather: AirportWeather? = null,
    val enroute: List<EnRoutePoint> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class RouteWeatherViewModel @Inject constructor(
    private val flights: FlightRepository,
    private val weather: WeatherRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    val faFlightId: String = checkNotNull(savedState["faFlightId"])

    private val _ui = MutableStateFlow(RouteWeatherUiState())
    val ui: StateFlow<RouteWeatherUiState> = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)

            val flight = flights.getFlightSnapshot(faFlightId)
            if (flight == null) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = "Flight data not cached yet. Open the flight once with a connection, then try again.",
                )
                return@launch
            }
            val route = flights.getCachedRoute(faFlightId)

            // Fire all fetches concurrently.
            val originWeatherDef = async {
                weather.getAirportWeather(
                    icaoCode = flight.origin.icao,
                    lat = flight.origin.lat,
                    lon = flight.origin.lon,
                )
            }
            val destWeatherDef = async {
                weather.getAirportWeather(
                    icaoCode = flight.destination.icao,
                    lat = flight.destination.lat,
                    lon = flight.destination.lon,
                )
            }

            // Build the great-circle path and sample 3 points (25%, 50%, 75%)
            // for en-route conditions. If we don't have origin/destination
            // coordinates we skip the en-route pass.
            val pathPoints = buildPath(flight, route)
            val densified = if (pathPoints.size >= 2)
                Geo.densifyGreatCircle(pathPoints, maxSegmentKm = 50.0)
            else emptyList()

            val enrouteSamples = if (densified.size >= 3) {
                listOf(0.25, 0.50, 0.75).map { f -> f to pointAtFraction(densified, f) }
            } else emptyList()

            val enrouteDef = enrouteSamples.map { (frac, p) ->
                async { EnRoutePoint(
                    label = when (frac) { 0.25 -> "25% of route"; 0.50 -> "Halfway"; 0.75 -> "75% of route"; else -> "" },
                    fractionAlongPath = frac,
                    weather = weather.getPointWeather(p.lat, p.lon),
                )}
            }

            val originWeather = originWeatherDef.await()
            val destWeather = destWeatherDef.await()
            val enroute = enrouteDef.awaitAll()

            _ui.value = RouteWeatherUiState(
                loading = false,
                flight = flight,
                origin = flight.origin,
                destination = flight.destination,
                originWeather = originWeather,
                destinationWeather = destWeather,
                enroute = enroute,
                error = null,
            )
        }
    }

    private fun buildPath(flight: Flight, route: List<com.lewishadden.flighttracker.domain.model.RouteFix>): List<LatLon> {
        val list = mutableListOf<LatLon>()
        flight.origin.lat?.let { lat -> flight.origin.lon?.let { lon -> list.add(LatLon(lat, lon)) } }
        route.forEach { list.add(LatLon(it.lat, it.lon)) }
        flight.destination.lat?.let { lat -> flight.destination.lon?.let { lon -> list.add(LatLon(lat, lon)) } }
        return list
    }

    /** Point at fraction f∈[0,1] along a densified polyline, by cumulative distance. */
    private fun pointAtFraction(path: List<LatLon>, f: Double): LatLon {
        if (path.size < 2) return path.first()
        val cum = DoubleArray(path.size)
        for (i in 1 until path.size) {
            cum[i] = cum[i - 1] + Geo.haversineKm(path[i - 1], path[i])
        }
        val total = cum.last()
        val target = total * f
        // Find the segment containing `target`.
        var idx = cum.indexOfFirst { it >= target }
        if (idx <= 0) idx = 1
        val prev = cum[idx - 1]
        val seg = cum[idx] - prev
        val segT = if (seg > 0) (target - prev) / seg else 0.0
        val a = path[idx - 1]; val b = path[idx]
        return LatLon(
            lat = a.lat + (b.lat - a.lat) * segT,
            lon = a.lon + (b.lon - a.lon) * segT,
        )
    }
}
