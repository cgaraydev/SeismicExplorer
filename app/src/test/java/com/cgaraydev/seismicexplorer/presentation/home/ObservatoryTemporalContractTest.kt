package com.cgaraydev.seismicexplorer.presentation.home

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservatoryTemporalContractTest {
    private val end = Instant.parse("2026-11-17T16:00:00Z")

    @Test
    fun allStatisticsUseExactlyTheSameTemporalPopulation() {
        val data = listOf(
            earthquake("before", 9.0, 1000.0, end.minusSeconds(86_401)),
            earthquake("start", 4.5, 20.0, end.minusSeconds(86_400)),
            earthquake("inside", 2.5, 40.0, end.minusMillis(1)),
            earthquake("end", 9.1, 2000.0, end)
        )
        val stats = calculateObservatoryStatistics(data, TimeWindow.LAST_DAY, end)
        assertEquals(2, stats.eventCount)
        assertEquals(2, stats.bucketCounts.sum())
        assertEquals("start", stats.largestEvent?.id)
        assertEquals(1, stats.magnitudeAtLeast45)
        assertEquals(2, stats.magnitudeAtLeast25)
        assertEquals(30.0, stats.averageDepthKilometers!!, 0.00001)
        assertEquals(40.0, stats.maxDepthKilometers!!, 0.00001)
    }

    private fun earthquake(id: String, magnitude: Double, depth: Double, time: Instant) =
        Earthquake(
            id = id,
            magnitude = magnitude,
            place = "Prueba",
            coordinates = GeoCoordinates(-33.0, -71.0),
            depthKilometers = depth,
            occurredAt = time,
            updatedAt = time,
            detailUrl = null
        )
}
