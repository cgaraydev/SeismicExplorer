package com.cgaraydev.seismicexplorer.domain.repository

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

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
}