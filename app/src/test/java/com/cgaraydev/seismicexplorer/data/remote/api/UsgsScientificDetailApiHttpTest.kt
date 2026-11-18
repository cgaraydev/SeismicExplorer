package com.cgaraydev.seismicexplorer.data.remote.api

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit

class UsgsScientificDetailApiHttpTest {
    @Test fun fixedEndpointUsesRequestedIdWithoutUrlRedirectParameter() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("{}"))
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .build()
                .create(UsgsScientificDetailApi::class.java)
            api.getDetail("us123").close()
            assertEquals(
                "/earthquakes/feed/v1.0/detail/us123.geojson",
                server.takeRequest().path
            )
        } finally {
            server.shutdown()
        }
    }
}
