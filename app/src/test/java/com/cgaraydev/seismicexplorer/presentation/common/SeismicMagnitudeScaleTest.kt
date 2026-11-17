package com.cgaraydev.seismicexplorer.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test

class SeismicMagnitudeScaleTest {
    @Test
    fun unknownMagnitude_usesUnknownBand() {
        assertEquals(
            SeismicMagnitudeBand.UNKNOWN,
            SeismicMagnitudeScale.band(null)
        )
    }

    @Test
    fun scale_usesTheSameThresholdsAsMapLayers() {
        assertEquals(SeismicMagnitudeBand.LOW, SeismicMagnitudeScale.band(-1.0))
        assertEquals(SeismicMagnitudeBand.LOW, SeismicMagnitudeScale.band(2.49))
        assertEquals(SeismicMagnitudeBand.MEDIUM, SeismicMagnitudeScale.band(2.5))
        assertEquals(SeismicMagnitudeBand.MEDIUM, SeismicMagnitudeScale.band(4.49))
        assertEquals(SeismicMagnitudeBand.HIGH, SeismicMagnitudeScale.band(4.5))
        assertEquals(SeismicMagnitudeBand.HIGH, SeismicMagnitudeScale.band(8.0))
    }
}
