package com.cgaraydev.seismicexplorer.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EarthquakeRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var queryEndTime: Instant = clock.instant()

    private var observationJob: Job? = null
    private var refreshJob: Job? = null

    private var observationVersion = 0L
    private var refreshVersion = 0L

    init {
        observeCurrentSelection(clearEvents = true)
        refreshCurrentWindow()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.SelectTimeWindow -> {
                selectTimeWindow(action.value)
            }

            is HomeAction.SelectMagnitude -> {
                selectMagnitude(action.value)
            }

            HomeAction.Refresh -> {
                queryEndTime = clock.instant()
                observeCurrentSelection(clearEvents = false)
                refreshCurrentWindow()
            }
        }
    }

    private fun selectTimeWindow(window: TimeWindow) {
        if (_uiState.value.timeWindow == window) return

        _uiState.update { current ->
            current.copy(timeWindow = window)
        }

        queryEndTime = clock.instant()

        observeCurrentSelection(clearEvents = true)
        refreshCurrentWindow()
    }

    private fun selectMagnitude(threshold: MagnitudeThreshold) {
        if (_uiState.value.minimumMagnitude == threshold) return

        _uiState.update { current ->
            current.copy(minimumMagnitude = threshold)
        }

        observeCurrentSelection(clearEvents = true)
    }

    private fun observeCurrentSelection(clearEvents: Boolean) {
        observationVersion++
        val version = observationVersion

        observationJob?.cancel()

        val selectedState = _uiState.value
        val query = selectedState.timeWindow.toRecentWindow().toQuery(
            endTime = queryEndTime,
            minimumMagnitude = selectedState.minimumMagnitude.toValue()
        )

        _uiState.update { current ->
            current.copy(
                earthquakes = if (clearEvents) {
                    emptyList()
                } else {
                    current.earthquakes
                },
                isReadingLocal = true,
                localReadFailed = false
            )
        }

        observationJob = viewModelScope.launch {
            repository.observeEarthquakes(query)
                .catch { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

                    Log.e(
                        "HomeViewModel",
                        "Failed to read cached earthquakes",
                        exception
                    )

                    if (version == observationVersion) {
                        _uiState.update { current ->
                            current.copy(
                                isReadingLocal = false,
                                localReadFailed = true
                            )
                        }
                    }
                }
                .collect { earthquakes ->
                    if (version == observationVersion) {
                        _uiState.update { current ->
                            current.copy(
                                earthquakes = earthquakes,
                                isReadingLocal = false,
                                localReadFailed = false
                            )
                        }
                    }
                }
        }
    }

    private fun refreshCurrentWindow() {
        refreshVersion++
        val version = refreshVersion

        refreshJob?.cancel()

        val window = _uiState.value.timeWindow.toRecentWindow()

        _uiState.update { current ->
            current.copy(
                refreshState = HomeRefreshState.Refreshing
            )
        }

        refreshJob = viewModelScope.launch {
            val refreshState = try {
                when (val result = repository.refreshRecent(window)) {
                    RefreshResult.Success -> {
                        HomeRefreshState.Completed
                    }

                    is RefreshResult.Failure -> {
                        HomeRefreshState.Failed(result.reason)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    "HomeViewModel",
                    "Unexpected earthquake refresh failure",
                    exception
                )

                HomeRefreshState.Failed(reason = null)
            }

            if (version == refreshVersion) {
                _uiState.update { current ->
                    current.copy(refreshState = refreshState)
                }
            }
        }
    }

    private fun TimeWindow.toRecentWindow(): RecentWindow {
        return when (this) {
            TimeWindow.LAST_DAY -> RecentWindow.LAST_DAY
            TimeWindow.LAST_WEEK -> RecentWindow.LAST_WEEK
            TimeWindow.LAST_MONTH -> RecentWindow.LAST_30_DAYS
        }
    }

    private fun MagnitudeThreshold.toValue(): Double? {
        return when (this) {
            MagnitudeThreshold.ALL -> null
            MagnitudeThreshold.MAGNITUDE_2_5 -> 2.5
            MagnitudeThreshold.MAGNITUDE_4_5 -> 4.5
        }
    }
}