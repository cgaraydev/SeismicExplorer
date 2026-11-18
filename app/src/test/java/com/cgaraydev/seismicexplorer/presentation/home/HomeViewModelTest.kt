package com.cgaraydev.seismicexplorer.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
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
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus

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
    fun initialState_checksDailyFeedWithoutForcingRefresh() = runTest {
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
            repository.syncRequests
        )
        assertTrue(repository.refreshRequests.isEmpty())
        assertFalse(viewModel.uiState.value.isReadingLocal)
        assertEquals(
            HomeRefreshState.UpToDate,
            viewModel.uiState.value.refreshState
        )
    }

    @Test
    fun changingMagnitude_filtersRoomWithoutNewSync() = runTest {
        repository.events.value = listOf(
            earthquake("small", 1.2),
            earthquake("strong", 4.8)
        )

        createViewModel()
        advanceUntilIdle()

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
            repository.syncRequests
        )
        assertTrue(repository.refreshRequests.isEmpty())
        assertEquals(
            2.5,
            repository.observedQueries.last().minimumMagnitude!!,
            0.0
        )
        assertEquals(listOf("small", "strong"),
            viewModel.uiState.value.observatorySnapshot!!.earthquakes.map { it.id })
        assertEquals(2, repository.observedQueries.count { it.minimumMagnitude == null })
        assertEquals(3, repository.observedQueries.size)
    }

    @Test
    fun changingPeriod_checksSelectedFeedAndKeepsMagnitude() = runTest {
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

        assertEquals(
            TimeWindow.LAST_WEEK,
            viewModel.uiState.value.timeWindow
        )
        assertEquals(
            MagnitudeThreshold.MAGNITUDE_4_5,
            viewModel.uiState.value.minimumMagnitude
        )
        assertEquals(
            listOf(
                RecentWindow.LAST_DAY,
                RecentWindow.LAST_WEEK
            ),
            repository.syncRequests
        )
        assertTrue(repository.refreshRequests.isEmpty())
        assertEquals(
            now.minusSeconds(7L * 24 * 60 * 60),
            repository.observedQueries.last().startTime
        )
    }

    @Test
    fun roomEmission_updatesUiWithoutNewSync() = runTest {
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
        assertEquals(1, repository.syncRequests.size)
        assertTrue(repository.refreshRequests.isEmpty())
    }

    @Test
    fun syncFailure_preservesCachedEvents() = runTest {
        repository.events.value = listOf(
            earthquake("cached", 4.0)
        )

        repository.syncHandler = {
            FeedSyncResult.Failed(RefreshFailure.CONNECTION)
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
    fun successfulAutomaticSync_reportsCompleted() = runTest {
        repository.syncHandler = {
            FeedSyncResult.Updated
        }

        createViewModel()
        advanceUntilIdle()

        assertEquals(
            HomeRefreshState.Completed,
            viewModel.uiState.value.refreshState
        )
        assertTrue(repository.refreshRequests.isEmpty())
    }

    @Test
    fun manualRefresh_forcesDownloadAndRenewsQuery() = runTest {
        createViewModel()
        advanceUntilIdle()

        clock.currentInstant = now.plusSeconds(120)

        viewModel.onAction(HomeAction.Refresh)
        advanceUntilIdle()

        assertEquals(
            listOf(RecentWindow.LAST_DAY),
            repository.syncRequests
        )
        assertEquals(
            listOf(RecentWindow.LAST_DAY),
            repository.refreshRequests
        )
        assertEquals(
            now.plusSeconds(120),
            repository.observedQueries.last().endTime
        )
        assertEquals(
            HomeRefreshState.Completed,
            viewModel.uiState.value.refreshState
        )
    }

    @Test
    fun changingPeriod_cancelsOldSyncAndPreventsStaleError() = runTest {
        val pendingDailyResult =
            CompletableDeferred<FeedSyncResult>()

        var dailyCancelled = false

        repository.syncHandler = { window ->
            if (window == RecentWindow.LAST_DAY) {
                try {
                    pendingDailyResult.await()
                } finally {
                    dailyCancelled = true
                }
            } else {
                FeedSyncResult.Failed(RefreshFailure.SERVICE)
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

        pendingDailyResult.complete(FeedSyncResult.Updated)
        advanceUntilIdle()

        assertEquals(
            HomeRefreshState.Failed(RefreshFailure.SERVICE),
            viewModel.uiState.value.refreshState
        )
    }

    @Test
    fun failedSync_preservesStaleCacheStatus() = runTest {
        val staleStatus = FeedCacheStatus.Synced(
            lastSuccessEpochMillis =
                now.minusSeconds(60 * 60).toEpochMilli(),
            isFresh = false
        )

        repository.cacheStatus = staleStatus

        repository.syncHandler = {
            FeedSyncResult.Failed(
                RefreshFailure.CONNECTION
            )
        }

        createViewModel()
        advanceUntilIdle()

        assertEquals(
            staleStatus,
            viewModel.uiState.value.feedCacheStatus
        )

        assertEquals(
            HomeRefreshState.Failed(
                RefreshFailure.CONNECTION
            ),
            viewModel.uiState.value.refreshState
        )
    }

    @Test
    fun firstResume_doesNotDuplicateInitialRefreshOrObservation() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onResumed()
        advanceUntilIdle()

        assertEquals(listOf(RecentWindow.LAST_DAY), repository.syncRequests)
        assertEquals(2, repository.observedQueries.size)
        assertTrue(repository.refreshRequests.isEmpty())
    }

    @Test
    fun returningFromMap_includesNewEarthquakesBeyondOldQueryEnd() = runTest {
        createViewModel()
        advanceUntilIdle()
        viewModel.onResumed() // Primera apertura: init ya consulto Room.

        repository.events.value = listOf(
            earthquake("new-after-map-sync", 4.2).copy(
                occurredAt = now.plusSeconds(60)
            )
        )
        advanceUntilIdle()

        // El evento no encaja en la ventana original de Home.
        assertTrue(viewModel.uiState.value.earthquakes.isEmpty())

        clock.currentInstant = now.plusSeconds(120)
        viewModel.onResumed()
        advanceUntilIdle()

        assertEquals(
            listOf("new-after-map-sync"),
            viewModel.uiState.value.earthquakes.map { it.id }
        )
        assertEquals(now.plusSeconds(120), repository.observedQueries.last().endTime)
        assertEquals(2, repository.syncRequests.size)
        assertTrue(repository.refreshRequests.isEmpty())
    }

    @Test
    fun periodSelection_isAtomicAndOlderObservationCannotReplaceNewWindow() = runTest {
        repository.events.value = listOf(earthquake("daily", 3.0))
        createViewModel()
        advanceUntilIdle()
        val first = viewModel.uiState.value.observatorySnapshot!!
        assertEquals(TimeWindow.LAST_DAY, first.timeWindow)

        viewModel.onAction(HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK))
        assertEquals(null, viewModel.uiState.value.observatorySnapshot)
        assertTrue(viewModel.uiState.value.isReadingObservatory)
        viewModel.onAction(HomeAction.SelectTimeWindow(TimeWindow.LAST_MONTH))
        advanceUntilIdle()

        val latest = viewModel.uiState.value.observatorySnapshot!!
        assertEquals(TimeWindow.LAST_MONTH, latest.timeWindow)
        assertEquals(now, latest.endAt)
        assertEquals(listOf("daily"), latest.earthquakes.map { it.id })
        assertEquals(RecentWindow.LAST_30_DAYS.duration,
            java.time.Duration.between(repository.observedQueries.last().startTime,
                repository.observedQueries.last().endTime))
    }

    @Test
    fun refreshKeepsOldSnapshotAndItsEndUntilReplacementArrives() = runTest {
        repository.events.value = listOf(earthquake("old", 3.2))
        createViewModel()
        advanceUntilIdle()
        val previous = viewModel.uiState.value.observatorySnapshot!!
        clock.currentInstant = now.plusSeconds(120)

        viewModel.onAction(HomeAction.Refresh)
        val pending = viewModel.uiState.value
        assertEquals(previous, pending.observatorySnapshot)
        assertTrue(pending.isReadingObservatory)
        assertEquals(now, pending.observatorySnapshot!!.endAt)
        assertEquals(now.plusSeconds(120), pending.periodEndAt)

        advanceUntilIdle()
        assertEquals(now.plusSeconds(120), viewModel.uiState.value.observatorySnapshot!!.endAt)
        assertEquals(previous.earthquakes, viewModel.uiState.value.observatorySnapshot!!.earthquakes)
    }

    @Test
    fun failedSyncStillShowsConsistentCachedObservatorySnapshot() = runTest {
        repository.events.value = listOf(earthquake("cached", 4.2))
        repository.syncHandler = { FeedSyncResult.Failed(RefreshFailure.CONNECTION) }
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HomeRefreshState.Failed(RefreshFailure.CONNECTION), state.refreshState)
        assertEquals(listOf("cached"), state.observatorySnapshot!!.earthquakes.map { it.id })
        assertEquals(now, state.observatorySnapshot.endAt)
    }

    @Test
    fun differentViewModelInstancesKeepIndependentSelections() = runTest {
        val summary = HomeViewModel(repository, clock, SavedStateHandle())
        val catalog = HomeViewModel(repository, clock, SavedStateHandle())
        viewModel = summary
        advanceUntilIdle()

        summary.onAction(HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK))
        catalog.onAction(HomeAction.SelectTimeWindow(TimeWindow.LAST_MONTH))
        catalog.onAction(HomeAction.SelectMagnitude(MagnitudeThreshold.MAGNITUDE_4_5))
        advanceUntilIdle()

        assertEquals(TimeWindow.LAST_WEEK, summary.uiState.value.timeWindow)
        assertEquals(MagnitudeThreshold.ALL, summary.uiState.value.minimumMagnitude)
        assertEquals(TimeWindow.LAST_MONTH, catalog.uiState.value.timeWindow)
        assertEquals(MagnitudeThreshold.MAGNITUDE_4_5, catalog.uiState.value.minimumMagnitude)
        catalog.viewModelScope.cancel()
    }

    @Test
    fun savedStateRestoresPeriodAndMagnitudeAfterRecreation() = runTest {
        val handle = SavedStateHandle()
        viewModel = HomeViewModel(repository, clock, handle)
        advanceUntilIdle()

        viewModel.onAction(HomeAction.SelectTimeWindow(TimeWindow.LAST_MONTH))
        viewModel.onAction(HomeAction.SelectMagnitude(MagnitudeThreshold.MAGNITUDE_2_5))
        advanceUntilIdle()
        assertEquals("LAST_MONTH", handle.get<String>("home_selected_time_window"))
        assertEquals("MAGNITUDE_2_5", handle.get<String>("home_selected_magnitude"))

        viewModel.viewModelScope.cancel()
        viewModel = HomeViewModel(repository, clock, handle)
        advanceUntilIdle()
        assertEquals(TimeWindow.LAST_MONTH, viewModel.uiState.value.timeWindow)
        assertEquals(MagnitudeThreshold.MAGNITUDE_2_5, viewModel.uiState.value.minimumMagnitude)
        assertEquals(RecentWindow.LAST_30_DAYS, repository.syncRequests.last())
    }

    @Test
    fun invalidSavedFilterNamesFallBackToDefaults() = runTest {
        viewModel = HomeViewModel(
            repository, clock, SavedStateHandle(
                mapOf(
                    "home_selected_time_window" to "REMOVED_WINDOW",
                    "home_selected_magnitude" to "REMOVED_MAGNITUDE"
                )
            )
        )
        advanceUntilIdle()
        assertEquals(TimeWindow.LAST_DAY, viewModel.uiState.value.timeWindow)
        assertEquals(MagnitudeThreshold.ALL, viewModel.uiState.value.minimumMagnitude)
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

        val events =
            MutableStateFlow<List<Earthquake>>(emptyList())

        val syncRequests = mutableListOf<RecentWindow>()
        val refreshRequests = mutableListOf<RecentWindow>()
        val observedQueries = mutableListOf<EarthquakeQuery>()

        var syncHandler: suspend (RecentWindow) -> FeedSyncResult = {
            FeedSyncResult.UpToDate
        }

        var refreshHandler: suspend (RecentWindow) -> RefreshResult = {
            RefreshResult.Success
        }

        var cacheStatus: FeedCacheStatus =
            FeedCacheStatus.Unknown

        override suspend fun getFeedCacheStatus(
            window: RecentWindow
        ): FeedCacheStatus {
            return cacheStatus
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
                                                earthquake.magnitude >=
                                                query.minimumMagnitude
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
            syncRequests.add(window)
            return syncHandler(window)
        }
    }
}