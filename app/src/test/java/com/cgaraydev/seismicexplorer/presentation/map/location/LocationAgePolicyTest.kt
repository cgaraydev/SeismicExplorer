package com.cgaraydev.seismicexplorer.presentation.map.location

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationAgePolicyTest {
    private val now = TimeUnit.MINUTES.toNanos(20)
    private val maxAgeMs = 5 * 60_000L

    @Test
    fun acceptsRecentCachedLocation() {
        assertTrue(LocationAgePolicy.isFresh(now - TimeUnit.MINUTES.toNanos(2), now, maxAgeMs))
    }

    @Test
    fun acceptsLocationExactlyAtAgeLimit() {
        assertTrue(LocationAgePolicy.isFresh(now - TimeUnit.MINUTES.toNanos(5), now, maxAgeMs))
    }

    @Test
    fun rejectsOutdatedLocation() {
        assertFalse(LocationAgePolicy.isFresh(now - TimeUnit.MINUTES.toNanos(6), now, maxAgeMs))
    }

    @Test
    fun rejectsFutureTimestamp() {
        assertFalse(LocationAgePolicy.isFresh(now + 1L, now, maxAgeMs))
    }
}
