package com.cgaraydev.seismicexplorer.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EarthquakeDaoTest {

    private lateinit var database: SeismicDatabase
    private lateinit var dao: EarthquakeDao

    private val startOfDay =
        Instant.parse("2026-11-14T00:00:00Z").toEpochMilli()

    private val startOfWeek =
        Instant.parse("2026-11-08T00:00:00Z").toEpochMilli()

    private val endOfDay =
        Instant.parse("2026-11-15T00:00:00Z").toEpochMilli()

    private val recentTime =
        Instant.parse("2026-11-14T12:00:00Z").toEpochMilli()

    private val olderTime =
        Instant.parse("2026-11-11T12:00:00Z").toEpochMilli()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder<SeismicDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()

        dao = database.earthquakeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsert_updatesExistingRecordWithoutDuplicatingIt() = runTest {
        dao.upsertEarthquakes(
            listOf(sampleEntity("us1", recentTime, 3.0))
        )

        dao.upsertEarthquakes(
            listOf(
                sampleEntity("us1", recentTime, 4.5)
                    .copy(place = "Updated location")
            )
        )

        val events = dao.observeEarthquakes(
            startTime = startOfDay,
            endTime = endOfDay,
            minimumMagnitude = null
        ).first()

        assertEquals(1, events.size)
        assertEquals(4.5, events.single().magnitude!!, 0.0)
        assertEquals("Updated location", events.single().place)
    }

    @Test
    fun observeEarthquakes_filtersByPeriodAndSortsNewestFirst() = runTest {
        dao.upsertEarthquakes(
            listOf(
                sampleEntity("older", olderTime, 3.0),
                sampleEntity("newer", recentTime, 4.0),
                sampleEntity("boundary", endOfDay, 5.0)
            )
        )

        val dailyEvents = dao.observeEarthquakes(
            startTime = startOfDay,
            endTime = endOfDay,
            minimumMagnitude = null
        ).first()

        assertEquals(listOf("newer"), dailyEvents.map { it.id })

        val weeklyEvents = dao.observeEarthquakes(
            startTime = startOfWeek,
            endTime = endOfDay,
            minimumMagnitude = null
        ).first()

        assertEquals(
            listOf("newer", "older"),
            weeklyEvents.map { it.id }
        )
    }

    @Test
    fun magnitudeFilter_excludesLowerAndUnknownMagnitudes() = runTest {
        dao.upsertEarthquakes(
            listOf(
                sampleEntity("strong", recentTime, 4.5),
                sampleEntity("weak", recentTime, 1.5),
                sampleEntity("unknown", recentTime, null)
            )
        )

        val filtered = dao.observeEarthquakes(
            startTime = startOfDay,
            endTime = endOfDay,
            minimumMagnitude = 2.5
        ).first()

        assertEquals(listOf("strong"), filtered.map { it.id })

        val unfiltered = dao.observeEarthquakes(
            startTime = startOfDay,
            endTime = endOfDay,
            minimumMagnitude = null
        ).first()

        assertEquals(3, unfiltered.size)
    }

    @Test
    fun insertingRecentEvents_doesNotDeleteOlderCachedEvents() = runTest {
        dao.upsertEarthquakes(
            listOf(sampleEntity("older", olderTime, 3.0))
        )

        dao.upsertEarthquakes(
            listOf(sampleEntity("recent", recentTime, 4.0))
        )

        val events = dao.observeEarthquakes(
            startTime = startOfWeek,
            endTime = endOfDay,
            minimumMagnitude = null
        ).first()

        assertEquals(2, events.size)
        assertTrue(events.any { it.id == "older" })
        assertTrue(events.any { it.id == "recent" })
    }

    @Test
    fun upsertIfNewer_preservesMostRecentRevision() = runTest {
        val original = sampleEntity(
            id = "us1",
            occurredAt = recentTime,
            magnitude = 4.5
        ).copy(
            updatedAtEpochMillis = recentTime + 20_000,
            place = "Latest location"
        )

        dao.upsertIfNewer(listOf(original))

        val olderRevision = original.copy(
            magnitude = 2.0,
            place = "Outdated location",
            updatedAtEpochMillis = recentTime + 10_000
        )

        dao.upsertIfNewer(listOf(olderRevision))

        val events = dao.observeEarthquakes(
            startTime = startOfDay,
            endTime = endOfDay,
            minimumMagnitude = null
        ).first()

        assertEquals(1, events.size)
        assertEquals(4.5, events.single().magnitude!!, 0.0)
        assertEquals("Latest location", events.single().place)
    }

    private fun sampleEntity(
        id: String,
        occurredAt: Long,
        magnitude: Double?
    ) = EarthquakeEntity(
        id = id,
        magnitude = magnitude,
        place = "Test location",
        latitude = -33.4,
        longitude = -71.6,
        depthKilometers = 12.0,
        occurredAtEpochMillis = occurredAt,
        updatedAtEpochMillis = occurredAt,
        detailUrl = null
    )
}