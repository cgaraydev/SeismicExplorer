package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(
        state: HomeUiState = HomeUiState(isReadingLocal = false),
        onAction: (HomeAction) -> Unit = {},
        onEvent: (String) -> Unit = {},
        onMap: () -> Unit = {}
    ) {
        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(state, onAction, onEarthquakeClick = onEvent, onMapClick = onMap)
            }
        }
    }

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag(tag))
    }

    @Test fun editorialHeader_isPresent_withoutModeTabs() {
        show()
        composeRule.onNodeWithTag("editorial_header").assertIsDisplayed()
        composeRule.onNodeWithText("El planeta, hoy").assertIsDisplayed()
        composeRule.onNodeWithTag("explore_map").assertDoesNotExist()
        composeRule.onNodeWithTag("explore_list").assertDoesNotExist()
    }

    @Test fun selectingPeriod_emitsExpectedAction() {
        var selected: HomeAction? = null
        show(onAction = { selected = it })
        scrollTo("explore_filters_toggle")
        composeRule.onNodeWithTag("explore_filters_toggle").performClick()
        scrollTo("explore_period_LAST_WEEK")
        composeRule.onNodeWithTag("explore_period_LAST_WEEK").performClick()
        composeRule.runOnIdle {
            assertEquals(HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK), selected)
        }
    }

    @Test fun selectingMagnitude_emitsExpectedAction() {
        var selected: HomeAction? = null
        show(onAction = { selected = it })
        scrollTo("explore_filters_toggle")
        composeRule.onNodeWithTag("explore_filters_toggle").performClick()
        scrollTo("explore_magnitude_MAGNITUDE_4_5")
        composeRule.onNodeWithTag("explore_magnitude_MAGNITUDE_4_5").performClick()
        composeRule.runOnIdle {
            assertEquals(HomeAction.SelectMagnitude(MagnitudeThreshold.MAGNITUDE_4_5), selected)
        }
    }

    @Test fun filtersAreCollapsedByDefault_andCanBeExpanded() {
        show()
        composeRule.onNodeWithTag("explore_filter_panel").assertDoesNotExist()
        scrollTo("explore_filters_toggle")
        composeRule.onNodeWithTag("explore_filters_toggle").performClick()
        composeRule.onNodeWithTag("explore_filter_panel").assertExists()
    }

    @Test fun refreshButton_emitsRefreshAction() {
        var selected: HomeAction? = null
        show(onAction = { selected = it })
        scrollTo("explore_refresh")
        composeRule.onNodeWithTag("explore_refresh").performClick()
        composeRule.runOnIdle { assertEquals(HomeAction.Refresh, selected) }
    }

    @Test fun emptyState_displaysInformativeMessage() {
        show(HomeUiState(isReadingLocal = false, refreshState = HomeRefreshState.Completed))
        composeRule.onNodeWithTag("home_list").performScrollToNode(
            hasText("No hay eventos para estos filtros"))
        composeRule.onNodeWithText("No hay eventos para estos filtros").assertIsDisplayed()
    }

    @Test fun featuredEvent_isMaximumKnownMagnitude_andOpensItsId() {
        var selectedId: String? = null
        show(HomeUiState(isReadingLocal = false, earthquakes = listOf(
            sample("unknown", null), sample("small", 2.3), sample("largest", 5.2))),
            onEvent = { selectedId = it })
        scrollTo("editorial_open_featured")
        composeRule.onNodeWithTag("editorial_open_featured").performClick()
        composeRule.runOnIdle { assertEquals("largest", selectedId) }
    }

    @Test fun unknownMagnitudes_doNotCreateAnInventedFeaturedEvent() {
        show(HomeUiState(isReadingLocal = false, earthquakes = listOf(sample("unknown", null))))
        composeRule.onNodeWithTag("editorial_featured").assertDoesNotExist()
        scrollTo("earthquake_unknown")
        composeRule.onNodeWithTag("earthquake_unknown").assertIsDisplayed()
    }

    @Test fun featuredEvent_changesWhenResultsChange() {
        var state by mutableStateOf(HomeUiState(isReadingLocal = false,
            earthquakes = listOf(sample("first", 5.0))))
        var opened: String? = null
        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(state, {}, onEarthquakeClick = { opened = it })
            }
        }
        composeRule.runOnIdle {
            state = state.copy(earthquakes = listOf(sample("second", 4.0)))
        }
        scrollTo("editorial_open_featured")
        composeRule.onNodeWithTag("editorial_open_featured").performClick()
        composeRule.runOnIdle { assertEquals("second", opened) }
    }

    @Test fun mapAction_opensExistingMap() {
        var requested = false
        show(onMap = { requested = true })
        scrollTo("editorial_open_map")
        composeRule.onNodeWithTag("editorial_open_map").performClick()
        composeRule.runOnIdle { assertTrue(requested) }
    }

    @Test fun learningAction_opensAndClosesExplanation() {
        show()
        scrollTo("editorial_learn")
        composeRule.onNodeWithTag("editorial_learn").performClick()
        composeRule.onNodeWithTag("editorial_learning_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Entendido").performClick()
        composeRule.onNodeWithTag("editorial_learning_dialog").assertDoesNotExist()
    }

    @Test fun chart_isExplicitlyIllustrative_andCanBeCollapsed() {
        show()
        scrollTo("editorial_chart")
        composeRule.onNodeWithText("VISTA PREVIA · DATOS DE EJEMPLO").assertIsDisplayed()
        scrollTo("editorial_chart_toggle")
        composeRule.onNodeWithTag("editorial_chart_toggle").performClick()
        composeRule.onNodeWithText("Mostrar ejemplo").assertIsDisplayed()
    }

    @Test fun eventsAfterChart_remainReachableAndClickable() {
        var opened: String? = null
        show(HomeUiState(isReadingLocal = false,
            earthquakes = (1..8).map { sample("event$it", 3.0) }), onEvent = { opened = it })
        scrollTo("earthquake_event8")
        composeRule.onNodeWithTag("earthquake_event8").performClick()
        composeRule.runOnIdle { assertEquals("event8", opened) }
    }

    @Test fun cachedEvents_remainAvailableWhenRefreshFails() {
        show(HomeUiState(isReadingLocal = false, earthquakes = listOf(sample("cached", 4.5)),
            refreshState = HomeRefreshState.Failed(RefreshFailure.CONNECTION)))
        composeRule.onNodeWithTag("home_list").performScrollToNode(
            hasText("No se pudo conectar con USGS. Se mantienen los datos guardados."))
        composeRule.onNodeWithText("No se pudo conectar con USGS. Se mantienen los datos guardados.")
            .assertIsDisplayed()
        scrollTo("earthquake_cached")
        composeRule.onNodeWithTag("earthquake_cached").assertIsDisplayed()
    }

    @Test fun refreshingState_keepsCachedEventsAvailable() {
        show(HomeUiState(isReadingLocal = false, earthquakes = listOf(sample("cached", 4.5)),
            refreshState = HomeRefreshState.Refreshing))
        scrollTo("explore_refresh")
        composeRule.onNodeWithTag("explore_refresh").assertIsNotEnabled()
        scrollTo("earthquake_cached")
        composeRule.onNodeWithTag("earthquake_cached").assertIsDisplayed()
    }

    @Test fun staleFeed_displaysLastSyncStatus() {
        show(HomeUiState(isReadingLocal = false,
            feedCacheStatus = FeedCacheStatus.Synced(
                Instant.parse("2026-11-14T12:00:00Z").toEpochMilli(), false)))
        composeRule.onNodeWithTag("home_list").performScrollToNode(
            hasText("Datos desactualizados", substring = true))
        composeRule.onNode(hasText("Datos desactualizados", substring = true)).assertIsDisplayed()
    }

    private fun sample(id: String, magnitude: Double?) = Earthquake(
        id = id, magnitude = magnitude, place = "Coast of Chile",
        coordinates = GeoCoordinates(-33.4, -71.6), depthKilometers = 12.0,
        occurredAt = Instant.parse("2026-11-14T10:00:00Z"),
        updatedAt = Instant.parse("2026-11-14T10:30:00Z"), detailUrl = null)
}
