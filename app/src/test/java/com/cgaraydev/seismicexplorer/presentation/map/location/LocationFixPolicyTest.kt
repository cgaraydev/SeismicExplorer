package com.cgaraydev.seismicexplorer.presentation.map.location

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFixPolicyTest {
    private val now = TimeUnit.MINUTES.toNanos(10)
    private val maxAgeMs = 5 * 60_000L

    private fun valid(latitude: Double = -33.45, longitude: Double = -70.65,
                      timestamp: Long = now) = LocationFixPolicy.isUsable(
        latitude = latitude,
        longitude = longitude,
        locationElapsedNanos = timestamp,
        nowElapsedNanos = now,
        maxAgeMs = maxAgeMs
    )

    @Test fun acceptsApproximateRecentCoordinates() {
        assertTrue(valid())
        assertTrue(valid(timestamp = now - TimeUnit.MINUTES.toNanos(5)))
        assertTrue(valid(latitude = -90.0, longitude = 180.0))
    }

    @Test fun rejectsOutOfRangeAndNonFiniteCoordinates() {
        assertFalse(valid(latitude = 90.01))
        assertFalse(valid(longitude = -180.01))
        assertFalse(valid(latitude = Double.NaN))
        assertFalse(valid(longitude = Double.POSITIVE_INFINITY))
    }

    @Test fun rejectsOutdatedAndFutureFixes() {
        assertFalse(valid(timestamp = now - TimeUnit.MINUTES.toNanos(6)))
        assertFalse(valid(timestamp = now + 1L))
    }
}
