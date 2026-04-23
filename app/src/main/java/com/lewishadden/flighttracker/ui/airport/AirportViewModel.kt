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

            // Info needs to complete first so we have ICAO + lat/lon for the
            // weather lookup. Departures and arrivals can fan out alongside.
            val infoDeferred = async { repo.getInfo(airportCode) }
            val depsDeferred = async { repo.getScheduledDepartures(airportCode) }
            val arrsDeferred = async { repo.getScheduledArrivals(airportCode) }

            val info = infoDeferred.await()
            val weatherDeferred = async {
                repo.getWeather(
                    icaoCode = info?.codeIcao ?: airportCode.takeIf { it.length == 4 },
                    lat = info?.lat,
                    lon = info?.lon,
                )
            }

            val (departures, arrivals, weather) = listOf(
                depsDeferred,
                arrsDeferred,
                weatherDeferred,
            ).awaitAll().let { Triple(it[0], it[1], it[2]) }

            @Suppress("UNCHECKED_CAST")
            _ui.value = AirportUiState(
                loading = false,
                info = info,
                weather = weather as AirportWeather?,
                departures = departures as List<Flight>,
                arrivals = arrivals as List<Flight>,
                error = if (info == null && weather == null &&
                    (departures as List<*>).isEmpty() && (arrivals as List<*>).isEmpty()
                ) "Couldn't load airport data. Check your connection and try again." else null,
            )
        }
    }
}
