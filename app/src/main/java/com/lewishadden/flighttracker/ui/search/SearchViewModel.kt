package com.lewishadden.flighttracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.prefs.UserPreferences
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.domain.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Best-available departure time for ordering purposes — actual when known,
 * else estimated, else scheduled. Flights with no time at all sort last.
 */
private fun Flight.departureSortKey(): Instant =
    actualOut ?: estimatedOut ?: scheduledOut ?: Instant.MAX

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<Flight> = emptyList(),
    val cached: List<Flight> = emptyList(),
    val recents: List<String> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: FlightRepository,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Combined flow: cached flights + persisted search history.
            combine(
                repo.observeCachedFlights(),
                prefs.settings,
            ) { cached, settings ->
                _state.value.copy(
                    cached = cached.sortedBy { it.departureSortKey() },
                    recents = settings.searchHistory,
                )
            }.collect { _state.value = it }
        }
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q, error = null)
    }

    fun searchTerm(term: String) {
        _state.value = _state.value.copy(query = term)
        search()
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repo.searchByIdent(q) }
                .onSuccess { results ->
                    val sorted = results.sortedBy { it.departureSortKey() }
                    _state.value = _state.value.copy(loading = false, results = sorted)
                    if (sorted.isEmpty()) {
                        _state.value = _state.value.copy(error = "No flights found for \"$q\"")
                    } else {
                        // Only persist successful, non-empty searches so the
                        // recents row doesn't fill with typos.
                        prefs.addSearchHistory(q)
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

    fun clearRecents() {
        viewModelScope.launch { prefs.clearSearchHistory() }
    }

    fun prefetch(faFlightId: String) {
        viewModelScope.launch {
            runCatching { repo.refreshFlight(faFlightId) }
        }
    }
}
