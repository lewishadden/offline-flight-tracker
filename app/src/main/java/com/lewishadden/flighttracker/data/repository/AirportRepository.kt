package com.lewishadden.flighttracker.data.repository

import com.lewishadden.flighttracker.data.api.AeroApiService
import com.lewishadden.flighttracker.data.toDomain
import com.lewishadden.flighttracker.domain.model.AirportSummary
import com.lewishadden.flighttracker.domain.model.AirportWeather
import com.lewishadden.flighttracker.domain.model.Flight
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirportRepository @Inject constructor(
    private val api: AeroApiService,
    private val weather: WeatherRepository,
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

    /**
     * Most recent weather for the airport. Tries NOAA METAR first (proper
     * aviation data — free, no key) using the ICAO code; falls back to
     * Open-Meteo's lat/lon-based current conditions when METAR isn't
     * available (some smaller airports or non-ICAO inputs).
     *
     * Pass lat/lon when known (e.g. from a prior [getInfo] call) to enable
     * the fallback path.
     */
    suspend fun getWeather(
        icaoCode: String?,
        lat: Double? = null,
        lon: Double? = null,
    ): AirportWeather? = weather.getAirportWeather(icaoCode, lat, lon)

    suspend fun getScheduledDepartures(code: String): List<Flight> {
        val resp = runCatching { api.getScheduledDepartures(code) }.getOrNull() ?: return emptyList()
        return resp.items.map { it.toDomain() }
    }

    suspend fun getScheduledArrivals(code: String): List<Flight> {
        val resp = runCatching { api.getScheduledArrivals(code) }.getOrNull() ?: return emptyList()
        return resp.items.map { it.toDomain() }
    }
}
