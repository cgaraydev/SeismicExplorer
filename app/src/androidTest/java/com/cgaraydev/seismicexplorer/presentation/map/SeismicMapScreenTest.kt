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

class SeismicMapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapScreen_displaysTitleAndContent() {
        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    mapContent = { _, _ ->
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
                    mapContent = { _, _ ->
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
                    mapContent = { _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Eventos almacenados (7 días): 3")
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
                    mapContent = { _, selectEarthquake ->
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

}