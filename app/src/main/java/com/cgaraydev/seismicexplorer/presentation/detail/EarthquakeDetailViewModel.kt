package com.cgaraydev.seismicexplorer.presentation.detail

import android.util.Log
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import com.cgaraydev.seismicexplorer.domain.repository.ScientificDetailRepository
import com.cgaraydev.seismicexplorer.domain.repository.ShakeMapImageRepository
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageResult
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResourceKind
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val shakeMapImageRepository: ShakeMapImageRepository,
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
    private val _shakeMapImageState = MutableStateFlow<ShakeMapImageUiState>(ShakeMapImageUiState.Idle)
    val shakeMapImageState: StateFlow<ShakeMapImageUiState> = _shakeMapImageState
    private var shakeMapImageJob: Job? = null

    /** Solo disponible tras consultar el inventario y pulsar el botón del mapa. */
    fun requestShakeMapImage() {
        val detail = (_scientificState.value as? ScientificDetailUiState.Available)?.detail ?: return
        val resource = detail.shakeMap?.resources
            ?.firstOrNull { it.kind == ShakeMapResourceKind.INTENSITY_MAP } ?: return
        if (_shakeMapImageState.value == ShakeMapImageUiState.Loading) return
        shakeMapImageJob?.cancel()
        _shakeMapImageState.value = ShakeMapImageUiState.Loading
        shakeMapImageJob = viewModelScope.launch {
            val result = try {
                shakeMapImageRepository.load(resource)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e("EarthquakeDetailViewModel", "ShakeMap request failed", exception)
                ShakeMapImageResult.Failed(ShakeMapImageFailure.SERVICE)
            }
            _shakeMapImageState.value = when (result) {
                is ShakeMapImageResult.Failed -> ShakeMapImageUiState.Error(result.reason)
                is ShakeMapImageResult.Available -> {
                    val decoded = withContext(Dispatchers.Default) {
                        decodeBoundedJpeg(result.bytes)
                    }
                    if (decoded == null) {
                        ShakeMapImageUiState.Error(ShakeMapImageFailure.INVALID_IMAGE)
                    } else {
                        ShakeMapImageUiState.Available(decoded, result.fromCache, result.isStale)
                    }
                }
            }
        }
    }

    /** Decodificación muestreada: no expandir un JPEG comprimido a un bitmap gigantesco. */
    private fun decodeBoundedJpeg(bytes: ByteArray): androidx.compose.ui.graphics.ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width !in 1..16_384 || height !in 1..16_384 ||
            width.toLong() * height.toLong() > 100_000_000L) return null
        var sampleSize = 1
        while (width / sampleSize > 2048 || height / sampleSize > 2048 ||
            (width.toLong() / sampleSize) * (height.toLong() / sampleSize) > 4_000_000L) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize })?.asImageBitmap()
    }

    /** Una apertura del detalle nunca inicia esta consulta: solo una acción del usuario. */
    fun requestScientificDetail() {
        if (_scientificState.value == ScientificDetailUiState.Loading) return
        val forceRefresh = _scientificState.value != ScientificDetailUiState.Idle
        val knownUpdatedAt = (uiState.value as? EarthquakeDetailUiState.Content)
            ?.earthquake?.updatedAt?.toEpochMilli()
        scientificJob?.cancel()
        shakeMapImageJob?.cancel()
        _shakeMapImageState.value = ShakeMapImageUiState.Idle
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