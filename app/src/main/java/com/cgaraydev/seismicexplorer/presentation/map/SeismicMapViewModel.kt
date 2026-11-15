package com.cgaraydev.seismicexplorer.presentation.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.di.MapComputationDispatcher
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeismicMapViewModel @Inject constructor(
    private val repository: EarthquakeRepository,
    private val clock: Clock,
    @MapComputationDispatcher
    computationDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val selectedFilters = MutableStateFlow(
        MapFilterSelection()
    )

    fun selectTimeWindow(window: RecentWindow) {
        selectedFilters.update { current ->
            current.copy(timeWindow = window)
        }
    }

    fun selectMagnitude(magnitude: MapMagnitudeFilter) {
        selectedFilters.update { current ->
            current.copy(magnitude = magnitude)
        }
    }

    val uiState: StateFlow<SeismicMapUiState> =
        selectedFilters
            .flatMapLatest { filters ->

                val query = filters.timeWindow.toQuery(
                    endTime = clock.instant(),
                    minimumMagnitude =
                        filters.magnitude.minimumMagnitude
                )

                repository.observeEarthquakes(query)
                    .map { earthquakes ->
                        SeismicMapUiState(
                            geoJson = MapEarthquakeGeoJson.encode(
                                earthquakes
                            ),
                            eventCount = earthquakes.size,
                            eventBounds =
                                MapEventBounds.fromEarthquakes(
                                    earthquakes
                                ),
                            filters = filters,
                            isLoading = false
                        )
                    }
                    .onStart {
                        emit(
                            SeismicMapUiState(
                                filters = filters,
                                isLoading = true
                            )
                        )
                    }
                    .catch { exception ->
                        if (exception is CancellationException) {
                            throw exception
                        }

                        Log.e(
                            "SeismicMapViewModel",
                            "Failed to load filtered map events",
                            exception
                        )

                        emit(
                            SeismicMapUiState(
                                filters = filters,
                                isLoading = false,
                                hasError = true
                            )
                        )
                    }
            }
            .flowOn(computationDispatcher)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SeismicMapUiState()
            )
}