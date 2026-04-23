package com.lewishadden.flighttracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Flight> = emptyList(),
    val cached: List<Flight> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: FlightRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeCachedFlights().collect { cached ->
                _state.value = _state.value.copy(cached = cached)
            }
        }
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q, error = null)
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repo.searchByIdent(q) }
                .onSuccess { results ->
                    _state.value = _state.value.copy(loading = false, results = results)
                    if (results.isEmpty()) {
                        _state.value = _state.value.copy(error = "No flights found for \"$q\"")
                    }
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Search failed",
                    )
                }
        }
    }

    fun prefetch(faFlightId: String) {
        viewModelScope.launch {
            runCatching { repo.refreshFlight(faFlightId) }
        }
    }
}
