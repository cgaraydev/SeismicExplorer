
package com.cgaraydev.seismicexplorer.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    private val viewModel = HomeViewModel()

    @Test
    fun initialState_hasDefaultFilters() {
        val state = viewModel.uiState.value

        assertEquals(TimeWindow.LAST_DAY, state.timeWindow)
        assertEquals(MagnitudeThreshold.ALL, state.minimumMagnitude)
    }

    @Test
    fun selectTimeWindow_updatesSelectedPeriod() {
        viewModel.onAction(
            HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK)
        )

        assertEquals(
            TimeWindow.LAST_WEEK,
            viewModel.uiState.value.timeWindow
        )
    }

    @Test
    fun selectMagnitude_updatesSelectedThreshold() {
        viewModel.onAction(
            HomeAction.SelectMagnitude(MagnitudeThreshold.MAGNITUDE_4_5)
        )

        assertEquals(
            MagnitudeThreshold.MAGNITUDE_4_5,
            viewModel.uiState.value.minimumMagnitude
        )
    }

    @Test
    fun changingPeriod_preservesSelectedMagnitude() {
        viewModel.onAction(
            HomeAction.SelectMagnitude(MagnitudeThreshold.MAGNITUDE_2_5)
        )

        viewModel.onAction(
            HomeAction.SelectTimeWindow(TimeWindow.LAST_MONTH)
        )

        assertEquals(
            TimeWindow.LAST_MONTH,
            viewModel.uiState.value.timeWindow
        )

        assertEquals(
            MagnitudeThreshold.MAGNITUDE_2_5,
            viewModel.uiState.value.minimumMagnitude
        )
    }
}
