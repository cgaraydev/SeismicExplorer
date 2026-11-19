package com.cgaraydev.seismicexplorer.di

import com.cgaraydev.seismicexplorer.data.repository.OfflineFirstEarthquakeRepository
import com.cgaraydev.seismicexplorer.data.repository.CachedShakeMapImageRepository
import com.cgaraydev.seismicexplorer.domain.repository.ShakeMapImageRepository
import com.cgaraydev.seismicexplorer.data.repository.OfflineFirstScientificDetailRepository
import com.cgaraydev.seismicexplorer.domain.repository.ScientificDetailRepository
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindEarthquakeRepository(
        implementation: OfflineFirstEarthquakeRepository
    ): EarthquakeRepository

    @Binds
    abstract fun bindShakeMapImageRepository(
        implementation: CachedShakeMapImageRepository
    ): ShakeMapImageRepository

    @Binds
    abstract fun bindScientificDetailRepository(
        implementation: OfflineFirstScientificDetailRepository
    ): ScientificDetailRepository

    companion object {

        @Provides
        @Singleton
        fun provideClock(): Clock {
            return Clock.systemUTC()
        }
    }
}