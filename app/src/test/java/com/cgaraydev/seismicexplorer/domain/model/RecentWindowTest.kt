package com.cgaraydev.seismicexplorer.domain.model

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentWindowTest {

    private val now = Instant.parse("2026-11-14T12:00:00Z")

    @Test
    fun lastDay_createsExact24HourQuery() {
        val query = RecentWindow.LAST_DAY.toQuery(now)

        assertEquals(
            now.minus(24, ChronoUnit.HOURS),
            query.startTime
        )
        assertEquals(now, query.endTime)
    }

    @Test
    fun lastWeek_createsExact7DayQuery() {
        val query = RecentWindow.LAST_WEEK.toQuery(now)

        assertEquals(
            now.minus(7, ChronoUnit.DAYS),
            query.startTime
        )
        assertEquals(now, query.endTime)
    }

    @Test
    fun last30Days_createsExact30DayQuery() {
        val query = RecentWindow.LAST_30_DAYS.toQuery(now)

        assertEquals(
            now.minus(30, ChronoUnit.DAYS),
            query.startTime
        )
        assertEquals(now, query.endTime)
    }

    @Test
    fun movingReferenceTime_shiftsBothQueryBoundaries() {
        val later = now.plusSeconds(10)

        val first = RecentWindow.LAST_30_DAYS.toQuery(now)
        val second = RecentWindow.LAST_30_DAYS.toQuery(later)

        assertEquals(first.startTime.plusSeconds(10), second.startTime)
        assertEquals(first.endTime.plusSeconds(10), second.endTime)
    }

    @Test
    fun query_preservesMinimumMagnitude() {
        val query = RecentWindow.LAST_WEEK.toQuery(
            endTime = now,
            minimumMagnitude = 2.5
        )

        assertEquals(2.5, query.minimumMagnitude!!, 0.0)
    }
}