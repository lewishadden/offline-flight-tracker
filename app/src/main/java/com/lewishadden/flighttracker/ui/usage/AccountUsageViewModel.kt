package com.lewishadden.flighttracker.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewishadden.flighttracker.data.api.dto.AccountUsageDto
import com.lewishadden.flighttracker.data.repository.FlightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UsageUiState(
    val loading: Boolean = true,
    val data: AccountUsageDto? = null,
    val error: String? = null,
)

@HiltViewModel
class AccountUsageViewModel @Inject constructor(
    private val repo: FlightRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(UsageUiState())
    val ui: StateFlow<UsageUiState> = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = UsageUiState(loading = true)
            _ui.value = runCatching { repo.getAccountUsage() }
                .fold(
                    onSuccess = { UsageUiState(loading = false, data = it) },
                    onFailure = { UsageUiState(loading = false, error = it.message ?: "Failed to load usage") },
                )
        }
    }
}
