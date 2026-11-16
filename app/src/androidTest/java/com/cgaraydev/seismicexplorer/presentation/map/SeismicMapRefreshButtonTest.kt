package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SeismicMapRefreshButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyMap_allowsManualRefresh() {
        var refreshRequests = 0

        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    state = SeismicMapUiState(
                        isLoading = false
                    ),
                    syncState = MapSyncState.Ready(
                        window = RecentWindow.LAST_WEEK,
                        downloaded = false
                    ),
                    onRefreshClick = {
                        refreshRequests++
                    },
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Actualizar")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, refreshRequests)
        }
    }

    @Test
    fun syncingMap_hidesManualRefreshAction() {
        composeRule.setContent {
            SeismicExplorerTheme {
                SeismicMapScreen(
                    onBack = {},
                    state = SeismicMapUiState(
                        isLoading = false
                    ),
                    syncState = MapSyncState.Syncing(
                        window = RecentWindow.LAST_WEEK
                    ),
                    mapContent = { _, _, _, _, _ ->
                        Text("Mapa de prueba")
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Actualizar")
            .assertDoesNotExist()
    }
}