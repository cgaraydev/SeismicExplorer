package com.cgaraydev.seismicexplorer.domain.model

sealed interface FeedSyncResult {

    data object UpToDate : FeedSyncResult

    data object Updated : FeedSyncResult

    data class Failed(
        val reason: RefreshFailure
    ) : FeedSyncResult
}