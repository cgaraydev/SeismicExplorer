package com.cgaraydev.seismicexplorer.data.repository

import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeVersion
import com.cgaraydev.seismicexplorer.data.local.dao.FeedSyncDao
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import com.cgaraydev.seismicexplorer.data.local.entity.FeedSyncEntity
import com.cgaraydev.seismicexplorer.data.remote.api.UsgsEarthquakeApi
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeCollectionDto
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas deterministas del contrato observable sin una base de datos Android. */
class OfflineFirstEarthquakeRepositoryObservationTest {

    private val now = Instant.parse("2026-11-16T12:00:00Z")

    @Test
    fun identicalSqlRowsDoNotTriggerRepeatedDomainEmissions() = runBlocking {
        val first = event("us1", 3.5)
        val revised = first.copy(magnitude = 4.5, updatedAtEpochMillis = first.updatedAtEpochMillis + 1)
        val dao = StubEarthquakeDao().apply {
            eventObservations = flowOf(
                listOf(first),
                listOf(first.copy()),
                listOf(revised)
            )
        }

        val received = repository(dao)
            .observeEarthquakes(RecentWindow.LAST_DAY.toQuery(now))
            .toList()

        assertEquals(2, received.size)
        assertEquals(listOf(3.5, 4.5), received.map { it.single().magnitude })
    }

    @Test
    fun legitimateEmptyResultsArePreservedAcrossTransitions() = runBlocking {
        val dao = StubEarthquakeDao().apply {
            eventObservations = flowOf(
                emptyList(),
                listOf(event("us1", 3.5)),
                emptyList()
            )
        }

        val received = repository(dao)
            .observeEarthquakes(RecentWindow.LAST_DAY.toQuery(now))
            .toList()

        assertEquals(listOf(0, 1, 0), received.map { it.size })
    }

    @Test
    fun detailObservationPreservesRevisionsAndDeletion() = runBlocking {
        val first = event("us1", 3.5)
        val revised = first.copy(magnitude = 4.5, updatedAtEpochMillis = first.updatedAtEpochMillis + 1)
        val dao = StubEarthquakeDao().apply {
            individualObservations = flowOf(first, first.copy(), revised, null)
        }

        val received = repository(dao).observeEarthquake("us1").toList()

        assertEquals(3, received.size)
        assertEquals(listOf(3.5, 4.5, null), received.map { it?.magnitude })
    }

    private fun repository(dao: StubEarthquakeDao) = OfflineFirstEarthquakeRepository(
        api = object : UsgsEarthquakeApi {
            override suspend fun getFeed(feedName: String) =
                UsgsEarthquakeCollectionDto(features = emptyList())
        },
        dao = dao,
        feedSyncDao = object : FeedSyncDao {
            override suspend fun getLastSuccessEpochMillis(feedName: String): Long? = null
            override suspend fun upsert(entity: FeedSyncEntity) = Unit
        },
        clock = Clock.fixed(now, ZoneOffset.UTC)
    )

    private fun event(id: String, magnitude: Double) = EarthquakeEntity(
        id = id,
        magnitude = magnitude,
        place = "Chile",
        latitude = -33.4,
        longitude = -71.6,
        depthKilometers = 10.0,
        occurredAtEpochMillis = now.minusSeconds(3600).toEpochMilli(),
        updatedAtEpochMillis = now.minusSeconds(1800).toEpochMilli(),
        detailUrl = null
    )

    private class StubEarthquakeDao : EarthquakeDao() {
        var eventObservations: Flow<List<EarthquakeEntity>> = flowOf(emptyList())
        var individualObservations: Flow<EarthquakeEntity?> = flowOf(null)

        override fun observeEarthquakes(
            startTime: Long,
            endTime: Long,
            minimumMagnitude: Double?,
            depthFilter: String
        ): Flow<List<EarthquakeEntity>> = eventObservations

        override fun observeEarthquake(id: String): Flow<EarthquakeEntity?> = individualObservations

        protected override suspend fun upsertEarthquakes(earthquakes: List<EarthquakeEntity>) = Unit
        protected override suspend fun getExistingVersions(ids: List<String>): List<EarthquakeVersion> = emptyList()
        protected override suspend fun fillMissingScientificMetadata(
            id: String,
            updatedAtEpochMillis: Long,
            magType: String?,
            reviewStatus: String?
        ) = Unit
        protected override suspend fun upsertFeedSync(entity: FeedSyncEntity) = Unit
        protected override suspend fun findReconciliationCandidates(
            startMillis: Long,
            endMillis: Long,
            generatedMillis: Long
        ): List<String> = emptyList()
        protected override suspend fun deleteByIds(ids: List<String>) = Unit
        protected override suspend fun deleteExpired(cutoffMillis: Long) = Unit
    }
}
