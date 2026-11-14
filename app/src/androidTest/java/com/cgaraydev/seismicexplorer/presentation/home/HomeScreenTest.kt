package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_displaysInformativeMessage() {
        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onAction = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Todavía no hay eventos cargados")
            .assertIsDisplayed()
    }

    @Test
    fun selectingPeriod_emitsExpectedAction() {
        var emittedAction: HomeAction? = null

        composeRule.setContent {
            SeismicExplorerTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onAction = { emittedAction = it }
                )
            }
        }

        composeRule
            .onNodeWithText("7 días")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                HomeAction.SelectTimeWindow(TimeWindow.LAST_WEEK),
                emittedAction
            )
        }
    }
}