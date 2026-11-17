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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.hasTestTag
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import androidx.compose.ui.test.hasText

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
            .onNodeWithTag("explore_period_LAST_WEEK")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK),
                emittedAction
            )
        }
    }

    @Test
    fun selectingMagnitudeFromCompactMenu_emitsExpectedAction() {
        var emittedAction: HomeAction? = null
        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onAction = { emittedAction = it }
                )
            }
        }
        composeRule.onNodeWithTag("explore_magnitude").performClick()
        composeRule.onNodeWithText("M 4.5+").performClick()
        composeRule.runOnIdle {
            assertEquals(
                HomeAction.SelectMagnitude(MagnitudeThreshold.MAGNITUDE_4_5),
                emittedAction
            )
        }
    }

    @Test
    fun illustratedHeader_isPresent() {
        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(state = HomeUiState(), onAction = {})
            }
        }
        composeRule.onNodeWithTag("explore_header").assertIsDisplayed()
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

    @Test
    fun mapTab_displaysIntegratedPreview_withoutOpeningFullScreen() {
        var fullscreenRequested = false

        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(isReadingLocal = false),
                    onAction = {},
                    onMapClick = { fullscreenRequested = true },
                    // El test verifica la navegacion sin iniciar MapLibre nativo.
                    mapPreview = { _, _, _, _, onExpand ->
                        Column {
                            Text(
                                text = "Vista integrada",
                                modifier = Modifier.testTag("fake_map_preview")
                            )
                            TextButton(onClick = onExpand) {
                                Text("Ampliar mapa")
                            }
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithTag("explore_map").performClick()
        composeRule.onNodeWithTag("fake_map_preview").assertExists()
        composeRule.runOnIdle {
            assertFalse(fullscreenRequested)
        }
        composeRule.onNodeWithText("Ampliar mapa").performClick()
        composeRule.runOnIdle {
            assertTrue(fullscreenRequested)
        }
    }

    @Test
    fun staleFeed_displaysLastSyncStatus() {
        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(
                        isReadingLocal = false,
                        refreshState = HomeRefreshState.UpToDate,
                        feedCacheStatus = FeedCacheStatus.Synced(
                            lastSuccessEpochMillis =
                                Instant.parse(
                                    "2026-11-14T12:00:00Z"
                                ).toEpochMilli(),
                            isFresh = false
                        )
                    ),
                    onAction = {}
                )
            }
        }

        composeRule
            .onNodeWithTag("home_list")
            .performScrollToNode(
                hasText("Datos desactualizados", substring = true)
            )

        composeRule
            .onNode(
                hasText("Datos desactualizados", substring = true)
            )
            .assertIsDisplayed()
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