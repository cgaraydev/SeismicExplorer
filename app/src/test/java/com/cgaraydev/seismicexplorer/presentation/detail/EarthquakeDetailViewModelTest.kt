package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import com.cgaraydev.seismicexplorer.domain.repository.ScientificDetailRepository
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetail
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailFailure
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailResult
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailSource
import com.cgaraydev.seismicexplorer.presentation.navigation.EarthquakeDetailDestination
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult

@OptIn(ExperimentalCoroutinesApi::class)
class EarthquakeDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeRepository
    private lateinit var scienceRepository: FakeScientificRepository
    private lateinit var viewModel: EarthquakeDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRepository()
        scienceRepository = FakeScientificRepository()
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.cancel()
        }

        Dispatchers.resetMain()
    }

    @Test
    fun missingEarthquake_emitsNotFound() = runTest {
        createViewModel("missing")

        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        assertEquals(
            EarthquakeDetailUiState.NotFound,
            viewModel.uiState.value
        )
        assertEquals("missing", repository.observedId)
    }

    @Test
    fun cachedEarthquake_emitsContent() = runTest {
        val earthquake = sampleEarthquake()
        repository.earthquake.value = earthquake

        createViewModel("us123")

        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        assertEquals(
            EarthquakeDetailUiState.Content(earthquake),
            viewModel.uiState.value
        )
        assertEquals("us123", repository.observedId)
    }

    @Test
    fun updatingStoredEvent_updatesDetailState() = runTest {
        repository.earthquake.value = sampleEarthquake()

        createViewModel("us123")

        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        val updated = sampleEarthquake().copy(
            magnitude = 5.6,
            updatedAt = Instant.parse("2026-11-14T11:00:00Z")
        )

        repository.earthquake.value = updated
        advanceUntilIdle()

        assertEquals(
            EarthquakeDetailUiState.Content(updated),
            viewModel.uiState.value
        )
    }

    @Test
    fun scientificDetailIsNotRequestedJustByOpeningEarthquake() = runTest {
        repository.earthquake.value = sampleEarthquake()
        createViewModel("us123")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()
        assertEquals(ScientificDetailUiState.Idle, viewModel.scientificState.value)
        assertTrue(scienceRepository.calls.isEmpty())
    }

    @Test
    fun scientificRequestPassesEventRevisionAndRefreshesOnlyWhenPressedAgain() = runTest {
        repository.earthquake.value = sampleEarthquake()
        scienceRepository.result = ScientificDetailResult.Available(
            detail = ScientificDetail("us123", 1_700_000_000_000L, 1_700_000_010_000L,
                listOf("dyfi", "shakemap"), 3),
            source = ScientificDetailSource.CACHE,
            isStale = true
        )
        createViewModel("us123")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()
        viewModel.requestScientificDetail()
        advanceUntilIdle()
        assertEquals(1, scienceRepository.calls.size)
        assertEquals(Triple("us123", sampleEarthquake().updatedAt.toEpochMilli(), false),
            scienceRepository.calls.single())
        val available = viewModel.scientificState.value as ScientificDetailUiState.Available
        assertTrue(available.isStale)
        assertEquals(listOf("dyfi", "shakemap"), available.detail.availableProductTypes)
        viewModel.requestScientificDetail()
        advanceUntilIdle()
        assertEquals(2, scienceRepository.calls.size)
        assertEquals(true, scienceRepository.calls.last().third)
    }

    @Test
    fun scientificFailureIsAnExplicitViewState() = runTest {
        scienceRepository.result = ScientificDetailResult.Failed(ScientificDetailFailure.EVENT_NOT_FOUND)
        createViewModel("us123")
        viewModel.requestScientificDetail()
        advanceUntilIdle()
        assertEquals(ScientificDetailUiState.Error(ScientificDetailFailure.EVENT_NOT_FOUND),
            viewModel.scientificState.value)
    }

    private fun createViewModel(id: String) {
        viewModel = EarthquakeDetailViewModel(
            repository = repository,
            scientificRepository = scienceRepository,
            destination = EarthquakeDetailDestination(
                earthquakeId = id
            )
        )
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
        occurredAt = Instant.parse("2026-11-14T10:00:00Z"),
        updatedAt = Instant.parse("2026-11-14T10:30:00Z"),
        detailUrl = null
    )

    private class FakeScientificRepository : ScientificDetailRepository {
        val calls = mutableListOf<Triple<String, Long?, Boolean>>()
        var result: ScientificDetailResult =
            ScientificDetailResult.Failed(ScientificDetailFailure.CONNECTION)
        override suspend fun getDetail(
            eventId: String,
            knownUpdatedAtEpochMillis: Long?,
            forceRefresh: Boolean
        ): ScientificDetailResult {
            calls += Triple(eventId, knownUpdatedAtEpochMillis, forceRefresh)
            return result
        }
    }

    private class FakeRepository : EarthquakeRepository {

        val earthquake = MutableStateFlow<Earthquake?>(null)

        var observedId: String? = null

        override suspend fun getFeedCacheStatus(
            window: RecentWindow
        ): FeedCacheStatus {
            return FeedCacheStatus.Unknown
        }

        override fun observeEarthquakes(
            query: EarthquakeQuery
        ): Flow<List<Earthquake>> {
            return flowOf(emptyList())
        }

        override fun observeEarthquake(
            id: String
        ): Flow<Earthquake?> {
            observedId = id
            return earthquake
        }

        override suspend fun refreshRecent(
            window: RecentWindow
        ): RefreshResult {
            return RefreshResult.Success
        }

        override suspend fun refreshIfStale(
            window: RecentWindow
        ): FeedSyncResult {
            return FeedSyncResult.UpToDate
        }
    }
}