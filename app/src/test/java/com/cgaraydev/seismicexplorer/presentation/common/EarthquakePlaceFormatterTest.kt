package com.cgaraydev.seismicexplorer.presentation.common

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EarthquakePlaceFormatterTest {
    private val spanish = Locale.forLanguageTag("es-CL")
    private val english = Locale.ENGLISH

    @Test
    fun translatesSouthGeorgiaRegionWithoutChangingUnknownNames() {
        org.junit.Assert.assertEquals("región de la isla Georgia del Sur",
            EarthquakePlaceFormatter.format("South Georgia Island region", java.util.Locale("es")))
        org.junit.Assert.assertEquals("South Georgia Island region",
            EarthquakePlaceFormatter.format("South Georgia Island region", java.util.Locale.ENGLISH))
    }

    @Test fun translatedSixteenPointCompassBearings() {
        val expected = mapOf(
            "N" to "N", "NNE" to "NNE", "NE" to "NE", "ENE" to "ENE",
            "E" to "E", "ESE" to "ESE", "SE" to "SE", "SSE" to "SSE",
            "S" to "S", "SSW" to "SSO", "SW" to "SO", "WSW" to "OSO",
            "W" to "O", "WNW" to "ONO", "NW" to "NO", "NNW" to "NNO"
        )
        expected.forEach { (source, target) ->
            assertEquals("8 km al $target de Valparaíso", EarthquakePlaceFormatter.format(
                "8 km $source of Valparaíso", spanish
            ))
        }
    }

    @Test fun directionWithoutDistanceIsSupported() {
        assertEquals("al NNO de Valparaíso", EarthquakePlaceFormatter.format(
            "NNW of Valparaíso", spanish
        ))
    }

    @Test fun distanceDecimalMilesAndCaseArePreserved() {
        assertEquals("12.5 mi al ONO de Anchorage", EarthquakePlaceFormatter.format(
            "12.5 mi wnw of Anchorage", spanish
        ))
    }

    @Test fun fixedExpressionsTranslateWithoutGuessingTectonics() {
        assertEquals("Costa de Chile", EarthquakePlaceFormatter.format("Coast of Chile", spanish))
        assertEquals("frente a la costa de Chile", EarthquakePlaceFormatter.format(
            "off the coast of Chile", spanish
        ))
        assertEquals("cerca de la costa este de Honshu, Japón", EarthquakePlaceFormatter.format(
            "Near the east coast of Honshu, Japan", spanish
        ))
        assertEquals("región de las islas Sandwich del Sur", EarthquakePlaceFormatter.format(
            "South Sandwich Islands region", spanish
        ))
    }

    @Test fun pacificAntarcticRidgeHasConservativeSpanishName() {
        assertEquals("Dorsal del Pacífico-Antártico", EarthquakePlaceFormatter.format(
            "Pacific-Antarctic Ridge", spanish
        ))
        assertEquals("Pacific-Antarctic Ridge", EarthquakePlaceFormatter.format(
            "Pacific-Antarctic Ridge", english
        ))
    }

    @Test fun unknownToponymIsPreservedAndNonSpanishLocaleNotTranslated() {
        assertEquals("Unknown Shelf offshore", EarthquakePlaceFormatter.format(
            "Unknown Shelf offshore", spanish
        ))
        assertEquals("19 km NNW of Hokkaido", EarthquakePlaceFormatter.format(
            "19 km NNW of Hokkaido", english
        ))
    }

    @Test fun nullAndBlankRemainUnavailable() {
        assertNull(EarthquakePlaceFormatter.format(null, spanish))
        assertNull(EarthquakePlaceFormatter.format(" \t ", spanish))
    }
}
