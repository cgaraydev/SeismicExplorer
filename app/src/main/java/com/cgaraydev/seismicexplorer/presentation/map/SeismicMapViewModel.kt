package com.cgaraydev.seismicexplorer.presentation.map

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.di.MapComputationDispatcher
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeDepthFilter
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeismicMapViewModel @Inject constructor(
    private val repository: EarthquakeRepository,
    private val clock: Clock,
    @MapComputationDispatcher
    computationDispatcher: CoroutineDispatcher,
    // Hilt proporciona el handle asociado al estado de esta pantalla.
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Compatibilidad con los tests anteriores que construyen el
    // ViewModel directamente. Este constructor no participa en Hilt.
    internal constructor(
        repository: EarthquakeRepository,
        clock: Clock,
        computationDispatcher: CoroutineDispatcher
    ) : this(
        repository,
        clock,
        computationDispatcher,
        SavedStateHandle()
    )

    // Solo persistimos las opciones del usuario, no GeoJSON, caché,
    // cámara ni estados transitorios de sincronización.
    private val selectedFilters = MutableStateFlow(
        MapFilterSelection(
            timeWindow = restoredEnum(
                SAVED_TIME_WINDOW,
                RecentWindow.LAST_WEEK
            ),
            magnitude = restoredEnum(
                SAVED_MAGNITUDE,
                MapMagnitudeFilter.ALL
            ),
            depthFilter = restoredEnum(
                SAVED_DEPTH_FILTER,
                EarthquakeDepthFilter.ALL
            )
        )
    )

    val filters: StateFlow<MapFilterSelection> =
        selectedFilters.asStateFlow()

    // Mantiene estable el intervalo temporal
    // cuando se modifican magnitud o profundidad.
    private val queryEndTime = MutableStateFlow(
        clock.instant()
    )

    // Solicitar otra lectura local no cambia filtros,
    // período ni estado de sincronización con USGS.
    private val localRetryGeneration = MutableStateFlow(0L)

    fun retryLocalObservation() {
        localRetryGeneration.update { it + 1L }
    }

    private val _syncState = MutableStateFlow<MapSyncState>(
        MapSyncState.Idle
    )

    val syncState: StateFlow<MapSyncState> =
        _syncState.asStateFlow()

    // Serializa las solicitudes de sincronización.
    // Las nuevas esperan en lugar de descartarse.
    private val syncMutex = Mutex()

    suspend fun ensureFeedFresh(window: RecentWindow) {
        synchronizeFeed(
            window = window,
            force = false
        )
    }

    suspend fun refreshFeed(window: RecentWindow) {
        synchronizeFeed(
            window = window,
            force = true
        )
    }

    private suspend fun synchronizeFeed(
        window: RecentWindow,
        force: Boolean
    ) {
        syncMutex.withLock {
            // Si el usuario seleccionó otro período
            // mientras esta solicitud esperaba,
            // ya no necesitamos ejecutarla.
            if (selectedFilters.value.timeWindow != window) {
                return@withLock
            }

            var completed = false

            try {
                _syncState.value = MapSyncState.Syncing(window)

                val initialCacheStatus =
                    readFeedCacheStatus(window)

                _syncState.value = MapSyncState.Syncing(
                    window = window,
                    cacheStatus = initialCacheStatus
                )

                val result = if (force) {
                    when (
                        val forcedResult =
                            repository.refreshRecent(window)
                    ) {
                        RefreshResult.Success ->
                            FeedSyncResult.Updated

                        is RefreshResult.Failure ->
                            FeedSyncResult.Failed(
                                forcedResult.reason
                            )
                    }
                } else {
                    repository.refreshIfStale(window)
                }

                val finalCacheStatus =
                    readFeedCacheStatus(window)

                // Si el usuario cambió de período
                // durante la descarga, no publicamos
                // el resultado de la selección anterior.
                if (selectedFilters.value.timeWindow == window) {
                    _syncState.value = when (result) {
                        FeedSyncResult.UpToDate ->
                            MapSyncState.Ready(
                                window = window,
                                downloaded = false,
                                cacheStatus = finalCacheStatus
                            )

                        FeedSyncResult.Updated ->
                            MapSyncState.Ready(
                                window = window,
                                downloaded = true,
                                cacheStatus = finalCacheStatus
                            )

                        is FeedSyncResult.Failed ->
                            MapSyncState.Failed(
                                window = window,
                                reason = result.reason,
                                cacheStatus = finalCacheStatus
                            )
                    }
                }

                completed = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Unexpected feed synchronization failure",
                    exception
                )

                val cacheStatus = readFeedCacheStatus(window)

                if (selectedFilters.value.timeWindow == window) {
                    _syncState.value = MapSyncState.Failed(
                        window = window,
                        cacheStatus = cacheStatus
                    )
                }

                completed = true
            } finally {
                if (
                    completed &&
                    selectedFilters.value.timeWindow == window
                ) {
                    // Renueva el intervalo incluso si
                    // el feed estaba al día o falló.
                    queryEndTime.value = clock.instant()
                } else {
                    // No dejamos un Syncing antiguo
                    // después de cancelar o cambiar
                    // de período.
                    val currentState = _syncState.value

                    if (
                        currentState is MapSyncState.Syncing &&
                        currentState.window == window
                    ) {
                        _syncState.value = MapSyncState.Idle
                    }
                }
            }
        }
    }

    // Los valores se guardan como String para que Android los pueda
    // conservar en el estado de la pantalla durante la recreación.
    // Si una versión futura cambia los nombres, los valores antiguos
    // desconocidos vuelven a la opción predeterminada sin fallar.
    private inline fun <reified T : Enum<T>> restoredEnum(
        key: String,
        default: T
    ): T {
        val savedName = savedStateHandle.get<Any>(key) as? String
        return enumValues<T>().firstOrNull {
            it.name == savedName
        } ?: default
    }

    fun selectTimeWindow(window: RecentWindow) {
        savedStateHandle[SAVED_TIME_WINDOW] = window.name
        selectedFilters.update { current ->
            current.copy(timeWindow = window)
        }
    }

    fun selectMagnitude(magnitude: MapMagnitudeFilter) {
        savedStateHandle[SAVED_MAGNITUDE] = magnitude.name
        selectedFilters.update { current ->
            current.copy(magnitude = magnitude)
        }
    }

    fun selectDepthFilter(depthFilter: EarthquakeDepthFilter) {
        savedStateHandle[SAVED_DEPTH_FILTER] = depthFilter.name
        selectedFilters.update { current ->
            current.copy(depthFilter = depthFilter)
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
                "Could not read feed cache status",
                exception
            )
            FeedCacheStatus.Unknown
        }
    }

    // Sobrevive a la pausa de StateFlow y evita vaciar MapLibre al
    // iniciar una nueva consulta de filtros o al reanudar la pantalla.
    private val mapLoadingContinuity = MapLoadingContinuity()

    val uiState: StateFlow<SeismicMapUiState> =
        combine(
            selectedFilters,
            queryEndTime,
            localRetryGeneration
        ) { filters, endTime, _ ->
            filters to endTime
        }
            .flatMapLatest { (filters, endTime) ->
                val query = filters.timeWindow.toQuery(
                    endTime = endTime,
                    minimumMagnitude =
                        filters.magnitude.minimumMagnitude,
                    depthFilter = filters.depthFilter
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
                            TAG,
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
            .map(mapLoadingContinuity::accept)
            .flowOn(computationDispatcher)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SeismicMapUiState()
            )

    companion object {
        internal const val SAVED_TIME_WINDOW = "seismic.map.filter.window"
        internal const val SAVED_MAGNITUDE = "seismic.map.filter.magnitude"
        internal const val SAVED_DEPTH_FILTER = "seismic.map.filter.depth"
        private const val TAG = "SeismicMapViewModel"
    }
}
