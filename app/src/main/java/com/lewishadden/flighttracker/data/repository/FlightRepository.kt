package com.lewishadden.flighttracker.data.repository

import com.lewishadden.flighttracker.data.api.AeroApiService
import com.lewishadden.flighttracker.data.db.FlightDao
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
        val flightsResp = api.getFlightsByIdent(faFlightId)
        val dto = flightsResp.flights.firstOrNull { it.faFlightId == faFlightId }
            ?: flightsResp.flights.firstOrNull()
            ?: error("Flight $faFlightId not found")
        val base = dto.toDomain()

        val originCoords = base.origin.icao?.let { code ->
            runCatching { api.getAirport(code) }.getOrNull()
        }
        val destCoords = base.destination.icao?.let { code ->
            runCatching { api.getAirport(code) }.getOrNull()
        }
        val flight = base.copy(
            origin = base.origin.copy(
                lat = originCoords?.latitude ?: base.origin.lat,
                lon = originCoords?.longitude ?: base.origin.lon,
            ),
            destination = base.destination.copy(
                lat = destCoords?.latitude ?: base.destination.lat,
                lon = destCoords?.longitude ?: base.destination.lon,
            ),
        )
        dao.upsertFlight(flight.toEntity())

        val routeResp = runCatching { api.getRoute(flight.faFlightId) }.getOrNull()
        val fixes = routeResp?.fixes?.map { it.toDomain() }.orEmpty()
        dao.replaceRoute(flight.faFlightId, fixes.toEntities(flight.faFlightId))

        return FlightWithRoute(flight, fixes)
    }

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
}
