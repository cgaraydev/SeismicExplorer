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
import com.cgaraydev.seismicexplorer.presentation.detail.EarthquakeDetailRoute
import com.cgaraydev.seismicexplorer.presentation.detail.EarthquakeDetailViewModel
import com.cgaraydev.seismicexplorer.presentation.home.HomeViewModel
import com.cgaraydev.seismicexplorer.presentation.home.ObservatoryDashboard
import com.cgaraydev.seismicexplorer.presentation.home.SeismicCatalogScreen
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

/** Cada pestaña gestiona sus filtros; todas usan el mismo repositorio y caché Room. */
@Composable
private fun ObservatoryTabs(
    onEarthquakeClick: (String) -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(ObservatoryTab.SUMMARY) }
    val tabStates = rememberSaveableStateHolder()

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
                    // Clave estable: la selección del Resumen no se comparte con Eventos.
                    val summaryViewModel: HomeViewModel = hiltViewModel(key = "observatory_summary")
                    val summaryState by summaryViewModel.uiState.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(summaryViewModel) {
                        summaryViewModel.onResumed()
                        onPauseOrDispose { }
                    }
                    ObservatoryDashboard(
                        state = summaryState,
                        onAction = summaryViewModel::onAction,
                        onOpenCatalog = { currentTab = ObservatoryTab.CATALOG },
                        onOpenEarthquake = onEarthquakeClick
                    )
                }
                ObservatoryTab.CATALOG -> tabStates.SaveableStateProvider("catalog") {
                    val catalogViewModel: HomeViewModel = hiltViewModel(key = "observatory_catalog")
                    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(catalogViewModel) {
                        catalogViewModel.onResumed()
                        onPauseOrDispose { }
                    }
                    SeismicCatalogScreen(
                        state = catalogState,
                        onAction = catalogViewModel::onAction,
                        onEarthquakeClick = onEarthquakeClick
                    )
                }
                ObservatoryTab.MAP -> tabStates.SaveableStateProvider("map") {
                    SeismicMapRoute(
                        onBack = { currentTab = ObservatoryTab.SUMMARY },
                        onEarthquakeClick = onEarthquakeClick,
                        inTabNavigation = true
                    )
                }
            }
        }
    }
}
