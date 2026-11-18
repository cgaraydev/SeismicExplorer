package com.cgaraydev.seismicexplorer.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import com.cgaraydev.seismicexplorer.presentation.detail.EarthquakeDetailRoute
import com.cgaraydev.seismicexplorer.presentation.detail.EarthquakeDetailViewModel
import com.cgaraydev.seismicexplorer.presentation.home.HomeAction
import com.cgaraydev.seismicexplorer.presentation.home.HomeViewModel
import com.cgaraydev.seismicexplorer.presentation.home.MagnitudeThreshold
import com.cgaraydev.seismicexplorer.presentation.home.ObservatoryDashboard
import com.cgaraydev.seismicexplorer.presentation.home.SeismicCatalogScreen
import com.cgaraydev.seismicexplorer.presentation.home.TimeWindow
import com.cgaraydev.seismicexplorer.presentation.map.MapFilterSelection
import com.cgaraydev.seismicexplorer.presentation.map.MapMagnitudeFilter
import com.cgaraydev.seismicexplorer.presentation.map.SeismicMapRoute

private enum class ObservatoryTab(val labelRes: Int, val drawableRes: Int, val testTag: String) {
    SUMMARY(R.string.observatory_tab_summary, R.drawable.ic_observatory_summary, "tab_summary"),
    MAP(R.string.observatory_tab_map, R.drawable.ic_atlas_map, "tab_map"),
    CATALOG(R.string.observatory_tab_catalog, R.drawable.ic_atlas_events, "tab_catalog")
}

@Composable
fun SeismicNavHost() {
    val backStack = rememberNavBackStack(HomeDestination)
    val navigateBack = {
        if (backStack.size > 1) backStack.removeLastOrNull()
        Unit
    }

    NavDisplay(
        backStack = backStack,
        onBack = navigateBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<HomeDestination> {
                ObservatoryTabs(
                    onEarthquakeClick = { earthquakeId ->
                        if (backStack.lastOrNull() == HomeDestination) {
                            backStack.add(EarthquakeDetailDestination(earthquakeId))
                        }
                    }
                )
            }
            // Compatibilidad con cualquier navegación antigua a MapDestination.
            entry<MapDestination> {
                SeismicMapRoute(
                    onBack = navigateBack,
                    onEarthquakeClick = { earthquakeId ->
                        if (backStack.lastOrNull() == MapDestination) {
                            backStack.add(EarthquakeDetailDestination(earthquakeId))
                        }
                    }
                )
            }
            entry<EarthquakeDetailDestination> { destination ->
                val viewModel = hiltViewModel<
                    EarthquakeDetailViewModel,
                    EarthquakeDetailViewModel.Factory
                >(
                    creationCallback = { factory -> factory.create(destination) }
                )
                EarthquakeDetailRoute(viewModel = viewModel, onBack = navigateBack)
            }
        }
    )
}

/** Un ViewModel y una selección compartida para Resumen y Catálogo. */
@Composable
private fun ObservatoryTabs(
    onEarthquakeClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(ObservatoryTab.SUMMARY) }
    val tabStates = rememberSaveableStateHolder()

    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        onPauseOrDispose { }
    }
    BackHandler(enabled = currentTab != ObservatoryTab.SUMMARY) {
        currentTab = ObservatoryTab.SUMMARY
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                ObservatoryTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        modifier = Modifier.testTag(tab.testTag),
                        icon = {
                            Icon(painterResource(tab.drawableRes), contentDescription = null)
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when (currentTab) {
                ObservatoryTab.SUMMARY -> tabStates.SaveableStateProvider("summary") {
                    ObservatoryDashboard(
                        state = state,
                        onAction = viewModel::onAction,
                        onOpenCatalog = { currentTab = ObservatoryTab.CATALOG },
                        onOpenEarthquake = onEarthquakeClick
                    )
                }
                ObservatoryTab.CATALOG -> tabStates.SaveableStateProvider("catalog") {
                    SeismicCatalogScreen(
                        state = state,
                        onAction = viewModel::onAction,
                        onEarthquakeClick = onEarthquakeClick
                    )
                }
                ObservatoryTab.MAP -> tabStates.SaveableStateProvider("map") {
                    val sharedFilters = MapFilterSelection(
                        timeWindow = when (state.timeWindow) {
                            TimeWindow.LAST_DAY -> RecentWindow.LAST_DAY
                            TimeWindow.LAST_WEEK -> RecentWindow.LAST_WEEK
                            TimeWindow.LAST_MONTH -> RecentWindow.LAST_30_DAYS
                        },
                        magnitude = when (state.minimumMagnitude) {
                            MagnitudeThreshold.ALL -> MapMagnitudeFilter.ALL
                            MagnitudeThreshold.MAGNITUDE_2_5 -> MapMagnitudeFilter.MAGNITUDE_2_5
                            MagnitudeThreshold.MAGNITUDE_4_5 -> MapMagnitudeFilter.MAGNITUDE_4_5
                        }
                    )
                    SeismicMapRoute(
                        onBack = { currentTab = ObservatoryTab.SUMMARY },
                        onEarthquakeClick = onEarthquakeClick,
                        sharedFilters = sharedFilters,
                        inTabNavigation = true,
                        onSharedTimeWindowChanged = { window ->
                            val homeWindow = when (window) {
                                RecentWindow.LAST_DAY -> TimeWindow.LAST_DAY
                                RecentWindow.LAST_WEEK -> TimeWindow.LAST_WEEK
                                RecentWindow.LAST_30_DAYS -> TimeWindow.LAST_MONTH
                            }
                            viewModel.onAction(HomeAction.SelectTimeWindow(homeWindow))
                        },
                        onSharedMagnitudeChanged = { magnitude ->
                            val selected = when (magnitude) {
                                MapMagnitudeFilter.ALL -> MagnitudeThreshold.ALL
                                MapMagnitudeFilter.MAGNITUDE_2_5 -> MagnitudeThreshold.MAGNITUDE_2_5
                                MapMagnitudeFilter.MAGNITUDE_4_5 -> MagnitudeThreshold.MAGNITUDE_4_5
                            }
                            viewModel.onAction(HomeAction.SelectMagnitude(selected))
                        }
                    )
                }
            }
        }
    }
}
