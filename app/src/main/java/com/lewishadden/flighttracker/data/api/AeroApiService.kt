package com.lewishadden.flighttracker.data.api

import com.lewishadden.flighttracker.data.api.dto.AccountUsageDto
import com.lewishadden.flighttracker.data.api.dto.AirportInfoDto
import com.lewishadden.flighttracker.data.api.dto.FlightPositionResponse
import com.lewishadden.flighttracker.data.api.dto.FlightsResponse
import com.lewishadden.flighttracker.data.api.dto.RouteResponse
import com.lewishadden.flighttracker.data.api.dto.TrackResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AeroApiService {

    @GET("flights/{ident}")
    suspend fun getFlightsByIdent(
        @Path("ident") ident: String,
        @Query("max_pages") maxPages: Int = 1,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
    ): FlightsResponse

    @GET("flights/{faFlightId}/route")
    suspend fun getRoute(
        @Path("faFlightId") faFlightId: String
    ): RouteResponse

    @GET("flights/{faFlightId}/track")
    suspend fun getTrack(
        @Path("faFlightId") faFlightId: String,
        @Query("include_estimated_positions") includeEstimated: Boolean = true
    ): TrackResponse

    /**
     * Last known position of the aircraft. Lighter than the full track —
     * one position object, refreshed every minute or so by AeroAPI's data
     * pipeline. Used for live in-flight tracking.
     */
    @GET("flights/{faFlightId}/position")
    suspend fun getPosition(
        @Path("faFlightId") faFlightId: String
    ): FlightPositionResponse

    @GET("airports/{id}")
    suspend fun getAirport(
        @Path("id") id: String
    ): AirportInfoDto

    @GET("account/usage")
    suspend fun getAccountUsage(): AccountUsageDto
}
