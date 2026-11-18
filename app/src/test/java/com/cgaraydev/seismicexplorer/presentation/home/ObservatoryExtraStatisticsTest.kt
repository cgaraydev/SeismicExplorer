package com.cgaraydev.seismicexplorer.presentation.home

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifica los nuevos valores sin sobrescribir ObservatoryStatisticsTest.kt. */
class ObservatoryExtraStatisticsTest {
    private val end = Instant.parse("2026-11-17T16:00:00Z")

    @Test
    fun calculatesM25ThresholdAndMaximumKnownDepth() {
        val events = listOf(
            event("mild", 2.4, 12.0),
            event("boundary", 2.5, 32.0),
            event("strong", 5.2, null),
            event("unknown", null, -2.0)
        )
        val stats = calculateObservatoryStatistics(events, TimeWindow.LAST_DAY, end)
        assertEquals(2, stats.magnitudeAtLeast25)
        assertEquals(32.0, stats.maxDepthKilometers!!, 0.00001)
        assertEquals(22.0, stats.averageDepthKilometers!!, 0.00001)
    }

    @Test
    fun noEventsHaveNoMaximumDepthAndNoM25Events() {
        val stats = calculateObservatoryStatistics(emptyList(), TimeWindow.LAST_WEEK, end)
        assertEquals(0, stats.magnitudeAtLeast25)
        assertNull(stats.maxDepthKilometers)
    }

    private fun event(id: String, magnitude: Double?, depth: Double?) = Earthquake(
        id = id,
        magnitude = magnitude,
        place = "Zona de prueba",
        coordinates = GeoCoordinates(latitude = -33.0, longitude = -71.0),
        depthKilometers = depth,
        occurredAt = end.minusSeconds(900),
        updatedAt = end.minusSeconds(900),
        detailUrl = null
    )
}
