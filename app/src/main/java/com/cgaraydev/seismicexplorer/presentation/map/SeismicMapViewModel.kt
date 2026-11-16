package com.cgaraydev.seismicexplorer.presentation.map

import android.util.Log
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

    val filters: StateFlow<MapFilterSelection> =
        selectedFilters.asStateFlow()

    // El tiempo de consulta es independiente
    // de los filtros de magnitud y profundidad.
    private val queryEndTime = MutableStateFlow(
        clock.instant()
    )

    private val _syncState = MutableStateFlow<MapSyncState>(
        MapSyncState.Idle
    )

    val syncState: StateFlow<MapSyncState> =
        _syncState.asStateFlow()

    // Evita sincronizaciones simultáneas iniciadas
    // desde esta pantalla.
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
        // Una solicitud adicional no debe iniciar
        // otra descarga mientras una sigue activa.
        if (!syncMutex.tryLock()) return

        var completed = false

        try {
            _syncState.value = MapSyncState.Syncing(window)

            try {
                val initialCacheStatus =
                    readFeedCacheStatus(window)

                _syncState.value = MapSyncState.Syncing(
                    window = window,
                    cacheStatus = initialCacheStatus
                )

                val result = if (force) {
                    when (val forcedResult =
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

                completed = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Unexpected feed synchronization failure",
                    exception
                )

                _syncState.value = MapSyncState.Failed(
                    window = window,
                    cacheStatus = readFeedCacheStatus(window)
                )

                completed = true
            }
        } finally {
            if (completed) {
                // El intervalo avanza también si la
                // descarga ha fallado o estaba al día.
                if (
                    selectedFilters.value.timeWindow == window
                ) {
                    queryEndTime.value = clock.instant()
                }
            } else {
                // Una sincronización cancelada no debe
                // dejar el estado bloqueado en Syncing.
                val currentState = _syncState.value

                if (
                    currentState is MapSyncState.Syncing &&
                    currentState.window == window
                ) {
                    _syncState.value = MapSyncState.Idle
                }
            }

            syncMutex.unlock()
        }
    }

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

    fun selectDepthFilter(
        depthFilter: EarthquakeDepthFilter
    ) {
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

    val uiState: StateFlow<SeismicMapUiState> =
        combine(
            selectedFilters,
            queryEndTime
        ) { filters, endTime ->
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
            .flowOn(computationDispatcher)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SeismicMapUiState()
            )

    private companion object {
        const val TAG = "SeismicMapViewModel"
    }
}