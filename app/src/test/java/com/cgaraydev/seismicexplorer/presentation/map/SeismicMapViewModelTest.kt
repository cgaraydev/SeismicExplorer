package com.cgaraydev.seismicexplorer.presentation.map

import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeismicMapViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val now = Instant.parse("2026-11-14T12:00:00Z")

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

        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        val query = repository.observedQueries.single()

        assertEquals(now, query.endTime)
        assertEquals(now.minusSeconds(7L * 24 * 60 * 60), query.startTime)
        assertEquals(null, query.minimumMagnitude)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, viewModel.uiState.value.eventCount)
    }

    @Test
    fun roomEmission_updatesMapWithoutNetworkRequests() = runTest {
        createViewModel()

        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        repository.events.value = listOf(sampleEarthquake())
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.eventCount)

        val geoJson = viewModel.uiState.value.geoJson

        assertEquals(
            MapEarthquakeGeoJson.encode(listOf(sampleEarthquake())),
            geoJson
        )

        assertEquals(0, repository.refreshRequests)
    }


    @Test
    fun changingMapFilters_updatesRoomQueryWithoutNetworkRefresh() = runTest {
        createViewModel()

        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiState.collect()
        }

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
        assertEquals(4.5, query.minimumMagnitude)

        assertEquals(
            RecentWindow.LAST_30_DAYS,
            viewModel.uiState.value.filters.timeWindow
        )
        assertEquals(
            MapMagnitudeFilter.MAGNITUDE_4_5,
            viewModel.uiState.value.filters.magnitude
        )

        assertEquals(0, repository.refreshRequests)
    }


    private fun createViewModel() {
        viewModel = SeismicMapViewModel(
            repository = repository,
            clock = Clock.fixed(now, ZoneOffset.UTC),
            computationDispatcher = dispatcher
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
        occurredAt = now.minusSeconds(3600),
        updatedAt = now.minusSeconds(1800),
        detailUrl = null
    )

    private class FakeRepository : EarthquakeRepository {

        val events = MutableStateFlow<List<Earthquake>>(emptyList())

        val observedQueries = mutableListOf<EarthquakeQuery>()

        var refreshRequests = 0

        override fun observeEarthquakes(
            query: EarthquakeQuery
        ): Flow<List<Earthquake>> {
            observedQueries.add(query)
            return events
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