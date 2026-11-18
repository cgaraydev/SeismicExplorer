package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ObservatoryDataAvailabilityTest {
    @get:Rule val rule = createComposeRule()
    private val end = Instant.parse("2026-11-17T16:00:00Z")

    @Test
    fun pendingLocalReadDoesNotDisplayZeroPrematurely() {
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                ObservatoryDashboard(HomeUiState(), {}, {}, {})
            }
        }
        rule.onNodeWithTag("observatory_unavailable").assertTextContains("Cargando", substring = true)
    }

    @Test
    fun emptyWithoutConfirmedFeedShowsUnavailableAndNotZero() {
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                ObservatoryDashboard(
                    HomeUiState(
                        observatorySnapshot = ObservatorySnapshot(emptyList(), TimeWindow.LAST_DAY, end),
                        isReadingObservatory = false,
                        feedCacheStatus = FeedCacheStatus.Unverified
                    ), {}, {}, {}
                )
            }
        }
        rule.onNodeWithTag("observatory_unavailable").assertTextContains("Sin datos disponibles", substring = true)
    }

    @Test
    fun emptyAfterConfirmedSyncIsExplicitAboutAvailableRecords() {
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                ObservatoryDashboard(
                    HomeUiState(
                        observatorySnapshot = ObservatorySnapshot(emptyList(), TimeWindow.LAST_DAY, end),
                        isReadingObservatory = false,
                        feedCacheStatus = FeedCacheStatus.Synced(end.toEpochMilli(), true)
                    ), {}, {}, {}
                )
            }
        }
        rule.onNodeWithTag("observatory_events_count", useUnmergedTree = true)
            .assertTextContains("0")
        rule.onNodeWithText("0 eventos disponibles para este período.").assertExists()
    }

    @Test
    fun openEventsPreservesExplicitMagnitudeSelection() {
        var opened by mutableStateOf(false)
        var filterActions = 0
        val event = Earthquake(
            id = "event-1", magnitude = 5.0, place = "Chile",
            coordinates = GeoCoordinates(-33.0, -71.0), depthKilometers = 12.0,
            occurredAt = end.minusSeconds(300), updatedAt = end, detailUrl = null
        )
        val state = HomeUiState(
            timeWindow = TimeWindow.LAST_DAY,
            minimumMagnitude = MagnitudeThreshold.MAGNITUDE_4_5,
            earthquakes = listOf(event),
            isReadingLocal = false,
            observatorySnapshot = ObservatorySnapshot(listOf(event), TimeWindow.LAST_DAY, end),
            isReadingObservatory = false
        )
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                if (opened) {
                    SeismicCatalogScreen(state, { filterActions++ }, {})
                } else {
                    ObservatoryDashboard(state, { filterActions++ }, { opened = true }, {})
                }
            }
        }
        rule.onNodeWithTag("observatory_events_card").performClick()
        rule.onNodeWithTag("catalog_filter_summary").assertTextContains("M 4.5+", substring = true)
        rule.runOnIdle { assertEquals(0, filterActions) }
    }
}
