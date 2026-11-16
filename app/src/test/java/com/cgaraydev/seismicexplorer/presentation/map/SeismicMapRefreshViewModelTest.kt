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
    fun overlappingRequests_doNotRunSimultaneously() =
        runTest {
            // Mantiene suspendida la primera descarga.
            val gate = CompletableDeferred<Unit>()

            repository.conditionalGate = gate

            // Usamos el scope de runTest, no backgroundScope.
            val firstRequest = launch {
                viewModel.ensureFeedFresh(
                    RecentWindow.LAST_WEEK
                )
            }

            // Ejecuta la primera corutina hasta
            // que quede suspendida en gate.await().
            runCurrent()

            assertEquals(
                1,
                repository.conditionalRequests.size
            )

            assertTrue(firstRequest.isActive)

            assertTrue(
                viewModel.syncState.value
                        is MapSyncState.Syncing
            )

            // Intentamos otra descarga mientras
            // la primera todavía tiene el Mutex.
            viewModel.refreshFeed(
                RecentWindow.LAST_WEEK
            )

            // No debe comenzar una descarga forzada.
            assertTrue(
                repository.forcedRequests.isEmpty()
            )

            assertEquals(
                1,
                repository.conditionalRequests.size
            )

            // Permitimos terminar la primera descarga.
            gate.complete(Unit)

            // join() garantiza que terminó el finally
            // y que el estado ya está actualizado.
            firstRequest.join()

            assertEquals(
                MapSyncState.Ready(
                    window = RecentWindow.LAST_WEEK,
                    downloaded = false
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

            // Ejecuta hasta suspender en el repositorio.
            runCurrent()

            assertEquals(
                1,
                repository.conditionalRequests.size
            )

            assertTrue(firstRequest.isActive)

            assertTrue(
                viewModel.syncState.value
                        is MapSyncState.Syncing
            )

            // Cancela y espera a que termine.
            // Así verificamos el estado después
            // de ejecutarse el bloque finally.
            firstRequest.cancelAndJoin()

            assertEquals(
                MapSyncState.Idle,
                viewModel.syncState.value
            )

            // El siguiente intento no debe quedar
            // bloqueado por la cancelación anterior.
            repository.conditionalGate = null

            viewModel.refreshFeed(
                RecentWindow.LAST_WEEK
            )

            assertEquals(
                1,
                repository.forcedRequests.size
            )

            assertEquals(
                MapSyncState.Ready(
                    window = RecentWindow.LAST_WEEK,
                    downloaded = true
                ),
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

            // Simula una descarga de duración
            // controlada por la prueba.
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