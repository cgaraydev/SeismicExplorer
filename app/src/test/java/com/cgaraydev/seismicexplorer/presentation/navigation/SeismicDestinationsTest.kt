package com.cgaraydev.seismicexplorer.presentation.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SeismicDestinationsTest {

    @Test
    fun detailDestination_roundTripsThroughSerialization() {
        val original = EarthquakeDetailDestination(
            earthquakeId = "us123"
        )

        val encoded = Json.encodeToString(original)

        val restored = Json.decodeFromString<
                EarthquakeDetailDestination
                >(encoded)

        assertEquals(original, restored)
    }

    @Test
    fun homeDestination_roundTripsThroughSerialization() {
        val encoded = Json.encodeToString(HomeDestination)

        val restored = Json.decodeFromString<
                HomeDestination
                >(encoded)

        assertEquals(HomeDestination, restored)
    }

    @Test
    fun mapDestination_roundTripsThroughSerialization() {
        val encoded = Json.encodeToString(MapDestination)

        val restored = Json.decodeFromString<MapDestination>(encoded)

        assertEquals(MapDestination, restored)
    }
}