package com.cgaraydev.seismicexplorer.presentation.detail

import com.cgaraydev.seismicexplorer.domain.model.ScientificDetail
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailFailure
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailSource

/** Estado exclusivo de la consulta voluntaria de productos científicos. */
sealed interface ScientificDetailUiState {
    data object Idle : ScientificDetailUiState
    data object Loading : ScientificDetailUiState

    data class Available(
        val detail: ScientificDetail,
        val source: ScientificDetailSource,
        val isStale: Boolean
    ) : ScientificDetailUiState

    data class Error(val failure: ScientificDetailFailure) : ScientificDetailUiState
}
