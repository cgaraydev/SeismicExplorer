package com.cgaraydev.seismicexplorer.data.remote

import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class UsgsFeedTest {

    @Test
    fun lastDay_selectsDailyFeed() {
        val feed = UsgsFeed.forWindow(RecentWindow.LAST_DAY)

        assertEquals("all_day", feed.feedName)
    }

    @Test
    fun lastWeek_selectsWeeklyFeed() {
        val feed = UsgsFeed.forWindow(RecentWindow.LAST_WEEK)

        assertEquals("all_week", feed.feedName)
    }

    @Test
    fun last30Days_selectsMonthlyFeed() {
        val feed = UsgsFeed.forWindow(RecentWindow.LAST_30_DAYS)

        assertEquals("all_month", feed.feedName)
    }
}