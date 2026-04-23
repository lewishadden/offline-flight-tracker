package com.lewishadden.flighttracker.data.db

import com.lewishadden.flighttracker.data.db.entities.FlightEntity
import com.lewishadden.flighttracker.data.db.entities.RouteFixEntity
import com.lewishadden.flighttracker.domain.model.Airport
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.domain.model.RouteFix
import java.time.Instant

fun Flight.toEntity(nowMs: Long = System.currentTimeMillis()) = FlightEntity(
    faFlightId = faFlightId,
    ident = ident,
    operatorIata = operatorIata,
    flightNumber = flightNumber,
    aircraftType = aircraftType,
    originIcao = origin.icao,
    originIata = origin.iata,
    originName = origin.name,
    originCity = origin.city,
    originTimezone = origin.timezone,
    originLat = origin.lat,
    originLon = origin.lon,
    destIcao = destination.icao,
    destIata = destination.iata,
    destName = destination.name,
    destCity = destination.city,
    destTimezone = destination.timezone,
    destLat = destination.lat,
    destLon = destination.lon,
    scheduledOut = scheduledOut?.toEpochMilli(),
    estimatedOut = estimatedOut?.toEpochMilli(),
    actualOut = actualOut?.toEpochMilli(),
    scheduledOff = scheduledOff?.toEpochMilli(),
    estimatedOff = estimatedOff?.toEpochMilli(),
    actualOff = actualOff?.toEpochMilli(),
    scheduledOn = scheduledOn?.toEpochMilli(),
    estimatedOn = estimatedOn?.toEpochMilli(),
    actualOn = actualOn?.toEpochMilli(),
    scheduledIn = scheduledIn?.toEpochMilli(),
    estimatedIn = estimatedIn?.toEpochMilli(),
    actualIn = actualIn?.toEpochMilli(),
    gateOrigin = gateOrigin,
    gateDestination = gateDestination,
    terminalOrigin = terminalOrigin,
    terminalDestination = terminalDestination,
    baggageClaim = baggageClaim,
    departureDelayMin = departureDelayMin,
    arrivalDelayMin = arrivalDelayMin,
    status = status,
    progressPercent = progressPercent,
    cancelled = cancelled,
    diverted = diverted,
    routeDistanceNm = routeDistanceNm,
    filedEteSec = filedEteSec,
    filedAltitudeFt100 = filedAltitudeFt100,
    filedAirspeedKts = filedAirspeedKts,
    lastFetchedEpochMs = nowMs,
    subscribed = subscribed,
)

fun FlightEntity.toDomain() = Flight(
    faFlightId = faFlightId,
    ident = ident,
    operatorIata = operatorIata,
    flightNumber = flightNumber,
    aircraftType = aircraftType,
    origin = Airport(originIcao, originIata, originName, originCity, originTimezone, originLat, originLon),
    destination = Airport(destIcao, destIata, destName, destCity, destTimezone, destLat, destLon),
    scheduledOut = scheduledOut?.let(Instant::ofEpochMilli),
    estimatedOut = estimatedOut?.let(Instant::ofEpochMilli),
    actualOut = actualOut?.let(Instant::ofEpochMilli),
    scheduledOff = scheduledOff?.let(Instant::ofEpochMilli),
    estimatedOff = estimatedOff?.let(Instant::ofEpochMilli),
    actualOff = actualOff?.let(Instant::ofEpochMilli),
    scheduledOn = scheduledOn?.let(Instant::ofEpochMilli),
    estimatedOn = estimatedOn?.let(Instant::ofEpochMilli),
    actualOn = actualOn?.let(Instant::ofEpochMilli),
    scheduledIn = scheduledIn?.let(Instant::ofEpochMilli),
    estimatedIn = estimatedIn?.let(Instant::ofEpochMilli),
    actualIn = actualIn?.let(Instant::ofEpochMilli),
    gateOrigin = gateOrigin,
    gateDestination = gateDestination,
    terminalOrigin = terminalOrigin,
    terminalDestination = terminalDestination,
    baggageClaim = baggageClaim,
    departureDelayMin = departureDelayMin,
    arrivalDelayMin = arrivalDelayMin,
    status = status,
    progressPercent = progressPercent,
    cancelled = cancelled,
    diverted = diverted,
    routeDistanceNm = routeDistanceNm,
    filedEteSec = filedEteSec,
    filedAltitudeFt100 = filedAltitudeFt100,
    filedAirspeedKts = filedAirspeedKts,
    subscribed = subscribed,
)

fun List<RouteFix>.toEntities(faFlightId: String): List<RouteFixEntity> =
    mapIndexed { i, fix ->
        RouteFixEntity(
            faFlightId = faFlightId,
            sequence = i,
            name = fix.name,
            lat = fix.lat,
            lon = fix.lon,
            distanceFromOriginNm = fix.distanceFromOriginNm,
            type = fix.type,
        )
    }

fun RouteFixEntity.toDomain() = RouteFix(
    name = name, lat = lat, lon = lon, distanceFromOriginNm = distanceFromOriginNm, type = type
)
