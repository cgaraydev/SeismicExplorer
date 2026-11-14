package com.cgaraydev.seismicexplorer.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EarthquakeDao {

    @Query(
        """
        SELECT * FROM earthquakes
        WHERE occurredAtEpochMillis >= :startTime
          AND occurredAtEpochMillis < :endTime
          AND (
              :minimumMagnitude IS NULL
              OR magnitude >= :minimumMagnitude
          )
        ORDER BY occurredAtEpochMillis DESC, id ASC
        """
    )
    abstract fun observeEarthquakes(
        startTime: Long,
        endTime: Long,
        minimumMagnitude: Double?
    ): Flow<List<EarthquakeEntity>>

    @Upsert
    abstract suspend fun upsertEarthquakes(
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