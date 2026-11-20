package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EarthquakeEventTabsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun allTabsSelectExpectedSection() {
        var selected by mutableStateOf(EventDetailTab.RESUMEN)
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                EarthquakeEventTabs(selected = selected, onSelect = { selected = it })
            }
        }
        composeRule.onNodeWithTag("detail_tab_summary").assertIsDisplayed()
        composeRule.onNodeWithTag("detail_tab_impact").performClick()
        composeRule.runOnIdle { assertEquals(EventDetailTab.IMPACTO, selected) }
        composeRule.onNodeWithTag("detail_tab_science").performClick()
        composeRule.runOnIdle { assertEquals(EventDetailTab.CIENCIA, selected) }
        composeRule.onNodeWithTag("detail_tab_data").performClick()
        composeRule.runOnIdle { assertEquals(EventDetailTab.DATOS, selected) }
        composeRule.onNodeWithTag("detail_tab_summary").performClick()
        composeRule.runOnIdle { assertEquals(EventDetailTab.RESUMEN, selected) }
    }
}
