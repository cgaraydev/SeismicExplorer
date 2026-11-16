package com.cgaraydev.seismicexplorer.presentation.map

import androidx.lifecycle.viewModelScope
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeismicMapRefreshViewModelTest {

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

        viewModel = SeismicMapViewModel(
            repository = repository,
            clock = Clock.fixed(
                now,
                ZoneOffset.UTC
            ),
            computationDispatcher = dispatcher
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun automaticRefresh_usesConditionalSynchronization() =
        runTest {
            viewModel.ensureFeedFresh(
                RecentWindow.LAST_WEEK
            )

            assertEquals(
                listOf(RecentWindow.LAST_WEEK),
                repository.conditionalRequests
            )

            assertTrue(
                repository.forcedRequests.isEmpty()
            )

            assertEquals(
                MapSyncState.Ready(
                    window = RecentWindow.LAST_WEEK,
                    downloaded = false
                ),
                viewModel.syncState.value
            )
        }

    @Test
    fun manualRefresh_forcesDownloadWithoutChangingFilters() =
        runTest {
            viewModel.refreshFeed(
                RecentWindow.LAST_WEEK
            )

            assertEquals(
                listOf(RecentWindow.LAST_WEEK),
                repository.forcedRequests
            )

            assertTrue(
                repository.conditionalRequests.isEmpty()
            )

            assertEquals(
                RecentWindow.LAST_WEEK,
                viewModel.filters.value.timeWindow
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
    fun manualRefreshFailure_reportsFailure() =
        runTest {
            repository.forcedResult =
                RefreshResult.Failure(
                    RefreshFailure.CONNECTION
                )

            viewModel.refreshFeed(
                RecentWindow.LAST_WEEK
            )

            assertEquals(
                MapSyncState.Failed(
                    window = RecentWindow.LAST_WEEK,
                    reason = RefreshFailure.CONNECTION
                ),
                viewModel.syncState.value
            )
        }

    @Test
    fun overlappingRequests_areSerializedNotDiscarded() =
        runTest {
            val gate = CompletableDeferred<Unit>()

            repository.conditionalGate = gate

            val firstRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_WEEK
                )
            }

            runCurrent()

            assertEquals(
                1,
                repository.conditionalRequests.size
            )

            assertTrue(firstRequest.isActive)

            val secondRequest = launch {
                viewModel.refreshFeed(
                    RecentWindow.LAST_WEEK
                )
            }

            runCurrent()

            // La segunda espera: no hay concurrencia.
            assertTrue(
                repository.forcedRequests.isEmpty()
            )

            assertTrue(secondRequest.isActive)

            // Terminamos la primera solicitud.
            gate.complete(Unit)

            firstRequest.join()
            secondRequest.join()

            // Ahora la solicitud manual sí se ejecutó.
            assertEquals(
                listOf(RecentWindow.LAST_WEEK),
                repository.forcedRequests
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
    fun cancellation_releasesSynchronizationLock() =
        runTest {
            val gate = CompletableDeferred<Unit>()

            repository.conditionalGate = gate

            val firstRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_WEEK
                )
            }

            runCurrent()

            assertTrue(firstRequest.isActive)

            assertTrue(
                viewModel.syncState.value
                        is MapSyncState.Syncing
            )

            firstRequest.cancelAndJoin()

            assertEquals(
                MapSyncState.Idle,
                viewModel.syncState.value
            )

            repository.conditionalGate = null

            viewModel.refreshFeed(
                RecentWindow.LAST_WEEK
            )

            assertEquals(
                listOf(RecentWindow.LAST_WEEK),
                repository.forcedRequests
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
    fun changingPeriod_waitsAndSynchronizesNewWindow() =
        runTest {
            val gate = CompletableDeferred<Unit>()

            repository.conditionalGate = gate

            // El período inicial es LAST_WEEK.
            val firstRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_WEEK
                )
            }

            runCurrent()

            viewModel.selectTimeWindow(
                RecentWindow.LAST_30_DAYS
            )

            val newWindowRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_30_DAYS
                )
            }

            runCurrent()

            // El nuevo período todavía está esperando.
            assertEquals(
                listOf(RecentWindow.LAST_WEEK),
                repository.conditionalRequests
            )

            gate.complete(Unit)

            firstRequest.join()
            newWindowRequest.join()

            // La solicitud nueva no se perdió.
            assertEquals(
                listOf(
                    RecentWindow.LAST_WEEK,
                    RecentWindow.LAST_30_DAYS
                ),
                repository.conditionalRequests
            )

            assertEquals(
                MapSyncState.Ready(
                    window = RecentWindow.LAST_30_DAYS,
                    downloaded = false
                ),
                viewModel.syncState.value
            )
        }

    @Test
    fun supersededWaitingWindow_isSkipped() =
        runTest {
            val gate = CompletableDeferred<Unit>()

            repository.conditionalGate = gate

            val firstRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_WEEK
                )
            }

            runCurrent()

            // Primera selección pendiente.
            viewModel.selectTimeWindow(
                RecentWindow.LAST_30_DAYS
            )

            val obsoleteRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_30_DAYS
                )
            }

            runCurrent()

            // El usuario vuelve a cambiar de período.
            viewModel.selectTimeWindow(
                RecentWindow.LAST_DAY
            )

            val latestRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_DAY
                )
            }

            runCurrent()

            gate.complete(Unit)

            firstRequest.join()
            obsoleteRequest.join()
            latestRequest.join()

            // LAST_30_DAYS ya no es relevante.
            assertEquals(
                listOf(
                    RecentWindow.LAST_WEEK,
                    RecentWindow.LAST_DAY
                ),
                repository.conditionalRequests
            )

            assertEquals(
                MapSyncState.Ready(
                    window = RecentWindow.LAST_DAY,
                    downloaded = false
                ),
                viewModel.syncState.value
            )
        }

    @Test
    fun cancelledWaitingRequest_doesNotDownload() =
        runTest {
            val gate = CompletableDeferred<Unit>()

            repository.conditionalGate = gate

            val firstRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_WEEK
                )
            }

            runCurrent()

            viewModel.selectTimeWindow(
                RecentWindow.LAST_30_DAYS
            )

            val waitingRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_30_DAYS
                )
            }

            runCurrent()

            // Simula que la pantalla deja de necesitar
            // esta solicitud antes de adquirir el Mutex.
            waitingRequest.cancelAndJoin()

            gate.complete(Unit)
            firstRequest.join()

            assertEquals(
                listOf(RecentWindow.LAST_WEEK),
                repository.conditionalRequests
            )

            assertEquals(
                MapSyncState.Idle,
                viewModel.syncState.value
            )
        }

    private class FakeRepository : EarthquakeRepository {

        val conditionalRequests =
            mutableListOf<RecentWindow>()

        val forcedRequests =
            mutableListOf<RecentWindow>()

        var conditionalGate: CompletableDeferred<Unit>? = null

        var forcedResult: RefreshResult =
            RefreshResult.Success

        override fun observeEarthquakes(
            query: EarthquakeQuery
        ): Flow<List<Earthquake>> {
            return flowOf(emptyList())
        }

        override fun observeEarthquake(
            id: String
        ): Flow<Earthquake?> {
            return flowOf(null)
        }

        override suspend fun getFeedCacheStatus(
            window: RecentWindow
        ): FeedCacheStatus {
            return FeedCacheStatus.Unknown
        }

        override suspend fun refreshIfStale(
            window: RecentWindow
        ): FeedSyncResult {
            conditionalRequests.add(window)

            // Permite suspender la sincronización
            // en las pruebas de concurrencia.
            conditionalGate?.await()

            return FeedSyncResult.UpToDate
        }

        override suspend fun refreshRecent(
            window: RecentWindow
        ): RefreshResult {
            forcedRequests.add(window)

            return forcedResult
        }
    }
}