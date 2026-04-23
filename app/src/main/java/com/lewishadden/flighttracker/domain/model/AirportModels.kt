package com.lewishadden.flighttracker.domain.model

import java.time.Instant

/** Rich airport metadata returned by the airport-detail endpoint. */
data class AirportSummary(
    val code: String,
    val codeIcao: String?,
    val codeIata: String?,
    val name: String?,
    val city: String?,
    val countryCode: String?,
    val timezone: String?,
    val lat: Double?,
    val lon: Double?,
    val elevationFt: Int?,
    val wikiUrl: String?,
)

/** Decoded current weather report (METAR) for an airport. */
data class AirportWeather(
    val raw: String?,
    val time: Instant?,
    val temperatureC: Int?,
    val pressure: Int?,
    val pressureUnits: String?,
    val wind: String?,
    val visibility: String?,
    val conditions: String?,
    val clouds: String?,
)
