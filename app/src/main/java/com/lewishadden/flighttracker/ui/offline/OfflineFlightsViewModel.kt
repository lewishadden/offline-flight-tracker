package com.lewishadden.flighttracker.ui.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.db.entities.OfflineRegionEntity
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.map.OfflineMapManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OfflineEntry(
    val region: OfflineRegionEntity,
    val flight: Flight?,
)

@HiltViewModel
class OfflineFlightsViewModel @Inject constructor(
    private val repo: FlightRepository,
    private val offlineManager: OfflineMapManager,
) : ViewModel() {

    val entries: StateFlow<List<OfflineEntry>> = combine(
        repo.observeOfflineRegions(),
        repo.observeCachedFlights(),
    ) { regions, flights ->
        val byId = flights.associateBy { it.faFlightId }
        regions.map { OfflineEntry(region = it, flight = byId[it.faFlightId]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(faFlightId: String) {
        viewModelScope.launch { offlineManager.deleteFlightRegion(faFlightId) }
    }
}
