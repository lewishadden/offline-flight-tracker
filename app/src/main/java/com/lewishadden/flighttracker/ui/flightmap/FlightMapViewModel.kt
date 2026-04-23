package com.lewishadden.flighttracker.ui.flightmap

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.db.FlightDao
import com.lewishadden.flighttracker.data.prefs.UnitSystem
import com.lewishadden.flighttracker.data.prefs.UserPreferences
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.domain.model.RouteFix
import com.lewishadden.flighttracker.domain.model.TrackPoint
import com.lewishadden.flighttracker.location.LocationController
import com.lewishadden.flighttracker.util.Geo
import com.lewishadden.flighttracker.util.LatLon
import com.lewishadden.flighttracker.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PositionSource { NONE, AIRCRAFT, GPS }

data class FlightMapUiState(
    val flight: Flight? = null,
    val route: List<RouteFix> = emptyList(),
    val densifiedPath: List<LatLon> = emptyList(),
    val location: Location? = null,
    val positionSource: PositionSource = PositionSource.NONE,
    val progressAlongRouteKm: Double? = null,
    val totalRouteKm: Double? = null,
    val crossTrackKm: Double? = null,
    /** Bearing along the route at the user's projected position, for plane marker rotation. */
    val headingDeg: Float? = null,
    val hasOfflineRegion: Boolean = false,
)

@HiltViewModel
class FlightMapViewModel @Inject constructor(
    private val repo: FlightRepository,
    private val networkMonitor: NetworkMonitor,
    prefs: UserPreferences,
    dao: FlightDao,
    savedState: SavedStateHandle,
) : ViewModel() {

    val faFlightId: String = checkNotNull(savedState["faFlightId"])

    private val _state = MutableStateFlow(FlightMapUiState())
    val state: StateFlow<FlightMapUiState> = _state.asStateFlow()

    val units: StateFlow<UnitSystem> = prefs.settings
        .map { it.units }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnitSystem.IMPERIAL)

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

        // Phone GPS — used directly when offline OR when no aircraft fix yet.
        viewModelScope.launch {
            LocationController.currentLocation.collect { loc ->
                loc ?: return@collect
                if (_state.value.positionSource == PositionSource.AIRCRAFT) {
                    // We have a live aircraft fix from AeroAPI — keep using that.
                    return@collect
                }
                applyPosition(loc.latitude, loc.longitude, source = PositionSource.GPS, raw = loc)
            }
        }

        // Live aircraft position polling — only when online + airborne.
        viewModelScope.launch { liveAircraftPositionLoop() }
    }

    /**
     * Polls AeroAPI's `/flights/{id}/position` whenever the device is online.
     *
     * We don't gate on a local "airborne" guess because AeroAPI's `actualOff`/
     * `actualOut` fields can lag the real wheels-up event by minutes — so the
     * VM would refuse to fetch when the aircraft was already in the air. The
     * API itself is the source of truth: a non-null `last_position` means there
     * IS a live fix; a null response means there isn't (pre-departure, on the
     * ground, or out of ADS-B coverage) and we degrade to phone GPS.
     */
    private suspend fun liveAircraftPositionLoop() {
        while (viewModelScope.isActive) {
            val flight = _state.value.flight
            val online = networkMonitor.snapshot().online

            if (flight == null) {
                // Flight still loading from DB — try again shortly.
                delay(POLL_BOOTSTRAP_MS)
                continue
            }
            if (!online) {
                releaseAircraftLock()
                delay(POLL_OFF_MS)
                continue
            }

            val tp = runCatching { repo.getLivePosition(faFlightId) }.getOrNull()
            if (tp != null) {
                val asLocation = Location("aeroapi").apply {
                    latitude = tp.lat
                    longitude = tp.lon
                    tp.altitudeFt100?.let { altitude = (it * 100).toDouble() * 0.3048 }
                    tp.groundspeedKts?.let { speed = (it * 0.514444).toFloat() }
                    tp.headingDeg?.let { bearing = it.toFloat() }
                    time = tp.timestamp.toEpochMilli()
                }
                applyPosition(tp.lat, tp.lon, source = PositionSource.AIRCRAFT, raw = asLocation)
                delay(POLL_LIVE_MS)
            } else {
                // No live position — let GPS take back over.
                releaseAircraftLock()
                delay(POLL_OFF_MS)
            }
        }
    }

    /** Releases the AIRCRAFT source so the GPS collector is allowed through. */
    private fun releaseAircraftLock() {
        if (_state.value.positionSource == PositionSource.AIRCRAFT) {
            _state.value = _state.value.copy(positionSource = PositionSource.GPS)
        }
    }

    private fun applyPosition(lat: Double, lon: Double, source: PositionSource, raw: Location) {
        val path = _state.value.densifiedPath
        if (path.size >= 2) {
            val here = LatLon(lat, lon)
            val nearest = Geo.nearestPointOnPolyline(path, here)
            if (nearest != null) {
                val km = cumulativeKmTo(path, nearest.segmentIndex, nearest.segmentT)
                val segStart = path[nearest.segmentIndex]
                val segEnd = path.getOrNull(nearest.segmentIndex + 1) ?: segStart
                val heading = if (segStart != segEnd) {
                    Geo.bearingDeg(segStart, segEnd).toFloat()
                } else null
                _state.value = _state.value.copy(
                    location = raw,
                    positionSource = source,
                    progressAlongRouteKm = km,
                    crossTrackKm = nearest.crossTrackKm,
                    headingDeg = heading,
                )
                return
            }
        }
        _state.value = _state.value.copy(location = raw, positionSource = source)
    }

    private fun isAirborne(flight: Flight): Boolean {
        if (flight.cancelled) return false
        val departed = flight.actualOff != null || flight.actualOut != null
        val arrived = flight.actualOn != null || flight.actualIn != null
        return departed && !arrived
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

    /** Promote a track point we already have (e.g. seeded once we mount). */
    @Suppress("unused") private fun promote(tp: TrackPoint) = Unit

    companion object {
        // Live aircraft position cadence — 30 s in flight is plenty given AeroAPI
        // updates the underlying position every ~minute.
        private const val POLL_LIVE_MS = 30_000L
        // No live fix or offline — back off so we're not pinging AeroAPI for nothing.
        private const val POLL_OFF_MS = 60_000L
        // Tight retry while waiting for the flight row to load from the DB on screen mount.
        private const val POLL_BOOTSTRAP_MS = 500L
    }
}
