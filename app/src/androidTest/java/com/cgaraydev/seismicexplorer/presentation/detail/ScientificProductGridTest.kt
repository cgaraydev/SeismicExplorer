package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScientificProductGridTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun shakeMapNavigatesAndPlannedViewerExplainsWithoutNavigating() {
        var opened = 0
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                ScientificProductGrid("Consultar productos", { opened++ })
            }
        }
        composeRule.onNodeWithTag("detail_jump_intensity").performClick()
        composeRule.runOnIdle { assertEquals(1, opened) }
        composeRule.onNodeWithTag("detail_product_dyfi").performClick()
        composeRule.onNodeWithTag("detail_product_planned_dialog").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun narrowLayoutStacksCardsInReadingOrder() {
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = false) {
                Box(Modifier.width(280.dp)) {
                    ScientificProductGrid("Consultar productos", {})
                }
            }
        }
        val shake = composeRule.onNodeWithTag("detail_jump_intensity").fetchSemanticsNode().boundsInRoot
        val dyfi = composeRule.onNodeWithTag("detail_product_dyfi").fetchSemanticsNode().boundsInRoot
        assertTrue(dyfi.top >= shake.bottom)
        assertEquals(shake.left, dyfi.left, 1f)
    }

    @Test fun normalWidthUsesTwoColumns() {
        composeRule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                val config = android.content.res.Configuration(LocalConfiguration.current).apply { fontScale = 1f }
                CompositionLocalProvider(LocalConfiguration provides config) {
                    Box(Modifier.width(320.dp)) {
                        ScientificProductGrid("Consultar productos", {})
                    }
                }
            }
        }
        val shake = composeRule.onNodeWithTag("detail_jump_intensity").fetchSemanticsNode().boundsInRoot
        val dyfi = composeRule.onNodeWithTag("detail_product_dyfi").fetchSemanticsNode().boundsInRoot
        assertTrue(dyfi.left >= shake.right)
        assertEquals(shake.top, dyfi.top, 1f)
        assertEquals(shake.bottom, dyfi.bottom, 1f)
    }
}
