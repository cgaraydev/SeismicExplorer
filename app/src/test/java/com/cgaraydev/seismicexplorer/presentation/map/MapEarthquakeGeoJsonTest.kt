package com.cgaraydev.seismicexplorer.presentation.map

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MapEarthquakeGeoJsonTest {

    @Test
    fun emptyList_createsEmptyFeatureCollection() {
        val result = MapEarthquakeGeoJson.encode(emptyList())

        assertEquals(MapEarthquakeGeoJson.EMPTY, result)

        val json = Json.parseToJsonElement(result).jsonObject

        assertEquals(
            "FeatureCollection",
            json.getValue("type").jsonPrimitive.content
        )

        assertEquals(
            0,
            json.getValue("features").jsonArray.size
        )
    }

    @Test
    fun earthquake_coordinatesUseLongitudeLatitudeOrder() {
        val result = MapEarthquakeGeoJson.encode(
            listOf(sampleEarthquake())
        )

        val root = Json.parseToJsonElement(result).jsonObject
        val features = root.getValue("features").jsonArray
        val feature = features.first().jsonObject
        val geometry = feature.getValue("geometry").jsonObject
        val coordinates = geometry.getValue("coordinates").jsonArray

        assertEquals(1, features.size)
        assertEquals(-71.6, coordinates[0].jsonPrimitive.double, 0.0)
        assertEquals(-33.4, coordinates[1].jsonPrimitive.double, 0.0)
    }

    @Test
    fun earthquake_preservesIdMagnitudeAndPlace() {
        val result = MapEarthquakeGeoJson.encode(
            listOf(sampleEarthquake())
        )

        val root = Json.parseToJsonElement(result).jsonObject
        val feature = root.getValue("features")
            .jsonArray.first().jsonObject
        val properties = feature.getValue("properties").jsonObject

        assertEquals("us123", feature.getValue("id").jsonPrimitive.content)
        assertEquals(
            "us123",
            properties.getValue("eventId").jsonPrimitive.content
        )
        assertEquals(
            4.5,
            properties.getValue("magnitude").jsonPrimitive.double,
            0.0
        )
        assertEquals(
            "Coast of Chile",
            properties.getValue("place").jsonPrimitive.content
        )
    }

    @Test
    fun nullMagnitude_isOmitted() {
        val result = MapEarthquakeGeoJson.encode(
            listOf(sampleEarthquake().copy(magnitude = null))
        )

        val root = Json.parseToJsonElement(result).jsonObject
        val properties = root.getValue("features")
            .jsonArray.first().jsonObject
            .getValue("properties").jsonObject

        assertFalse(properties.containsKey("magnitude"))
    }

    @Test
    fun specialCharacters_areProperlyEscaped() {
        val place = "Región del Biobío \"Chile\""

        val result = MapEarthquakeGeoJson.encode(
            listOf(sampleEarthquake().copy(place = place))
        )

        val root = Json.parseToJsonElement(result).jsonObject
        val properties = root.getValue("features")
            .jsonArray.first().jsonObject
            .getValue("properties").jsonObject

        assertEquals(
            place,
            properties.getValue("place").jsonPrimitive.content
        )
    }

    private fun sampleEarthquake() = Earthquake(
        id = "us123",
        magnitude = 4.5,
        place = "Coast of Chile",
        coordinates = GeoCoordinates(
            latitude = -33.4,
            longitude = -71.6
        ),
        depthKilometers = 12.0,
        occurredAt = Instant.parse("2026-11-14T10:00:00Z"),
        updatedAt = Instant.parse("2026-11-14T10:30:00Z"),
        detailUrl = null
    )
}