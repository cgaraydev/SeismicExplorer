package com.cgaraydev.seismicexplorer.data.remote

import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UsgsFeedTest {

    private val now = Instant.parse("2026-11-14T12:00:00Z")

    @Test
    fun recentQuery_selectsDailyFeed() {
        val feed = UsgsFeed.forQuery(
            query = queryStartingHoursAgo(12),
            now = now
        )

        assertEquals(UsgsFeed.DAY, feed)
    }

    @Test
    fun multiDayQuery_selectsWeeklyFeed() {
        val feed = UsgsFeed.forQuery(
            query = queryStartingDaysAgo(3),
            now = now
        )

        assertEquals(UsgsFeed.WEEK, feed)
    }

    @Test
    fun longerQuery_selectsMonthlyFeed() {
        val feed = UsgsFeed.forQuery(
            query = queryStartingDaysAgo(14),
            now = now
        )

        assertEquals(UsgsFeed.MONTH, feed)
    }

    @Test
    fun historicalQuery_isRejectedUntilFdsnIsSupported() {
        assertThrows(UnsupportedOperationException::class.java) {
            UsgsFeed.forQuery(
                query = queryStartingDaysAgo(31),
                now = now
            )
        }
    }

    private fun queryStartingHoursAgo(hours: Long) = EarthquakeQuery(
        startTime = now.minus(hours, ChronoUnit.HOURS),
        endTime = now
    )

    private fun queryStartingDaysAgo(days: Long) = EarthquakeQuery(
        startTime = now.minus(days, ChronoUnit.DAYS),
        endTime = now
    )
}