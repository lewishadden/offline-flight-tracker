package com.lewishadden.flighttracker.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlightsResponse(
    val flights: List<FlightDto> = emptyList()
)

@Serializable
data class FlightDto(
    @SerialName("fa_flight_id") val faFlightId: String,
    val ident: String? = null,
    @SerialName("ident_icao") val identIcao: String? = null,
    @SerialName("ident_iata") val identIata: String? = null,
    @SerialName("operator") val operatorIcao: String? = null,
    @SerialName("operator_iata") val operatorIata: String? = null,
    @SerialName("flight_number") val flightNumber: String? = null,
    val registration: String? = null,
    @SerialName("aircraft_type") val aircraftType: String? = null,
    val origin: AirportDto? = null,
    val destination: AirportDto? = null,
    @SerialName("scheduled_out") val scheduledOut: String? = null,
    @SerialName("estimated_out") val estimatedOut: String? = null,
    @SerialName("actual_out") val actualOut: String? = null,
    @SerialName("scheduled_off") val scheduledOff: String? = null,
    @SerialName("estimated_off") val estimatedOff: String? = null,
    @SerialName("actual_off") val actualOff: String? = null,
    @SerialName("scheduled_on") val scheduledOn: String? = null,
    @SerialName("estimated_on") val estimatedOn: String? = null,
    @SerialName("actual_on") val actualOn: String? = null,
    @SerialName("scheduled_in") val scheduledIn: String? = null,
    @SerialName("estimated_in") val estimatedIn: String? = null,
    @SerialName("actual_in") val actualIn: String? = null,
    @SerialName("gate_origin") val gateOrigin: String? = null,
    @SerialName("gate_destination") val gateDestination: String? = null,
    @SerialName("terminal_origin") val terminalOrigin: String? = null,
    @SerialName("terminal_destination") val terminalDestination: String? = null,
    @SerialName("baggage_claim") val baggageClaim: String? = null,
    @SerialName("departure_delay") val departureDelaySec: Long? = null,
    @SerialName("arrival_delay") val arrivalDelaySec: Long? = null,
    val status: String? = null,
    val progress_percent: Int? = null,
    val cancelled: Boolean? = null,
    val diverted: Boolean? = null,
    @SerialName("route_distance") val routeDistance: Int? = null,
    @SerialName("filed_ete") val filedEte: Int? = null,
    @SerialName("filed_altitude") val filedAltitude: Int? = null,
    @SerialName("filed_airspeed") val filedAirspeed: Int? = null,
    @SerialName("route") val filedRoute: String? = null,
)

@Serializable
data class AirportDto(
    val code: String? = null,
    @SerialName("code_icao") val codeIcao: String? = null,
    @SerialName("code_iata") val codeIata: String? = null,
    val name: String? = null,
    val city: String? = null,
    @SerialName("airport_info_url") val airportInfoUrl: String? = null,
    val timezone: String? = null,
)

@Serializable
data class AirportInfoDto(
    @SerialName("airport_code") val airportCode: String? = null,
    @SerialName("code_icao") val codeIcao: String? = null,
    @SerialName("code_iata") val codeIata: String? = null,
    val name: String? = null,
    val city: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val timezone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Int? = null,
    @SerialName("wiki_url") val wikiUrl: String? = null,
)

/**
 * Wraps the various airport-flight list endpoints
 * (`/airports/{id}/flights/scheduled_departures` etc). AeroAPI uses different
 * keys depending on the endpoint, so this DTO accepts any of them and the
 * caller picks via [items].
 */
@Serializable
data class AirportFlightsResponse(
    val scheduled: List<FlightDto>? = null,
    val flights: List<FlightDto>? = null,
    val arrivals: List<FlightDto>? = null,
    val departures: List<FlightDto>? = null,
    @SerialName("scheduled_departures") val scheduledDepartures: List<FlightDto>? = null,
    @SerialName("scheduled_arrivals") val scheduledArrivals: List<FlightDto>? = null,
) {
    val items: List<FlightDto>
        get() = scheduledDepartures
            ?: scheduledArrivals
            ?: scheduled
            ?: departures
            ?: arrivals
            ?: flights
            ?: emptyList()
}

/**
 * AeroAPI's `/airports/{id}/weather/metar` returns a single METAR object
 * directly (no wrapper). This DTO is defensive though — it accepts EITHER
 * a `reports`-wrapped response OR a flat single-report shape, since the
 * exact schema isn't always consistent across API versions / tiers.
 */
@Serializable
data class MetarResponse(
    // Wrapped variant
    val reports: List<MetarReportDto>? = null,

    // Direct variant — same fields as MetarReportDto, hoisted to top level
    @SerialName("airport_code") val airportCode: String? = null,
    @SerialName("raw_data") val rawData: String? = null,
    val time: String? = null,
    val temperature: Int? = null,
    @SerialName("temp_air") val tempAir: Int? = null,
    val pressure: Int? = null,
    @SerialName("pressure_units") val pressureUnits: String? = null,
    @SerialName("wind_friendly") val windFriendly: String? = null,
    @SerialName("visibility_friendly") val visibilityFriendly: String? = null,
    val conditions: String? = null,
    @SerialName("cloud_friendly") val cloudFriendly: String? = null,
    @SerialName("clouds_friendly") val cloudsFriendly: String? = null,
) {
    /** Picks whichever shape the API actually returned. */
    fun firstReport(): MetarReportDto? {
        reports?.firstOrNull()?.let { return it }
        // Direct-variant detection: any meaningful field present.
        if (rawData == null && temperature == null && tempAir == null &&
            windFriendly == null && conditions == null && pressure == null) {
            return null
        }
        return MetarReportDto(
            airportCode = airportCode,
            rawData = rawData,
            time = time,
            temperature = temperature ?: tempAir,
            pressure = pressure,
            pressureUnits = pressureUnits,
            windFriendly = windFriendly,
            visibilityFriendly = visibilityFriendly,
            conditions = conditions,
            cloudsFriendly = cloudFriendly ?: cloudsFriendly,
        )
    }
}

@Serializable
data class MetarReportDto(
    @SerialName("airport_code") val airportCode: String? = null,
    @SerialName("raw_data") val rawData: String? = null,
    val time: String? = null,
    // AeroAPI returns Celsius temperature as either `temperature` (older /
    // some endpoints) or `temp_air` (current schema).
    val temperature: Int? = null,
    @SerialName("temp_air") val tempAir: Int? = null,
    val pressure: Int? = null,
    @SerialName("pressure_units") val pressureUnits: String? = null,
    @SerialName("wind_friendly") val windFriendly: String? = null,
    @SerialName("visibility_friendly") val visibilityFriendly: String? = null,
    val conditions: String? = null,
    // AeroAPI uses `cloud_friendly` (singular).
    @SerialName("cloud_friendly") val cloudFriendly: String? = null,
    @SerialName("clouds_friendly") val cloudsFriendly: String? = null,
) {
    val effectiveTemperature: Int? get() = temperature ?: tempAir
    val effectiveCloudsFriendly: String? get() = cloudFriendly ?: cloudsFriendly
}

@Serializable
data class RouteResponse(
    @SerialName("route_distance") val routeDistance: String? = null,
    val fixes: List<RouteFixDto> = emptyList()
)

@Serializable
data class RouteFixDto(
    val name: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerialName("distance_from_origin") val distanceFromOrigin: Double? = null,
    @SerialName("distance_this_leg") val distanceThisLeg: Double? = null,
    @SerialName("outbound_course") val outboundCourse: Double? = null,
    val type: String? = null,
)

@Serializable
data class AccountUsageDto(
    @SerialName("total_cost") val totalCost: Double? = null,
    @SerialName("plan_cap") val planCap: Double? = null,
    @SerialName("plan_overage_enabled") val planOverageEnabled: Boolean? = null,
    @SerialName("period_start") val periodStart: String? = null,
    @SerialName("period_end") val periodEnd: String? = null,
)

@Serializable
data class TrackResponse(
    val positions: List<PositionDto> = emptyList()
)

/**
 * Wraps the `/flights/{id}/position` response. AeroAPI returns the most
 * recent position under `last_position` plus extra context fields we don't use.
 */
@Serializable
data class FlightPositionResponse(
    @SerialName("last_position") val lastPosition: PositionDto? = null,
    val ident: String? = null,
)

@Serializable
data class PositionDto(
    @SerialName("fa_flight_id") val faFlightId: String? = null,
    val altitude: Int? = null,
    @SerialName("altitude_change") val altitudeChange: String? = null,
    val groundspeed: Int? = null,
    val heading: Int? = null,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    @SerialName("update_type") val updateType: String? = null,
)
