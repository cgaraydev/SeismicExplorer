package com.cgaraydev.seismicexplorer.domain.model

import java.time.Instant

data class EarthquakeQuery(
    val startTime: Instant,
    val endTime: Instant,
    val minimumMagnitude: Double? = null,
    val depthFilter: EarthquakeDepthFilter =
        EarthquakeDepthFilter.ALL
) {
    init {
        require(startTime.isBefore(endTime)) {
            "Start time must be before end time"
        }

        require(
            minimumMagnitude == null ||
                    minimumMagnitude.isFinite()
        ) {
            "Minimum magnitude must be finite when specified"
        }
    }
}