package com.lewishadden.flighttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lewishadden.flighttracker.data.prefs.UserPreferences
import com.lewishadden.flighttracker.data.prefs.UserSettings
import com.lewishadden.flighttracker.ui.FlightTrackerNavHost
import com.lewishadden.flighttracker.ui.theme.FlightTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Initial value uses the defaults so first frame doesn't flash —
            // the real persisted theme is collected on the next composition.
            val settings by prefs.settings.collectAsState(initial = UserSettings())
            FlightTrackerTheme(themeMode = settings.theme) {
                FlightTrackerNavHost()
            }
        }
    }
}
