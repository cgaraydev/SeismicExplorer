package com.cgaraydev.seismicexplorer.domain.model

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedFreshnessPolicyTest {

    private val now = Instant.parse(
        "2026-11-15T12:00:00Z"
    ).toEpochMilli()

    @Test
    fun missingTimestamp_requiresRefresh() {
        assertTrue(
            FeedFreshnessPolicy.shouldRefresh(
                window = RecentWindow.LAST_DAY,
                lastSuccessEpochMillis = null,
                nowEpochMillis = now
            )
        )
    }

    @Test
    fun recentDailyFeed_doesNotRequireRefresh() {
        assertFalse(
            FeedFreshnessPolicy.shouldRefresh(
                window = RecentWindow.LAST_DAY,
                lastSuccessEpochMillis =
                    now - Duration.ofMinutes(4).toMillis(),
                nowEpochMillis = now
            )
        )
    }

    @Test
    fun expiredDailyFeed_requiresRefresh() {
        assertTrue(
            FeedFreshnessPolicy.shouldRefresh(
                window = RecentWindow.LAST_DAY,
                lastSuccessEpochMillis =
                    now - Duration.ofMinutes(5).toMillis(),
                nowEpochMillis = now
            )
        )
    }

    @Test
    fun weeklyAndMonthlyFeeds_haveDifferentLimits() {
        val lastSync =
            now - Duration.ofMinutes(20).toMillis()

        assertTrue(
            FeedFreshnessPolicy.shouldRefresh(
                RecentWindow.LAST_WEEK,
                lastSync,
                now
            )
        )

        assertFalse(
            FeedFreshnessPolicy.shouldRefresh(
                RecentWindow.LAST_30_DAYS,
                lastSync,
                now
            )
        )
    }

    @Test
    fun futureTimestamp_requiresRefresh() {
        assertTrue(
            FeedFreshnessPolicy.shouldRefresh(
                RecentWindow.LAST_DAY,
                now + 60_000,
                now
            )
        )
    }
}