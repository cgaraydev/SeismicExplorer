package com.cgaraydev.seismicexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.cgaraydev.seismicexplorer.di.RepositoryModule
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeQuery
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.FeedSyncResult
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.domain.model.RefreshResult
import com.cgaraydev.seismicexplorer.domain.repository.EarthquakeRepository
import com.cgaraydev.seismicexplorer.domain.repository.ScientificDetailRepository
import com.cgaraydev.seismicexplorer.domain.repository.ShakeMapImageRepository
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageResult
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResource
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailFailure
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailResult
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
class MainActivityTest {

    private val fakeRepository = FakeEarthquakeRepository()

    @BindValue
    @JvmField
    val repository: EarthquakeRepository = fakeRepository

    // RepositoryModule se desinstala en este test Hilt: hay que ligar también
    // el repositorio científico requerido por el ViewModel del detalle.
    @BindValue
    @JvmField
    val scientificRepository: ScientificDetailRepository = object : ScientificDetailRepository {
        override suspend fun getDetail(
            eventId: String,
            knownUpdatedAtEpochMillis: Long?,
            forceRefresh: Boolean
        ): ScientificDetailResult =
            ScientificDetailResult.Failed(ScientificDetailFailure.EVENT_NOT_FOUND)
    }

    @BindValue
    @JvmField
    val imageRepository: ShakeMapImageRepository = object : ShakeMapImageRepository {
        override suspend fun load(resource: ShakeMapResource): ShakeMapImageResult =
            ShakeMapImageResult.Failed(ShakeMapImageFailure.NOT_FOUND)
    }

    @BindValue
    @JvmField
    val clock: Clock = Clock.fixed(
        Instant.parse("2026-11-14T12:00:00Z"),
        ZoneOffset.UTC
    )

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule =
        createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_displaysHomeScreen() {
        composeRule.onNodeWithText("Observatorio").assertIsDisplayed()
        composeRule.onNodeWithTag("observatory_window").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_summary").assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            fakeRepository.syncRequests.contains(
                RecentWindow.LAST_DAY
            )
        }

        assertTrue(
            fakeRepository.syncRequests.contains(
                RecentWindow.LAST_DAY
            )
        )
    }

    @Test
    fun selectingPeriod_updatesStateThroughViewModel() {
        composeRule.onNodeWithTag("observatory_window").performClick()
        composeRule.onNodeWithText("7 días").performClick()
        composeRule.onNodeWithTag("observatory_window")
            .assertTextContains("7 días", substring = true)


        composeRule.waitUntil(timeoutMillis = 5_000) {
            fakeRepository.syncRequests.contains(
                RecentWindow.LAST_WEEK
            )
        }

        assertTrue(
            fakeRepository.syncRequests.contains(
                RecentWindow.LAST_WEEK
            )
        )
    }

    @Test
    fun bottomNavigation_opensCatalog() {
        composeRule.onNodeWithTag("tab_catalog").performClick()
        composeRule.onNodeWithTag("catalog_list").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_summary").performClick()
        composeRule.onNodeWithTag("observatory_dashboard").assertIsDisplayed()
    }

    @Test
    fun summaryAndCatalogKeepIndependentPeriodsAndMagnitude() {
        // El período elegido en Resumen no cambia el filtro del catálogo.
        composeRule.onNodeWithTag("observatory_window").performClick()
        composeRule.onNodeWithText("7 días").performClick()
        composeRule.onNodeWithTag("tab_catalog").performClick()
        composeRule.onNodeWithTag("catalog_filter_summary")
            .assertTextContains("24 horas", substring = true)

        // El catálogo modifica solamente su propia selección.
        composeRule.onNodeWithTag("catalog_filter_toggle").performClick()
        composeRule.onNodeWithTag("catalog_list")
            .performScrollToNode(hasTestTag("catalog_period_LAST_MONTH"))
        composeRule.onNodeWithTag("catalog_period_LAST_MONTH").performClick()
        composeRule.onNodeWithTag("catalog_list")
            .performScrollToNode(hasTestTag("catalog_magnitude_MAGNITUDE_4_5"))
        composeRule.onNodeWithTag("catalog_magnitude_MAGNITUDE_4_5").performClick()
        composeRule.onNodeWithTag("catalog_filter_summary")
            .assertTextContains("30 días", substring = true)
        composeRule.onNodeWithTag("catalog_filter_summary")
            .assertTextContains("M 4.5+", substring = true)

        composeRule.onNodeWithTag("tab_summary").performClick()
        composeRule.onNodeWithTag("observatory_window")
            .assertTextContains("7 días", substring = true)
        composeRule.onNodeWithTag("tab_catalog").performClick()
        composeRule.onNodeWithTag("catalog_filter_summary")
            .assertTextContains("30 días", substring = true)
        composeRule.onNodeWithTag("catalog_filter_summary")
            .assertTextContains("M 4.5+", substring = true)
    }

    @Test
    fun catalogFilterChangesDoNotAffectSummaryPeriod() {
        composeRule.onNodeWithTag("tab_catalog").performClick()
        composeRule.onNodeWithTag("catalog_filter_toggle").performClick()
        composeRule.onNodeWithTag("catalog_period_LAST_WEEK").performClick()
        composeRule.onNodeWithTag("tab_summary").performClick()
        composeRule.onNodeWithTag("observatory_window")
            .assertTextContains("24 h", substring = true)
    }

    @Test
    fun learningTabOpensArticleAndKeepsSummaryFilters() {
        composeRule.onNodeWithTag("tab_learn").performClick()
        composeRule.onNodeWithTag("learn_home").assertIsDisplayed()
        composeRule.onNodeWithTag("learn_card_magnitude").performClick()
        composeRule.onNodeWithTag("learn_article").assertIsDisplayed()
        composeRule.onNodeWithTag("learn_back").performClick()
        composeRule.onNodeWithTag("learn_home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_summary").performClick()
        composeRule.onNodeWithTag("observatory_window")
            .assertTextContains("24 h", substring = true)
    }

    @Test
    fun learningDepthArticleOpensAndReturnsWithoutChangingSummary() {
        composeRule.onNodeWithTag("tab_learn").performClick()
        composeRule.onNodeWithTag("learn_home")
            .performScrollToNode(hasTestTag("learn_card_depth"))
        composeRule.onNodeWithTag("learn_card_depth").performClick()
        composeRule.onNodeWithTag("learn_article").assertIsDisplayed()
        composeRule.onNodeWithText("¿Dónde comienza un terremoto?").assertIsDisplayed()
        composeRule.onNodeWithTag("learn_back").performClick()
        composeRule.onNodeWithTag("learn_home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_summary").performClick()
        composeRule.onNodeWithTag("observatory_window")
            .assertTextContains("24 h", substring = true)
    }

    @Test
    fun learningLaboratoryOpensRelatedArticleAndReturnsToLaboratory() {
        composeRule.onNodeWithTag("tab_learn").performClick()
        composeRule.onNodeWithTag("learn_home")
            .performScrollToNode(hasTestTag("learn_featured"))
        composeRule.onNodeWithTag("learn_featured").performClick()
        composeRule.onNodeWithTag("learn_lab").assertIsDisplayed()
        composeRule.onNodeWithTag("learn_lab")
            .performScrollToNode(hasTestTag("learn_lab_open_magnitude"))
        composeRule.onNodeWithTag("learn_lab_open_magnitude").performClick()
        composeRule.onNodeWithTag("learn_article").assertIsDisplayed()
        composeRule.onNodeWithTag("learn_back").performClick()
        composeRule.onNodeWithTag("learn_lab").assertIsDisplayed()
        composeRule.onNodeWithTag("learn_lab_back").performClick()
        composeRule.onNodeWithTag("learn_home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_summary").performClick()
        composeRule.onNodeWithTag("observatory_window")
            .assertTextContains("24 h", substring = true)
    }

    @Test
    fun learningRevisionFaqOpensArticleAndReturnsToAtlas() {
        composeRule.onNodeWithTag("tab_learn").performClick()
        composeRule.onNodeWithTag("learn_home")
            .performScrollToNode(hasTestTag("learn_faq_revisions"))
        composeRule.onNodeWithTag("learn_faq_revisions").performClick()
        composeRule.onNodeWithTag("learn_article").assertIsDisplayed()
        composeRule.onNodeWithText("¿Por qué cambian los datos de un terremoto?").assertIsDisplayed()
        composeRule.onNodeWithTag("learn_back").performClick()
        composeRule.onNodeWithTag("learn_home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_summary").performClick()
        composeRule.onNodeWithTag("observatory_window")
            .assertTextContains("24 h", substring = true)
    }

    private class FakeEarthquakeRepository :
        EarthquakeRepository {

        private val events =
            MutableStateFlow<List<Earthquake>>(
                emptyList()
            )

        val refreshRequests =
            ConcurrentLinkedQueue<RecentWindow>()

        val syncRequests =
            ConcurrentLinkedQueue<RecentWindow>()

        override suspend fun getFeedCacheStatus(
            window: RecentWindow
        ): FeedCacheStatus {
            return FeedCacheStatus.Unknown
        }

        override fun observeEarthquakes(
            query: EarthquakeQuery
        ): Flow<List<Earthquake>> {
            return events
        }

        override fun observeEarthquake(
            id: String
        ): Flow<Earthquake?> {
            return flowOf(null)
        }

        override suspend fun refreshRecent(
            window: RecentWindow
        ): RefreshResult {
            refreshRequests.add(window)
            return RefreshResult.Success
        }

        override suspend fun refreshIfStale(
            window: RecentWindow
        ): FeedSyncResult {
            syncRequests.add(window)
            return FeedSyncResult.UpToDate
        }
    }
}
