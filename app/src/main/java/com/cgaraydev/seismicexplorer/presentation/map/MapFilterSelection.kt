package com.cgaraydev.seismicexplorer.presentation.map

import com.cgaraydev.seismicexplorer.domain.model.EarthquakeDepthFilter
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow

enum class MapMagnitudeFilter(
    val minimumMagnitude: Double?
) {
    ALL(null),
    MAGNITUDE_2_5(2.5),
    MAGNITUDE_4_5(4.5)
}

data class MapFilterSelection(
    val timeWindow: RecentWindow = RecentWindow.LAST_WEEK,
    val magnitude: MapMagnitudeFilter = MapMagnitudeFilter.ALL,
    val depthFilter: EarthquakeDepthFilter =
        EarthquakeDepthFilter.ALL
)