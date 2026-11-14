package com.cgaraydev.seismicexplorer.presentation.detail

import com.cgaraydev.seismicexplorer.domain.model.Earthquake

sealed interface EarthquakeDetailUiState {

    data object Loading : EarthquakeDetailUiState

    data class Content(
        val earthquake: Earthquake
    ) : EarthquakeDetailUiState

    data object NotFound : EarthquakeDetailUiState

    data object Error : EarthquakeDetailUiState
}