package com.cgaraydev.seismicexplorer.data.repository

import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.local.mapper.toDomain
import com.cgaraydev.seismicexplorer.data.local.mapper.toEntity
import com.cgaraydev.seismicexplorer.data.mapper.toDomain
import com.cgaraydev.seismicexplorer.data.remote.UsgsFeed
import com.cgaraydev.seismicexplorer.data.remote.api.UsgsEarthquakeApi
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstEarthquakeRepository @Inject constructor(
    private val api: UsgsEarthquakeApi,
    private val dao: EarthquakeDao,
    private val clock: Clock
) : EarthquakeRepository {

    override fun observeEarthquakes(
        query: EarthquakeQuery
    ): Flow<List<Earthquake>> {
        return dao.observeEarthquakes(
            startTime = query.startTime.toEpochMilli(),
            endTime = query.endTime.toEpochMilli(),
            minimumMagnitude = query.minimumMagnitude
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refresh(
        query: EarthquakeQuery
    ) {
        val feed = UsgsFeed.forQuery(
            query = query,
            now = clock.instant()
        )

        val response = api.getFeed(feed.feedName)

        val entities = response.toDomain()
            .map { it.toEntity() }

        dao.upsertIfNewer(entities)
    }
}