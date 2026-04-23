package com.lewishadden.flighttracker.ui.airport

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.repository.AirportRepository
import com.lewishadden.flighttracker.domain.model.AirportSummary
import com.lewishadden.flighttracker.domain.model.AirportWeather
import com.lewishadden.flighttracker.domain.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AirportUiState(
    val loading: Boolean = true,
    val info: AirportSummary? = null,
    val weather: AirportWeather? = null,
    val departures: List<Flight> = emptyList(),
    val arrivals: List<Flight> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class AirportViewModel @Inject constructor(
    private val repo: AirportRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    val airportCode: String = checkNotNull(savedState["code"])

    private val _ui = MutableStateFlow(AirportUiState())
    val ui: StateFlow<AirportUiState> = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            // All four fetches are independent — fan out concurrently so the
            // screen comes up at the speed of the slowest call rather than the
            // sum of all of them.
            val results = listOf(
                async { repo.getInfo(airportCode) },
                async { repo.getWeather(airportCode) },
                async { repo.getScheduledDepartures(airportCode) },
                async { repo.getScheduledArrivals(airportCode) },
            ).awaitAll()

            @Suppress("UNCHECKED_CAST")
            val info = results[0] as AirportSummary?
            @Suppress("UNCHECKED_CAST")
            val weather = results[1] as AirportWeather?
            @Suppress("UNCHECKED_CAST")
            val departures = results[2] as List<Flight>
            @Suppress("UNCHECKED_CAST")
            val arrivals = results[3] as List<Flight>

            _ui.value = AirportUiState(
                loading = false,
                info = info,
                weather = weather,
                departures = departures,
                arrivals = arrivals,
                error = if (info == null && weather == null && departures.isEmpty() && arrivals.isEmpty()) {
                    "Couldn't load airport data. Check your connection and try again."
                } else null,
            )
        }
    }
}
