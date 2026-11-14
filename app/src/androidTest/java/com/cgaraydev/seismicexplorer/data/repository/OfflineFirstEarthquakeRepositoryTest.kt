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
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineFirstEarthquakeRepositoryTest {

    private lateinit var database: SeismicDatabase
    private lateinit var dao: EarthquakeDao
    private lateinit var api: FakeUsgsApi
    private lateinit var repository: OfflineFirstEarthquakeRepository

    private val now = Instant.parse("2026-11-14T12:00:00Z")

    private val dayQuery = EarthquakeQuery(
        startTime = now.minus(12, ChronoUnit.HOURS),
        endTime = now
    )

    private val weekQuery = EarthquakeQuery(
        startTime = now.minus(5, ChronoUnit.DAYS),
        endTime = now
    )

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

        repository.refresh(dayQuery)

        val events = repository.observeEarthquakes(dayQuery).first()

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

        repository.refresh(dayQuery)

        api.response = sampleResponse(
            id = "us1",
            magnitude = 3.0,
            occurredAt = now.minus(2, ChronoUnit.HOURS),
            updatedAt = now.minus(90, ChronoUnit.MINUTES)
        )

        repository.refresh(dayQuery)

        val events = repository.observeEarthquakes(dayQuery).first()

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

        repository.refresh(weekQuery)

        api.response = sampleResponse(
            id = "recent",
            magnitude = 4.2,
            occurredAt = now.minus(2, ChronoUnit.HOURS),
            updatedAt = now.minus(1, ChronoUnit.HOURS)
        )

        repository.refresh(dayQuery)

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

        repository.refresh(dayQuery)

        api.failure = IOException("Network unavailable")

        var failureReceived = false

        try {
            repository.refresh(dayQuery)
        } catch (exception: IOException) {
            failureReceived = true
        }

        assertTrue(failureReceived)

        val events = repository.observeEarthquakes(dayQuery).first()

        assertEquals(1, events.size)
        assertEquals("cached", events.single().id)
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

        var failure: IOException? = null

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