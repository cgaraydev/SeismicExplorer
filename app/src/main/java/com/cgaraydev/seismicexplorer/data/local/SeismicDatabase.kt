package com.cgaraydev.seismicexplorer.data.local

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.local.dao.FeedSyncDao
import com.cgaraydev.seismicexplorer.data.local.dao.ScientificDetailCacheDao
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import com.cgaraydev.seismicexplorer.data.local.entity.FeedSyncEntity
import com.cgaraydev.seismicexplorer.data.local.entity.ScientificDetailCacheEntity

@Database(
    entities = [
        EarthquakeEntity::class,
        FeedSyncEntity::class,
        ScientificDetailCacheEntity::class
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class SeismicDatabase : RoomDatabase() {

    abstract fun earthquakeDao(): EarthquakeDao

    abstract fun feedSyncDao(): FeedSyncDao

    abstract fun scientificDetailCacheDao(): ScientificDetailCacheDao
}
