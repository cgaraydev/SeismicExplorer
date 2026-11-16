package com.cgaraydev.seismicexplorer.presentation.map

import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
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
import kotlinx.coroutines.CompletableDeferred
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

@OptIn(ExperimentalCoroutinesApi::class)
class SeismicMapLoadingContinuityViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-14T12:00:00Z")
    private val repository = DelayedFilterRepository(now)
    private lateinit var viewModel: SeismicMapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
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
    fun delayedFilterKeepsGeoJsonUntilRoomReturnsRealEmptyResult() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val initial = viewModel.uiState.value
        assertFalse(initial.isLoading)
        assertEquals(1, initial.eventCount)
        assertEquals(1, repository.observations)

        viewModel.selectMagnitude(MapMagnitudeFilter.MAGNITUDE_4_5)
        advanceUntilIdle()

        val loading = viewModel.uiState.value
        assertTrue(loading.isLoading)
        assertEquals(MapMagnitudeFilter.MAGNITUDE_4_5, loading.filters.magnitude)
        assertEquals(initial.geoJson, loading.geoJson)
        assertEquals(initial.eventBounds, loading.eventBounds)
        assertEquals(1, loading.eventCount)
        assertEquals(2, repository.observations)

        repository.completeFilteredQuery()
        advanceUntilIdle()

        val emptyResult = viewModel.uiState.value
        assertFalse(emptyResult.isLoading)
        assertFalse(emptyResult.hasError)
        assertEquals(0, emptyResult.eventCount)
        assertEquals(MapEarthquakeGeoJson.EMPTY, emptyResult.geoJson)
        assertEquals(null, emptyResult.eventBounds)
    }

    private class DelayedFilterRepository(
        private val now: Instant
    ) : EarthquakeRepository {

        private val filteredQueryGate = CompletableDeferred<Unit>()
        var observations = 0
            private set

        override fun observeEarthquakes(query: EarthquakeQuery): Flow<List<Earthquake>> = flow {
            observations++
            if (query.minimumMagnitude != null) {
                filteredQueryGate.await()
                emit(emptyList())
            } else {
                emit(
                    listOf(
                        Earthquake(
                            id = "us123",
                            magnitude = 2.5,
                            place = "Coast of Chile",
                            coordinates = GeoCoordinates(-33.4, -71.6),
                            depthKilometers = 12.0,
                            occurredAt = now.minusSeconds(3_600),
                            updatedAt = now.minusSeconds(1_800),
                            detailUrl = null
                        )
                    )
                )
            }
        }

        fun completeFilteredQuery() {
            filteredQueryGate.complete(Unit)
        }

        override fun observeEarthquake(id: String): Flow<Earthquake?> = flowOf(null)

        override suspend fun refreshRecent(window: RecentWindow): RefreshResult =
            RefreshResult.Success

        override suspend fun refreshIfStale(window: RecentWindow): FeedSyncResult =
            FeedSyncResult.UpToDate

        override suspend fun getFeedCacheStatus(window: RecentWindow): FeedCacheStatus =
            FeedCacheStatus.Unknown
    }
}
