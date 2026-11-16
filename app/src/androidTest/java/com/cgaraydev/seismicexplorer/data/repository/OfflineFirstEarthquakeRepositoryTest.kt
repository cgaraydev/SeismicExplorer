package com.cgaraydev.seismicexplorer.data.repository

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cgaraydev.seismicexplorer.data.local.SeismicDatabase
import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.remote.api.UsgsEarthquakeApi
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeCollectionDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeFeatureDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeMetadataDto
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeGeometryDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakePropertiesDto
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import java.time.Clock
import java.time.ZoneOffset
import org.junit.Assert.assertNull
import com.cgaraydev.seismicexplorer.data.local.entity.FeedSyncEntity
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus

@RunWith(AndroidJUnit4::class)
class OfflineFirstEarthquakeRepositoryTest {

    private lateinit var database: SeismicDatabase
    private lateinit var dao: EarthquakeDao
    private lateinit var api: FakeUsgsApi
    private lateinit var repository: OfflineFirstEarthquakeRepository

    private val now = Instant.parse("2026-11-14T12:00:00Z")

    private val dayQuery = RecentWindow.LAST_DAY.toQuery(now)
    private val weekQuery = RecentWindow.LAST_WEEK.toQuery(now)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder<SeismicDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()

        dao = database.earthquakeDao()
        api = FakeUsgsApi()

        repository = OfflineFirstEarthquakeRepository(
            api = api,
            dao = dao,
            feedSyncDao = database.feedSyncDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun refresh_persistsRemoteEventsAndReadsThemFromRoom() = runTest {
        api.response = sampleResponse(
            id = "us1",
            magnitude = 4.5,
            occurredAt = now.minus(2, ChronoUnit.HOURS),
            updatedAt = now.minus(1, ChronoUnit.HOURS)
        )

        val result = repository.refreshRecent(RecentWindow.LAST_DAY)
        val events = repository.observeEarthquakes(dayQuery).first()

        assertEquals(RefreshResult.Success, result)
        assertEquals(listOf("all_day"), api.requestedFeeds)
        assertEquals(1, events.size)
        assertEquals("us1", events.single().id)
        assertEquals(4.5, events.single().magnitude!!, 0.0)
    }

    @Test
    fun refresh_doesNotReplaceNewerDataWithOlderRevision() = runTest {
        api.response = sampleResponse(
            id = "us1",
            magnitude = 5.2,
            occurredAt = now.minus(2, ChronoUnit.HOURS),
            updatedAt = now.minus(30, ChronoUnit.MINUTES)
        )

        repository.refreshRecent(RecentWindow.LAST_DAY)

        api.response = sampleResponse(
            id = "us1",
            magnitude = 3.0,
            occurredAt = now.minus(2, ChronoUnit.HOURS),
            updatedAt = now.minus(90, ChronoUnit.MINUTES)
        )

        val result = repository.refreshRecent(RecentWindow.LAST_DAY)
        val events = repository.observeEarthquakes(dayQuery).first()

        assertEquals(RefreshResult.Success, result)
        assertEquals(1, events.size)
        assertEquals(5.2, events.single().magnitude!!, 0.0)
    }

    @Test
    fun refreshingDailyFeed_preservesOlderCachedEvents() = runTest {
        api.response = sampleResponse(
            id = "older",
            magnitude = 3.5,
            occurredAt = now.minus(4, ChronoUnit.DAYS),
            updatedAt = now.minus(3, ChronoUnit.DAYS)
        )

        repository.refreshRecent(RecentWindow.LAST_WEEK)

        api.response = sampleResponse(
            id = "recent",
            magnitude = 4.2,
            occurredAt = now.minus(2, ChronoUnit.HOURS),
            updatedAt = now.minus(1, ChronoUnit.HOURS)
        )

        repository.refreshRecent(RecentWindow.LAST_DAY)

        val events = repository.observeEarthquakes(weekQuery).first()

        assertEquals(listOf("all_week", "all_day"), api.requestedFeeds)
        assertEquals(2, events.size)
        assertEquals(listOf("recent", "older"), events.map { it.id })
    }

    @Test
    fun networkFailure_preservesPreviouslyCachedData() = runTest {
        api.response = sampleResponse(
            id = "cached",
            magnitude = 4.0,
            occurredAt = now.minus(2, ChronoUnit.HOURS),
            updatedAt = now.minus(1, ChronoUnit.HOURS)
        )

        repository.refreshRecent(RecentWindow.LAST_DAY)

        api.failure = IOException("Network unavailable")

        val result = repository.refreshRecent(RecentWindow.LAST_DAY)
        val events = repository.observeEarthquakes(dayQuery).first()

        assertEquals(
            RefreshResult.Failure(RefreshFailure.CONNECTION),
            result
        )
        assertEquals(1, events.size)
        assertEquals("cached", events.single().id)
    }

    @Test
    fun initialNetworkFailure_returnsFailureAndLeavesCacheEmpty() = runTest {
        api.failure = IOException("Network unavailable")

        val result = repository.refreshRecent(RecentWindow.LAST_DAY)
        val events = repository.observeEarthquakes(dayQuery).first()

        assertEquals(
            RefreshResult.Failure(RefreshFailure.CONNECTION),
            result
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun httpError_returnsServiceFailureWithoutDeletingCache() = runTest {
        api.response = sampleResponse(
            id = "cached",
            magnitude = 4.0,
            occurredAt = now.minus(2, ChronoUnit.HOURS),
            updatedAt = now.minus(1, ChronoUnit.HOURS)
        )

        repository.refreshRecent(RecentWindow.LAST_DAY)

        api.failure = HttpException(
            Response.error<String>(
                503,
                "Service unavailable".toResponseBody(
                    "text/plain".toMediaType()
                )
            )
        )

        val result = repository.refreshRecent(RecentWindow.LAST_DAY)
        val events = repository.observeEarthquakes(dayQuery).first()

        assertEquals(
            RefreshResult.Failure(RefreshFailure.SERVICE),
            result
        )
        assertEquals(listOf("cached"), events.map { it.id })
    }

    @Test
    fun invalidResponse_returnsFailureWithoutDeletingCache() = runTest {
        api.response = sampleResponse(
            id = "cached",
            magnitude = 4.0,
            occurredAt = now.minus(2, ChronoUnit.HOURS),
            updatedAt = now.minus(1, ChronoUnit.HOURS)
        )

        repository.refreshRecent(RecentWindow.LAST_DAY)

        api.failure = SerializationException("Invalid GeoJSON")

        val result = repository.refreshRecent(RecentWindow.LAST_DAY)
        val events = repository.observeEarthquakes(dayQuery).first()

        assertEquals(
            RefreshResult.Failure(RefreshFailure.INVALID_RESPONSE),
            result
        )
        assertEquals(listOf("cached"), events.map { it.id })
    }

    @Test
    fun cancellation_isPropagated() = runTest {
        api.failure = CancellationException("Refresh cancelled")

        val exception = runCatching {
            repository.refreshRecent(RecentWindow.LAST_DAY)
        }.exceptionOrNull()

        assertTrue(exception is CancellationException)
        assertTrue(repository.observeEarthquakes(dayQuery).first().isEmpty())
    }

    @Test
    fun unexpectedException_isNotHidden() = runTest {
        api.failure = IllegalStateException("Unexpected state")

        val exception = runCatching {
            repository.refreshRecent(RecentWindow.LAST_DAY)
        }.exceptionOrNull()

        assertTrue(exception is IllegalStateException)
    }

    @Test
    fun monthlyRefresh_isIndependentOfPreviousQueryTimestamp() = runTest {
        val earlierQuery = RecentWindow.LAST_30_DAYS.toQuery(
            endTime = now.minus(10, ChronoUnit.MINUTES)
        )

        api.response = sampleResponse(
            id = "monthly",
            magnitude = 4.8,
            occurredAt = now.minus(29, ChronoUnit.DAYS),
            updatedAt = now.minus(28, ChronoUnit.DAYS)
        )

        val result = repository.refreshRecent(RecentWindow.LAST_30_DAYS)
        val events = repository.observeEarthquakes(earlierQuery).first()

        assertEquals(RefreshResult.Success, result)
        assertEquals(listOf("all_month"), api.requestedFeeds)
        assertEquals(1, events.size)
        assertEquals("monthly", events.single().id)
    }

    @Test
    fun successfulRefresh_recordsOnlySelectedFeedTimestamp() = runTest {
        val result = repository.refreshRecent(
            RecentWindow.LAST_DAY
        )

        assertEquals(RefreshResult.Success, result)

        assertEquals(
            now.toEpochMilli(),
            database.feedSyncDao()
                .getLastSuccessEpochMillis("all_day")
        )

        assertNull(
            database.feedSyncDao()
                .getLastSuccessEpochMillis("all_week")
        )

        assertNull(
            database.feedSyncDao()
                .getLastSuccessEpochMillis("all_month")
        )
    }

    @Test
    fun failedRefresh_doesNotMarkFeedAsSynchronized() = runTest {
        api.failure = IOException("Network unavailable")

        val result = repository.refreshRecent(
            RecentWindow.LAST_WEEK
        )

        assertEquals(
            RefreshResult.Failure(
                RefreshFailure.CONNECTION
            ),
            result
        )

        assertNull(
            database.feedSyncDao()
                .getLastSuccessEpochMillis("all_week")
        )
    }

    @Test
    fun refreshIfStale_skipsRecentlyDownloadedFeed() = runTest {
        val first = repository.refreshIfStale(
            RecentWindow.LAST_DAY
        )

        val second = repository.refreshIfStale(
            RecentWindow.LAST_DAY
        )

        assertEquals(FeedSyncResult.Updated, first)
        assertEquals(FeedSyncResult.UpToDate, second)
        assertEquals(
            listOf("all_day"),
            api.requestedFeeds
        )
    }

    @Test
    fun refreshIfStale_downloadsExpiredFeed() = runTest {
        database.feedSyncDao().upsert(
            FeedSyncEntity(
                feedName = "all_week",
                lastSuccessEpochMillis =
                    now.minus(Duration.ofMinutes(16))
                        .toEpochMilli()
            )
        )

        val result = repository.refreshIfStale(
            RecentWindow.LAST_WEEK
        )

        assertEquals(FeedSyncResult.Updated, result)
        assertEquals(
            listOf("all_week"),
            api.requestedFeeds
        )
    }

    @Test
    fun failedConditionalRefresh_doesNotUpdateTimestamp() = runTest {
        api.failure = IOException("Network unavailable")

        val result = repository.refreshIfStale(
            RecentWindow.LAST_WEEK
        )

        assertEquals(
            FeedSyncResult.Failed(
                RefreshFailure.CONNECTION
            ),
            result
        )

        assertNull(
            database.feedSyncDao()
                .getLastSuccessEpochMillis("all_week")
        )

        api.failure = null

        val retry = repository.refreshIfStale(
            RecentWindow.LAST_WEEK
        )

        assertEquals(FeedSyncResult.Updated, retry)
        assertEquals(
            listOf("all_week", "all_week"),
            api.requestedFeeds
        )
    }


    @Test
    fun concurrentConditionalRefresh_downloadsSameFeedOnlyOnce() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val releaseResponse = CompletableDeferred<Unit>()

        api.beforeResponse = {
            // Señal de que getFeed() fue invocado.
            requestStarted.complete(Unit)

            // Mantener la primera solicitud pendiente
            // para comprobar el bloqueo del repositorio.
            releaseResponse.await()
        }

        val first = async(
            start = CoroutineStart.UNDISPATCHED
        ) {
            repository.refreshIfStale(
                RecentWindow.LAST_WEEK
            )
        }

        val second = async(
            start = CoroutineStart.UNDISPATCHED
        ) {
            repository.refreshIfStale(
                RecentWindow.LAST_WEEK
            )
        }

        // Esperar a que la primera solicitud llegue
        // realmente al API simulado.
        requestStarted.await()

        // La segunda solicitud no debe iniciar
        // una descarga adicional mientras espera el Mutex.
        assertEquals(
            listOf("all_week"),
            api.requestedFeeds
        )

        // Permitir que termine la primera descarga.
        releaseResponse.complete(Unit)

        assertEquals(
            FeedSyncResult.Updated,
            first.await()
        )

        // La segunda debe detectar el feed actualizado.
        assertEquals(
            FeedSyncResult.UpToDate,
            second.await()
        )

        // En total solo debe existir una descarga.
        assertEquals(
            listOf("all_week"),
            api.requestedFeeds
        )
    }

    @Test
    fun feedCacheStatus_reflectsSyncHistoryAndFreshness() = runTest {
        val window = RecentWindow.LAST_WEEK

        assertEquals(
            FeedCacheStatus.Unverified,
            repository.getFeedCacheStatus(window)
        )

        val oldTimestamp = now
            .minus(Duration.ofMinutes(20))
            .toEpochMilli()

        database.feedSyncDao().upsert(
            FeedSyncEntity(
                feedName = "all_week",
                lastSuccessEpochMillis = oldTimestamp
            )
        )

        assertEquals(
            FeedCacheStatus.Synced(
                lastSuccessEpochMillis = oldTimestamp,
                isFresh = false
            ),
            repository.getFeedCacheStatus(window)
        )

        api.failure = IOException("Network unavailable")

        assertEquals(
            FeedSyncResult.Failed(
                RefreshFailure.CONNECTION
            ),
            repository.refreshIfStale(window)
        )

        // Un fallo no debe cambiar la fecha registrada.
        assertEquals(
            FeedCacheStatus.Synced(
                lastSuccessEpochMillis = oldTimestamp,
                isFresh = false
            ),
            repository.getFeedCacheStatus(window)
        )

        api.failure = null

        assertEquals(
            FeedSyncResult.Updated,
            repository.refreshIfStale(window)
        )

        assertEquals(
            FeedCacheStatus.Synced(
                lastSuccessEpochMillis = now.toEpochMilli(),
                isFresh = true
            ),
            repository.getFeedCacheStatus(window)
        )
    }

    @Test
    fun verifiedDailySnapshot_removesWithdrawnButKeepsOlderWeeklyEvent() = runTest {
        val recent = now.minus(2, ChronoUnit.HOURS).toEpochMilli()
        val older = now.minus(4, ChronoUnit.DAYS).toEpochMilli()
        dao.upsertIfNewer(
            listOf(
                storedEvent("withdrawn", recent),
                storedEvent("older", older)
            )
        )
        val response = sampleResponse(
            id = "present",
            magnitude = 4.1,
            occurredAt = now.minus(1, ChronoUnit.HOURS),
            updatedAt = now.minus(30, ChronoUnit.MINUTES)
        )
        api.response = response.copy(
            metadata = UsgsEarthquakeMetadataDto(
                generated = now.toEpochMilli(),
                count = response.features.size,
                status = 200
            )
        )

        assertEquals(RefreshResult.Success, repository.refreshRecent(RecentWindow.LAST_DAY))
        assertEquals(
            setOf("present", "older"),
            repository.observeEarthquakes(weekQuery).first().map { it.id }.toSet()
        )
    }

    @Test
    fun inconsistentFeedMetadata_doesNotMutateRoomOrSyncTimestamp() = runTest {
        val response = sampleResponse(
            id = "invalid",
            magnitude = 4.1,
            occurredAt = now.minus(1, ChronoUnit.HOURS),
            updatedAt = now.minus(30, ChronoUnit.MINUTES)
        )
        api.response = response.copy(
            metadata = UsgsEarthquakeMetadataDto(
                generated = now.toEpochMilli(), count = 8, status = 200
            )
        )

        assertEquals(
            RefreshResult.Failure(RefreshFailure.INVALID_RESPONSE),
            repository.refreshRecent(RecentWindow.LAST_DAY)
        )
        assertTrue(repository.observeEarthquakes(dayQuery).first().isEmpty())
        assertNull(database.feedSyncDao().getLastSuccessEpochMillis("all_day"))
    }

    private fun storedEvent(id: String, occurredAt: Long) = EarthquakeEntity(
        id = id, magnitude = 4.0, place = "Stored", latitude = -33.4,
        longitude = -71.6, depthKilometers = 10.0,
        occurredAtEpochMillis = occurredAt,
        updatedAtEpochMillis = occurredAt,
        detailUrl = null
    )

    private fun sampleResponse(
        id: String,
        magnitude: Double,
        occurredAt: Instant,
        updatedAt: Instant
    ): UsgsEarthquakeCollectionDto {
        return UsgsEarthquakeCollectionDto(
            features = listOf(
                UsgsEarthquakeFeatureDto(
                    id = id,
                    properties = UsgsEarthquakePropertiesDto(
                        mag = magnitude,
                        place = "Coast of Chile",
                        time = occurredAt.toEpochMilli(),
                        updated = updatedAt.toEpochMilli(),
                        type = "earthquake"
                    ),
                    geometry = UsgsEarthquakeGeometryDto(
                        type = "Point",
                        coordinates = listOf(-71.6, -33.4, 12.0)
                    )
                )
            )
        )
    }

    private class FakeUsgsApi : UsgsEarthquakeApi {

        var response = UsgsEarthquakeCollectionDto(
            features = emptyList()
        )

        var failure: Exception? = null

        val requestedFeeds = mutableListOf<String>()

        var beforeResponse: suspend () -> Unit = {}

        override suspend fun getFeed(
            feedName: String
        ): UsgsEarthquakeCollectionDto {
            requestedFeeds.add(feedName)
            beforeResponse()
            failure?.let { throw it }

            return response
        }
    }
}