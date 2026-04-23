package com.lewishadden.flighttracker.data.repository

import com.lewishadden.flighttracker.data.api.AeroApiService
import com.lewishadden.flighttracker.data.toDomain
import com.lewishadden.flighttracker.domain.model.AirportSummary
import com.lewishadden.flighttracker.domain.model.AirportWeather
import com.lewishadden.flighttracker.domain.model.Flight
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirportRepository @Inject constructor(
    private val api: AeroApiService,
) {

    /**
     * Look up rich airport metadata by IATA or ICAO code.
     * Returns null if the API call fails (e.g. unknown code, offline).
     */
    suspend fun getInfo(code: String): AirportSummary? {
        val dto = runCatching { api.getAirport(code) }.getOrNull() ?: return null
        return AirportSummary(
            code = dto.codeIcao ?: dto.codeIata ?: dto.airportCode ?: code,
            codeIcao = dto.codeIcao,
            codeIata = dto.codeIata,
            name = dto.name,
            city = dto.city,
            countryCode = dto.countryCode,
            timezone = dto.timezone,
            lat = dto.latitude,
            lon = dto.longitude,
            elevationFt = dto.elevation,
            wikiUrl = dto.wikiUrl,
        )
    }

    /** Most recent METAR weather report for the airport, or null if unavailable. */
    suspend fun getWeather(code: String): AirportWeather? {
        val resp = runCatching { api.getMetar(code) }.getOrNull() ?: return null
        // AeroAPI returns the METAR fields at top level (not wrapped in
        // `reports`). The DTO accepts both shapes; firstReport() picks one.
        val first = resp.firstReport() ?: return null
        return AirportWeather(
            raw = first.rawData,
            time = first.time?.let { runCatching { Instant.parse(it) }.getOrNull() },
            temperatureC = first.effectiveTemperature,
            pressure = first.pressure,
            pressureUnits = first.pressureUnits,
            wind = first.windFriendly,
            visibility = first.visibilityFriendly,
            conditions = first.conditions,
            clouds = first.effectiveCloudsFriendly,
        )
    }

    suspend fun getScheduledDepartures(code: String): List<Flight> {
        val resp = runCatching { api.getScheduledDepartures(code) }.getOrNull() ?: return emptyList()
        return resp.items.map { it.toDomain() }
    }

    suspend fun getScheduledArrivals(code: String): List<Flight> {
        val resp = runCatching { api.getScheduledArrivals(code) }.getOrNull() ?: return emptyList()
        return resp.items.map { it.toDomain() }
    }
}
