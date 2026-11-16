package com.cgaraydev.seismicexplorer.presentation.map

import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeDepthFilter
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeismicMapViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val now = Instant.parse(
        "2026-11-14T12:00:00Z"
    )

    private lateinit var repository: FakeRepository
    private lateinit var viewModel: SeismicMapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRepository()
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.cancel()
        }

        Dispatchers.resetMain()
    }

    @Test
    fun initialObservation_usesLastSevenDays() = runTest {
        createViewModel()
        collectUiState()

        advanceUntilIdle()

        val query = repository.observedQueries.single()

        assertEquals(now, query.endTime)

        assertEquals(
            now.minusSeconds(7L * 24 * 60 * 60),
            query.startTime
        )

        assertEquals(null, query.minimumMagnitude)

        assertEquals(
            EarthquakeDepthFilter.ALL,
            query.depthFilter
        )

        assertFalse(viewModel.uiState.value.isLoading)

        assertEquals(
            0,
            viewModel.uiState.value.eventCount
        )
    }

    @Test
    fun roomEmission_updatesMapWithoutNetworkRequests() = runTest {
        createViewModel()
        collectUiState()

        advanceUntilIdle()

        repository.events.value = listOf(
            sampleEarthquake()
        )

        advanceUntilIdle()

        assertEquals(
            1,
            viewModel.uiState.value.eventCount
        )

        assertEquals(
            MapEarthquakeGeoJson.encode(
                listOf(sampleEarthquake())
            ),
            viewModel.uiState.value.geoJson
        )

        assertEquals(
            0,
            repository.refreshRequests
        )
    }

    @Test
    fun changingMapFilters_updatesRoomQueryWithoutNetworkRefresh() =
        runTest {
            createViewModel()
            collectUiState()

            advanceUntilIdle()

            viewModel.selectTimeWindow(
                RecentWindow.LAST_30_DAYS
            )

            advanceUntilIdle()

            viewModel.selectMagnitude(
                MapMagnitudeFilter.MAGNITUDE_4_5
            )

            advanceUntilIdle()

            val query = repository.observedQueries.last()

            assertEquals(now, query.endTime)

            assertEquals(
                now.minusSeconds(30L * 24 * 60 * 60),
                query.startTime
            )

            assertEquals(
                4.5,
                query.minimumMagnitude
            )

            assertEquals(
                RecentWindow.LAST_30_DAYS,
                viewModel.uiState.value.filters.timeWindow
            )

            assertEquals(
                MapMagnitudeFilter.MAGNITUDE_4_5,
                viewModel.uiState.value.filters.magnitude
            )

            assertEquals(
                0,
                repository.refreshRequests
            )
        }

    @Test
    fun ensureFeedFresh_updatesSyncState() = runTest {
        createViewModel()

        viewModel.ensureFeedFresh(
            RecentWindow.LAST_WEEK
        )

        assertEquals(
            listOf(RecentWindow.LAST_WEEK),
            repository.syncRequests
        )

        assertEquals(
            MapSyncState.Ready(
                window = RecentWindow.LAST_WEEK,
                downloaded = true
            ),
            viewModel.syncState.value
        )
    }

    @Test
    fun ensureFeedFresh_reportsLastKnownCacheStatus() = runTest {
        createViewModel()

        val staleStatus = FeedCacheStatus.Synced(
            lastSuccessEpochMillis =
                now.minusSeconds(60 * 60).toEpochMilli(),
            isFresh = false
        )

        repository.cacheStatus = staleStatus

        viewModel.ensureFeedFresh(
            RecentWindow.LAST_WEEK
        )

        assertEquals(
            MapSyncState.Ready(
                window = RecentWindow.LAST_WEEK,
                downloaded = true,
                cacheStatus = staleStatus
            ),
            viewModel.syncState.value
        )
    }

    @Test
    fun changingDepthFilter_updatesRoomQueryWithoutNetworkRefresh() =
        runTest {
            createViewModel()
            collectUiState()

            advanceUntilIdle()

            viewModel.selectDepthFilter(
                EarthquakeDepthFilter.DEEP
            )

            advanceUntilIdle()

            assertEquals(
                EarthquakeDepthFilter.DEEP,
                repository.observedQueries.last().depthFilter
            )

            assertEquals(
                EarthquakeDepthFilter.DEEP,
                viewModel.uiState.value.filters.depthFilter
            )

            assertEquals(
                0,
                repository.refreshRequests
            )

            assertEquals(
                emptyList<RecentWindow>(),
                repository.syncRequests
            )
        }

    @Test
    fun changingFilters_doesNotShiftTemporalAnchor() = runTest {
        val adjustableClock = AdjustableClock(now)

        createViewModel(adjustableClock)
        collectUiState()

        advanceUntilIdle()

        // Avanza el reloj cinco minutos.
        adjustableClock.current = now.plusSeconds(300)

        viewModel.selectMagnitude(
            MapMagnitudeFilter.MAGNITUDE_4_5
        )

        viewModel.selectDepthFilter(
            EarthquakeDepthFilter.DEEP
        )

        advanceUntilIdle()

        val query = repository.observedQueries.last()

        // La selección cambia, pero el tiempo no.
        assertEquals(
            now,
            query.endTime
        )

        assertEquals(
            now.minusSeconds(7L * 24 * 60 * 60),
            query.startTime
        )

        assertEquals(
            4.5,
            query.minimumMagnitude
        )

        assertEquals(
            EarthquakeDepthFilter.DEEP,
            query.depthFilter
        )

        assertEquals(
            emptyList<RecentWindow>(),
            repository.syncRequests
        )
    }

    @Test
    fun completedSync_includesEventsOccurredAfterMapOpened() =
        runTest {
            val adjustableClock = AdjustableClock(now)

            createViewModel(adjustableClock)
            collectUiState()

            advanceUntilIdle()

            val newEarthquake = sampleEarthquake().copy(
                id = "new-event",
                occurredAt = now.plusSeconds(60),
                updatedAt = now.plusSeconds(60)
            )

            repository.events.value = listOf(
                newEarthquake
            )

            advanceUntilIdle()

            // Ocurrió después de la apertura.
            assertEquals(
                0,
                viewModel.uiState.value.eventCount
            )

            // Avanzamos el reloj a las 12:02.
            adjustableClock.current = now.plusSeconds(120)

            viewModel.ensureFeedFresh(
                RecentWindow.LAST_WEEK
            )

            advanceUntilIdle()

            assertEquals(
                adjustableClock.current,
                repository.observedQueries.last().endTime
            )

            assertEquals(
                1,
                viewModel.uiState.value.eventCount
            )

            assertEquals(
                listOf(RecentWindow.LAST_WEEK),
                repository.syncRequests
            )
        }

    @Test
    fun completedSync_excludesEventsOlderThanNewWindow() =
        runTest {
            val adjustableClock = AdjustableClock(now)

            createViewModel(adjustableClock)
            collectUiState()

            // Está a un minuto de salir del período.
            val almostExpired = sampleEarthquake().copy(
                occurredAt = now.minusSeconds(
                    7L * 24 * 60 * 60 - 60
                )
            )

            repository.events.value = listOf(
                almostExpired
            )

            advanceUntilIdle()

            assertEquals(
                1,
                viewModel.uiState.value.eventCount
            )

            // Pasan dos minutos.
            adjustableClock.current = now.plusSeconds(120)

            viewModel.ensureFeedFresh(
                RecentWindow.LAST_WEEK
            )

            advanceUntilIdle()

            assertEquals(
                0,
                viewModel.uiState.value.eventCount
            )

            assertEquals(
                adjustableClock.current.minusSeconds(
                    7L * 24 * 60 * 60
                ),
                repository.observedQueries.last().startTime
            )
        }

    private fun createViewModel(
        clock: Clock = Clock.fixed(
            now,
            ZoneOffset.UTC
        )
    ) {
        viewModel = SeismicMapViewModel(
            repository = repository,
            clock = clock,
            computationDispatcher = dispatcher
        )
    }

    // Mantiene activa la observación de StateFlow
    // durante las pruebas.
    private fun kotlinx.coroutines.test.TestScope.collectUiState() {
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }
    }

    private fun sampleEarthquake() = Earthquake(
        id = "us123",
        magnitude = 4.5,
        place = "Coast of Chile",
        coordinates = GeoCoordinates(
            latitude = -33.4,
            longitude = -71.6
        ),
        depthKilometers = 12.0,
        occurredAt = now.minusSeconds(3600),
        updatedAt = now.minusSeconds(1800),
        detailUrl = null
    )

    private class AdjustableClock(
        initial: Instant
    ) : Clock() {

        var current: Instant = initial

        override fun getZone(): ZoneId =
            ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock =
            Clock.fixed(current, zone)

        override fun instant(): Instant =
            current
    }

    private class FakeRepository : EarthquakeRepository {

        val events = MutableStateFlow<List<Earthquake>>(
            emptyList()
        )

        val observedQueries = mutableListOf<EarthquakeQuery>()

        var refreshRequests = 0

        val syncRequests = mutableListOf<RecentWindow>()

        var cacheStatus: FeedCacheStatus =
            FeedCacheStatus.Unknown

        override suspend fun getFeedCacheStatus(
            window: RecentWindow
        ): FeedCacheStatus {
            return cacheStatus
        }

        override suspend fun refreshIfStale(
            window: RecentWindow
        ): FeedSyncResult {
            syncRequests.add(window)
            return FeedSyncResult.Updated
        }

        override fun observeEarthquakes(
            query: EarthquakeQuery
        ): Flow<List<Earthquake>> {
            observedQueries.add(query)

            // Simula los límites temporales de Room.
            return events.map { earthquakes ->
                earthquakes.filter { earthquake ->
                    !earthquake.occurredAt.isBefore(
                        query.startTime
                    ) &&
                            earthquake.occurredAt.isBefore(
                                query.endTime
                            )
                }
            }
        }

        override fun observeEarthquake(
            id: String
        ): Flow<Earthquake?> {
            return flowOf(null)
        }

        override suspend fun refreshRecent(
            window: RecentWindow
        ): RefreshResult {
            refreshRequests++
            return RefreshResult.Success
        }
    }
}