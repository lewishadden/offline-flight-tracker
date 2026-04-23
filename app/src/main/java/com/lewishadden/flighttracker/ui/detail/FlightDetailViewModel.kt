package com.lewishadden.flighttracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.db.FlightDao
import com.lewishadden.flighttracker.data.prefs.UnitSystem
import com.lewishadden.flighttracker.data.prefs.UserPreferences
import com.lewishadden.flighttracker.data.repository.AircraftPhoto
import com.lewishadden.flighttracker.data.repository.AircraftPhotoRepository
import com.lewishadden.flighttracker.data.repository.FlightRepository
import com.lewishadden.flighttracker.data.repository.FlightWithRoute
import com.lewishadden.flighttracker.notify.FlightSubscriptionScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val refreshing: Boolean = false,
    val data: FlightWithRoute? = null,
    val hasOfflineRegion: Boolean = false,
    val photo: AircraftPhoto? = null,
    val error: String? = null,
)

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    private val repo: FlightRepository,
    private val photoRepo: AircraftPhotoRepository,
    private val dao: FlightDao,
    private val subscriptionScheduler: FlightSubscriptionScheduler,
    prefs: UserPreferences,
    savedState: SavedStateHandle,
) : ViewModel() {

    val units: StateFlow<UnitSystem> = prefs.settings
        .map { it.units }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnitSystem.IMPERIAL)

    val faFlightId: String = checkNotNull(savedState["faFlightId"])

    private val _ui = MutableStateFlow(DetailUiState())
    val ui: StateFlow<DetailUiState> = _ui.asStateFlow()

    val hasOfflineRegion: StateFlow<Boolean> = dao.observeOfflineRegion(faFlightId)
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val subscribed: StateFlow<Boolean> = repo.observeFlight(faFlightId)
        .map { it?.flight?.subscribed == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            repo.observeFlight(faFlightId).collect { data ->
                _ui.value = _ui.value.copy(data = data)
            }
        }
        // When the registration becomes known (post-refresh), look up the photo.
        // Re-fires only when the registration string actually changes.
        viewModelScope.launch {
            repo.observeFlight(faFlightId)
                .map { it?.flight?.registration }
                .distinctUntilChanged()
                .collect { reg ->
                    val photo = photoRepo.fetchByRegistration(reg)
                    _ui.value = _ui.value.copy(photo = photo)
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

    fun toggleSubscription() {
        viewModelScope.launch {
            if (subscribed.value) {
                subscriptionScheduler.unsubscribe(faFlightId)
            } else {
                subscriptionScheduler.subscribe(faFlightId)
            }
        }
    }

    /**
     * Trip Mode — refresh latest data + subscribe (which fans out to boarding
     * reminders, polling, and calendar event creation). Offline map download
     * is handled by the caller navigating to the PreDownload screen.
     */
    fun prepareForTrip(onReadyForOfflineDownload: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.refreshFlight(faFlightId) }
            if (!subscribed.value) {
                subscriptionScheduler.subscribe(faFlightId)
            }
            if (!hasOfflineRegion.value) {
                onReadyForOfflineDownload()
            }
        }
    }
}
