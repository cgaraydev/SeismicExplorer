package com.cgaraydev.seismicexplorer.presentation.map

data class SeismicMapUiState(
    val geoJson: String = MapEarthquakeGeoJson.EMPTY,
    val eventCount: Int = 0,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val eventBounds: MapEventBounds? = null
)