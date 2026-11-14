package com.cgaraydev.seismicexplorer.data.remote.api

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class UsgsEarthquakeApiHttpTest {

    private lateinit var server: MockWebServer
    private lateinit var api: UsgsEarthquakeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = Json {
            ignoreUnknownKeys = true
        }

        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(UsgsEarthquakeApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun successfulResponse_deserializesGeoJson() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/geo+json")
                .setBody(validGeoJson)
        )

        val response = api.getFeed("all_day")
        val request = server.takeRequest()

        assertEquals(
            "/earthquakes/feed/v1.0/summary/all_day.geojson",
            request.path
        )
        assertEquals(1, response.features.size)
        assertEquals("us123", response.features.single().id)
        assertEquals(
            4.5,
            response.features.single().properties?.mag!!,
            0.0
        )
    }

    @Test
    fun emptyFeatureCollection_isAccepted() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"features":[]}""")
        )

        val response = api.getFeed("all_week")

        assertTrue(response.features.isEmpty())
    }

    @Test
    fun httpErrors_arePropagatedAsHttpException() = runBlocking {
        assertHttpError(404)
        assertHttpError(500)
        assertHttpError(503)
    }

    @Test
    fun malformedJson_isRejected() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"features":[}""")
        )

        val exception = runCatching {
            api.getFeed("all_day")
        }.exceptionOrNull()

        assertTrue(exception is SerializationException)
    }

    @Test
    fun incompatibleJsonType_isRejected() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                        "features": [
                            {
                                "id": "us123",
                                "properties": {
                                    "mag": {"value": 4.5}
                                }
                            }
                        ]
                    }
                    """.trimIndent()
                )
        )

        val exception = runCatching {
            api.getFeed("all_day")
        }.exceptionOrNull()

        assertTrue(exception is SerializationException)
    }

    @Test
    fun missingRequiredFeatures_isRejected() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"type":"FeatureCollection"}""")
        )

        val exception = runCatching {
            api.getFeed("all_day")
        }.exceptionOrNull()

        assertTrue(exception is SerializationException)
    }

    private suspend fun assertHttpError(statusCode: Int) {
        server.enqueue(
            MockResponse()
                .setResponseCode(statusCode)
                .setBody("""{"message":"Service error"}""")
        )

        val exception = runCatching {
            api.getFeed("all_day")
        }.exceptionOrNull()

        assertTrue(exception is HttpException)
        assertEquals(
            statusCode,
            (exception as HttpException).code()
        )
    }

    private val validGeoJson = """
        {
            "type": "FeatureCollection",
            "metadata": {
                "title": "USGS Earthquakes",
                "count": 1
            },
            "features": [
                {
                    "type": "Feature",
                    "id": "us123",
                    "properties": {
                        "mag": 4.5,
                        "place": "Coast of Chile",
                        "time": 1700000000000,
                        "updated": 1700000010000,
                        "type": "earthquake",
                        "status": "reviewed"
                    },
                    "geometry": {
                        "type": "Point",
                        "coordinates": [-71.6, -33.4, 12.0]
                    }
                }
            ]
        }
    """.trimIndent()
}