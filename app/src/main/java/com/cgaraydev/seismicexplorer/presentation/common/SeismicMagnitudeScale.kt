package com.cgaraydev.seismicexplorer.presentation.common

import androidx.compose.ui.graphics.Color

/** Fronteras idénticas a las capas actuales de MapLibre: 2.5 y 4.5. */
internal enum class SeismicMagnitudeBand {
    UNKNOWN, LOW, MEDIUM, HIGH
}

internal object SeismicMagnitudeScale {
    fun band(magnitude: Double?): SeismicMagnitudeBand = when {
        magnitude == null -> SeismicMagnitudeBand.UNKNOWN
        magnitude < 2.5 -> SeismicMagnitudeBand.LOW
        magnitude < 4.5 -> SeismicMagnitudeBand.MEDIUM
        else -> SeismicMagnitudeBand.HIGH
    }

    fun accent(magnitude: Double?): Color = when (band(magnitude)) {
        SeismicMagnitudeBand.UNKNOWN -> Color(0xFF8492A6)
        SeismicMagnitudeBand.LOW -> Color(0xFF35B4B9)
        SeismicMagnitudeBand.MEDIUM -> Color(0xFFF2B34A)
        SeismicMagnitudeBand.HIGH -> Color(0xFFD45845)
    }
}
