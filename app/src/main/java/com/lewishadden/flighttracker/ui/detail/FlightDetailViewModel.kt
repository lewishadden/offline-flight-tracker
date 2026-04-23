package com.lewishadden.flighttracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.db.FlightDao
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.data.repository.FlightWithRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val refreshing: Boolean = false,
    val data: FlightWithRoute? = null,
    val hasOfflineRegion: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    private val repo: FlightRepository,
    private val dao: FlightDao,
    savedState: SavedStateHandle,
) : ViewModel() {

    val faFlightId: String = checkNotNull(savedState["faFlightId"])

    private val _ui = MutableStateFlow(DetailUiState())
    val ui: StateFlow<DetailUiState> = _ui.asStateFlow()

    val hasOfflineRegion: StateFlow<Boolean> = dao.observeOfflineRegion(faFlightId)
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            repo.observeFlight(faFlightId).collect { data ->
                _ui.value = _ui.value.copy(data = data)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(refreshing = true, error = null)
            runCatching { repo.refreshFlight(faFlightId) }
                .onFailure {
                    _ui.value = _ui.value.copy(error = it.message ?: "Refresh failed")
                }
            _ui.value = _ui.value.copy(refreshing = false)
        }
    }
}
