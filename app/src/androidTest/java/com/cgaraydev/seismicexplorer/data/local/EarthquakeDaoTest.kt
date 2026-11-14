package com.cgaraydev.seismicexplorer.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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
    fun upsertIfNewer_updatesExistingRecordWithoutDuplicatingIt() = runTest {
        dao.upsertIfNewer(
            listOf(sampleEntity("us1", recentTime, 3.0))
        )

        dao.upsertIfNewer(
            listOf(
                sampleEntity("us1", recentTime, 4.5).copy(
                    updatedAtEpochMillis = recentTime + 1_000,
                    place = "Updated location"
                )
            )
        )

        val events = observeAll().first()

        assertEquals(1, events.size)
        assertEquals(4.5, events.single().magnitude!!, 0.0)
        assertEquals("Updated location", events.single().place)
    }

    @Test
    fun observeEarthquakes_filtersByPeriodAndSortsNewestFirst() = runTest {
        dao.upsertIfNewer(
            listOf(
                sampleEntity("older", olderTime, 3.0),
                sampleEntity("newer", recentTime, 4.0),
                sampleEntity("boundary", endOfDay, 5.0)
            )
        )

        val dailyEvents = observeAll().first()

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
        dao.upsertIfNewer(
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
        assertEquals(3, observeAll().first().size)
    }

    @Test
    fun insertingRecentEvents_doesNotDeleteOlderCachedEvents() = runTest {
        dao.upsertIfNewer(
            listOf(sampleEntity("older", olderTime, 3.0))
        )

        dao.upsertIfNewer(
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
        val original = sampleEntity("us1", recentTime, 4.5).copy(
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

        val events = observeAll().first()

        assertEquals(1, events.size)
        assertEquals(4.5, events.single().magnitude!!, 0.0)
        assertEquals("Latest location", events.single().place)
    }

    @Test
    fun duplicateIds_keepHighestRevisionWithinSameBatch() = runTest {
        val older = sampleEntity("us1", recentTime, 2.0).copy(
            updatedAtEpochMillis = recentTime + 1_000
        )

        val newer = older.copy(
            magnitude = 5.0,
            updatedAtEpochMillis = recentTime + 3_000
        )

        dao.upsertIfNewer(
            listOf(newer, older, older.copy(magnitude = 1.0))
        )

        val events = observeAll().first()

        assertEquals(1, events.size)
        assertEquals(5.0, events.single().magnitude!!, 0.0)
    }

    @Test
    fun equalTimestamps_preserveFirstStoredRevision() = runTest {
        val first = sampleEntity("us1", recentTime, 4.0).copy(
            place = "First revision"
        )

        val second = first.copy(
            magnitude = 5.0,
            place = "Second revision"
        )

        dao.upsertIfNewer(listOf(first, second))
        dao.upsertIfNewer(listOf(second))

        val stored = observeAll().first().single()

        assertEquals(4.0, stored.magnitude!!, 0.0)
        assertEquals("First revision", stored.place)
    }

    @Test
    fun batchesLargerThan400_preserveAllRecordsAndUpdateThem() = runTest {
        val original = (1..805).map { index ->
            sampleEntity(
                id = "us$index",
                occurredAt = recentTime - index.toLong(),
                magnitude = 2.0
            )
        }

        dao.upsertIfNewer(original)

        val updated = original.reversed().map { earthquake ->
            earthquake.copy(
                magnitude = 3.5,
                updatedAtEpochMillis =
                    earthquake.updatedAtEpochMillis + 1_000
            )
        }

        dao.upsertIfNewer(updated)

        val stored = observeAll().first()

        assertEquals(805, stored.size)
        assertEquals(805, stored.map { it.id }.toSet().size)
        assertTrue(stored.all { it.magnitude == 3.5 })
    }

    @Test
    fun concurrentUpserts_preserveNewestRevision() = runBlocking {
        val older = (1..20).map { index ->
            sampleEntity(
                id = "concurrent$index",
                occurredAt = recentTime,
                magnitude = 2.0
            ).copy(
                updatedAtEpochMillis = recentTime + 1_000
            )
        }

        val newer = older.map { earthquake ->
            earthquake.copy(
                magnitude = 5.0,
                updatedAtEpochMillis = recentTime + 2_000
            )
        }

        coroutineScope {
            val first = async(Dispatchers.Default) {
                dao.upsertIfNewer(older)
            }

            val second = async(Dispatchers.Default) {
                dao.upsertIfNewer(newer)
            }

            awaitAll(first, second)
        }

        val stored = observeAll().first()

        assertEquals(20, stored.size)
        assertTrue(stored.all { it.magnitude == 5.0 })
        assertTrue(
            stored.all {
                it.updatedAtEpochMillis == recentTime + 2_000
            }
        )
    }

    @Test
    fun activeObserver_receivesInsertionsAndUpdates() = runBlocking {
        val emissions = Channel<List<EarthquakeEntity>>(Channel.UNLIMITED)

        val observer = launch(Dispatchers.Default) {
            observeAll().collect { earthquakes ->
                emissions.send(earthquakes)
            }
        }

        try {
            val initial = withTimeout(10_000) {
                emissions.receive()
            }

            assertTrue(initial.isEmpty())

            dao.upsertIfNewer(
                listOf(sampleEntity("observed", recentTime, 3.0))
            )

            val afterInsert = withTimeout(10_000) {
                emissions.receiveAsFlow().first { earthquakes ->
                    earthquakes.any { it.id == "observed" }
                }
            }

            assertEquals(1, afterInsert.size)
            assertEquals(3.0, afterInsert.single().magnitude!!, 0.0)

            dao.upsertIfNewer(
                listOf(
                    sampleEntity("observed", recentTime, 5.0).copy(
                        updatedAtEpochMillis = recentTime + 1_000
                    )
                )
            )

            val afterUpdate = withTimeout(10_000) {
                emissions.receiveAsFlow().first { earthquakes ->
                    earthquakes.singleOrNull()?.magnitude == 5.0
                }
            }

            assertEquals("observed", afterUpdate.single().id)
            assertEquals(5.0, afterUpdate.single().magnitude!!, 0.0)
        } finally {
            observer.cancelAndJoin()
            emissions.close()
        }
    }

    private fun observeAll() = dao.observeEarthquakes(
        startTime = startOfDay,
        endTime = endOfDay,
        minimumMagnitude = null
    )

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