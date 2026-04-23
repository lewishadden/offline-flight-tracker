package com.lewishadden.flighttracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lewishadden.flighttracker.data.db.entities.FlightEntity
import com.lewishadden.flighttracker.data.db.entities.OfflineRegionEntity
import com.lewishadden.flighttracker.data.db.entities.RouteFixEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlight(flight: FlightEntity)

    @Query("SELECT * FROM flights WHERE faFlightId = :id")
    suspend fun getFlight(id: String): FlightEntity?

    @Query("SELECT * FROM flights WHERE faFlightId = :id")
    fun observeFlight(id: String): Flow<FlightEntity?>

    @Query("SELECT * FROM flights ORDER BY scheduledOut DESC")
    fun observeAllFlights(): Flow<List<FlightEntity>>

    @Query("UPDATE flights SET subscribed = :subscribed WHERE faFlightId = :id")
    suspend fun setSubscribed(id: String, subscribed: Boolean)

    @Query("SELECT * FROM flights WHERE subscribed = 1")
    suspend fun getSubscribedFlights(): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE subscribed = 1 ORDER BY scheduledOut ASC")
    fun observeSubscribedFlights(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM offline_regions ORDER BY completedEpochMs DESC")
    fun observeOfflineRegions(): Flow<List<OfflineRegionEntity>>

    @Query("SELECT COUNT(*) FROM flights WHERE subscribed = 1")
    suspend fun countSubscribed(): Int

    @Query("DELETE FROM route_fixes WHERE faFlightId = :id")
    suspend fun deleteRouteFor(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixes(fixes: List<RouteFixEntity>)

    @Query("SELECT * FROM route_fixes WHERE faFlightId = :id ORDER BY sequence ASC")
    suspend fun getRoute(id: String): List<RouteFixEntity>

    @Query("SELECT * FROM route_fixes WHERE faFlightId = :id ORDER BY sequence ASC")
    fun observeRoute(id: String): Flow<List<RouteFixEntity>>

    @Transaction
    suspend fun replaceRoute(id: String, fixes: List<RouteFixEntity>) {
        deleteRouteFor(id)
        if (fixes.isNotEmpty()) insertFixes(fixes)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfflineRegion(region: OfflineRegionEntity)

    @Query("SELECT * FROM offline_regions WHERE faFlightId = :id")
    suspend fun getOfflineRegion(id: String): OfflineRegionEntity?

    @Query("SELECT * FROM offline_regions WHERE faFlightId = :id")
    fun observeOfflineRegion(id: String): Flow<OfflineRegionEntity?>

    @Query("DELETE FROM offline_regions WHERE faFlightId = :id")
    suspend fun deleteOfflineRegion(id: String)
}
