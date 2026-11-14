package com.cgaraydev.seismicexplorer.presentation.home

sealed interface HomeAction {

    data class SelectTimeWindow(
        val value: TimeWindow
    ) : HomeAction

    data class SelectMagnitude(
        val value: MagnitudeThreshold
    ) : HomeAction
}
