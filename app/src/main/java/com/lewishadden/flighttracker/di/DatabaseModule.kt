package com.lewishadden.flighttracker.di

import android.content.Context
import androidx.room.Room
import com.lewishadden.flighttracker.data.db.AppDatabase
import com.lewishadden.flighttracker.data.db.FlightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "flighttracker.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun flightDao(db: AppDatabase): FlightDao = db.flightDao()
}
