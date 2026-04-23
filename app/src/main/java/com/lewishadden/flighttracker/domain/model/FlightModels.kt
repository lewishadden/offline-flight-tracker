package com.lewishadden.flighttracker.domain.model

import java.time.Instant

data class Flight(
    val faFlightId: String,
    val ident: String,
    val operatorIata: String?,
    val flightNumber: String?,
    val aircraftType: String?,
    val origin: Airport,
    val destination: Airport,
    val scheduledOut: Instant?,
    val estimatedOut: Instant?,
    val actualOut: Instant?,
    val scheduledOff: Instant?,
    val estimatedOff: Instant?,
    val actualOff: Instant?,
    val scheduledOn: Instant?,
    val estimatedOn: Instant?,
    val actualOn: Instant?,
    val scheduledIn: Instant?,
    val estimatedIn: Instant?,
    val actualIn: Instant?,
    val gateOrigin: String?,
    val gateDestination: String?,
    val terminalOrigin: String?,
    val terminalDestination: String?,
    val baggageClaim: String?,
    val departureDelayMin: Long?,
    val arrivalDelayMin: Long?,
    val status: String?,
    val progressPercent: Int?,
    val cancelled: Boolean,
    val diverted: Boolean,
    val routeDistanceNm: Int?,
    val filedEteSec: Int?,
    val filedAltitudeFt100: Int?,
    val filedAirspeedKts: Int?,
    val subscribed: Boolean = false,
)

data class Airport(
    val icao: String?,
    val iata: String?,
    val name: String?,
    val city: String?,
    val timezone: String?,
    val lat: Double? = null,
    val lon: Double? = null,
)

data class RouteFix(
    val name: String?,
    val lat: Double,
    val lon: Double,
    val distanceFromOriginNm: Double?,
    val type: String?,
)

data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val altitudeFt100: Int?,
    val groundspeedKts: Int?,
    val headingDeg: Int?,
    val timestamp: Instant,
)
