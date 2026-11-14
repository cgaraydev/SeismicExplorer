package com.cgaraydev.seismicexplorer.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination : NavKey

@Serializable
data object MapDestination : NavKey

@Serializable
data class EarthquakeDetailDestination(
    val earthquakeId: String
) : NavKey