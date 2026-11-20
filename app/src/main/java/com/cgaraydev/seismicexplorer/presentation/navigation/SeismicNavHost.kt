package com.cgaraydev.seismicexplorer.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.ui.platform.LocalUriHandler
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
import com.cgaraydev.seismicexplorer.presentation.learn.LearnArticleScreen
import com.cgaraydev.seismicexplorer.presentation.learn.LearnCatalog
import com.cgaraydev.seismicexplorer.presentation.learn.LearnHomeScreen
import com.cgaraydev.seismicexplorer.presentation.learn.LearnMagnitudeLabScreen
import com.cgaraydev.seismicexplorer.presentation.map.SeismicMapRoute

private enum class ObservatoryTab(val labelRes: Int, val drawableRes: Int, val testTag: String) {
    SUMMARY(R.string.observatory_tab_summary, R.drawable.ic_observatory_summary, "tab_summary"),
    MAP(R.string.observatory_tab_map, R.drawable.ic_atlas_map, "tab_map"),
    CATALOG(R.string.observatory_tab_catalog, R.drawable.ic_atlas_events, "tab_catalog"),
    LEARN(R.string.observatory_tab_learn, R.drawable.ic_observatory_learn, "tab_learn")
}

/**
 * El contenedor de navegación es dueño de la pestaña seleccionada y de la barra inferior.
 * La ficha de un sismo no crea una segunda barra ni duplica el estado del Observatorio.
 */
@Composable
fun SeismicNavHost() {
    val backStack = rememberNavBackStack(HomeDestination)
    var currentTab by rememberSaveable { mutableStateOf(ObservatoryTab.SUMMARY) }

    val navigateBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull()
        Unit
    }
    val top = backStack.lastOrNull()
    // Las pantallas de aprendizaje abiertas desde un detalle usan navegación de retorno,
    // sin duplicar la barra de navegación de primer nivel.
    val showBottomBar = top == HomeDestination || top is EarthquakeDetailDestination

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                ObservatoryBottomBar(
                    selectedTab = currentTab,
                    onSelectTab = { destination ->
                        // Cambiar de sección desde un detalle DESCARTA ese detalle.
                        // Atrás no debe volver a una ficha oculta tras una pestaña.
                        if (backStack.lastOrNull() != HomeDestination) {
                            while (backStack.size > 1) backStack.removeLastOrNull()
                        }
                        currentTab = destination
                    }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(padding)
        ) {
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
                            currentTab = currentTab,
                            onSelectTab = { currentTab = it },
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
                        EarthquakeDetailRoute(
                            viewModel = viewModel,
                            onBack = navigateBack,
                            onOpenLearn = {
                                if (backStack.lastOrNull() == destination) {
                                    backStack.add(LearnArticleDestination(LearnCatalog.MAGNITUDE_INTENSITY))
                                }
                            },
                            onOpenDepthLearn = {
                                if (backStack.lastOrNull() == destination) {
                                    backStack.add(LearnArticleDestination(LearnCatalog.EPICENTER_DEPTH))
                                }
                            },
                            onOpenRevisionLearn = {
                                if (backStack.lastOrNull() == destination) {
                                    backStack.add(LearnArticleDestination(LearnCatalog.REVISIONS))
                                }
                            }
                        )
                    }
                    entry<LearnArticleDestination> { destination ->
                        val uriHandler = LocalUriHandler.current
                        LearnArticleScreen(
                            articleId = destination.articleId,
                            onBack = navigateBack,
                            onOpenSource = uriHandler::openUri
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ObservatoryBottomBar(
    selectedTab: ObservatoryTab,
    onSelectTab: (ObservatoryTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier.testTag("observatory_bottom_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        ObservatoryTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
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

/** Cada pestaña gestiona sus filtros; todas usan el mismo repositorio y caché Room. */
@Composable
private fun ObservatoryTabs(
    currentTab: ObservatoryTab,
    onSelectTab: (ObservatoryTab) -> Unit,
    onEarthquakeClick: (String) -> Unit
) {
    var selectedLearnArticle by rememberSaveable { mutableStateOf<String?>(null) }
    var showMagnitudeLab by rememberSaveable { mutableStateOf(false) }
    var returnToLabFromArticle by rememberSaveable { mutableStateOf(false) }
    val tabStates = rememberSaveableStateHolder()

    BackHandler(enabled = currentTab != ObservatoryTab.SUMMARY) {
        if (currentTab == ObservatoryTab.LEARN && selectedLearnArticle != null) {
            selectedLearnArticle = null
            if (returnToLabFromArticle) showMagnitudeLab = true
            returnToLabFromArticle = false
        } else if (currentTab == ObservatoryTab.LEARN && showMagnitudeLab) {
            showMagnitudeLab = false
        } else {
            onSelectTab(ObservatoryTab.SUMMARY)
        }
    }

    Box(Modifier.fillMaxSize()) {
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
                    onOpenCatalog = { onSelectTab(ObservatoryTab.CATALOG) },
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
                    onBack = { onSelectTab(ObservatoryTab.SUMMARY) },
                    onEarthquakeClick = onEarthquakeClick,
                    inTabNavigation = true
                )
            }
            ObservatoryTab.LEARN -> tabStates.SaveableStateProvider("learn") {
                val articleId = selectedLearnArticle
                if (articleId != null) {
                    val uriHandler = LocalUriHandler.current
                    LearnArticleScreen(
                        articleId = articleId,
                        onBack = {
                            selectedLearnArticle = null
                            if (returnToLabFromArticle) showMagnitudeLab = true
                            returnToLabFromArticle = false
                        },
                        onOpenSource = uriHandler::openUri
                    )
                } else if (showMagnitudeLab) {
                    val uriHandler = LocalUriHandler.current
                    LearnMagnitudeLabScreen(
                        onBack = { showMagnitudeLab = false },
                        onOpenArticle = {
                            selectedLearnArticle = it
                            returnToLabFromArticle = true
                            showMagnitudeLab = false
                        },
                        onOpenSource = uriHandler::openUri
                    )
                } else {
                    LearnHomeScreen(
                        onOpenArticle = {
                            selectedLearnArticle = it
                            returnToLabFromArticle = false
                        },
                        onOpenLaboratory = { showMagnitudeLab = true }
                    )
                }
            }
        }
    }
}
