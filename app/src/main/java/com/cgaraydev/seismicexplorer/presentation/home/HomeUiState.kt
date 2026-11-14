package com.cgaraydev.seismicexplorer.presentation.home

enum class TimeWindow {
    LAST_DAY,
    LAST_WEEK,
    LAST_MONTH
}

enum class MagnitudeThreshold {
    ALL,
    MAGNITUDE_2_5,
    MAGNITUDE_4_5
}

data class HomeUiState(
    val timeWindow: TimeWindow = TimeWindow.LAST_DAY,
    val minimumMagnitude: MagnitudeThreshold = MagnitudeThreshold.ALL
)
