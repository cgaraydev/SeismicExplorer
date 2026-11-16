package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.Style

class SeismicMapStyleLoadFailureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun invalidBundledStyle_displaysErrorAndAllowsRetry() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        composeRule.runOnUiThread {
            MapLibre.getInstance(context)
        }

        // No depende de la red: este asset deliberadamente no existe.
        val invalidStyle = Style.Builder().fromUri(
            "asset://seismic_explorer_missing_style_test.json"
        )

        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapView(
                    geoJson = MapEarthquakeGeoJson.EMPTY,
                    styleBuilder = invalidStyle
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithText("Reintentar mapa")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText("Reintentar mapa")
            .assertIsDisplayed()
            .performClick()

        // El mismo estilo inválido volverá a fallar, pero la
        // pantalla debe conservar su acción de recuperación.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithText("Reintentar mapa")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
