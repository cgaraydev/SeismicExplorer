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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SeismicMapViewModel @Inject constructor(
    repository: EarthquakeRepository,
    clock: Clock,
    @MapComputationDispatcher
    computationDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val query = RecentWindow.LAST_WEEK.toQuery(
        endTime = clock.instant()
    )

    val uiState: StateFlow<SeismicMapUiState> =
        repository.observeEarthquakes(query)
            .map { earthquakes ->
                SeismicMapUiState(
                    geoJson = MapEarthquakeGeoJson.encode(earthquakes),
                    eventCount = earthquakes.size,
                    isLoading = false
                )
            }
            .flowOn(computationDispatcher)
            .catch { exception ->
                if (exception is CancellationException) {
                    throw exception
                }

                Log.e(
                    "SeismicMapViewModel",
                    "Failed to load cached map events",
                    exception
                )

                emit(
                    SeismicMapUiState(
                        isLoading = false,
                        hasError = true
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SeismicMapUiState()
            )
}