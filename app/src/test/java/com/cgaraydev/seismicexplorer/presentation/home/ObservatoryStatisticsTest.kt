package com.cgaraydev.seismicexplorer.presentation.home

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObservatoryStatisticsTest {
    private val end = Instant.parse("2026-11-17T16:00:00Z")

    @Test
    fun lastDay_groupsActualEventsByHourAndDoesNotInventData() {
        val events = listOf(
            event("oldest", 4.8, 12.0, end.minusSeconds(86_400)),
            event("recent", 5.2, 20.0, end.minusSeconds(600)),
            event("unknown", null, null, end.minusSeconds(3600)),
            event("latest", 2.1, 32.0, end.minusSeconds(120))
        )
        val stats = calculateObservatoryStatistics(events, TimeWindow.LAST_DAY, end)
        assertEquals(4, stats.eventCount)
        assertEquals("recent", stats.largestEvent?.id)
        assertEquals(2, stats.magnitudeAtLeast45)
        assertEquals(24, stats.bucketCounts.size)
        assertEquals(1, stats.bucketCounts[0])
        assertEquals(3, stats.bucketCounts[23])
        assertEquals(4, stats.bucketCounts.sum())
        assertEquals((12.0 + 20.0 + 32.0) / 3, stats.averageDepthKilometers!!, 0.00001)
    }

    @Test
    fun lastDay_excludesEventsAtEndAndBeforeLowerBoundFromChart() {
        val events = listOf(
            event("before", 3.1, null, end.minusSeconds(86_401)),
            event("future", 3.2, null, end),
            event("within", 3.3, null, end.minusSeconds(15))
        )
        val stats = calculateObservatoryStatistics(events, TimeWindow.LAST_DAY, end)
        assertEquals(3, stats.eventCount) // La lista recibida sigue siendo la fuente del total.
        assertEquals(1, stats.bucketCounts.sum()) // El histograma respeta el intervalo.
    }

    @Test
    fun lastWeek_groupsSevenDailyBuckets() {
        val stats = calculateObservatoryStatistics(
            listOf(
                event("one", 1.0, null, end.minusSeconds(7 * 86_400 - 50)),
                event("two", 2.0, null, end.minusSeconds(2 * 86_400)),
                event("three", 3.0, null, end.minusSeconds(10))
            ), TimeWindow.LAST_WEEK, end
        )
        assertEquals(7, stats.bucketCounts.size)
        assertEquals(1, stats.bucketCounts.first())
        assertEquals(1, stats.bucketCounts.last())
        assertEquals(3, stats.bucketCounts.sum())
    }

    @Test
    fun emptyCollectionHasUnknownMagnitudeAndDepth() {
        val stats = calculateObservatoryStatistics(emptyList(), TimeWindow.LAST_MONTH, end)
        assertEquals(0, stats.eventCount)
        assertEquals(30, stats.bucketCounts.size)
        assertEquals(0, stats.bucketCounts.sum())
        assertNull(stats.largestEvent)
        assertNull(stats.averageDepthKilometers)
    }

    private fun event(id: String, magnitude: Double?, depth: Double?, occurredAt: Instant) =
        Earthquake(
            id = id,
            magnitude = magnitude,
            place = "Zona de prueba",
            coordinates = GeoCoordinates(latitude = -33.0, longitude = -71.0),
            depthKilometers = depth,
            occurredAt = occurredAt,
            updatedAt = occurredAt,
            detailUrl = null
        )
}
