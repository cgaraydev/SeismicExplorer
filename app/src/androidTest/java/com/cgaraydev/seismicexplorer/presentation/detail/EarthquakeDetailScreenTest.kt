package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EarthquakeDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentState_displaysEarthquakeInformation() {
        composeRule.setContent {
            SeismicExplorerTheme {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(
                        sampleEarthquake()
                    ),
                    onBack = {},
                    onOpenSource = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Coast of Chile")
            .assertIsDisplayed()
    }

    @Test
    fun notFoundState_displaysInformativeMessage() {
        composeRule.setContent {
            SeismicExplorerTheme {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.NotFound,
                    onBack = {},
                    onOpenSource = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Evento no encontrado")
            .assertIsDisplayed()
    }

    @Test
    fun backButton_invokesCallback() {
        var backRequested = false

        composeRule.setContent {
            SeismicExplorerTheme {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.NotFound,
                    onBack = { backRequested = true },
                    onOpenSource = {}
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
    fun officialSourceButton_opensExpectedUrl() {
        var openedUrl: String? = null

        composeRule.setContent {
            SeismicExplorerTheme {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(
                        sampleEarthquake()
                    ),
                    onBack = {},
                    onOpenSource = { openedUrl = it }
                )
            }
        }

        composeRule
            .onNodeWithText("Ver evento en USGS")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                "https://earthquake.usgs.gov/earthquakes/eventpage/us123",
                openedUrl
            )
        }
    }

    @Test
    fun untrustedSourceUrl_doesNotDisplayOpenButton() {
        val earthquake = sampleEarthquake().copy(
            detailUrl = "https://earthquake.usgs.gov.ejemplo.com/event"
        )

        composeRule.setContent {
            SeismicExplorerTheme {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(earthquake),
                    onBack = {},
                    onOpenSource = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Ver evento en USGS")
            .assertDoesNotExist()
    }

    private fun sampleEarthquake() = Earthquake(
        id = "us123",
        magnitude = 4.5,
        place = "Coast of Chile",
        coordinates = GeoCoordinates(
            latitude = -33.4,
            longitude = -71.6
        ),
        depthKilometers = 12.0,
        occurredAt = Instant.parse("2026-11-14T10:00:00Z"),
        updatedAt = Instant.parse("2026-11-14T10:30:00Z"),
        detailUrl = "https://earthquake.usgs.gov/earthquakes/eventpage/us123"
    )
}