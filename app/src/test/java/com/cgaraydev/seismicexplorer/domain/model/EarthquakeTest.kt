package com.cgaraydev.seismicexplorer.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class EarthquakeTest {

    private fun createEarthquake() = Earthquake(
        id = "us123456",
        magnitude = 4.5,
        place = "Near the coast of Chile",
        coordinates = GeoCoordinates(
            latitude = -33.4,
            longitude = -71.6
        ),
        depthKilometers = 15.0,
        occurredAt = Instant.parse("2026-11-13T10:00:00Z"),
        updatedAt = Instant.parse("2026-11-13T10:15:00Z"),
        detailUrl = "https://earthquake.usgs.gov/"
    )

    @Test
    fun validEarthquake_preservesItsData() {
        val earthquake = createEarthquake()

        assertEquals("us123456", earthquake.id)
        assertEquals(4.5, earthquake.magnitude!!, 0.0)
        assertEquals(-33.4, earthquake.coordinates.latitude, 0.0)
    }

    @Test
    fun missingOptionalData_isAccepted() {
        val earthquake = createEarthquake().copy(
            magnitude = null,
            place = null,
            depthKilometers = null,
            detailUrl = null
        )

        assertNull(earthquake.magnitude)
        assertNull(earthquake.place)
        assertNull(earthquake.depthKilometers)
        assertNull(earthquake.detailUrl)
    }

    @Test
    fun negativeMagnitudeAndDepth_areAccepted() {
        val earthquake = createEarthquake().copy(
            magnitude = -0.5,
            depthKilometers = -1.0
        )

        assertEquals(-0.5, earthquake.magnitude!!, 0.0)
        assertEquals(-1.0, earthquake.depthKilometers!!, 0.0)
    }

    @Test
    fun blankId_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            createEarthquake().copy(id = " ")
        }
    }

    @Test
    fun nonFiniteMagnitudeOrDepth_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            createEarthquake().copy(magnitude = Double.NaN)
        }

        assertThrows(IllegalArgumentException::class.java) {
            createEarthquake().copy(
                depthKilometers = Double.POSITIVE_INFINITY
            )
        }
    }
}