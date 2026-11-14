package com.cgaraydev.seismicexplorer.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNull
import org.junit.Test

class EarthquakeQueryTest {

    private val startTime = Instant.parse("2026-11-12T00:00:00Z")
    private val endTime = Instant.parse("2026-11-13T00:00:00Z")

    @Test
    fun validQuery_preservesFilters() {
        val query = EarthquakeQuery(
            startTime = startTime,
            endTime = endTime,
            minimumMagnitude = 2.5
        )

        assertEquals(startTime, query.startTime)
        assertEquals(endTime, query.endTime)
        assertEquals(2.5, query.minimumMagnitude!!, 0.0)
    }

    @Test
    fun minimumMagnitude_isOptional() {
        val query = EarthquakeQuery(
            startTime = startTime,
            endTime = endTime
        )

        assertNull(query.minimumMagnitude)
    }

    @Test
    fun invalidTimeRange_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            EarthquakeQuery(
                startTime = endTime,
                endTime = startTime
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            EarthquakeQuery(
                startTime = startTime,
                endTime = startTime
            )
        }
    }

    @Test
    fun nonFiniteMinimumMagnitude_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            EarthquakeQuery(
                startTime = startTime,
                endTime = endTime,
                minimumMagnitude = Double.NaN
            )
        }
    }
}