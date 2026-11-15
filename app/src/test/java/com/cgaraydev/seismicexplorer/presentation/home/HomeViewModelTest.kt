package com.cgaraydev.seismicexplorer.presentation.home

import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
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
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val now = Instant.parse("2026-11-14T12:00:00Z")
    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeEarthquakeRepository
    private lateinit var clock: MutableClock
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        repository = FakeEarthquakeRepository()
        clock = MutableClock(now)
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.cancel()
        }

        Dispatchers.resetMain()
    }

    @Test
    fun initialState_observesLocalCacheAndRefreshesDailyFeed() = runTest {
        createViewModel()
        advanceUntilIdle()

        assertEquals(
            TimeWindow.LAST_DAY,
            viewModel.uiState.value.timeWindow
        )
        assertEquals(
            MagnitudeThreshold.ALL,
            viewModel.uiState.value.minimumMagnitude
        )
        assertEquals(
            listOf(RecentWindow.LAST_DAY),
            repository.refreshRequests
        )
        assertFalse(viewModel.uiState.value.isReadingLocal)
        assertEquals(
            HomeRefreshState.Completed,
            viewModel.uiState.value.refreshState
        )
    }

    @Test
    fun changingMagnitude_filtersRoomWithoutRefreshingNetwork() = runTest {
        repository.events.value = listOf(
            earthquake("small", 1.2),
            earthquake("strong", 4.8)
        )

        createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.earthquakes.size)

        viewModel.onAction(
            HomeAction.SelectMagnitude(
                MagnitudeThreshold.MAGNITUDE_2_5
            )
        )
        advanceUntilIdle()

        assertEquals(
            listOf("strong"),
            viewModel.uiState.value.earthquakes.map { it.id }
        )
        assertEquals(
            listOf(RecentWindow.LAST_DAY),
            repository.refreshRequests
        )
        assertEquals(
            2.5,
            repository.observedQueries.last().minimumMagnitude!!,
            0.0
        )
    }

    @Test
    fun changingPeriod_refreshesSelectedFeedAndKeepsMagnitude() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onAction(
            HomeAction.SelectMagnitude(
                MagnitudeThreshold.MAGNITUDE_4_5
            )
        )
        advanceUntilIdle()

        viewModel.onAction(
            HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(TimeWindow.LAST_WEEK, state.timeWindow)
        assertEquals(
            MagnitudeThreshold.MAGNITUDE_4_5,
            state.minimumMagnitude
        )
        assertEquals(
            listOf(
                RecentWindow.LAST_DAY,
                RecentWindow.LAST_WEEK
            ),
            repository.refreshRequests
        )
        assertEquals(
            now.minusSeconds(7L * 24 * 60 * 60),
            repository.observedQueries.last().startTime
        )
    }

    @Test
    fun roomEmission_updatesUiWithoutNewNetworkRequest() = runTest {
        createViewModel()
        advanceUntilIdle()

        repository.events.value = listOf(
            earthquake("new-event", 4.1)
        )
        advanceUntilIdle()

        assertEquals(
            listOf("new-event"),
            viewModel.uiState.value.earthquakes.map { it.id }
        )
        assertEquals(1, repository.refreshRequests.size)
    }

    @Test
    fun refreshFailure_preservesCachedEvents() = runTest {
        repository.events.value = listOf(
            earthquake("cached", 4.0)
        )

        repository.refreshHandler = {
            RefreshResult.Failure(RefreshFailure.CONNECTION)
        }

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(
            listOf("cached"),
            state.earthquakes.map { it.id }
        )
        assertEquals(
            HomeRefreshState.Failed(RefreshFailure.CONNECTION),
            state.refreshState
        )
        assertFalse(state.isReadingLocal)
    }

    @Test
    fun manualRefresh_renewsQueryAndRequestsCurrentFeed() = runTest {
        createViewModel()
        advanceUntilIdle()

        clock.currentInstant = now.plusSeconds(120)

        viewModel.onAction(HomeAction.Refresh)
        advanceUntilIdle()

        assertEquals(
            listOf(
                RecentWindow.LAST_DAY,
                RecentWindow.LAST_DAY
            ),
            repository.refreshRequests
        )
        assertEquals(
            now.plusSeconds(120),
            repository.observedQueries.last().endTime
        )
    }

    @Test
    fun changingPeriod_cancelsOldRefreshAndPreventsStaleError() = runTest {
        val pendingDailyResult = CompletableDeferred<RefreshResult>()
        var dailyCancelled = false

        repository.refreshHandler = { window ->
            if (window == RecentWindow.LAST_DAY) {
                try {
                    pendingDailyResult.await()
                } finally {
                    dailyCancelled = true
                }
            } else {
                RefreshResult.Failure(RefreshFailure.SERVICE)
            }
        }

        createViewModel()
        advanceUntilIdle()

        assertEquals(
            HomeRefreshState.Refreshing,
            viewModel.uiState.value.refreshState
        )

        viewModel.onAction(
            HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK)
        )
        advanceUntilIdle()

        assertTrue(dailyCancelled)
        assertEquals(
            HomeRefreshState.Failed(RefreshFailure.SERVICE),
            viewModel.uiState.value.refreshState
        )

        pendingDailyResult.complete(RefreshResult.Success)
        advanceUntilIdle()

        assertEquals(
            HomeRefreshState.Failed(RefreshFailure.SERVICE),
            viewModel.uiState.value.refreshState
        )
    }

    private fun createViewModel() {
        viewModel = HomeViewModel(
            repository = repository,
            clock = clock
        )
    }

    private fun earthquake(
        id: String,
        magnitude: Double
    ): Earthquake {
        return Earthquake(
            id = id,
            magnitude = magnitude,
            place = "Coast of Chile",
            coordinates = GeoCoordinates(
                latitude = -33.4,
                longitude = -71.6
            ),
            depthKilometers = 12.0,
            occurredAt = now.minusSeconds(3_600),
            updatedAt = now.minusSeconds(1_800),
            detailUrl = null
        )
    }

    private class MutableClock(
        var currentInstant: Instant
    ) : Clock() {

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock {
            return Clock.fixed(currentInstant, zone)
        }

        override fun instant(): Instant = currentInstant
    }

    private class FakeEarthquakeRepository : EarthquakeRepository {

        val events = MutableStateFlow<List<Earthquake>>(emptyList())

        val refreshRequests = mutableListOf<RecentWindow>()
        val observedQueries = mutableListOf<EarthquakeQuery>()

        var refreshHandler: suspend (RecentWindow) -> RefreshResult = {
            RefreshResult.Success
        }

        override fun observeEarthquakes(
            query: EarthquakeQuery
        ): Flow<List<Earthquake>> {
            observedQueries.add(query)

            return events.map { earthquakes ->
                earthquakes.filter { earthquake ->
                    val isWithinPeriod =
                        !earthquake.occurredAt.isBefore(query.startTime) &&
                                earthquake.occurredAt.isBefore(query.endTime)

                    val matchesMagnitude =
                        query.minimumMagnitude == null ||
                                (
                                        earthquake.magnitude != null &&
                                                earthquake.magnitude >= query.minimumMagnitude
                                        )

                    isWithinPeriod && matchesMagnitude
                }
            }
        }

        override fun observeEarthquake(
            id: String
        ): Flow<Earthquake?> {
            return events.map { earthquakes ->
                earthquakes.firstOrNull { it.id == id }
            }
        }

        override suspend fun refreshRecent(
            window: RecentWindow
        ): RefreshResult {
            refreshRequests.add(window)
            return refreshHandler(window)
        }


        override suspend fun refreshIfStale(
            window: RecentWindow
        ): FeedSyncResult {
            return FeedSyncResult.UpToDate
        }

    }
}