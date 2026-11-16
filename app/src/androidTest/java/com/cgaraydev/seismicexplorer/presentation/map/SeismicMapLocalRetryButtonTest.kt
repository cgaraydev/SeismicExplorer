package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SeismicMapLocalRetryButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun localReadError_exposesIndependentRetry() {
        var localAttempts = 0
        var networkAttempts = 0

        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    state = SeismicMapUiState(
                        isLoading = false,
                        hasError = true
                    ),
                    onLocalRetry = { localAttempts++ },
                    onRefreshClick = { networkAttempts++ },
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Reintentar lectura local")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, localAttempts)
            assertEquals(0, networkAttempts)
        }
    }

    @Test
    fun successfulLocalRead_hidesLocalRetry() {
        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    state = SeismicMapUiState(
                        isLoading = false,
                        hasError = false
                    ),
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Reintentar lectura local")
            .assertDoesNotExist()
    }
}
