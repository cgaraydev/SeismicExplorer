package com.cgaraydev.seismicexplorer.data.local

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.local.dao.FeedSyncDao
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import com.cgaraydev.seismicexplorer.data.local.entity.FeedSyncEntity

@Database(
    entities = [
        EarthquakeEntity::class,
        FeedSyncEntity::class
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
abstract class SeismicDatabase : RoomDatabase() {

    abstract fun earthquakeDao(): EarthquakeDao

    abstract fun feedSyncDao(): FeedSyncDao
}