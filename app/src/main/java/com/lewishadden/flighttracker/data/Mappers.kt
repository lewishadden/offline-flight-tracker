package com.lewishadden.flighttracker.data

import com.lewishadden.flighttracker.data.api.dto.AirportDto
import com.lewishadden.flighttracker.data.api.dto.FlightDto
import com.lewishadden.flighttracker.data.api.dto.PositionDto
import com.lewishadden.flighttracker.data.api.dto.RouteFixDto
import com.lewishadden.flighttracker.domain.model.Airport
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.domain.model.RouteFix
import com.lewishadden.flighttracker.domain.model.TrackPoint
import java.time.Instant

private fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun AirportDto.toDomain() = Airport(
    icao = codeIcao ?: code,
    iata = codeIata,
    name = name,
    city = city,
    timezone = timezone,
)

fun FlightDto.toDomain(): Flight = Flight(
    faFlightId = faFlightId,
    ident = ident ?: identIcao ?: identIata ?: faFlightId,
    operatorIata = operatorIata,
    flightNumber = flightNumber,
    aircraftType = aircraftType,
    registration = registration,
    origin = origin?.toDomain() ?: Airport(null, null, null, null, null),
    destination = destination?.toDomain() ?: Airport(null, null, null, null, null),
    scheduledOut = scheduledOut.toInstantOrNull(),
    estimatedOut = estimatedOut.toInstantOrNull(),
    actualOut = actualOut.toInstantOrNull(),
    scheduledOff = scheduledOff.toInstantOrNull(),
    estimatedOff = estimatedOff.toInstantOrNull(),
    actualOff = actualOff.toInstantOrNull(),
    scheduledOn = scheduledOn.toInstantOrNull(),
    estimatedOn = estimatedOn.toInstantOrNull(),
    actualOn = actualOn.toInstantOrNull(),
    scheduledIn = scheduledIn.toInstantOrNull(),
    estimatedIn = estimatedIn.toInstantOrNull(),
    actualIn = actualIn.toInstantOrNull(),
    gateOrigin = gateOrigin,
    gateDestination = gateDestination,
    terminalOrigin = terminalOrigin,
    terminalDestination = terminalDestination,
    baggageClaim = baggageClaim,
    departureDelayMin = departureDelaySec?.let { it / 60 },
    arrivalDelayMin = arrivalDelaySec?.let { it / 60 },
    status = status,
    progressPercent = progress_percent,
    cancelled = cancelled == true,
    diverted = diverted == true,
    routeDistanceNm = routeDistance,
    filedEteSec = filedEte,
    filedAltitudeFt100 = filedAltitude,
    filedAirspeedKts = filedAirspeed,
)

fun RouteFixDto.toDomain() = RouteFix(
    name = name,
    lat = latitude,
    lon = longitude,
    distanceFromOriginNm = distanceFromOrigin,
    type = type,
)

fun PositionDto.toDomain(): TrackPoint? {
    val ts = timestamp.toInstantOrNull() ?: return null
    return TrackPoint(
        lat = latitude,
        lon = longitude,
        altitudeFt100 = altitude,
        groundspeedKts = groundspeed,
        headingDeg = heading,
        timestamp = ts,
    )
}
