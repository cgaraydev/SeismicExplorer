package com.cgaraydev.seismicexplorer.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.cgaraydev.seismicexplorer.data.local.entity.ScientificDetailCacheEntity

/** Almacenamiento independiente de la retencion y reconciliacion de feeds. */
@Dao
interface ScientificDetailCacheDao {
    @Query("SELECT * FROM scientific_detail_cache WHERE eventId = :eventId LIMIT 1")
    suspend fun get(eventId: String): ScientificDetailCacheEntity?

    @Upsert
    suspend fun upsert(entity: ScientificDetailCacheEntity)

    @Query("DELETE FROM scientific_detail_cache WHERE fetchedAtEpochMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("""
        DELETE FROM scientific_detail_cache
        WHERE eventId NOT IN (
            SELECT eventId FROM scientific_detail_cache
            ORDER BY fetchedAtEpochMillis DESC, eventId ASC LIMIT :maxEntries
        )
    """)
    suspend fun keepLatest(maxEntries: Int)
}
