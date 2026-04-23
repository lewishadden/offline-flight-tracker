package com.lewishadden.flighttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lewishadden.flighttracker.ui.FlightTrackerNavHost
import com.lewishadden.flighttracker.ui.theme.FlightTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlightTrackerTheme {
                FlightTrackerNavHost()
            }
        }
    }
}
