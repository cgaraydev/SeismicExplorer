package com.cgaraydev.seismicexplorer.data.remote

import com.cgaraydev.seismicexplorer.domain.model.RecentWindow

internal enum class UsgsFeed(
    val feedName: String
) {
    DAY("all_day"),
    WEEK("all_week"),
    MONTH("all_month");

    companion object {

        fun forWindow(
            window: RecentWindow
        ): UsgsFeed {
            return when (window) {
                RecentWindow.LAST_DAY -> DAY
                RecentWindow.LAST_WEEK -> WEEK
                RecentWindow.LAST_30_DAYS -> MONTH
            }
        }
    }
}