package com.cgaraydev.seismicexplorer.data.remote

import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import java.time.Duration
import java.time.Instant

internal enum class UsgsFeed(
    val feedName: String
) {
    DAY("all_day"),
    WEEK("all_week"),
    MONTH("all_month");

    companion object {

        fun forQuery(
            query: EarthquakeQuery,
            now: Instant
        ): UsgsFeed {
            require(!query.startTime.isAfter(now)) {
                "Query cannot start in the future"
            }

            val elapsed = Duration.between(query.startTime, now)

            return when {
                elapsed <= Duration.ofDays(1) -> DAY
                elapsed <= Duration.ofDays(7) -> WEEK
                elapsed <= Duration.ofDays(30) -> MONTH

                else -> throw UnsupportedOperationException(
                    "Historical queries require the USGS FDSN API"
                )
            }
        }
    }
}