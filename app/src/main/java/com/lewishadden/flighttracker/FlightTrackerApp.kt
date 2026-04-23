package com.lewishadden.flighttracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import javax.inject.Inject

@HiltAndroidApp
class FlightTrackerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        createLocationChannel()
        createUpdatesChannel()
    }

    private fun createLocationChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            LOCATION_CHANNEL_ID,
            "In-flight tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows while tracking your position against the cached flight path."
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }

    private fun createUpdatesChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            UPDATES_CHANNEL_ID,
            "Flight updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Gate, boarding time, delays, and status changes for subscribed flights."
        }
        mgr.createNotificationChannel(channel)
    }

    companion object {
        const val LOCATION_CHANNEL_ID = "flight_location"
        const val UPDATES_CHANNEL_ID = "flight_updates"
    }
}
