package com.lewishadden.flighttracker.ui.subscribed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.model.Flight
import com.lewishadden.flighttracker.notify.FlightSubscriptionScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchedFlightsViewModel @Inject constructor(
    repo: FlightRepository,
    private val scheduler: FlightSubscriptionScheduler,
) : ViewModel() {

    val watched: StateFlow<List<Flight>> = repo.observeSubscribedFlights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unsubscribe(faFlightId: String) {
        viewModelScope.launch { scheduler.unsubscribe(faFlightId) }
    }
}
