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
    fun atlasOpensPublishedArticlesAndDisablesUnpublishedTopics() {
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
        rule.onNodeWithTag("learn_card_depth").performClick()
        rule.runOnIdle { assertEquals(LearnCatalog.EPICENTER_DEPTH, opened) }
        rule.onNodeWithTag("learn_home")
            .performScrollToNode(hasTestTag("learn_card_waves"))
        rule.onNodeWithTag("learn_card_waves").assertIsNotEnabled()
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
    fun featuredCardOpensLaboratoryWithoutOpeningAnArticle() {
        var openedArticle: String? = null
        var openedLab = false
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                LearnHomeScreen(
                    onOpenArticle = { openedArticle = it },
                    onOpenLaboratory = { openedLab = true }
                )
            }
        }
        rule.onNodeWithTag("learn_home")
            .performScrollToNode(hasTestTag("learn_featured"))
        rule.onNodeWithTag("learn_featured").performClick()
        rule.runOnIdle {
            assertEquals(true, openedLab)
            assertEquals(null, openedArticle)
        }
    }

    @Test
    fun atlasFaqOpensRevisionArticle() {
        var opened: String? = null
        rule.setContent {
            SeismicExplorerTheme(darkTheme = true) {
                LearnHomeScreen(onOpenArticle = { opened = it })
            }
        }
        rule.onNodeWithTag("learn_home")
            .performScrollToNode(hasTestTag("learn_faq_revisions"))
        rule.onNodeWithTag("learn_faq_revisions").performClick()
        rule.runOnIdle { assertEquals(LearnCatalog.REVISIONS, opened) }
    }

    @Test
    fun revisionArticleHasAccessibleDiagramAndSourcesInDarkMode() {
        articleTheme(dark = true, articleId = LearnCatalog.REVISIONS)
        rule.onNodeWithText("La primera estimación").assertExists()
        rule.onNodeWithTag("learn_article")
            .performScrollToNode(hasTestTag("learn_revision_diagram"))
        rule.onNodeWithTag("learn_revision_diagram").assertExists()
        rule.onNodeWithTag("learn_article")
            .performScrollToNode(hasTestTag("learn_sources"))
        rule.onNodeWithTag("learn_sources").assertExists()
    }

    @Test
    fun revisionArticleRendersInLightMode() {
        articleTheme(dark = false, articleId = LearnCatalog.REVISIONS)
        rule.onNodeWithText("La primera estimación").assertExists()
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
    fun depthArticleRendersDifferentContentAndDiagramInDarkMode() {
        articleTheme(dark = true, articleId = LearnCatalog.EPICENTER_DEPTH)
        rule.onNodeWithText("Hipocentro: origen de la ruptura").assertExists()
        rule.onNodeWithTag("learn_article")
            .performScrollToNode(hasTestTag("learn_depth_diagram"))
        rule.onNodeWithTag("learn_depth_diagram").assertExists()
        rule.onNodeWithTag("learn_article")
            .performScrollToNode(hasTestTag("learn_sources"))
        rule.onNodeWithTag("learn_sources").assertExists()
    }

    @Test
    fun depthArticleIsReadableInLightMode() {
        articleTheme(dark = false, articleId = LearnCatalog.EPICENTER_DEPTH)
        rule.onNodeWithText("Hipocentro: origen de la ruptura").assertExists()
    }

    @Test
    fun articleShowsScientificContentInLightMode() {
        articleTheme(dark = false)
        rule.onNodeWithText("Magnitud no es intensidad").assertExists()
    }

    private fun articleTheme(
        dark: Boolean,
        articleId: String = LearnCatalog.MAGNITUDE_INTENSITY
    ) {
        rule.setContent {
            SeismicExplorerTheme(darkTheme = dark) {
                LearnArticleScreen(
                    articleId = articleId,
                    onBack = {},
                    onOpenSource = {}
                )
            }
        }
    }
}
