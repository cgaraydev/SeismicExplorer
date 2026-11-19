package com.cgaraydev.seismicexplorer.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/** Host fijado por Retrofit: nunca se utiliza una URL arbitraria del JSON remoto. */
interface UsgsScientificDetailApi {
    // Impide que Retrofit almacene todo el cuerpo antes del limite de 2 MiB.
    @Streaming
    @GET("earthquakes/feed/v1.0/detail/{eventId}.geojson")
    suspend fun getDetail(@Path("eventId") eventId: String): ResponseBody
}
