package com.cgaraydev.seismicexplorer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeoCoordinatesTest {

    @Test
    fun validCoordinates_acceptGeographicBoundaries() {
        val coordinates = GeoCoordinates(
            latitude = -90.0,
            longitude = 180.0
        )

        assertEquals(-90.0, coordinates.latitude, 0.0)
        assertEquals(180.0, coordinates.longitude, 0.0)
    }

    @Test
    fun invalidLatitude_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoCoordinates(
                latitude = 91.0,
                longitude = 0.0
            )
        }
    }

    @Test
    fun invalidLongitude_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoCoordinates(
                latitude = 0.0,
                longitude = -181.0
            )
        }
    }

    @Test
    fun nonFiniteCoordinates_areRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoCoordinates(
                latitude = Double.NaN,
                longitude = 0.0
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            GeoCoordinates(
                latitude = 0.0,
                longitude = Double.POSITIVE_INFINITY
            )
        }
    }
}