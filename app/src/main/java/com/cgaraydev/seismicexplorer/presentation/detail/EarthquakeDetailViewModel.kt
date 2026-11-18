package com.cgaraydev.seismicexplorer.presentation.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import com.cgaraydev.seismicexplorer.domain.repository.ScientificDetailRepository
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailFailure
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailResult
import com.cgaraydev.seismicexplorer.presentation.navigation.EarthquakeDetailDestination
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val scientificRepository: ScientificDetailRepository,
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

    private val eventId = destination.earthquakeId
    private val _scientificState = MutableStateFlow<ScientificDetailUiState>(ScientificDetailUiState.Idle)
    val scientificState: StateFlow<ScientificDetailUiState> = _scientificState
    private var scientificJob: Job? = null

    /** Una apertura del detalle nunca inicia esta consulta: solo una acción del usuario. */
    fun requestScientificDetail() {
        if (_scientificState.value == ScientificDetailUiState.Loading) return
        val forceRefresh = _scientificState.value != ScientificDetailUiState.Idle
        val knownUpdatedAt = (uiState.value as? EarthquakeDetailUiState.Content)
            ?.earthquake?.updatedAt?.toEpochMilli()
        scientificJob?.cancel()
        _scientificState.value = ScientificDetailUiState.Loading
        scientificJob = viewModelScope.launch {
            val result = try {
                scientificRepository.getDetail(
                    eventId = eventId,
                    knownUpdatedAtEpochMillis = knownUpdatedAt,
                    forceRefresh = forceRefresh
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e("EarthquakeDetailViewModel", "Scientific detail request failed", exception)
                ScientificDetailResult.Failed(ScientificDetailFailure.SERVICE)
            }
            _scientificState.value = when (result) {
                is ScientificDetailResult.Available -> ScientificDetailUiState.Available(
                    detail = result.detail,
                    source = result.source,
                    isStale = result.isStale
                )
                is ScientificDetailResult.Failed -> ScientificDetailUiState.Error(result.reason)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            destination: EarthquakeDetailDestination
        ): EarthquakeDetailViewModel
    }
}