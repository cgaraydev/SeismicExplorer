package com.cgaraydev.seismicexplorer.domain.model

import java.time.Duration
import java.time.Instant

enum class RecentWindow(
    val duration: Duration
) {
    LAST_DAY(Duration.ofDays(1)),
    LAST_WEEK(Duration.ofDays(7)),
    LAST_30_DAYS(Duration.ofDays(30));

    fun toQuery(
        endTime: Instant,
        minimumMagnitude: Double? = null,
        depthFilter: EarthquakeDepthFilter =
            EarthquakeDepthFilter.ALL
    ): EarthquakeQuery {
        return EarthquakeQuery(
            startTime = endTime.minus(duration),
            endTime = endTime,
            minimumMagnitude = minimumMagnitude,
            depthFilter = depthFilter
        )
    }
}