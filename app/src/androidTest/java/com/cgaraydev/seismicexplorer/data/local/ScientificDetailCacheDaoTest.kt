package com.cgaraydev.seismicexplorer.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cgaraydev.seismicexplorer.data.local.entity.ScientificDetailCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScientificDetailCacheDaoTest {
    private lateinit var database: SeismicDatabase

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<SeismicDatabase>(context)
            .setDriver(AndroidSQLiteDriver()).build()
    }

    @After fun teardown() { database.close() }

    @Test fun cacheRetainsPayloadAndPrunesOldAndExcessEntries() = runTest {
        val dao = database.scientificDetailCacheDao()
        dao.upsert(ScientificDetailCacheEntity("us1", 100, 1000, "{\"test\":1}"))
        assertEquals("{\"test\":1}", dao.get("us1")?.payloadJson)
        dao.upsert(ScientificDetailCacheEntity("us2", 200, 2000, "{}"))
        dao.upsert(ScientificDetailCacheEntity("us3", 300, 3000, "{}"))
        dao.deleteOlderThan(1001)
        assertNull(dao.get("us1"))
        dao.keepLatest(1)
        assertNull(dao.get("us2"))
        assertEquals(300L, dao.get("us3")?.sourceUpdatedAtEpochMillis)
    }
}
