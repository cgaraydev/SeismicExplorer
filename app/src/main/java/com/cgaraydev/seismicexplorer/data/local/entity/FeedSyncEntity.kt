package com.cgaraydev.seismicexplorer.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "feed_sync")
data class FeedSyncEntity(
    @PrimaryKey
    val feedName: String,
    val lastSuccessEpochMillis: Long
)