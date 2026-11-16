package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SeismicMapStyleFailureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun styleFailure_displaysIndependentRetryAction() {
        var retries = 0

        composeRule.setContent {
            SeismicExplorerTheme {
                MapStyleFailureMessage(
                    onRetry = { retries++ }
                )
            }
        }

        composeRule
            .onNodeWithText(
                "No se pudo cargar el mapa base. Puedes reintentarlo sin cambiar los filtros."
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Reintentar mapa")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }
}
