package com.cgaraydev.seismicexplorer.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import com.cgaraydev.seismicexplorer.data.local.entity.FeedSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EarthquakeDao {


    @Transaction
    @Query(
        """
    SELECT * FROM earthquakes
    WHERE occurredAtEpochMillis >= :startTime
      AND occurredAtEpochMillis < :endTime
      AND (
          :minimumMagnitude IS NULL
          OR magnitude >= :minimumMagnitude
      )
      AND (
          :depthFilter = 'ALL'

          OR (
              :depthFilter = 'SHALLOW'
              AND depthKilometers >= 0.0
              AND depthKilometers <= 70.0
          )

          OR (
              :depthFilter = 'INTERMEDIATE'
              AND depthKilometers > 70.0
              AND depthKilometers <= 300.0
          )

          OR (
              :depthFilter = 'DEEP'
              AND depthKilometers > 300.0
          )
      )
    ORDER BY occurredAtEpochMillis DESC, id ASC
    """
    )
    abstract fun observeEarthquakes(
        startTime: Long,
        endTime: Long,
        minimumMagnitude: Double?,
        depthFilter: String = "ALL"
    ): Flow<List<EarthquakeEntity>>


    @Query(
        """
    SELECT * FROM earthquakes
    WHERE id = :id
    LIMIT 1
    """
    )
    abstract fun observeEarthquake(
        id: String
    ): Flow<EarthquakeEntity?>

    @Upsert
    protected abstract suspend fun upsertEarthquakes(
        earthquakes: List<EarthquakeEntity>
    )

    @Query(
        """
        SELECT id, updatedAtEpochMillis
        FROM earthquakes
        WHERE id IN (:ids)
        """
    )
    protected abstract suspend fun getExistingVersions(
        ids: List<String>
    ): List<EarthquakeVersion>

    @Upsert
    protected abstract suspend fun upsertFeedSync(entity: FeedSyncEntity)

    @Query(
        """
        SELECT id FROM earthquakes
        WHERE occurredAtEpochMillis >= :startMillis
          AND occurredAtEpochMillis < :endMillis
          AND updatedAtEpochMillis <= :generatedMillis
        """
    )
    protected abstract suspend fun findReconciliationCandidates(
        startMillis: Long,
        endMillis: Long,
        generatedMillis: Long
    ): List<String>

    @Query("DELETE FROM earthquakes WHERE id IN (:ids)")
    protected abstract suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM earthquakes WHERE occurredAtEpochMillis < :cutoffMillis")
    protected abstract suspend fun deleteExpired(cutoffMillis: Long)

    // Todas las escrituras quedan en una sola transacción Room.
    @Transaction
    open suspend fun persistFeedSnapshot(
        earthquakes: List<EarthquakeEntity>,
        feedSync: FeedSyncEntity,
        reconciliationScope: FeedReconciliationScope?,
        retentionCutoffMillis: Long
    ) {
        upsertIfNewer(earthquakes)

        if (reconciliationScope != null) {
            val presentIds = earthquakes.mapTo(HashSet()) { it.id }
            val candidates = findReconciliationCandidates(
                startMillis = reconciliationScope.startEpochMillis,
                endMillis = reconciliationScope.endEpochMillis,
                generatedMillis = reconciliationScope.endEpochMillis
            )
            candidates.filterNot { it in presentIds }
                .chunked(400)
                .forEach { ids -> deleteByIds(ids) }
        }

        // El producto solo permite consultas de hasta 30 días.
        // Un día extra evita podar eventos en el borde temporal.
        deleteExpired(retentionCutoffMillis)
        upsertFeedSync(feedSync)
    }

    @Transaction
    open suspend fun upsertIfNewer(
        earthquakes: List<EarthquakeEntity>
    ) {
        val newestPerId = earthquakes
            .groupBy { it.id }
            .map { (_, versions) ->
                versions.maxBy { it.updatedAtEpochMillis }
            }

        newestPerId.chunked(400).forEach { batch ->
            val existingVersions = getExistingVersions(
                batch.map { it.id }
            ).associate { it.id to it.updatedAtEpochMillis }

            val newOrUpdated = batch.filter { earthquake ->
                val storedVersion = existingVersions[earthquake.id]

                storedVersion == null ||
                        earthquake.updatedAtEpochMillis > storedVersion
            }

            if (newOrUpdated.isNotEmpty()) {
                upsertEarthquakes(newOrUpdated)
            }
        }
    }
}