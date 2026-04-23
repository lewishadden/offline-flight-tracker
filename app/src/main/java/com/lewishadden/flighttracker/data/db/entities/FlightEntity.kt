package com.lewishadden.flighttracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey val faFlightId: String,
    val ident: String,
    val operatorIata: String?,
    val flightNumber: String?,
    val aircraftType: String?,
    val registration: String?,
    val originIcao: String?,
    val originIata: String?,
    val originName: String?,
    val originCity: String?,
    val originTimezone: String?,
    val originLat: Double?,
    val originLon: Double?,
    val destIcao: String?,
    val destIata: String?,
    val destName: String?,
    val destCity: String?,
    val destTimezone: String?,
    val destLat: Double?,
    val destLon: Double?,
    val scheduledOut: Long?,
    val estimatedOut: Long?,
    val actualOut: Long?,
    val scheduledOff: Long?,
    val estimatedOff: Long?,
    val actualOff: Long?,
    val scheduledOn: Long?,
    val estimatedOn: Long?,
    val actualOn: Long?,
    val scheduledIn: Long?,
    val estimatedIn: Long?,
    val actualIn: Long?,
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
    val lastFetchedEpochMs: Long,
    val subscribed: Boolean = false,
)

@Entity(
    tableName = "route_fixes",
    primaryKeys = ["faFlightId", "sequence"],
    foreignKeys = [
        ForeignKey(
            entity = FlightEntity::class,
            parentColumns = ["faFlightId"],
            childColumns = ["faFlightId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("faFlightId")]
)
data class RouteFixEntity(
    val faFlightId: String,
    val sequence: Int,
    val name: String?,
    val lat: Double,
    val lon: Double,
    val distanceFromOriginNm: Double?,
    val type: String?,
)

@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey val faFlightId: String,
    val maplibreRegionId: Long,
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val minZoom: Double,
    val maxZoom: Double,
    val sizeBytes: Long,
    val completedEpochMs: Long,
)
