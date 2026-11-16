package com.cgaraydev.seismicexplorer.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cgaraydev.seismicexplorer.data.local.dao.FeedReconciliationScope
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import com.cgaraydev.seismicexplorer.data.local.entity.FeedSyncEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EarthquakeSnapshotDaoTest {
    private lateinit var database: SeismicDatabase
    private val epoch = 1_800_000_000_000L
    private val day = 86_400_000L

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<SeismicDatabase>(context)
            .setDriver(AndroidSQLiteDriver()).build()
    }

    @After fun tearDown() { database.close() }

    @Test fun reconcileOnlySelectedWindowAndKeepNewerRevision() = runTest {
        val dao = database.earthquakeDao()
        dao.upsertIfNewer(listOf(
            entity("withdrawn", epoch - 2_000),
            entity("kept", epoch - 4_000),
            entity("older-week", epoch - 4 * day),
            entity("newer-revision", epoch - 6_000).copy(updatedAtEpochMillis = epoch + 1)
        ))
        dao.persistFeedSnapshot(
            earthquakes = listOf(entity("kept", epoch - 4_000)),
            feedSync = FeedSyncEntity("all_day", epoch),
            reconciliationScope = FeedReconciliationScope(epoch - day, epoch),
            retentionCutoffMillis = epoch - 31 * day
        )
        val all = dao.observeEarthquakes(epoch - 10 * day, epoch + day, null).first()
        assertEquals(setOf("kept", "older-week", "newer-revision"), all.map { it.id }.toSet())
        assertEquals(epoch, database.feedSyncDao().getLastSuccessEpochMillis("all_day"))
    }

    @Test fun monthRetentionIsPrunedOnlyOnSuccessfulPersist() = runTest {
        val dao = database.earthquakeDao()
        dao.upsertIfNewer(listOf(
            entity("expired", epoch - 40 * day),
            entity("within-retention", epoch - 29 * day)
        ))
        assertNull(database.feedSyncDao().getLastSuccessEpochMillis("all_month"))
        dao.persistFeedSnapshot(
            earthquakes = emptyList(),
            feedSync = FeedSyncEntity("all_month", epoch),
            reconciliationScope = null,
            retentionCutoffMillis = epoch - 31 * day
        )
        val result = dao.observeEarthquakes(epoch - 50 * day, epoch + day, null).first()
        assertEquals(listOf("within-retention"), result.map { it.id })
        assertEquals(epoch, database.feedSyncDao().getLastSuccessEpochMillis("all_month"))
    }

    @Test fun noReconciliationRetainsNonExpiredEvents() = runTest {
        val dao = database.earthquakeDao()
        dao.upsertIfNewer(listOf(entity("unverified", epoch - 2_000)))
        dao.persistFeedSnapshot(
            earthquakes = emptyList(),
            feedSync = FeedSyncEntity("all_day", epoch),
            reconciliationScope = null,
            retentionCutoffMillis = epoch - 31 * day
        )
        assertEquals("unverified", dao.observeEarthquakes(epoch-day, epoch, null).first().single().id)
    }

    private fun entity(id: String, occurrence: Long) = EarthquakeEntity(
        id = id, magnitude = 3.5, place = "Test", latitude = -33.4,
        longitude = -71.6, depthKilometers = 10.0,
        occurredAtEpochMillis = occurrence,
        updatedAtEpochMillis = occurrence,
        detailUrl = null
    )
}
