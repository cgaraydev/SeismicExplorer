package com.cgaraydev.seismicexplorer.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.cgaraydev.seismicexplorer.data.local.entity.FeedSyncEntity

@Dao
interface FeedSyncDao {

    @Query(
        """
        SELECT lastSuccessEpochMillis
        FROM feed_sync
        WHERE feedName = :feedName
        LIMIT 1
        """
    )
    suspend fun getLastSuccessEpochMillis(
        feedName: String
    ): Long?

    @Upsert
    suspend fun upsert(
        entity: FeedSyncEntity
    )
}