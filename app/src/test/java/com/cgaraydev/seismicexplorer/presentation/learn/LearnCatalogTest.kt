package com.cgaraydev.seismicexplorer.presentation.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnCatalogTest {
    @Test
    fun publishedTopicsResolveToLocalArticlesWithDifferentSections() {
        val magnitude = LearnCatalog.articleOrNull(LearnCatalog.MAGNITUDE_INTENSITY)
        val depth = LearnCatalog.articleOrNull(LearnCatalog.EPICENTER_DEPTH)
        assertNotNull(magnitude)
        assertNotNull(depth)
        assertEquals(4, magnitude!!.sections.size)
        assertEquals(4, depth!!.sections.size)
        assertNotEquals(magnitude.diagram, depth.diagram)
        assertNotEquals(magnitude.titleRes, depth.titleRes)
        assertEquals(2, LearnCatalog.topics.count { it.articleId == LearnCatalog.MAGNITUDE_INTENSITY })
        assertEquals(1, LearnCatalog.topics.count { it.articleId == LearnCatalog.EPICENTER_DEPTH })
    }

    @Test
    fun unpublishedAndUnknownArticlesCannotBeOpened() {
        assertNull(LearnCatalog.articleOrNull("ondas"))
        assertFalse(LearnCatalog.contains("ondas"))
        assertTrue(LearnCatalog.topics.first { it.id == "waves" }.articleId == null)
    }

    @Test
    fun sourcesUseOfficialHttpsPagesOnly() {
        val catalog = listOfNotNull(
            LearnCatalog.articleOrNull(LearnCatalog.MAGNITUDE_INTENSITY),
            LearnCatalog.articleOrNull(LearnCatalog.EPICENTER_DEPTH)
        )
        assertTrue(catalog.all { article ->
            article.sources.isNotEmpty() && article.sources.all { source ->
                source.url.startsWith("https://") &&
                    (source.url.contains("usgs.gov/") || source.url.contains("sismologia.cl/"))
            }
        })
    }
}
