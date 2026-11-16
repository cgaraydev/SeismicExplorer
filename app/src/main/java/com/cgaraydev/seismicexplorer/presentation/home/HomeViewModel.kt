package com.cgaraydev.seismicexplorer.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EarthquakeRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    private var queryEndTime: Instant = clock.instant()

    private var observationJob: Job? = null
    private var refreshJob: Job? = null

    private var observationVersion = 0L
    private var refreshVersion = 0L

    init {
        observeCurrentSelection(clearEvents = true)
        refreshCurrentWindow(force = false)
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

                observeCurrentSelection(
                    clearEvents = false
                )

                // El usuario solicitó expresamente
                // una actualización desde USGS.
                refreshCurrentWindow(force = true)
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

        // Solo descargar si el feed está desactualizado.
        refreshCurrentWindow(force = false)
    }

    private fun selectMagnitude(
        threshold: MagnitudeThreshold
    ) {
        if (_uiState.value.minimumMagnitude == threshold) {
            return
        }

        _uiState.update { current ->
            current.copy(minimumMagnitude = threshold)
        }

        // La magnitud no requiere una descarga nueva.
        observeCurrentSelection(clearEvents = true)
    }

    private fun observeCurrentSelection(
        clearEvents: Boolean
    ) {
        observationVersion++
        val version = observationVersion

        observationJob?.cancel()

        val selectedState = _uiState.value

        val query = selectedState.timeWindow
            .toRecentWindow()
            .toQuery(
                endTime = queryEndTime,
                minimumMagnitude =
                    selectedState.minimumMagnitude.toValue()
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
                        TAG,
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

    private fun refreshCurrentWindow(force: Boolean) {
        refreshVersion++
        val version = refreshVersion

        refreshJob?.cancel()

        val window = _uiState.value.timeWindow
            .toRecentWindow()

        _uiState.update { current ->
            current.copy(
                refreshState = HomeRefreshState.Refreshing,
                feedCacheStatus = FeedCacheStatus.Unknown
            )
        }

        refreshJob = viewModelScope.launch {

            // Leer primero la última sincronización.
            // Permite mostrar datos antiguos mientras
            // está en curso la descarga.
            val initialCacheStatus =
                readFeedCacheStatus(window)

            if (version == refreshVersion) {
                _uiState.update { current ->
                    current.copy(
                        feedCacheStatus = initialCacheStatus
                    )
                }
            }

            val nextRefreshState = try {
                if (force) {
                    when (
                        val result = repository.refreshRecent(window)
                    ) {
                        RefreshResult.Success ->
                            HomeRefreshState.Completed

                        is RefreshResult.Failure ->
                            HomeRefreshState.Failed(result.reason)
                    }
                } else {
                    when (
                        val result = repository.refreshIfStale(window)
                    ) {
                        FeedSyncResult.UpToDate ->
                            HomeRefreshState.UpToDate

                        FeedSyncResult.Updated ->
                            HomeRefreshState.Completed

                        is FeedSyncResult.Failed ->
                            HomeRefreshState.Failed(result.reason)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Unexpected earthquake refresh failure",
                    exception
                )
                HomeRefreshState.Failed(reason = null)
            }

            // Consultar nuevamente el timestamp.
            // Si la actualización falló, se conserva
            // la última descarga confirmada.
            val finalCacheStatus =
                readFeedCacheStatus(window)

            if (version == refreshVersion) {
                _uiState.update { current ->
                    current.copy(
                        refreshState = nextRefreshState,
                        feedCacheStatus = finalCacheStatus
                    )
                }
            }
        }
    }

    private suspend fun readFeedCacheStatus(
        window: RecentWindow
    ): FeedCacheStatus {
        return try {
            repository.getFeedCacheStatus(window)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w(
                TAG,
                "Could not read cached feed sync status",
                exception
            )
            FeedCacheStatus.Unknown
        }
    }

    private fun TimeWindow.toRecentWindow(): RecentWindow {
        return when (this) {
            TimeWindow.LAST_DAY ->
                RecentWindow.LAST_DAY

            TimeWindow.LAST_WEEK ->
                RecentWindow.LAST_WEEK

            TimeWindow.LAST_MONTH ->
                RecentWindow.LAST_30_DAYS
        }
    }

    private fun MagnitudeThreshold.toValue(): Double? {
        return when (this) {
            MagnitudeThreshold.ALL -> null

            MagnitudeThreshold.MAGNITUDE_2_5 -> 2.5

            MagnitudeThreshold.MAGNITUDE_4_5 -> 4.5
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}