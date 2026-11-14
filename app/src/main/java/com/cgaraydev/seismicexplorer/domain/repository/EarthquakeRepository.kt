package com.cgaraydev.seismicexplorer.domain.repository

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import kotlinx.coroutines.flow.Flow

interface EarthquakeRepository {

    fun observeEarthquakes(
        query: EarthquakeQuery
    ): Flow<List<Earthquake>>

    suspend fun refresh(
        query: EarthquakeQuery
    )
}