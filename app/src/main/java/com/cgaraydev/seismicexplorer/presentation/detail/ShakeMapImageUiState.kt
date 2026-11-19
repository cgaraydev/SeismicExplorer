package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.ui.graphics.ImageBitmap
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure

sealed interface ShakeMapImageUiState {
    data object Idle : ShakeMapImageUiState
    data object Loading : ShakeMapImageUiState
    data class Available(
        val image: ImageBitmap,
        val fromCache: Boolean,
        val isStale: Boolean
    ) : ShakeMapImageUiState
    data class Error(val reason: ShakeMapImageFailure) : ShakeMapImageUiState
}
