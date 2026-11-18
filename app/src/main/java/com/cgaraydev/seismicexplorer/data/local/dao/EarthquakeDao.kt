package com.cgaraydev.seismicexplorer.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import com.cgaraydev.seismicexplorer.data.local.entity.FeedSyncEntity
import kotlinx.coroutines.flow.Flow

internal class SuspiciousFeedSnapshotException(
    cachedCount: Int,
    missingCount: Int,
    incomingCount: Int
) : IllegalStateException(
    "Suspicious USGS snapshot: cached=$cachedCount, " +
        "missing=$missingCount, incoming=$incomingCount"
)

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
        SELECT id, updatedAtEpochMillis, magType, reviewStatus
        FROM earthquakes
        WHERE id IN (:ids)
        """
    )
    protected abstract suspend fun getExistingVersions(
        ids: List<String>
    ): List<EarthquakeVersion>

    // Enriquecer registros migrados sin modificar el resto de una revisión ya almacenada.
    @Query(
        """
        UPDATE earthquakes
        SET magType = COALESCE(magType, :magType),
            reviewStatus = COALESCE(reviewStatus, :reviewStatus)
        WHERE id = :id AND updatedAtEpochMillis = :updatedAtEpochMillis
        """
    )
    protected abstract suspend fun fillMissingScientificMetadata(
        id: String,
        updatedAtEpochMillis: Long,
        magType: String?,
        reviewStatus: String?
    )

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
        // Evaluate candidates BEFORE modifying the database. A truncated feed
        // can report consistent metadata but still omit most events.
        val idsToRemove = if (reconciliationScope != null) {
            val presentIds = earthquakes.mapTo(HashSet()) { it.id }
            val candidates = findReconciliationCandidates(
                startMillis = reconciliationScope.startEpochMillis,
                endMillis = reconciliationScope.endEpochMillis,
                generatedMillis = reconciliationScope.endEpochMillis
            )
            val missingIds = candidates.filterNot { it in presentIds }

            // Fail closed for an empty snapshot over nonempty cached data,
            // or a large disappearance (at least 5 events and at least 75%).
            // Small, ordinary withdrawals remain allowed.
            val largeWithdrawal = missingIds.size >= 5 &&
                missingIds.size.toLong() * 4L >= candidates.size.toLong() * 3L
            if (missingIds.isNotEmpty() &&
                (earthquakes.isEmpty() || largeWithdrawal)
            ) {
                throw SuspiciousFeedSnapshotException(
                    cachedCount = candidates.size,
                    missingCount = missingIds.size,
                    incomingCount = earthquakes.size
                )
            }
            missingIds
        } else {
            emptyList()
        }

        upsertIfNewer(earthquakes)
        idsToRemove.chunked(400).forEach { ids -> deleteByIds(ids) }

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
            ).associateBy { it.id }

            val newOrUpdated = batch.filter { earthquake ->
                val storedVersion = existingVersions[earthquake.id]
                storedVersion == null ||
                    earthquake.updatedAtEpochMillis > storedVersion.updatedAtEpochMillis
            }
            if (newOrUpdated.isNotEmpty()) {
                upsertEarthquakes(newOrUpdated)
            }

            // Igual timestamp: mantener el registro anterior, pero completar metadatos
            // ausentes. No se sustituyen ni magnitud, ni ubicación, ni valores existentes.
            batch.forEach { earthquake ->
                val storedVersion = existingVersions[earthquake.id]
                if (storedVersion != null &&
                    storedVersion.updatedAtEpochMillis == earthquake.updatedAtEpochMillis &&
                    ((storedVersion.magType == null && earthquake.magType != null) ||
                        (storedVersion.reviewStatus == null && earthquake.reviewStatus != null))
                ) {
                    fillMissingScientificMetadata(
                        id = earthquake.id,
                        updatedAtEpochMillis = earthquake.updatedAtEpochMillis,
                        magType = earthquake.magType,
                        reviewStatus = earthquake.reviewStatus
                    )
                }
            }
        }
    }
}