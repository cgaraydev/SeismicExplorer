package com.cgaraydev.seismicexplorer.data.local

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeismicDatabaseMigrationTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val databaseName = "seismic_migration_test.db"

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = instrumentation,
        databaseClass = SeismicDatabase::class,
        driver = AndroidSQLiteDriver(),
        file = instrumentation.targetContext
            .getDatabasePath(databaseName)
    )

    @Test
    fun migrationFrom1To2_preservesEarthquakes() = runTest {
        val oldConnection = migrationHelper.createDatabase(1)

        oldConnection.execSQL(
            """
            INSERT INTO earthquakes (
                id,
                magnitude,
                place,
                latitude,
                longitude,
                depthKilometers,
                occurredAtEpochMillis,
                updatedAtEpochMillis,
                detailUrl
            ) VALUES (
                'migration-event',
                4.5,
                'Coast of Chile',
                -33.4,
                -71.6,
                12.0,
                1700000000000,
                1700000001000,
                NULL
            )
            """.trimIndent()
        )

        oldConnection.close()

        val database = Room.databaseBuilder<SeismicDatabase>(
            context = instrumentation.targetContext,
            name = databaseName
        )
            .setDriver(AndroidSQLiteDriver())
            .build()

        try {
            val earthquakes = database.earthquakeDao()
                .observeEarthquakes(
                    startTime = 0L,
                    endTime = Long.MAX_VALUE,
                    minimumMagnitude = null
                )
                .first()

            assertEquals(1, earthquakes.size)
            assertEquals(
                "migration-event",
                earthquakes.single().id
            )

            assertNull(
                database.feedSyncDao()
                    .getLastSuccessEpochMillis("all_day")
            )
        } finally {
            database.close()
        }
    }
}