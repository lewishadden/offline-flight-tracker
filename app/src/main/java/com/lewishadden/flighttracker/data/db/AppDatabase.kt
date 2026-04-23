package com.lewishadden.flighttracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lewishadden.flighttracker.data.db.entities.FlightEntity
import com.lewishadden.flighttracker.data.db.entities.OfflineRegionEntity
import com.lewishadden.flighttracker.data.db.entities.RouteFixEntity

@Database(
    entities = [FlightEntity::class, RouteFixEntity::class, OfflineRegionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
}
