package com.cgaraydev.seismicexplorer.presentation.home

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus

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

sealed interface HomeRefreshState {
    data object Idle : HomeRefreshState
    data object Refreshing : HomeRefreshState

    // USGS se descargó correctamente.
    data object Completed : HomeRefreshState

    // No fue necesario descargar: el feed sigue vigente.
    data object UpToDate : HomeRefreshState
    data class Failed(
        val reason: RefreshFailure?
    ) : HomeRefreshState
}

data class HomeUiState(
    val timeWindow: TimeWindow = TimeWindow.LAST_DAY,
    val minimumMagnitude: MagnitudeThreshold = MagnitudeThreshold.ALL,
    val earthquakes: List<Earthquake> = emptyList(),
    val isReadingLocal: Boolean = true,
    val localReadFailed: Boolean = false,
    val refreshState: HomeRefreshState = HomeRefreshState.Idle,
    val feedCacheStatus: FeedCacheStatus = FeedCacheStatus.Unknown
)