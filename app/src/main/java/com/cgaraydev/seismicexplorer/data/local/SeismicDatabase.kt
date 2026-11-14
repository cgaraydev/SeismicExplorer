package com.cgaraydev.seismicexplorer.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity

@Database(
    entities = [EarthquakeEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SeismicDatabase : RoomDatabase() {

    abstract fun earthquakeDao(): EarthquakeDao
}