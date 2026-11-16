package com.cgaraydev.seismicexplorer.data.repository

import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.local.dao.FeedSyncDao
import com.cgaraydev.seismicexplorer.data.local.entity.FeedSyncEntity
import com.cgaraydev.seismicexplorer.data.local.mapper.toDomain
import com.cgaraydev.seismicexplorer.data.local.mapper.toEntity
import com.cgaraydev.seismicexplorer.data.mapper.toDomain
import com.cgaraydev.seismicexplorer.data.remote.UsgsFeed
import com.cgaraydev.seismicexplorer.data.remote.api.UsgsEarthquakeApi
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.FeedFreshnessPolicy
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus

@Singleton
class OfflineFirstEarthquakeRepository @Inject constructor(
    private val api: UsgsEarthquakeApi,
    private val dao: EarthquakeDao,
    private val feedSyncDao: FeedSyncDao,
    private val clock: Clock
) : EarthquakeRepository {

    // Una exclusión mutua por período evita que Home
    // y Mapa descarguen simultáneamente el mismo feed.
    private val refreshLocks = RecentWindow.entries
        .associateWith { Mutex() }

    override fun observeEarthquakes(
        query: EarthquakeQuery
    ): Flow<List<Earthquake>> {
        return dao.observeEarthquakes(
            startTime = query.startTime.toEpochMilli(),
            endTime = query.endTime.toEpochMilli(),
            minimumMagnitude = query.minimumMagnitude,
            depthFilter = query.depthFilter.name
        ).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

    override fun observeEarthquake(
        id: String
    ): Flow<Earthquake?> {
        return dao.observeEarthquake(id)
            .map { entity ->
                entity?.toDomain()
            }
    }

    override suspend fun refreshRecent(
        window: RecentWindow
    ): RefreshResult {
        return refreshLocks.getValue(window).withLock {
            downloadAndStore(window)
        }
    }

    override suspend fun refreshIfStale(
        window: RecentWindow
    ): FeedSyncResult {
        return refreshLocks.getValue(window).withLock {

            val feed = UsgsFeed.forWindow(window)

            // La comprobación se hace dentro del lock:
            // otra pantalla podría haber terminado
            // una sincronización mientras esperábamos.
            val lastSuccess = feedSyncDao
                .getLastSuccessEpochMillis(feed.feedName)

            val shouldRefresh = FeedFreshnessPolicy.shouldRefresh(
                window = window,
                lastSuccessEpochMillis = lastSuccess,
                nowEpochMillis = clock.millis()
            )

            if (!shouldRefresh) {
                return@withLock FeedSyncResult.UpToDate
            }

            when (val result = downloadAndStore(window)) {
                RefreshResult.Success -> FeedSyncResult.Updated

                is RefreshResult.Failure -> FeedSyncResult.Failed(
                    result.reason
                )
            }
        }
    }

    override suspend fun getFeedCacheStatus(
        window: RecentWindow
    ): FeedCacheStatus {

        val feed = UsgsFeed.forWindow(window)

        val lastSuccess = feedSyncDao
            .getLastSuccessEpochMillis(feed.feedName)
            ?: return FeedCacheStatus.Unverified

        val isFresh = !FeedFreshnessPolicy.shouldRefresh(
            window = window,
            lastSuccessEpochMillis = lastSuccess,
            nowEpochMillis = clock.millis()
        )

        return FeedCacheStatus.Synced(
            lastSuccessEpochMillis = lastSuccess,
            isFresh = isFresh
        )
    }

    private suspend fun downloadAndStore(
        window: RecentWindow
    ): RefreshResult {
        val feed = UsgsFeed.forWindow(window)

        val response = try {
            api.getFeed(feed.feedName)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            return RefreshResult.Failure(
                RefreshFailure.SERVICE
            )
        } catch (exception: SerializationException) {
            return RefreshResult.Failure(
                RefreshFailure.INVALID_RESPONSE
            )
        } catch (exception: IOException) {
            return RefreshResult.Failure(
                RefreshFailure.CONNECTION
            )
        }

        val entities = withContext(Dispatchers.Default) {
            response.toDomain()
                .map { it.toEntity() }
        }

        dao.upsertIfNewer(entities)

        feedSyncDao.upsert(
            FeedSyncEntity(
                feedName = feed.feedName,
                lastSuccessEpochMillis = clock.millis()
            )
        )

        return RefreshResult.Success
    }
}