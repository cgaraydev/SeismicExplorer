package com.cgaraydev.seismicexplorer.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeismicDatabaseV5MigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val dbName = "seismic_migration_v5_test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        databaseClass = SeismicDatabase::class,
        driver = AndroidSQLiteDriver(),
        file = instrumentation.targetContext.getDatabasePath(dbName)
    )

    @Test
    fun from4To5_preservesEventsAndScientificCache() = runTest {
        instrumentation.targetContext.deleteDatabase(dbName)
        val old = helper.createDatabase(4)
        try {
            old.execSQL("""INSERT INTO earthquakes (
                id, magnitude, place, latitude, longitude, depthKilometers,
                occurredAtEpochMillis, updatedAtEpochMillis, detailUrl, magType, reviewStatus
            ) VALUES ('existing-v4', 5.5, 'Chile', -33.4, -71.6, 12.0,
                1700000000000, 1700000001000, NULL, 'mww', 'reviewed')""".trimIndent())
            old.execSQL("""INSERT INTO scientific_detail_cache (
                eventId, sourceUpdatedAtEpochMillis, fetchedAtEpochMillis, payloadJson
            ) VALUES ('existing-v4', 1700000001000, 1700000002000, '{}')""".trimIndent())
        } finally { old.close() }

        val migrated = helper.runMigrationsAndValidate(version = 5, migrations = emptyList())
        try {
            migrated.prepare("""SELECT id, magType, reviewStatus, feltReports,
                communityIntensity, modeledIntensity, tsunamiIndicator, significance,
                network, stationCount, detailGeoJsonUrl
                FROM earthquakes WHERE id = 'existing-v4'""".trimIndent()).use { stmt ->
                assertTrue(stmt.step())
                assertEquals("existing-v4", stmt.getText(0))
                assertEquals("mww", stmt.getText(1))
                assertEquals("reviewed", stmt.getText(2))
                for (index in 3..10) assertTrue("columna $index", stmt.isNull(index))
                assertFalse(stmt.step())
            }
            migrated.prepare("""SELECT payloadJson FROM scientific_detail_cache
                WHERE eventId = 'existing-v4'""".trimIndent()).use { stmt ->
                assertTrue(stmt.step())
                assertEquals("{}", stmt.getText(0))
            }
        } finally { migrated.close() }
    }

    @Test
    fun from1To5_keepsOriginalEventAndLeavesNewFieldsNull() = runTest {
        instrumentation.targetContext.deleteDatabase(dbName)
        val old = helper.createDatabase(1)
        try {
            old.execSQL("""INSERT INTO earthquakes (
                id, magnitude, place, latitude, longitude, depthKilometers,
                occurredAtEpochMillis, updatedAtEpochMillis, detailUrl
            ) VALUES ('existing-v1', 4.1, 'Chile', -33.4, -71.6, 12.0,
                1700000000000, 1700000001000, NULL)""".trimIndent())
        } finally { old.close() }

        val migrated = helper.runMigrationsAndValidate(version = 5, migrations = emptyList())
        try {
            migrated.prepare("""SELECT id, magnitude, feltReports, communityIntensity,
                detailGeoJsonUrl FROM earthquakes WHERE id = 'existing-v1'""".trimIndent()).use { stmt ->
                assertTrue(stmt.step())
                assertEquals("existing-v1", stmt.getText(0))
                assertEquals(4.1, stmt.getDouble(1), 0.0)
                assertTrue(stmt.isNull(2))
                assertTrue(stmt.isNull(3))
                assertTrue(stmt.isNull(4))
                assertFalse(stmt.step())
            }
        } finally { migrated.close() }
    }
}
