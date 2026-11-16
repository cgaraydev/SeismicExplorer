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

class MapEarthquakeGeoJsonStreamingTest {

    @Test
    fun encodesEscapedIdsAndPlacesWithoutLosingFeatureOrder() {
        val firstId = "us\"\\\n\t🌋"
        val firstPlace = "Región del Biobío \"Chile\" \\ costa\nNueva línea"
        val secondId = "us-second"
        val earthquakes = listOf(
            sampleEarthquake(firstId, firstPlace, 4.2),
            sampleEarthquake(secondId, "Other place", 2.0)
        )

        val root = Json.parseToJsonElement(
            MapEarthquakeGeoJson.encode(earthquakes)
        ).jsonObject
        val features = root.getValue("features").jsonArray

        assertEquals(2, features.size)
        assertEquals(
            listOf(firstId, secondId),
            features.map { it.jsonObject.getValue("id").jsonPrimitive.content }
        )
        assertEquals(
            firstId,
            features[0].jsonObject.getValue("properties").jsonObject
                .getValue("eventId").jsonPrimitive.content
        )
        assertEquals(
            firstPlace,
            features[0].jsonObject.getValue("properties").jsonObject
                .getValue("place").jsonPrimitive.content
        )
        assertEquals(
            4.2,
            features[0].jsonObject.getValue("properties").jsonObject
                .getValue("magnitude").jsonPrimitive.double,
            0.0
        )
    }

    @Test
    fun nullablePropertiesRemainAbsentNotNullLiterals() {
        val event = sampleEarthquake("missing-optionals", null, null)
        val feature = Json.parseToJsonElement(
            MapEarthquakeGeoJson.encode(listOf(event))
        ).jsonObject.getValue("features").jsonArray.single().jsonObject
        val properties = feature.getValue("properties").jsonObject

        assertEquals("missing-optionals", properties.getValue("eventId").jsonPrimitive.content)
        assertFalse(properties.containsKey("place"))
        assertFalse(properties.containsKey("magnitude"))
        assertEquals(2, feature.getValue("geometry").jsonObject
            .getValue("coordinates").jsonArray.size)
    }

    @Test
    fun largeInputYieldsOneValidFeaturePerEarthquake() {
        val events = (0 until 4_096).map { index ->
            sampleEarthquake("event-$index", "Chile $index", (index % 80) / 10.0)
        }

        val root = Json.parseToJsonElement(
            MapEarthquakeGeoJson.encode(events)
        ).jsonObject
        val features = root.getValue("features").jsonArray
        val ids = features.map { it.jsonObject.getValue("id").jsonPrimitive.content }

        assertEquals(4_096, features.size)
        assertEquals(4_096, ids.toSet().size)
        assertEquals(events.map { it.id }, ids)
        assertEquals(MapEarthquakeGeoJson.EMPTY, MapEarthquakeGeoJson.encode(emptyList()))
    }

    private fun sampleEarthquake(
        id: String,
        place: String?,
        magnitude: Double?
    ) = Earthquake(
        id = id,
        magnitude = magnitude,
        place = place,
        coordinates = GeoCoordinates(latitude = -33.4, longitude = -71.6),
        depthKilometers = 12.0,
        occurredAt = Instant.parse("2026-11-14T10:00:00Z"),
        updatedAt = Instant.parse("2026-11-14T10:30:00Z"),
        detailUrl = null
    )
}
