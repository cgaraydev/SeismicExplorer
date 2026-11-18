package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Pruebas incrementales: no modifican los tests locales del Observatorio 3.3.0. */
class ObservatoryMetricCardsTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun metricCards_navigateToEventsAndToStrongestEarthquake() {
        var eventsClicks = 0
        var openedId: String? = null
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                ObservatoryDashboard(
                    state = HomeUiState(
                        observatorySnapshot = ObservatorySnapshot(
                            listOf(event("one", 3.8), event("two", 5.4)),
                            TimeWindow.LAST_DAY,
                            Instant.parse("2026-11-17T16:00:00Z")
                        ),
                        isReadingObservatory = false,
                        isReadingLocal = false,
                        periodEndAt = Instant.parse("2026-11-17T16:00:00Z")
                    ),
                    onAction = {},
                    onOpenCatalog = { eventsClicks++ },
                    onOpenEarthquake = { openedId = it }
                )
            }
        }
        rule.onNodeWithTag("observatory_dashboard")
            .performScrollToNode(hasTestTag("observatory_events_card"))
        rule.onNodeWithTag("observatory_events_card").performClick()
        rule.runOnIdle { assertEquals(1, eventsClicks) }
        rule.onNodeWithTag("observatory_dashboard")
            .performScrollToNode(hasTestTag("observatory_max_card"))
        rule.onNodeWithTag("observatory_max_card").performClick()
        rule.runOnIdle { assertEquals("two", openedId) }
    }

    @Test
    fun maximumMetricDisabledWhenMagnitudeUnknown() {
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                ObservatoryDashboard(
                    state = HomeUiState(
                        observatorySnapshot = ObservatorySnapshot(
                            emptyList(),
                            TimeWindow.LAST_DAY,
                            Instant.parse("2026-11-17T16:00:00Z")
                        ),
                        feedCacheStatus = com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus.Synced(
                            lastSuccessEpochMillis = Instant.parse("2026-11-17T16:00:00Z").toEpochMilli(),
                            isFresh = true
                        ),
                        isReadingLocal = false,
                        isReadingObservatory = false
                    ),
                    onAction = {},
                    onOpenCatalog = {},
                    onOpenEarthquake = {}
                )
            }
        }
        rule.onNodeWithTag("observatory_dashboard")
            .performScrollToNode(hasTestTag("observatory_max_card"))
        rule.onNodeWithTag("observatory_max_card").assertIsNotEnabled()
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
