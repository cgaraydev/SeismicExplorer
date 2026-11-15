
package com.cgaraydev.seismicexplorer.presentation.map

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapEventBoundsTest {

    @Test
    fun emptyList_returnsNull() {
        assertNull(
            MapEventBounds.fromEarthquakes(emptyList())
        )
    }

    @Test
    fun singleEvent_createsSingleLocation() {
        val bounds = requireNotNull(
            MapEventBounds.fromEarthquakes(
                listOf(event("1", -33.4, -71.6))
            )
        )

        assertTrue(bounds.isSingleLocation)
        assertEquals(-33.4, bounds.south, 0.000001)
        assertEquals(-33.4, bounds.north, 0.000001)
        assertEquals(-71.6, bounds.west, 0.000001)
        assertEquals(-71.6, bounds.east, 0.000001)
    }

    @Test
    fun nearbyEvents_createExpectedBounds() {
        val bounds = requireNotNull(
            MapEventBounds.fromEarthquakes(
                listOf(
                    event("1", -33.4, -73.0),
                    event("2", -35.0, -71.0),
                    event("3", -34.0, -72.0)
                )
            )
        )

        assertFalse(bounds.isSingleLocation)
        assertEquals(-35.0, bounds.south, 0.000001)
        assertEquals(-33.4, bounds.north, 0.000001)
        assertEquals(-73.0, bounds.west, 0.000001)
        assertEquals(-71.0, bounds.east, 0.000001)
    }

    @Test
    fun antimeridianEvents_useShortestLongitudeArc() {
        val bounds = requireNotNull(
            MapEventBounds.fromEarthquakes(
                listOf(
                    event("1", -20.0, 179.0),
                    event("2", -21.0, -179.0)
                )
            )
        )

        assertEquals(179.0, bounds.west, 0.000001)
        assertEquals(181.0, bounds.east, 0.000001)

        assertEquals(
            2.0,
            bounds.east - bounds.west,
            0.000001
        )
    }

    @Test
    fun repeatedCoordinates_areSingleLocation() {
        val bounds = requireNotNull(
            MapEventBounds.fromEarthquakes(
                listOf(
                    event("1", -33.4, -71.6),
                    event("2", -33.4, -71.6)
                )
            )
        )

        assertTrue(bounds.isSingleLocation)
    }

    private fun event(
        id: String,
        latitude: Double,
        longitude: Double
    ) = Earthquake(
        id = id,
        magnitude = 3.5,
        place = "Test",
        coordinates = GeoCoordinates(
            latitude = latitude,
            longitude = longitude
        ),
        depthKilometers = 10.0,
        occurredAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        detailUrl = null
    )
}
