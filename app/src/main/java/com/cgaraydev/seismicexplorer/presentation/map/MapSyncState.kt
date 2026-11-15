package com.cgaraydev.seismicexplorer.presentation.map

import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure

sealed interface MapSyncState {

    data object Idle : MapSyncState

    data class Syncing(
        val window: RecentWindow
    ) : MapSyncState

    data class Ready(
        val window: RecentWindow,
        val downloaded: Boolean
    ) : MapSyncState

    data class Failed(
        val window: RecentWindow,
        val reason: RefreshFailure? = null
    ) : MapSyncState
}