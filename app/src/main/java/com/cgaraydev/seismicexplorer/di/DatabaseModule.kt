package com.cgaraydev.seismicexplorer.di

import android.content.Context
import androidx.room3.Room
import com.cgaraydev.seismicexplorer.data.local.SeismicDatabase
import com.cgaraydev.seismicexplorer.data.local.dao.EarthquakeDao
import androidx.sqlite.driver.AndroidSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.cgaraydev.seismicexplorer.data.local.dao.FeedSyncDao
import com.cgaraydev.seismicexplorer.data.local.dao.ScientificDetailCacheDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SeismicDatabase {
        return Room.databaseBuilder<SeismicDatabase>(
            context = context,
            name = "seismic_explorer.db"
        )
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    @Provides
    fun provideEarthquakeDao(
        database: SeismicDatabase
    ): EarthquakeDao {
        return database.earthquakeDao()
    }

    @Provides
    fun provideFeedSyncDao(
        database: SeismicDatabase
    ): FeedSyncDao {
        return database.feedSyncDao()
    }

    @Provides
    fun provideScientificDetailCacheDao(
        database: SeismicDatabase
    ): ScientificDetailCacheDao = database.scientificDetailCacheDao()

}