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
            dao = dao
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

        override suspend fun getFeed(
            feedName: String
        ): UsgsEarthquakeCollectionDto {
            requestedFeeds.add(feedName)

            failure?.let { throw it }

            return response
        }
    }
}