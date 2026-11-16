package com.cgaraydev.seismicexplorer.presentation.map

import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure

sealed interface MapSyncState {

    data object Idle : MapSyncState

    data class Syncing(
        val window: RecentWindow,
        val cacheStatus: FeedCacheStatus =
            FeedCacheStatus.Unknown
    ) : MapSyncState

    data class Ready(
        val window: RecentWindow,
        val downloaded: Boolean,
        val cacheStatus: FeedCacheStatus =
            FeedCacheStatus.Unknown
    ) : MapSyncState

    data class Failed(
        val window: RecentWindow,
        val reason: RefreshFailure? = null,
        val cacheStatus: FeedCacheStatus =
            FeedCacheStatus.Unknown
    ) : MapSyncState
}