package com.cgaraydev.seismicexplorer.data.mapper

import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeCollectionDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeFeatureDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeGeometryDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakePropertiesDto
import java.time.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsgsEarthquakeMapperTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun geoJsonResponse_isDeserializedAndMapped() {
        val response = """
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
                            "url": "https://earthquake.usgs.gov/earthquakes/eventpage/us123",
                            "type": "earthquake",
                            "status": "reviewed"
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [-71.6, -33.4, -1.0]
                        }
                    }
                ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<UsgsEarthquakeCollectionDto>(response)
        val earthquakes = dto.toDomain()

        assertEquals(1, earthquakes.size)

        val earthquake = earthquakes.single()

        assertEquals("us123", earthquake.id)
        assertEquals(4.5, earthquake.magnitude!!, 0.0)
        assertEquals(-33.4, earthquake.coordinates.latitude, 0.0)
        assertEquals(-71.6, earthquake.coordinates.longitude, 0.0)
        assertEquals(-1.0, earthquake.depthKilometers!!, 0.0)
        assertEquals(
            Instant.ofEpochMilli(1700000000000),
            earthquake.occurredAt
        )
    }

    @Test
    fun missingOptionalData_isAccepted() {
        val feature = validFeature().copy(
            properties = validProperties().copy(
                mag = null,
                place = null,
                url = null
            ),
            geometry = validGeometry().copy(
                coordinates = listOf(-71.6, -33.4)
            )
        )

        val earthquake = UsgsEarthquakeCollectionDto(
            features = listOf(feature)
        ).toDomain().single()

        assertNull(earthquake.magnitude)
        assertNull(earthquake.place)
        assertNull(earthquake.depthKilometers)
        assertNull(earthquake.detailUrl)
    }

    @Test
    fun invalidCoordinates_areDiscarded() {
        val invalidFeature = validFeature().copy(
            geometry = validGeometry().copy(
                coordinates = listOf(-200.0, -33.4, 10.0)
            )
        )

        val earthquakes = UsgsEarthquakeCollectionDto(
            features = listOf(validFeature(), invalidFeature)
        ).toDomain()

        assertEquals(1, earthquakes.size)
    }

    @Test
    fun missingRequiredTimestamp_isDiscarded() {
        val invalidFeature = validFeature().copy(
            properties = validProperties().copy(
                time = null
            )
        )

        val earthquakes = UsgsEarthquakeCollectionDto(
            features = listOf(invalidFeature)
        ).toDomain()

        assertTrue(earthquakes.isEmpty())
    }

    @Test
    fun nonEarthquakeEvents_areDiscarded() {
        val explosion = validFeature().copy(
            properties = validProperties().copy(
                type = "quarry blast"
            )
        )

        val earthquakes = UsgsEarthquakeCollectionDto(
            features = listOf(explosion)
        ).toDomain()

        assertTrue(earthquakes.isEmpty())
    }

    @Test
    fun nonFiniteMagnitude_isDiscarded() {
        val invalidFeature = validFeature().copy(
            properties = validProperties().copy(
                mag = Double.NaN
            )
        )

        val earthquakes = UsgsEarthquakeCollectionDto(
            features = listOf(invalidFeature)
        ).toDomain()

        assertTrue(earthquakes.isEmpty())
    }

    private fun validFeature() = UsgsEarthquakeFeatureDto(
        id = "us123",
        properties = validProperties(),
        geometry = validGeometry()
    )

    private fun validProperties() = UsgsEarthquakePropertiesDto(
        mag = 4.5,
        place = "Coast of Chile",
        time = 1700000000000,
        updated = 1700000010000,
        url = "https://earthquake.usgs.gov/",
        type = "earthquake"
    )

    private fun validGeometry() = UsgsEarthquakeGeometryDto(
        type = "Point",
        coordinates = listOf(-71.6, -33.4, 10.0)
    )
}