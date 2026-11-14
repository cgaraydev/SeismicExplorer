package com.cgaraydev.seismicexplorer.presentation.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onAction(action: HomeAction) {
        _uiState.update { currentState ->
            when (action) {
                is HomeAction.SelectTimeWindow -> {
                    currentState.copy(timeWindow = action.value)
                }

                is HomeAction.SelectMagnitude -> {
                    currentState.copy(minimumMagnitude = action.value)
                }
            }
        }
    }
}
