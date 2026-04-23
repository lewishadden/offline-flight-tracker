package com.lewishadden.flighttracker.ui.predownload

import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.di.MapStyleUrl
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.domain.model.RouteFix
import com.lewishadden.flighttracker.map.OfflineMapManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreDownloadUiState(
    val flight: Flight? = null,
    val route: List<RouteFix> = emptyList(),
    val downloading: Boolean = false,
    val percent: Float = 0f,
    val bytes: Long = 0L,
    val tilesDone: Long = 0L,
    val tilesTotal: Long = 0L,
    val done: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PreDownloadViewModel @Inject constructor(
    private val repo: FlightRepository,
    private val mapManager: OfflineMapManager,
    @MapStyleUrl private val styleUrl: String,
    savedState: SavedStateHandle,
) : ViewModel() {

    val faFlightId: String = checkNotNull(savedState["faFlightId"])

    private val _state = MutableStateFlow(PreDownloadUiState())
    val state: StateFlow<PreDownloadUiState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            repo.observeFlight(faFlightId).collect { fwr ->
                fwr ?: return@collect
                _state.value = _state.value.copy(flight = fwr.flight, route = fwr.route)
            }
        }
    }

    fun startDownload() {
        val s = _state.value
        val flight = s.flight ?: return
        if (s.downloading) return
        _state.value = s.copy(downloading = true, percent = 0f, error = null, done = false)

        downloadJob = viewModelScope.launch {
            val pixelRatio = Resources.getSystem().displayMetrics.density
            mapManager.downloadForFlight(
                faFlightId = flight.faFlightId,
                styleUrl = styleUrl,
                routeFixes = s.route,
                originLat = flight.origin.lat,
                originLon = flight.origin.lon,
                destLat = flight.destination.lat,
                destLon = flight.destination.lon,
                pixelRatio = pixelRatio,
            ).collect { p ->
                _state.value = _state.value.copy(
                    percent = p.percent,
                    bytes = p.completedBytes,
                    tilesDone = p.completedTiles,
                    tilesTotal = p.requiredTiles,
                    done = p.done && p.error == null,
                    downloading = !p.done,
                    error = p.error,
                )
            }
        }
    }

    fun cancel() {
        downloadJob?.cancel()
        _state.value = _state.value.copy(downloading = false)
    }
}
