package com.lewishadden.flighttracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class FlightTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        createLocationChannel()
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

    companion object {
        const val LOCATION_CHANNEL_ID = "flight_location"
    }
}
