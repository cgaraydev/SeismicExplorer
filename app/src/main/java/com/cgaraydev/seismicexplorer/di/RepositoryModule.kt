package com.cgaraydev.seismicexplorer.di

import com.cgaraydev.seismicexplorer.data.repository.OfflineFirstEarthquakeRepository
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindEarthquakeRepository(
        implementation: OfflineFirstEarthquakeRepository
    ): EarthquakeRepository
}