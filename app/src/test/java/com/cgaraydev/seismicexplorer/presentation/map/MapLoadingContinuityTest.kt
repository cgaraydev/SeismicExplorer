package com.cgaraydev.seismicexplorer.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLoadingContinuityTest {

    private val previousGeoJson =
        """{"type":"FeatureCollection","features":[{"type":"Feature"}]}"""

    private val previousBounds = MapEventBounds(
        south = -34.0,
        west = -72.0,
        north = -33.0,
        east = -71.0
    )

    private val success = SeismicMapUiState(
        geoJson = previousGeoJson,
        eventCount = 1,
        eventBounds = previousBounds,
        isLoading = false
    )

    @Test
    fun initialLoadingDoesNotInventPreviousMarkers() {
        val policy = MapLoadingContinuity()
        val loading = SeismicMapUiState(isLoading = true)

        assertEquals(loading, policy.accept(loading))
    }

    @Test
    fun newFilterKeepsPreviousMarkersOnlyWhileLoading() {
        val policy = MapLoadingContinuity()
        policy.accept(success)

        val newFilters = MapFilterSelection(
            magnitude = MapMagnitudeFilter.MAGNITUDE_4_5
        )
        val loading = SeismicMapUiState(
            filters = newFilters,
            isLoading = true
        )

        val result = policy.accept(loading)

        assertTrue(result.isLoading)
        assertEquals(newFilters, result.filters)
        assertEquals(previousGeoJson, result.geoJson)
        assertEquals(1, result.eventCount)
        assertEquals(previousBounds, result.eventBounds)
    }

    @Test
    fun realEmptyRoomResultReplacesPreviousMarkers() {
        val policy = MapLoadingContinuity()
        policy.accept(success)
        policy.accept(SeismicMapUiState(isLoading = true))

        val emptyResult = SeismicMapUiState(
            geoJson = MapEarthquakeGeoJson.EMPTY,
            eventCount = 0,
            isLoading = false
        )

        assertEquals(emptyResult, policy.accept(emptyResult))

        val nextLoading = policy.accept(
            SeismicMapUiState(isLoading = true)
        )
        assertEquals(MapEarthquakeGeoJson.EMPTY, nextLoading.geoJson)
        assertEquals(0, nextLoading.eventCount)
    }

    @Test
    fun failedQueryClearsMarkersAndDoesNotResurrectStaleData() {
        val policy = MapLoadingContinuity()
        policy.accept(success)

        val failedResult = SeismicMapUiState(
            hasError = true,
            isLoading = false
        )

        val error = policy.accept(failedResult)
        assertTrue(error.hasError)
        assertEquals(MapEarthquakeGeoJson.EMPTY, error.geoJson)

        val retry = policy.accept(SeismicMapUiState(isLoading = true))
        assertTrue(retry.isLoading)
        assertFalse(retry.hasError)
        assertEquals(MapEarthquakeGeoJson.EMPTY, retry.geoJson)
    }
}
