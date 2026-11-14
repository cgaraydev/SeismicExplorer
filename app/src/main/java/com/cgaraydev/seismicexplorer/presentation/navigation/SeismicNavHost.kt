package com.cgaraydev.seismicexplorer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.cgaraydev.seismicexplorer.presentation.detail.EarthquakeDetailRoute
import com.cgaraydev.seismicexplorer.presentation.detail.EarthquakeDetailViewModel
import com.cgaraydev.seismicexplorer.presentation.home.HomeRoute
import com.cgaraydev.seismicexplorer.presentation.map.SeismicMapScreen

@Composable
fun SeismicNavHost() {
    val backStack = rememberNavBackStack(HomeDestination)

    val navigateBack = {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
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
                HomeRoute(
                    onEarthquakeClick = { earthquakeId ->
                        if (backStack.lastOrNull() == HomeDestination) {
                            backStack.add(
                                EarthquakeDetailDestination(
                                    earthquakeId = earthquakeId
                                )
                            )
                        }
                    },
                    onMapClick = {
                        if (backStack.lastOrNull() == HomeDestination) {
                            backStack.add(MapDestination)
                        }
                    }
                )
            }

            entry<MapDestination> {
                SeismicMapScreen(
                    onBack = navigateBack
                )
            }

            entry<EarthquakeDetailDestination> { destination ->
                val viewModel = hiltViewModel<
                        EarthquakeDetailViewModel,
                        EarthquakeDetailViewModel.Factory
                        >(
                    creationCallback = { factory ->
                        factory.create(destination)
                    }
                )

                EarthquakeDetailRoute(
                    viewModel = viewModel,
                    onBack = navigateBack
                )
            }
        }
    )
}