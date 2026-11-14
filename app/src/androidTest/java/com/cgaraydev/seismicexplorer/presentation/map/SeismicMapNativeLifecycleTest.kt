
package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.Style

class SeismicMapNativeLifecycleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nativeMap_canBeCreatedAndReleasedRepeatedly() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        composeRule.runOnUiThread {
            MapLibre.getInstance(context)
        }

        composeRule.setContent {
            var showMap by remember {
                mutableStateOf(true)
            }

            val testStyle = remember {
                Style.Builder().fromJson(
                    """
                    {
                      "version": 8,
                      "name": "Local test style",
                      "sources": {},
                      "layers": []
                    }
                    """.trimIndent()
                )
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Button(
                    onClick = {
                        showMap = !showMap
                    }
                ) {
                    Text(
                        if (showMap) {
                            "Ocultar mapa"
                        } else {
                            "Mostrar mapa"
                        }
                    )
                }

                if (showMap) {
                    SeismicMapView(
                        geoJson = MapEarthquakeGeoJson.EMPTY,
                        styleBuilder = testStyle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        repeat(3) {
            composeRule
                .onNodeWithText("Ocultar mapa")
                .assertIsDisplayed()
                .performClick()

            composeRule
                .onNodeWithText("Mostrar mapa")
                .assertIsDisplayed()
                .performClick()
        }
    }
}
