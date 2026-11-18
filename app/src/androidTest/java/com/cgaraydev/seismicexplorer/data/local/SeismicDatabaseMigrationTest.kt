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
    fun migrationFrom3To4_preservesExistingEventsAndCreatesCache() = runTest {
        instrumentation.targetContext.deleteDatabase(databaseName)
        val old = migrationHelper.createDatabase(3)
        try {
            old.execSQL("""INSERT INTO earthquakes (
                id, magnitude, place, latitude, longitude, depthKilometers,
                occurredAtEpochMillis, updatedAtEpochMillis, detailUrl, magType, reviewStatus
            ) VALUES ('existing-v3', 5.5, 'Chile', -33.4, -71.6, 12.0,
                1700000000000, 1700000001000, NULL, 'mww', 'reviewed')""".trimIndent())
            old.execSQL("""INSERT INTO feed_sync (feedName, lastSuccessEpochMillis)
                VALUES ('all_day', 1700000002000)""".trimIndent())
        } finally {
            old.close()
        }
        val migrated = migrationHelper.runMigrationsAndValidate(version = 4, migrations = emptyList())
        try {
            migrated.prepare("SELECT id, magType, reviewStatus FROM earthquakes").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("existing-v3", stmt.getText(0))
                assertEquals("mww", stmt.getText(1))
                assertEquals("reviewed", stmt.getText(2))
                assertFalse(stmt.step())
            }
            migrated.prepare("SELECT lastSuccessEpochMillis FROM feed_sync").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(1700000002000L, stmt.getLong(0))
            }
            migrated.prepare("SELECT COUNT(*) FROM scientific_detail_cache").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(0L, stmt.getLong(0))
            }
        } finally { migrated.close() }
    }

    @Test
    fun migrationFrom1To4_preservesEventsThroughAllMigrations() = runTest {
        instrumentation.targetContext.deleteDatabase(databaseName)
        val old = migrationHelper.createDatabase(1)
        try {
            old.execSQL("""INSERT INTO earthquakes (
                id, magnitude, place, latitude, longitude, depthKilometers,
                occurredAtEpochMillis, updatedAtEpochMillis, detailUrl
            ) VALUES ('existing-v1', 4.1, 'Chile', -33.4, -71.6, 12.0,
                1700000000000, 1700000001000, NULL)""".trimIndent())
        } finally { old.close() }
        val migrated = migrationHelper.runMigrationsAndValidate(version = 4, migrations = emptyList())
        try {
            migrated.prepare("SELECT id, magType, reviewStatus FROM earthquakes").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("existing-v1", stmt.getText(0))
                assertTrue(stmt.isNull(1))
                assertTrue(stmt.isNull(2))
                assertFalse(stmt.step())
            }
            migrated.prepare("SELECT COUNT(*) FROM scientific_detail_cache").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(0L, stmt.getLong(0))
            }
        } finally { migrated.close() }
    }

    @Test
    fun migrationFrom2To3_preservesEventsAndFeedSync() = runTest {
        instrumentation.targetContext.deleteDatabase(databaseName)
        val old = migrationHelper.createDatabase(2)
        try {
            old.execSQL(
                """INSERT INTO earthquakes (
                    id, magnitude, place, latitude, longitude, depthKilometers,
                    occurredAtEpochMillis, updatedAtEpochMillis, detailUrl
                ) VALUES ('old-event', 5.2, 'Chile', -33.4, -71.6, 12.0,
                    1700000000000, 1700000001000, NULL)""".trimIndent()
            )
            old.execSQL(
                """INSERT INTO feed_sync (feedName, lastSuccessEpochMillis)
                VALUES ('all_day', 1700000002000)""".trimIndent()
            )
        } finally {
            old.close()
        }
        val migrated = migrationHelper.runMigrationsAndValidate(
            version = 3, migrations = emptyList()
        )
        try {
            migrated.prepare(
                "SELECT id, magnitude, magType, reviewStatus FROM earthquakes"
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("old-event", statement.getText(0))
                assertEquals(5.2, statement.getDouble(1), 0.0)
                assertTrue(statement.isNull(2))
                assertTrue(statement.isNull(3))
                assertFalse(statement.step())
            }
            migrated.prepare(
                "SELECT lastSuccessEpochMillis FROM feed_sync WHERE feedName = 'all_day'"
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals(1700000002000L, statement.getLong(0))
            }
        } finally {
            migrated.close()
        }
    }

    @Test
    fun migrationFrom1To3_preservesOldEvents() = runTest {
        instrumentation.targetContext.deleteDatabase(databaseName)
        val old = migrationHelper.createDatabase(1)
        try {
            old.execSQL(
                """INSERT INTO earthquakes (
                    id, magnitude, place, latitude, longitude, depthKilometers,
                    occurredAtEpochMillis, updatedAtEpochMillis, detailUrl
                ) VALUES ('v1-event', 4.0, 'Chile', -33.4, -71.6, 10.0,
                    1700000000000, 1700000001000, NULL)""".trimIndent()
            )
        } finally {
            old.close()
        }
        val migrated = migrationHelper.runMigrationsAndValidate(
            version = 3, migrations = emptyList()
        )
        try {
            migrated.prepare(
                "SELECT id, magType, reviewStatus FROM earthquakes"
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("v1-event", statement.getText(0))
                assertTrue(statement.isNull(1))
                assertTrue(statement.isNull(2))
                assertFalse(statement.step())
            }
        } finally {
            migrated.close()
        }
    }

    @Test
    fun migrationFrom1To2_preservesEarthquakes() = runTest {

        // Garantizar una base de prueba limpia.
        instrumentation.targetContext.deleteDatabase(
            databaseName
        )

        // Crear una base con el esquema de la versión 1.
        val oldConnection =
            migrationHelper.createDatabase(1)

        try {
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

            // Confirmar que el evento existe
            // ANTES de ejecutar la migración.
            oldConnection.prepare(
                "SELECT COUNT(*) FROM earthquakes"
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals(
                    1L,
                    statement.getLong(0)
                )
            }
        } finally {
            oldConnection.close()
        }

        // Ejecutar la migración automática 1 -> 2
        // y validar el esquema resultante.
        val migratedConnection =
            migrationHelper.runMigrationsAndValidate(
                version = 2,
                migrations = emptyList()
            )

        try {
            // Comprobar que el terremoto
            // conserva sus datos originales.
            migratedConnection.prepare(
                """
                SELECT id, magnitude, place,
                       occurredAtEpochMillis,
                       updatedAtEpochMillis
                FROM earthquakes
                WHERE id = 'migration-event'
                """.trimIndent()
            ).use { statement ->

                assertTrue(
                    "La migración debe conservar el terremoto",
                    statement.step()
                )

                assertEquals(
                    "migration-event",
                    statement.getText(0)
                )

                assertEquals(
                    4.5,
                    statement.getDouble(1),
                    0.0
                )

                assertEquals(
                    "Coast of Chile",
                    statement.getText(2)
                )

                assertEquals(
                    1700000000000L,
                    statement.getLong(3)
                )

                assertEquals(
                    1700000001000L,
                    statement.getLong(4)
                )

                assertFalse(
                    "No deben existir registros duplicados",
                    statement.step()
                )
            }

            // La versión 2 incorpora feed_sync,
            // que inicialmente debe estar vacía.
            migratedConnection.prepare(
                "SELECT COUNT(*) FROM feed_sync"
            ).use { statement ->

                assertTrue(statement.step())

                assertEquals(
                    0L,
                    statement.getLong(0)
                )
            }

        } finally {
            migratedConnection.close()
        }
    }
}