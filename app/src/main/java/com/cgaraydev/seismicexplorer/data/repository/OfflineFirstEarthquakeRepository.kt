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
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

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
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun refreshRecent(
        window: RecentWindow
    ): RefreshResult {
        val feed = UsgsFeed.forWindow(window)

        val response = try {
            api.getFeed(feed.feedName)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            return RefreshResult.Failure(RefreshFailure.SERVICE)
        } catch (exception: SerializationException) {
            return RefreshResult.Failure(RefreshFailure.INVALID_RESPONSE)
        } catch (exception: IOException) {
            return RefreshResult.Failure(RefreshFailure.CONNECTION)
        }

        val entities = withContext(Dispatchers.Default) {
            response.toDomain()
                .map { it.toEntity() }
        }

        dao.upsertIfNewer(entities)

        return RefreshResult.Success
    }
}