package com.cgaraydev.seismicexplorer.data.remote.api

import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeCollectionDto
import retrofit2.http.GET
import retrofit2.http.Path

interface UsgsEarthquakeApi {

    @GET("earthquakes/feed/v1.0/summary/{feedName}.geojson")
    suspend fun getFeed(
        @Path("feedName") feedName: String
    ): UsgsEarthquakeCollectionDto
}