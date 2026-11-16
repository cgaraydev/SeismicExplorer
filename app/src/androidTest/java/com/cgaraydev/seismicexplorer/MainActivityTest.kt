package com.cgaraydev.seismicexplorer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
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
        composeRule
            .onNodeWithText("Actividad sísmica mundial")
            .assertIsDisplayed()

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
        composeRule
            .onNodeWithText("7 días")
            .performClick()

        composeRule
            .onNodeWithText("7 días")
            .assertIsSelected()

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