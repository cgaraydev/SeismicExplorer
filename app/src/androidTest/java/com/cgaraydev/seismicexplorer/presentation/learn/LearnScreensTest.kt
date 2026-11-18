package com.cgaraydev.seismicexplorer.presentation.learn

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LearnScreensTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun atlasOpensOnlyPublishedArticle() {
        var opened: String? = null
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                LearnHomeScreen(onOpenArticle = { opened = it })
            }
        }
        rule.onNodeWithTag("learn_home").assertExists()
        rule.onNodeWithTag("learn_card_magnitude").performClick()
        rule.runOnIdle { assertEquals(LearnCatalog.MAGNITUDE_INTENSITY, opened) }
        rule.onNodeWithTag("learn_home")
            .performScrollToNode(hasTestTag("learn_card_depth"))
        rule.onNodeWithTag("learn_card_depth").assertIsNotEnabled()
    }

    @Test
    fun atlasSearchFiltersConceptsWithoutFakeResults() {
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) { LearnHomeScreen(onOpenArticle = {}) }
        }
        rule.onNodeWithTag("learn_search").performTextInput("texto inexistente")
        rule.onNodeWithTag("learn_no_results").assertExists()
        rule.onNodeWithTag("learn_card_magnitude").assertDoesNotExist()
    }

    @Test
    fun articleShowsScientificContentOfflineInDarkMode() {
        articleTheme(dark = true)
        rule.onNodeWithTag("learn_article").assertExists()
        rule.onNodeWithText("Magnitud no es intensidad").assertExists()
        rule.onNodeWithTag("learn_article")
            .performScrollToNode(hasTestTag("learn_diagram"))
        rule.onNodeWithTag("learn_diagram").assertExists()
        rule.onNodeWithTag("learn_article")
            .performScrollToNode(hasTestTag("learn_sources"))
        rule.onNodeWithTag("learn_sources").assertExists()
    }

    @Test
    fun articleShowsScientificContentInLightMode() {
        articleTheme(dark = false)
        rule.onNodeWithText("Magnitud no es intensidad").assertExists()
    }

    private fun articleTheme(dark: Boolean) {
        rule.setContent {
            SeismicExplorerTheme(darkTheme = dark) {
                LearnArticleScreen(
                    articleId = LearnCatalog.MAGNITUDE_INTENSITY,
                    onBack = {},
                    onOpenSource = {}
                )
            }
        }
    }
}
