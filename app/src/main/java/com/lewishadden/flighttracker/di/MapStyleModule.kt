package com.lewishadden.flighttracker.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MapStyleUrl

@Module
@InstallIn(SingletonComponent::class)
object MapStyleModule {

    /**
     * OpenFreeMap's liberty style — free, no token required, OpenMapTiles schema.
     * https://openfreemap.org
     * If it goes down, swap for any other MapLibre-compatible style URL.
     */
    @Provides
    @Singleton
    @MapStyleUrl
    fun styleUrl(): String = "https://tiles.openfreemap.org/styles/liberty"
}
