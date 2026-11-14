package com.cgaraydev.seismicexplorer.data.repository

import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.local.mapper.toDomain
import com.cgaraydev.seismicexplorer.data.local.mapper.toEntity
import com.cgaraydev.seismicexplorer.data.mapper.toDomain
import com.cgaraydev.seismicexplorer.data.remote.UsgsFeed
import com.cgaraydev.seismicexplorer.data.remote.api.UsgsEarthquakeApi
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstEarthquakeRepository @Inject constructor(
    private val api: UsgsEarthquakeApi,
    private val dao: EarthquakeDao
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

    override suspend fun refreshRecent(
        window: RecentWindow
    ) {
        val feed = UsgsFeed.forWindow(window)

        val response = api.getFeed(feed.feedName)

        val entities = response.toDomain()
            .map { it.toEntity() }

        dao.upsertIfNewer(entities)
    }
}