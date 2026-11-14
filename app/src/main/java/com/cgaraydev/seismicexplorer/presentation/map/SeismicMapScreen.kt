package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cgaraydev.seismicexplorer.R

@Composable
fun SeismicMapRoute(
    onBack: () -> Unit,
    viewModel: SeismicMapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SeismicMapScreen(
        onBack = onBack,
        state = state
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeismicMapScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    state: SeismicMapUiState = SeismicMapUiState(),
    mapContent: @Composable (String) -> Unit = { geoJson ->
        SeismicMapView(geoJson = geoJson)
    }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.map_title))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.detail_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            mapContent(state.geoJson)

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isLoading) {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(
                                R.string.map_loading_events
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(
                                R.string.map_event_count,
                                state.eventCount
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (state.hasError) {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(
                                R.string.map_local_error
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}