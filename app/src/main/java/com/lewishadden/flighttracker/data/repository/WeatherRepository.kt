package com.lewishadden.flighttracker.data.repository

import com.lewishadden.flighttracker.data.api.AviationWeatherApi
import com.lewishadden.flighttracker.data.api.NoaaMetarDto
import com.lewishadden.flighttracker.data.api.OpenMeteoApi
import com.lewishadden.flighttracker.domain.model.AirportWeather
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Lightweight current-weather snapshot for an arbitrary lat/lon point. */
data class PointWeather(
    val lat: Double,
    val lon: Double,
    val temperatureC: Int?,
    val wind: String?,
    val conditions: String?,
    val visibilityKm: Double?,
    val cloudCoverPct: Int?,
)

/**
 * Aggregates two free weather sources:
 *   - NOAA Aviation Weather Center (METAR for ICAO airports — proper aviation data)
 *   - Open-Meteo (current conditions at any lat/lon — fallback + en-route)
 *
 * For airports we prefer NOAA because METAR is the data pilots and travelers
 * actually use. When the airport has no ICAO code or NOAA returns nothing
 * (some smaller airports don't report METAR), we fall back to Open-Meteo
 * driven by the cached lat/lon so the user still sees a temperature and wind.
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val aviation: AviationWeatherApi,
    private val openMeteo: OpenMeteoApi,
) {

    /**
     * Best-effort airport weather. Tries NOAA METAR first (ICAO required),
     * falls back to Open-Meteo's lat/lon-based current conditions.
     */
    suspend fun getAirportWeather(
        icaoCode: String?,
        lat: Double?,
        lon: Double?,
    ): AirportWeather? {
        // Primary path: NOAA METAR.
        if (!icaoCode.isNullOrBlank()) {
            val noaa = runCatching { aviation.getMetars(icaoCode.trim().uppercase()) }
                .getOrNull()
                ?.firstOrNull()
            if (noaa != null) return noaa.toDomain()
        }
        // Fallback: Open-Meteo at the airport's lat/lon.
        if (lat != null && lon != null) {
            return runCatching { openMeteo.getCurrent(lat, lon) }
                .getOrNull()
                ?.let { it.current?.toAirportWeather(it.timezone) }
        }
        return null
    }

    /** Current conditions at an arbitrary geographic point (used for en-route weather). */
    suspend fun getPointWeather(lat: Double, lon: Double): PointWeather? {
        val resp = runCatching { openMeteo.getCurrent(lat, lon) }.getOrNull() ?: return null
        val cur = resp.current ?: return null
        return PointWeather(
            lat = lat,
            lon = lon,
            temperatureC = cur.temperatureC?.roundToInt(),
            wind = cur.formatWind(),
            conditions = cur.weatherCode?.let { weatherCodeToText(it) },
            visibilityKm = cur.visibility?.let { it / 1000.0 },
            cloudCoverPct = cur.cloudCoverPct,
        )
    }
}

private fun NoaaMetarDto.toDomain(): AirportWeather {
    val timeInstant = obsTime?.let { Instant.ofEpochSecond(it) }
        ?: reportTime?.let { runCatching { Instant.parse(it) }.getOrNull() }

    val windText = when {
        windDirDeg == null && windSpeedKt == null -> null
        windSpeedKt == 0 -> "Calm"
        else -> buildString {
            append("Wind ")
            if (windDirDeg != null) append("%03d°".format(windDirDeg)) else append("VRB")
            append(" at ")
            append(windSpeedKt ?: 0)
            append(" kt")
            windGustKt?.let { append(", gusts $it kt") }
        }
    }

    val cloudsText = clouds.takeIf { it.isNotEmpty() }?.joinToString(", ") { c ->
        val cover = when (c.cover) {
            "SKC", "CLR", "NCD", "NSC" -> "Clear"
            "FEW" -> "Few clouds"
            "SCT" -> "Scattered"
            "BKN" -> "Broken"
            "OVC" -> "Overcast"
            else -> c.cover ?: "—"
        }
        if (c.base != null) "$cover at ${c.base} ft" else cover
    }

    return AirportWeather(
        raw = rawOb,
        time = timeInstant,
        temperatureC = tempC?.roundToInt(),
        pressure = altimeterMb?.roundToInt(),
        pressureUnits = if (altimeterMb != null) "mb" else null,
        wind = windText,
        visibility = visibility?.let { v -> if (v.endsWith("+") || v.contains("SM")) v else "$v SM" },
        conditions = wxString,
        clouds = cloudsText,
    )
}

private fun OpenMeteoCurrentExt(): Unit = Unit
private fun com.lewishadden.flighttracker.data.api.OpenMeteoCurrent.formatWind(): String? {
    if (windSpeedKt == null && windDirectionDeg == null) return null
    val parts = mutableListOf<String>()
    parts.add("Wind")
    windDirectionDeg?.let { parts.add("%03d°".format(it)) }
    if (windSpeedKt != null) {
        parts.add("at ${windSpeedKt.roundToInt()} kt")
    }
    windGustsKt?.let { gust ->
        if (gust > (windSpeedKt ?: 0.0)) parts.add(", gusts ${gust.roundToInt()} kt")
    }
    return parts.joinToString(" ")
}

@Suppress("UNUSED_PARAMETER")
private fun com.lewishadden.flighttracker.data.api.OpenMeteoCurrent.toAirportWeather(timezone: String?): AirportWeather =
    AirportWeather(
        raw = null,
        time = time?.let { runCatching { Instant.parse("${it}:00Z") }.getOrNull() },
        temperatureC = temperatureC?.roundToInt(),
        pressure = null,
        pressureUnits = null,
        wind = formatWind(),
        visibility = visibility?.let { v -> "%.1f km".format(v / 1000.0) },
        conditions = weatherCode?.let { weatherCodeToText(it) },
        clouds = cloudCoverPct?.let { "Cloud cover $it%" },
    )

/**
 * Translates Open-Meteo / WMO weather codes into a plain-English summary.
 * Reference: https://open-meteo.com/en/docs (WMO Weather interpretation codes).
 */
internal fun weatherCodeToText(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51 -> "Light drizzle"
    53 -> "Moderate drizzle"
    55 -> "Dense drizzle"
    56, 57 -> "Freezing drizzle"
    61 -> "Light rain"
    63 -> "Moderate rain"
    65 -> "Heavy rain"
    66, 67 -> "Freezing rain"
    71 -> "Light snow"
    73 -> "Moderate snow"
    75 -> "Heavy snow"
    77 -> "Snow grains"
    80 -> "Light rain showers"
    81 -> "Rain showers"
    82 -> "Heavy rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Unknown ($code)"
}
