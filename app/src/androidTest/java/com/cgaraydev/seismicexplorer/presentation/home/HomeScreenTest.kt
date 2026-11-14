package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.hasTestTag

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_displaysInformativeMessage() {
        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(
                        isReadingLocal = false,
                        refreshState = HomeRefreshState.Completed
                    ),
                    onAction = {}
                )
            }
        }

        composeRule
            .onNodeWithTag("home_list")
            .performScrollToNode(
                hasText("No hay eventos para estos filtros")
            )

        composeRule
            .onNodeWithText("No hay eventos para estos filtros")
            .assertIsDisplayed()
    }

    @Test
    fun selectingPeriod_emitsExpectedAction() {
        var emittedAction: HomeAction? = null

        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onAction = { emittedAction = it }
                )
            }
        }

        composeRule
            .onNodeWithText("7 días")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK),
                emittedAction
            )
        }
    }

    @Test
    fun refreshButton_emitsRefreshAction() {
        var emittedAction: HomeAction? = null

        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(isReadingLocal = false),
                    onAction = { emittedAction = it }
                )
            }
        }

        composeRule
            .onNodeWithTag("home_list")
            .performScrollToNode(hasText("Actualizar"))

        composeRule
            .onNodeWithText("Actualizar")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(HomeAction.Refresh, emittedAction)
        }
    }

    @Test
    fun cachedEvents_remainVisibleWhenRefreshFails() {
        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(
                        earthquakes = listOf(sampleEarthquake()),
                        isReadingLocal = false,
                        refreshState = HomeRefreshState.Failed(
                            RefreshFailure.CONNECTION
                        )
                    ),
                    onAction = {}
                )
            }
        }

        composeRule
            .onNodeWithTag("home_list")
            .performScrollToNode(hasText("Coast of Chile"))

        composeRule
            .onNodeWithText("Coast of Chile")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("home_list")
            .performScrollToNode(
                hasText(
                    "No se pudo conectar con USGS. " +
                            "Se mantienen los datos guardados."
                )
            )

        composeRule
            .onNodeWithText(
                "No se pudo conectar con USGS. " +
                        "Se mantienen los datos guardados."
            )
            .assertIsDisplayed()
    }

    @Test
    fun refreshingState_keepsCachedEventsAvailable() {
        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(
                        earthquakes = listOf(sampleEarthquake()),
                        isReadingLocal = false,
                        refreshState = HomeRefreshState.Refreshing
                    ),
                    onAction = {}
                )
            }
        }

        composeRule
            .onNodeWithTag("home_list")
            .performScrollToNode(hasText("Coast of Chile"))

        composeRule
            .onNodeWithText("Coast of Chile")
            .assertIsDisplayed()
    }

    @Test
    fun selectingEarthquake_emitsItsId() {
        var selectedId: String? = null

        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(
                        earthquakes = listOf(sampleEarthquake()),
                        isReadingLocal = false
                    ),
                    onAction = {},
                    onEarthquakeClick = { selectedId = it }
                )
            }
        }

        composeRule
            .onNodeWithTag("home_list")
            .performScrollToNode(
                hasTestTag("earthquake_us123")
            )

        composeRule
            .onNodeWithTag("earthquake_us123")
            .performClick()

        composeRule.runOnIdle {
            assertEquals("us123", selectedId)
        }
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
        detailUrl = null
    )
}