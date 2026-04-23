package com.lewishadden.flighttracker.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Free NOAA Aviation Weather Center METAR endpoint — no key required.
 * Docs: https://aviationweather.gov/data/api/
 *
 * Returns proper aviation weather observations for any ICAO-coded airport.
 */
interface AviationWeatherApi {

    @GET("data/metar")
    suspend fun getMetars(
        @Query("ids") ids: String,
        @Query("format") format: String = "json",
        @Query("taf") taf: Boolean = false,
        @Query("hours") hours: Int = 2,
    ): List<NoaaMetarDto>
}

@Serializable
data class NoaaMetarDto(
    @SerialName("icaoId") val icaoId: String? = null,
    @SerialName("rawOb") val rawOb: String? = null,
    @SerialName("reportTime") val reportTime: String? = null,
    @SerialName("obsTime") val obsTime: Long? = null,
    @SerialName("temp") val tempC: Double? = null,
    @SerialName("dewp") val dewpointC: Double? = null,
    @SerialName("wdir") val windDirDeg: Int? = null,
    @SerialName("wspd") val windSpeedKt: Int? = null,
    @SerialName("wgst") val windGustKt: Int? = null,
    @SerialName("visib") val visibility: String? = null,
    @SerialName("altim") val altimeterMb: Double? = null,
    @SerialName("wxString") val wxString: String? = null,
    @SerialName("name") val name: String? = null,
    val clouds: List<NoaaCloudDto> = emptyList(),
)

@Serializable
data class NoaaCloudDto(
    /** SKC, CLR, NCD, FEW, SCT, BKN, OVC, etc. */
    val cover: String? = null,
    /** Cloud base in feet AGL. */
    val base: Int? = null,
)
