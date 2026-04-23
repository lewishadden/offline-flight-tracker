package com.lewishadden.flighttracker.data.repository

import com.lewishadden.flighttracker.data.api.AeroApiService
import com.lewishadden.flighttracker.data.api.dto.AccountUsageDto
import com.lewishadden.flighttracker.data.db.FlightDao
import com.lewishadden.flighttracker.data.db.entities.OfflineRegionEntity
import com.lewishadden.flighttracker.data.db.toDomain
import com.lewishadden.flighttracker.data.db.toEntities
import com.lewishadden.flighttracker.data.db.toEntity
import com.lewishadden.flighttracker.data.toDomain
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.domain.model.RouteFix
import com.lewishadden.flighttracker.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class FlightWithRoute(
    val flight: Flight,
    val route: List<RouteFix>,
)

data class RefreshResult(
    val previous: Flight?,
    val current: Flight,
    val route: List<RouteFix>,
)

@Singleton
class FlightRepository @Inject constructor(
    private val api: AeroApiService,
    private val dao: FlightDao,
) {

    /** Search AeroAPI by flight identifier (e.g. "UAL123", "BA283"). */
    suspend fun searchByIdent(ident: String): List<Flight> {
        val resp = api.getFlightsByIdent(ident.trim().uppercase())
        return resp.flights.map { it.toDomain() }
    }

    /** Refresh single flight + its route + airport coordinates, writing to cache. */
    suspend fun refreshFlight(faFlightId: String): FlightWithRoute {
        val result = refreshFlightWithDiff(faFlightId)
        return FlightWithRoute(result.current, result.route)
    }

    /**
     * Refresh and return both the pre-refresh cached flight and the new one,
     * so callers (e.g. the polling worker) can diff for update notifications.
     */
    suspend fun refreshFlightWithDiff(faFlightId: String): RefreshResult {
        val previous = dao.getFlight(faFlightId)?.toDomain()

        val flightsResp = api.getFlightsByIdent(faFlightId)
        val dto = flightsResp.flights.firstOrNull { it.faFlightId == faFlightId }
            ?: flightsResp.flights.firstOrNull()
            ?: error("Flight $faFlightId not found")
        val base = dto.toDomain()

        // Only fetch airport coords if we don't already have them cached —
        // airports don't move, so we save an API call on every refresh.
        val originCoords = if (previous?.origin?.lat == null || previous.origin.lon == null) {
            base.origin.icao?.let { code -> runCatching { api.getAirport(code) }.getOrNull() }
        } else null
        val destCoords = if (previous?.destination?.lat == null || previous.destination.lon == null) {
            base.destination.icao?.let { code -> runCatching { api.getAirport(code) }.getOrNull() }
        } else null

        val merged = base.copy(
            origin = base.origin.copy(
                lat = originCoords?.latitude ?: previous?.origin?.lat ?: base.origin.lat,
                lon = originCoords?.longitude ?: previous?.origin?.lon ?: base.origin.lon,
            ),
            destination = base.destination.copy(
                lat = destCoords?.latitude ?: previous?.destination?.lat ?: base.destination.lat,
                lon = destCoords?.longitude ?: previous?.destination?.lon ?: base.destination.lon,
            ),
            subscribed = previous?.subscribed ?: false,
        )
        dao.upsertFlight(merged.toEntity())

        val routeResp = runCatching { api.getRoute(merged.faFlightId) }.getOrNull()
        val fixes = routeResp?.fixes?.map { it.toDomain() }.orEmpty()
        if (fixes.isNotEmpty()) {
            dao.replaceRoute(merged.faFlightId, fixes.toEntities(merged.faFlightId))
        }

        return RefreshResult(previous = previous, current = merged, route = fixes)
    }

    suspend fun setSubscribed(faFlightId: String, subscribed: Boolean) {
        dao.setSubscribed(faFlightId, subscribed)
    }

    suspend fun getSubscribedFlights(): List<Flight> =
        dao.getSubscribedFlights().map { it.toDomain() }

    suspend fun getLiveTrack(faFlightId: String): List<TrackPoint> =
        api.getTrack(faFlightId).positions.mapNotNull { it.toDomain() }

    fun observeFlight(faFlightId: String): Flow<FlightWithRoute?> =
        combine(
            dao.observeFlight(faFlightId),
            dao.observeRoute(faFlightId),
        ) { flight, fixes ->
            flight?.let { FlightWithRoute(it.toDomain(), fixes.map { f -> f.toDomain() }) }
        }

    fun observeCachedFlights(): Flow<List<Flight>> =
        dao.observeAllFlights().map { list -> list.map { it.toDomain() } }

    suspend fun getCachedRoute(faFlightId: String): List<RouteFix> =
        dao.getRoute(faFlightId).map { it.toDomain() }

    fun observeSubscribedFlights(): Flow<List<Flight>> =
        dao.observeSubscribedFlights().map { list -> list.map { it.toDomain() } }

    fun observeOfflineRegions(): Flow<List<OfflineRegionEntity>> =
        dao.observeOfflineRegions()

    suspend fun getFlightSnapshot(faFlightId: String): Flight? =
        dao.getFlight(faFlightId)?.toDomain()

    suspend fun getAccountUsage(): AccountUsageDto = api.getAccountUsage()
}
