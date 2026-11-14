package com.cgaraydev.seismicexplorer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MapComputationDispatcher

@Module
@InstallIn(SingletonComponent::class)
object MapDispatcherModule {

    @Provides
    @MapComputationDispatcher
    fun provideMapComputationDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }
}