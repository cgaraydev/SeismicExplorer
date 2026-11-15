package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsNotEnabled
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow


class SeismicMapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapScreen_displaysTitleAndContent() {
        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Mapa sísmico")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Mapa de prueba")
            .assertIsDisplayed()
    }

    @Test
    fun backButton_invokesCallback() {
        var backRequested = false

        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {
                        backRequested = true
                    },
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Volver")
            .performClick()

        composeRule.runOnIdle {
            assertTrue(backRequested)
        }
    }

    @Test
    fun loadedState_displaysStoredEventCount() {
        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    state = SeismicMapUiState(
                        geoJson = MapEarthquakeGeoJson.EMPTY,
                        eventCount = 3,
                        isLoading = false
                    ),
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Eventos filtrados: 3")
            .assertIsDisplayed()
    }


    @Test
    fun selectingEarthquake_propagatesEventId() {
        var selectedEarthquakeId: String? = null

        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    onEarthquakeClick = { earthquakeId ->
                        selectedEarthquakeId = earthquakeId
                    },
                    state = SeismicMapUiState(
                        isLoading = false
                    ),
                    mapContent = { _, selectEarthquake, _, _, _ ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = {
                                    selectEarthquake("us-test-123")
                                }
                            ) {
                                Text("Seleccionar sismo")
                            }
                        }
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Seleccionar sismo")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                "us-test-123",
                selectedEarthquakeId
            )
        }
    }

    @Test
    fun myLocationButton_invokesCallback() {
        var locationRequested = false

        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    onLocateClick = {
                        locationRequested = true
                    },
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Mi ubicación")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(locationRequested)
        }
    }

    @Test
    fun fitEventsButton_requestsCameraFit() {
        val bounds = MapEventBounds(
            south = -35.0,
            west = -73.0,
            north = -33.0,
            east = -71.0
        )

        var receivedBounds: MapEventBounds? = null
        var receivedRequestId = 0

        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    state = SeismicMapUiState(
                        eventCount = 2,
                        eventBounds = bounds,
                        isLoading = false
                    ),
                    mapContent = { _, _, _, currentBounds, requestId ->
                        receivedBounds = currentBounds
                        receivedRequestId = requestId
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Encuadrar eventos")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(bounds, receivedBounds)
            assertEquals(1, receivedRequestId)
        }
    }

    @Test
    fun myLocationButton_isDisabledWhileLocating() {
        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    isLocating = true,
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(
                "Buscando ubicación…"
            )
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun mapFiltersSheet_dispatchesSelectionCallbacks() {
        var selectedWindow: RecentWindow? = null
        var selectedMagnitude: MapMagnitudeFilter? = null

        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    onTimeWindowSelect = {
                        selectedWindow = it
                    },
                    onMagnitudeSelect = {
                        selectedMagnitude = it
                    },
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(
                "Filtrar terremotos"
            )
            .performClick()

        composeRule
            .onNodeWithText("30 días")
            .performClick()

        composeRule
            .onNodeWithText("M 4.5+")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                RecentWindow.LAST_30_DAYS,
                selectedWindow
            )

            assertEquals(
                MapMagnitudeFilter.MAGNITUDE_4_5,
                selectedMagnitude
            )
        }
    }
}