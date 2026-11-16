package com.cgaraydev.seismicexplorer.presentation.map

import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SeismicMapLocalRetryInstrumentedTest {

    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-14T12:00:00Z")
    private lateinit var repository: FakeRepository
    private lateinit var viewModel: SeismicMapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRepository()
        viewModel = SeismicMapViewModel(
            repository = repository,
            clock = Clock.fixed(now, ZoneOffset.UTC),
            computationDispatcher = dispatcher
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun retryLocalObservation_recoversFromReadFailureWithoutNetwork() = runTest {
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasError)
        assertEquals(1, repository.observedQueries.size)
        assertEquals(MapSyncState.Idle, viewModel.syncState.value)

        repository.shouldFailObservation = false
        viewModel.retryLocalObservation()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.eventCount)
        assertEquals(2, repository.observedQueries.size)
        assertEquals(MapSyncState.Idle, viewModel.syncState.value)
        assertTrue(repository.conditionalRequests.isEmpty())
        assertTrue(repository.forcedRequests.isEmpty())
    }

    @Test
    fun repeatedLocalRetry_restartsReadWithoutChangingQueryOrFilters() = runTest {
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()
        val originalQuery = repository.observedQueries.single()
        assertTrue(viewModel.uiState.value.hasError)

        viewModel.retryLocalObservation()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasError)
        assertEquals(2, repository.observedQueries.size)

        viewModel.retryLocalObservation()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasError)
        assertEquals(3, repository.observedQueries.size)

        assertTrue(repository.observedQueries.all { it == originalQuery })
        assertEquals(RecentWindow.LAST_WEEK, viewModel.filters.value.timeWindow)
        assertEquals(MapMagnitudeFilter.ALL, viewModel.filters.value.magnitude)
        assertEquals(EarthquakeDepthFilter.ALL, viewModel.filters.value.depthFilter)
        assertTrue(repository.conditionalRequests.isEmpty())
        assertTrue(repository.forcedRequests.isEmpty())
    }

    @Test
    fun successfulNetworkCheckDoesNotAutomaticallyRepairTerminatedLocalFlow() = runTest {
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasError)

        // Con el reloj fijo, la comprobación del feed no altera el ancla.
        viewModel.ensureFeedFresh(RecentWindow.LAST_WEEK)
        advanceUntilIdle()

        assertEquals(1, repository.observedQueries.size)
        assertTrue(viewModel.uiState.value.hasError)
        assertEquals(1, repository.conditionalRequests.size)

        repository.shouldFailObservation = false
        viewModel.retryLocalObservation()
        advanceUntilIdle()
        assertEquals(2, repository.observedQueries.size)
        assertFalse(viewModel.uiState.value.hasError)
    }

    private inner class FakeRepository : EarthquakeRepository {
        var shouldFailObservation = true
        val observedQueries = mutableListOf<EarthquakeQuery>()
        val conditionalRequests = mutableListOf<RecentWindow>()
        val forcedRequests = mutableListOf<RecentWindow>()

        override fun observeEarthquakes(
            query: EarthquakeQuery
        ): Flow<List<Earthquake>> = flow {
            observedQueries.add(query)
            if (shouldFailObservation) {
                throw IllegalStateException("Room read failed")
            }
            emit(
                listOf(
                    Earthquake(
                        id = "us-local-test",
                        magnitude = 4.3,
                        place = "Chile",
                        coordinates = GeoCoordinates(-33.4, -71.6),
                        depthKilometers = 12.0,
                        occurredAt = now.minusSeconds(3600),
                        updatedAt = now.minusSeconds(1800),
                        detailUrl = null
                    )
                )
            )
        }

        override fun observeEarthquake(id: String): Flow<Earthquake?> =
            flowOf(null)

        override suspend fun refreshIfStale(
            window: RecentWindow
        ): FeedSyncResult {
            conditionalRequests.add(window)
            return FeedSyncResult.UpToDate
        }

        override suspend fun refreshRecent(
            window: RecentWindow
        ): RefreshResult {
            forcedRequests.add(window)
            return RefreshResult.Success
        }

        override suspend fun getFeedCacheStatus(
            window: RecentWindow
        ): FeedCacheStatus = FeedCacheStatus.Unknown
    }
}
