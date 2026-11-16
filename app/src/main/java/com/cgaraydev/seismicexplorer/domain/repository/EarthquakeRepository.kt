package com.cgaraydev.seismicexplorer.domain.repository

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus

interface EarthquakeRepository {

    fun observeEarthquakes(
        query: EarthquakeQuery
    ): Flow<List<Earthquake>>

    fun observeEarthquake(
        id: String
    ): Flow<Earthquake?>

    suspend fun refreshRecent(
        window: RecentWindow
    ): RefreshResult

    suspend fun refreshIfStale(
        window: RecentWindow
    ): FeedSyncResult

    suspend fun getFeedCacheStatus(
        window: RecentWindow
    ): FeedCacheStatus

}