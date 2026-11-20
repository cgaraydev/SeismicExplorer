package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EarthquakeVisualPilotTest {
    @get:Rule val composeRule = createComposeRule()

    private val event = Earthquake(
        id = "pilot",
        magnitude = 5.0,
        place = "Central East Pacific Rise",
        coordinates = GeoCoordinates(-18.4, -112.5),
        depthKilometers = 10.0,
        magType = "mb",
        reviewStatus = "reviewed",
        occurredAt = Instant.parse("2026-11-19T20:07:00Z"),
        updatedAt = Instant.parse("2026-11-19T20:10:00Z"),
        detailUrl = null
    )

    @Test fun magnitudIsDynamicReadableAndOpensEducation() {
        var opened = false
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeEssentialHero(
                    earthquake = event,
                    place = "Dorsal del Pacífico Oriental Central",
                    magnitude = "M 5.0",
                    onMagnitudeInfo = { opened = true }
                )
            }
        }
        composeRule.onNodeWithTag("detail_magnitude_dial", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("detail_magnitude_primary", useUnmergedTree = true)
            .assertTextContains("M 5.0")
        composeRule.onNodeWithTag("detail_place")
            .assertTextContains("Dorsal del Pacífico Oriental Central")
        composeRule.onNodeWithTag("detail_time_utc")
            .assertTextContains("UTC", substring = true)
        composeRule.onNodeWithTag("detail_magnitude_info").performClick()
        composeRule.runOnIdle { assertTrue(opened) }
    }

    @Test fun metricsStayAccessibleWithoutQuestionMarks() {
        var scaleOpened = false
        var depthOpened = false
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                Column {
                    EarthquakeEssentialMetrics(
                        earthquake = event,
                        onDepthInfo = { depthOpened = true },
                        onScaleInfo = { scaleOpened = true }
                    )
                }
            }
        }
        composeRule.onNodeWithTag("detail_essential_metrics").assertExists()
        composeRule.onNodeWithTag("detail_metric_status").assertExists()
        composeRule.onNodeWithText("?").assertDoesNotExist()
        composeRule.onNodeWithTag("detail_scale_info").performClick()
        composeRule.onNodeWithTag("detail_depth_info").performClick()
        composeRule.runOnIdle {
            assertTrue(scaleOpened)
            assertTrue(depthOpened)
        }
    }
}
