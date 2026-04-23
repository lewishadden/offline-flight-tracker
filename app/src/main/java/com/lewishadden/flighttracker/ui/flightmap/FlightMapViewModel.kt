package com.lewishadden.flighttracker.ui.flightmap

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.db.FlightDao
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.domain.model.RouteFix
import com.lewishadden.flighttracker.location.LocationController
import com.lewishadden.flighttracker.util.Geo
import com.lewishadden.flighttracker.util.LatLon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlightMapUiState(
    val flight: Flight? = null,
    val route: List<RouteFix> = emptyList(),
    val densifiedPath: List<LatLon> = emptyList(),
    val location: Location? = null,
    val progressAlongRouteKm: Double? = null,
    val totalRouteKm: Double? = null,
    val crossTrackKm: Double? = null,
    val hasOfflineRegion: Boolean = false,
)

@HiltViewModel
class FlightMapViewModel @Inject constructor(
    private val repo: FlightRepository,
    dao: FlightDao,
    savedState: SavedStateHandle,
) : ViewModel() {

    val faFlightId: String = checkNotNull(savedState["faFlightId"])

    private val _state = MutableStateFlow(FlightMapUiState())
    val state: StateFlow<FlightMapUiState> = _state.asStateFlow()

    val hasOfflineRegion: StateFlow<Boolean> = dao.observeOfflineRegion(faFlightId)
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            repo.observeFlight(faFlightId).collect { fwr ->
                fwr ?: return@collect
                val path = buildPath(fwr.flight, fwr.route)
                val densified = Geo.densifyGreatCircle(path, maxSegmentKm = 25.0)
                val total = routeLengthKm(densified)
                _state.value = _state.value.copy(
                    flight = fwr.flight,
                    route = fwr.route,
                    densifiedPath = densified,
                    totalRouteKm = total,
                )
            }
        }
        viewModelScope.launch {
            LocationController.currentLocation.collect { loc ->
                loc ?: return@collect
                val path = _state.value.densifiedPath
                if (path.size >= 2) {
                    val here = LatLon(loc.latitude, loc.longitude)
                    val nearest = Geo.nearestPointOnPolyline(path, here)
                    if (nearest != null) {
                        val km = cumulativeKmTo(path, nearest.segmentIndex, nearest.segmentT)
                        _state.value = _state.value.copy(
                            location = loc,
                            progressAlongRouteKm = km,
                            crossTrackKm = nearest.crossTrackKm,
                        )
                        return@collect
                    }
                }
                _state.value = _state.value.copy(location = loc)
            }
        }
    }

    private fun buildPath(flight: Flight, route: List<RouteFix>): List<LatLon> {
        val list = mutableListOf<LatLon>()
        flight.origin.lat?.let { lat -> flight.origin.lon?.let { lon -> list.add(LatLon(lat, lon)) } }
        route.forEach { list.add(LatLon(it.lat, it.lon)) }
        flight.destination.lat?.let { lat -> flight.destination.lon?.let { lon -> list.add(LatLon(lat, lon)) } }
        return list
    }

    private fun routeLengthKm(path: List<LatLon>): Double {
        if (path.size < 2) return 0.0
        var sum = 0.0
        for (i in 0 until path.size - 1) sum += Geo.haversineKm(path[i], path[i + 1])
        return sum
    }

    private fun cumulativeKmTo(path: List<LatLon>, segIndex: Int, t: Double): Double {
        var sum = 0.0
        for (i in 0 until segIndex) sum += Geo.haversineKm(path[i], path[i + 1])
        if (segIndex < path.size - 1) {
            sum += Geo.haversineKm(path[segIndex], path[segIndex + 1]) * t
        }
        return sum
    }
}
