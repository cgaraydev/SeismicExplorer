package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ObservatoryDashboardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun metricsDisplayOnlyProvidedEarthquakes() {
        val end = Instant.parse("2026-11-17T16:00:00Z")
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                ObservatoryDashboard(
                    state = HomeUiState(
                        earthquakes = listOf(event("one", 3.8), event("two", 5.4)),
                        isReadingLocal = false,
                        periodEndAt = end
                    ),
                    onAction = {},
                    onOpenCatalog = {},
                    onOpenEarthquake = {}
                )
            }
        }
        rule.onNodeWithTag("observatory_events_count", useUnmergedTree = true)
            .assertTextContains("2")
        rule.onNodeWithTag("observatory_dashboard")
            .performScrollToNode(hasTestTag("observatory_chart"))
        rule.onNodeWithTag("observatory_chart").assertExists()
        rule.onNodeWithTag("observatory_dashboard")
            .performScrollToNode(hasTestTag("observatory_extra_metrics"))
        rule.onNodeWithTag("observatory_extra_metrics").assertExists()
    }

    @Test
    fun selectPeriodEmitsAction() {
        var received: HomeAction? = null
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                ObservatoryDashboard(
                    state = HomeUiState(isReadingLocal = false),
                    onAction = { received = it },
                    onOpenCatalog = {},
                    onOpenEarthquake = {}
                )
            }
        }
        rule.onNodeWithTag("observatory_window").performClick()
        rule.onNodeWithText("7 días").performClick()
        rule.runOnIdle {
            assertEquals(HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK), received)
        }
    }

    private fun event(id: String, magnitude: Double) = Earthquake(
        id = id,
        magnitude = magnitude,
        place = "Lugar de prueba",
        coordinates = GeoCoordinates(latitude = -33.0, longitude = -71.0),
        depthKilometers = 12.0,
        occurredAt = Instant.parse("2026-11-17T15:00:00Z"),
        updatedAt = Instant.parse("2026-11-17T15:00:00Z"),
        detailUrl = null
    )
}
