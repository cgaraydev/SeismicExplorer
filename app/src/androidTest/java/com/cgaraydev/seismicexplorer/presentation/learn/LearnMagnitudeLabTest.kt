package com.cgaraydev.seismicexplorer.presentation.learn

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LearnMagnitudeLabTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun defaultValuesUseApproximateThirtyTwoTimesEnergy() {
        var opened: String? = null
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                LearnMagnitudeLabScreen(
                    onBack = {},
                    onOpenArticle = { opened = it },
                    onOpenSource = {}
                )
            }
        }
        rule.onNodeWithTag("learn_lab")
            .performScrollToNode(hasTestTag("learn_lab_energy_ratio"))
        rule.onNodeWithTag("learn_lab_energy_ratio")
            .assertTextContains("32", substring = true)
        rule.onNodeWithTag("learn_lab")
            .performScrollToNode(hasTestTag("learn_lab_open_magnitude"))
        rule.onNodeWithTag("learn_lab_open_magnitude").performClick()
        rule.runOnIdle { assertEquals(LearnCatalog.MAGNITUDE_INTENSITY, opened) }
    }

    @Test
    fun formulaIsAccessibleAndUsesOfficialSourceOnDemand() {
        var url: String? = null
        rule.setContent {
            SeismicExplorerTheme(darkTheme = false) {
                LearnMagnitudeLabScreen(
                    onBack = {},
                    onOpenArticle = {},
                    onOpenSource = { url = it }
                )
            }
        }
        rule.onNodeWithTag("learn_lab")
            .performScrollToNode(hasTestTag("learn_lab_formula_button"))
        rule.onNodeWithTag("learn_lab_formula_button").performClick()
        rule.onNodeWithTag("learn_lab_usgs").performClick()
        rule.runOnIdle {
            assertEquals("https://earthquake.usgs.gov/education/how_much_bigger.php", url)
        }
    }
}
