package com.cgaraydev.seismicexplorer.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EarthquakeDao {

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
    fun observeEarthquakes(
        startTime: Long,
        endTime: Long,
        minimumMagnitude: Double?
    ): Flow<List<EarthquakeEntity>>

    @Upsert
    suspend fun upsertEarthquakes(
        earthquakes: List<EarthquakeEntity>
    )
}