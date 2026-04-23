package com.lewishadden.flighttracker.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo current-weather endpoint. Free, no key, generous rate limits.
 * Docs: https://open-meteo.com/en/docs
 *
 * We use this for two scenarios:
 *  1. Airport weather fallback when NOAA doesn't have METAR for that ICAO.
 *  2. En-route current conditions sampled at points along the flight path.
 */
interface OpenMeteoApi {

    @GET("forecast")
    suspend fun getCurrent(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "temperature_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m,weather_code,visibility,cloud_cover",
        @Query("wind_speed_unit") windSpeedUnit: String = "kn",
        @Query("temperature_unit") tempUnit: String = "celsius",
        @Query("timezone") timezone: String = "UTC",
    ): OpenMeteoResponse
}

@Serializable
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val current: OpenMeteoCurrent? = null,
)

@Serializable
data class OpenMeteoCurrent(
    val time: String? = null,
    @SerialName("temperature_2m") val temperatureC: Double? = null,
    @SerialName("wind_speed_10m") val windSpeedKt: Double? = null,
    @SerialName("wind_direction_10m") val windDirectionDeg: Int? = null,
    @SerialName("wind_gusts_10m") val windGustsKt: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    /** Visibility in metres. */
    val visibility: Double? = null,
    /** Cloud cover percentage 0–100. */
    @SerialName("cloud_cover") val cloudCoverPct: Int? = null,
)
