package com.cgaraydev.seismicexplorer.di

import com.cgaraydev.seismicexplorer.data.remote.api.UsgsEarthquakeApi
import com.cgaraydev.seismicexplorer.data.remote.api.UsgsScientificDetailApi
import com.cgaraydev.seismicexplorer.data.remote.UsgsScientificDetailParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object UsgsNetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://earthquake.usgs.gov/")
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideUsgsEarthquakeApi(
        retrofit: Retrofit
    ): UsgsEarthquakeApi {
        return retrofit.create(UsgsEarthquakeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUsgsScientificDetailApi(retrofit: Retrofit): UsgsScientificDetailApi =
        retrofit.create(UsgsScientificDetailApi::class.java)

    @Provides
    @Singleton
    fun provideUsgsScientificDetailParser(json: Json): UsgsScientificDetailParser =
        UsgsScientificDetailParser(json)
}