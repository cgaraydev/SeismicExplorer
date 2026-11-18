package com.cgaraydev.seismicexplorer.presentation.learn

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.presentation.detail.EarthquakeDetailScreen
import com.cgaraydev.seismicexplorer.presentation.detail.EarthquakeDetailUiState
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LearnDetailAccessTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun depthInfoOpensSheetAndArticleCallback() {
        var requested = 0
        val time = Instant.parse("2026-11-14T10:00:00Z")
        val event = Earthquake(
            id = "us-depth-test",
            magnitude = 5.1,
            place = "Coast of Chile",
            coordinates = GeoCoordinates(-33.4, -71.6),
            depthKilometers = 12.0,
            occurredAt = time,
            updatedAt = time,
            detailUrl = null
        )
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(event),
                    onBack = {},
                    onOpenSource = {},
                    onOpenDepthLearn = { requested++ }
                )
            }
        }
        rule.onNodeWithTag("detail_depth_info").performScrollTo().performClick()
        rule.onNodeWithTag("detail_depth_sheet", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("detail_depth_learn_button").performClick()
        rule.runOnIdle { assertEquals(1, requested) }
    }

    @Test
    fun magnitudeInfoOpensSheetAndArticleCallback() {
        var requested = 0
        val time = Instant.parse("2026-11-14T10:00:00Z")
        val event = Earthquake(
            id = "us-test",
            magnitude = 5.1,
            place = "Coast of Chile",
            coordinates = GeoCoordinates(-33.4, -71.6),
            depthKilometers = 12.0,
            occurredAt = time,
            updatedAt = time,
            detailUrl = null
        )
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(event),
                    onBack = {},
                    onOpenSource = {},
                    onOpenLearn = { requested++ }
                )
            }
        }
        rule.onNodeWithTag("detail_magnitude_info").performClick()
        rule.onNodeWithTag("detail_magnitude_sheet", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("detail_magnitude_learn_button").performClick()
        rule.runOnIdle { assertEquals(1, requested) }
    }
}
