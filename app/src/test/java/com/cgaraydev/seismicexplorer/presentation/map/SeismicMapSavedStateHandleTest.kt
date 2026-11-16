package com.cgaraydev.seismicexplorer.presentation.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeDepthFilter
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeismicMapSavedStateHandleTest {

    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-16T12:00:00Z")

    private val viewModels = mutableListOf<SeismicMapViewModel>()
    private lateinit var repository: FakeRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRepository()
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun noSavedFilters_usesExistingDefaults() = runTest {
        val viewModel = createViewModel(SavedStateHandle())

        assertEquals(MapFilterSelection(), viewModel.filters.value)
        assertEquals(MapSyncState.Idle, viewModel.syncState.value)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val query = repository.observedQueries.single()
        assertEquals(now, query.endTime)
        assertEquals(now.minus(RecentWindow.LAST_WEEK.duration), query.startTime)
        assertEquals(null, query.minimumMagnitude)
        assertEquals(EarthquakeDepthFilter.ALL, query.depthFilter)
    }

    @Test
    fun filtersAreRestoredAfterViewModelRecreation() = runTest {
        val firstHandle = SavedStateHandle()
        val first = createViewModel(firstHandle)

        first.selectTimeWindow(RecentWindow.LAST_30_DAYS)
        first.selectMagnitude(MapMagnitudeFilter.MAGNITUDE_4_5)
        first.selectDepthFilter(EarthquakeDepthFilter.DEEP)

        assertEquals(
            RecentWindow.LAST_30_DAYS.name,
            firstHandle.get<String>(SeismicMapViewModel.SAVED_TIME_WINDOW)
        )
        assertEquals(
            MapMagnitudeFilter.MAGNITUDE_4_5.name,
            firstHandle.get<String>(SeismicMapViewModel.SAVED_MAGNITUDE)
        )
        assertEquals(
            EarthquakeDepthFilter.DEEP.name,
            firstHandle.get<String>(SeismicMapViewModel.SAVED_DEPTH_FILTER)
        )

        // Simula un nuevo SavedStateHandle reconstruido a partir de los
        // valores simples que Android puede conservar al restaurar estado.
        val restoredHandle = SavedStateHandle(
            mapOf(
                SeismicMapViewModel.SAVED_TIME_WINDOW to
                    firstHandle.get<String>(SeismicMapViewModel.SAVED_TIME_WINDOW),
                SeismicMapViewModel.SAVED_MAGNITUDE to
                    firstHandle.get<String>(SeismicMapViewModel.SAVED_MAGNITUDE),
                SeismicMapViewModel.SAVED_DEPTH_FILTER to
                    firstHandle.get<String>(SeismicMapViewModel.SAVED_DEPTH_FILTER)
            )
        )
        first.viewModelScope.cancel()

        val restoredClock = now.plusSeconds(600)
        val restored = createViewModel(
            savedStateHandle = restoredHandle,
            clock = Clock.fixed(restoredClock, ZoneOffset.UTC)
        )

        assertEquals(
            MapFilterSelection(
                timeWindow = RecentWindow.LAST_30_DAYS,
                magnitude = MapMagnitudeFilter.MAGNITUDE_4_5,
                depthFilter = EarthquakeDepthFilter.DEEP
            ),
            restored.filters.value
        )
        assertEquals(MapSyncState.Idle, restored.syncState.value)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            restored.uiState.collect()
        }
        advanceUntilIdle()

        val query = repository.observedQueries.single()
        assertEquals(restoredClock, query.endTime)
        assertEquals(
            restoredClock.minus(RecentWindow.LAST_30_DAYS.duration),
            query.startTime
        )
        assertEquals(4.5, query.minimumMagnitude)
        assertEquals(EarthquakeDepthFilter.DEEP, query.depthFilter)
        assertTrue(repository.syncRequests.isEmpty())
    }

    @Test
    fun unrecognizedSavedFilters_fallBackIndependently() = runTest {
        val handle = SavedStateHandle(
            mapOf(
                SeismicMapViewModel.SAVED_TIME_WINDOW to "REMOVED_PERIOD",
                SeismicMapViewModel.SAVED_MAGNITUDE to 100,
                SeismicMapViewModel.SAVED_DEPTH_FILTER to
                    EarthquakeDepthFilter.SHALLOW.name
            )
        )
        val viewModel = createViewModel(handle)

        assertEquals(
            MapFilterSelection(
                timeWindow = RecentWindow.LAST_WEEK,
                magnitude = MapMagnitudeFilter.ALL,
                depthFilter = EarthquakeDepthFilter.SHALLOW
            ),
            viewModel.filters.value
        )
    }

    @Test
    fun changingOneFilter_doesNotOverwriteOtherSavedFilters() = runTest {
        val handle = SavedStateHandle(
            mapOf(
                SeismicMapViewModel.SAVED_TIME_WINDOW to
                    RecentWindow.LAST_DAY.name,
                SeismicMapViewModel.SAVED_MAGNITUDE to
                    MapMagnitudeFilter.MAGNITUDE_2_5.name,
                SeismicMapViewModel.SAVED_DEPTH_FILTER to
                    EarthquakeDepthFilter.INTERMEDIATE.name
            )
        )
        val viewModel = createViewModel(handle)

        viewModel.selectMagnitude(MapMagnitudeFilter.MAGNITUDE_4_5)

        assertEquals(
            MapFilterSelection(
                timeWindow = RecentWindow.LAST_DAY,
                magnitude = MapMagnitudeFilter.MAGNITUDE_4_5,
                depthFilter = EarthquakeDepthFilter.INTERMEDIATE
            ),
            viewModel.filters.value
        )
        assertEquals(
            RecentWindow.LAST_DAY.name,
            handle.get<String>(SeismicMapViewModel.SAVED_TIME_WINDOW)
        )
        assertEquals(
            EarthquakeDepthFilter.INTERMEDIATE.name,
            handle.get<String>(SeismicMapViewModel.SAVED_DEPTH_FILTER)
        )
        assertTrue(repository.syncRequests.isEmpty())
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle,
        clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    ): SeismicMapViewModel = SeismicMapViewModel(
        repository = repository,
        clock = clock,
        computationDispatcher = dispatcher,
        savedStateHandle = savedStateHandle
    ).also(viewModels::add)

    private class FakeRepository : EarthquakeRepository {
        val observedQueries = mutableListOf<EarthquakeQuery>()
        val syncRequests = mutableListOf<RecentWindow>()

        override fun observeEarthquakes(
            query: EarthquakeQuery
        ): Flow<List<Earthquake>> {
            observedQueries.add(query)
            return flowOf(emptyList())
        }

        override fun observeEarthquake(id: String): Flow<Earthquake?> =
            flowOf(null)

        override suspend fun refreshIfStale(window: RecentWindow): FeedSyncResult {
            syncRequests.add(window)
            return FeedSyncResult.UpToDate
        }

        override suspend fun refreshRecent(window: RecentWindow): RefreshResult =
            RefreshResult.Success

        override suspend fun getFeedCacheStatus(window: RecentWindow): FeedCacheStatus =
            FeedCacheStatus.Unknown
    }
}
