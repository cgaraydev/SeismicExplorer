package com.cgaraydev.seismicexplorer.presentation.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import com.cgaraydev.seismicexplorer.presentation.navigation.EarthquakeDetailDestination
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(
    assistedFactory = EarthquakeDetailViewModel.Factory::class
)
class EarthquakeDetailViewModel @AssistedInject constructor(
    repository: EarthquakeRepository,
    @Assisted destination: EarthquakeDetailDestination
) : ViewModel() {

    val uiState: StateFlow<EarthquakeDetailUiState> =
        repository.observeEarthquake(destination.earthquakeId)
            .map { earthquake ->
                val state: EarthquakeDetailUiState =
                    if (earthquake == null) {
                        EarthquakeDetailUiState.NotFound
                    } else {
                        EarthquakeDetailUiState.Content(earthquake)
                    }

                state
            }
            .catch { exception ->
                if (exception is CancellationException) {
                    throw exception
                }

                Log.e(
                    "EarthquakeDetailViewModel",
                    "Failed to observe cached earthquake",
                    exception
                )

                emit(EarthquakeDetailUiState.Error)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = EarthquakeDetailUiState.Loading
            )

    @AssistedFactory
    interface Factory {
        fun create(
            destination: EarthquakeDetailDestination
        ): EarthquakeDetailViewModel
    }
}