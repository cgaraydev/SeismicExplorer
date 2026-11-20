package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetail
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailFailure
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailSource
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapProduct
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResource
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResourceKind
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
            .onNodeWithTag("detail_place")
            .assertIsDisplayed()
    }

    @Test
    fun detailShowsMagnitudeTypeAndReviewStatus() {
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(
                        sampleEarthquake().copy(magType = "mww", reviewStatus = "reviewed")
                    ),
                    onBack = {},
                    onOpenSource = {}
                )
            }
        }
        composeRule.onNodeWithText("mww").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Revisado").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scientificInventoryIsExplicitlyRequestedInDarkMode() {
        var requests = 0
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(sampleEarthquake()),
                    onBack = {}, onOpenSource = {},
                    onRequestScientificDetail = { requests++ }
                )
            }
        }
        composeRule.onNodeWithTag("detail_tab_impact").performClick()
        composeRule.onNodeWithTag("detail_science_idle").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_science_product_shakemap").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, requests) }
        composeRule.onNodeWithTag("detail_science_request").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun scientificProductsShowCacheStalenessAndEducationalAccess() {
        var learnRequested = 0
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = false) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(sampleEarthquake()),
                    onBack = {}, onOpenSource = {},
                    scientificState = ScientificDetailUiState.Available(
                        detail = ScientificDetail(
                            eventId = "us123", sourceUpdatedAtEpochMillis = 1700000000000L,
                            fetchedAtEpochMillis = 1700000010000L,
                            availableProductTypes = listOf("shakemap", "dyfi"),
                            availableProductCount = 3
                        ),
                        source = ScientificDetailSource.CACHE, isStale = true
                    ),
                    onOpenRevisionLearn = { learnRequested++ }
                )
            }
        }
        composeRule.onNodeWithTag("detail_tab_impact").performClick()
        composeRule.onNodeWithTag("detail_science_status").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_science_stale").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_science_product_shakemap")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_science_product_dyfi")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_science_learn").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, learnRequested) }
    }

    @Test
    fun scientificErrorsProvideRetryWithoutInventingProducts() {
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(sampleEarthquake()),
                    onBack = {}, onOpenSource = {},
                    scientificState = ScientificDetailUiState.Error(
                        ScientificDetailFailure.EVENT_NOT_FOUND
                    )
                )
            }
        }
        composeRule.onNodeWithTag("detail_tab_impact").performClick()
        composeRule.onNodeWithTag("detail_science_error").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_science_product_shakemap").assertDoesNotExist()
    }

    @Test
    fun imageButtonOnlyAppearsWhenVerifiedJpegIsPublished() {
        var requests = 0
        val image = ShakeMapResource(
            ShakeMapResourceKind.INTENSITY_MAP,
            "https://earthquake.usgs.gov/realtime/product/shakemap/us123/us/1700000000/download/intensity.jpg",
            "image/jpeg", 1000L
        )
        val scientific = ScientificDetail(
            "us123", 1700000000000L, 1700000000100L,
            listOf("shakemap"), 1, ShakeMapProduct(
                "us", "us123", 1700000000000L,
                "RELEASED", listOf(image)
            )
        )
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(sampleEarthquake()),
                    onBack = {}, onOpenSource = {},
                    scientificState = ScientificDetailUiState.Available(
                        scientific,
                        ScientificDetailSource.NETWORK, false
                    ),
                    onRequestShakeMapImage = { requests++ })
            }
        }
        composeRule.onNodeWithTag("detail_tab_impact").performClick()
        composeRule.onNodeWithTag("detail_shakemap_request").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, requests) }
        composeRule.onNodeWithTag("detail_shakemap_image").assertDoesNotExist()
    }

    @Test
    fun lossPagerProductUsesTranslatedLabel() {
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(sampleEarthquake()),
                    onBack = {}, onOpenSource = {},
                    scientificState = ScientificDetailUiState.Available(
                        ScientificDetail(
                            "us123", 1700000000000L, 1700000000100L,
                            listOf("losspager"), 1
                        ), ScientificDetailSource.CACHE, false
                    )
                )
            }
        }
        composeRule.onNodeWithTag("detail_tab_impact").performClick()
        composeRule.onNodeWithText("PAGER · estimaciones de impacto · losspager")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun essentialDetailDisplaysTwoExplicitTimeZonesAndNavigation() {
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(sampleEarthquake()),
                    onBack = {}, onOpenSource = {}
                )
            }
        }
        composeRule.onNodeWithTag("detail_time_utc")
            .assertTextContains("UTC", substring = true)
        composeRule.onNodeWithTag("detail_essential_metrics").assertExists()
        composeRule.onNodeWithTag("detail_jump_intensity").assertExists()
        composeRule.onNodeWithTag("detail_tab_data").performClick()
        composeRule.onNodeWithText("Hora local del dispositivo")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("detail_tab_science").performClick()
        composeRule.onNodeWithTag("detail_essential_region_section").assertExists()
        composeRule.onNodeWithTag("detail_tab_summary").performClick()
    }

    @Test
    fun scaleHelpOpensExplanationWithoutDepthIcon() {
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(
                        sampleEarthquake().copy(magType = "mww")
                    ),
                    onBack = {},
                    onOpenSource = {}
                )
            }
        }
        composeRule.onNodeWithTag("detail_scale_info").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail_scale_sheet", useUnmergedTree = true).assertExists()
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
            .onNodeWithTag("detail_back_button")
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

        composeRule.onNodeWithTag("detail_tab_data").performClick()
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

        composeRule.onNodeWithTag("detail_tab_data").performClick()
        composeRule
            .onNodeWithText("Ver evento en USGS")
            .assertDoesNotExist()
    }

    @Test
    fun futureToolsAreExplicitAndNeverRequestScientificData() {
        var requests = 0
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeDetailScreen(
                    state = EarthquakeDetailUiState.Content(sampleEarthquake()),
                    onBack = {}, onOpenSource = {},
                    onRequestScientificDetail = { requests++ },
                    onRequestShakeMapImage = { requests++ }
                )
            }
        }
        composeRule.onNodeWithTag("detail_tab_impact").performClick()
        composeRule.onNodeWithTag("detail_future_open").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail_future_sheet", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Cómo se sintió · DYFI").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, requests) }
        composeRule.onNodeWithTag("detail_future_close").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail_future_sheet", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun longPlaceIsPreservedInEssentialHero() {
        val place = "Región de la isla Georgia del Sur y una descripción geográfica extensa"
        composeRule.setContent {
            SeismicExplorerTheme {
                EarthquakeEssentialHero(sampleEarthquake(), place, "M 5,3", {})
            }
        }
        composeRule.onNodeWithTag("detail_place").assertTextContains(place).assertIsDisplayed()
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