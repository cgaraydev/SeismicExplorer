package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeRoute(
    onEarthquakeClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onEarthquakeClick = onEarthquakeClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    onEarthquakeClick: (String) -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item {
                Text(
                    text = stringResource(R.string.home_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text(
                    text = stringResource(R.string.home_period_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeWindow.entries.forEach { window ->
                        val labelRes = when (window) {
                            TimeWindow.LAST_DAY ->
                                R.string.home_period_day

                            TimeWindow.LAST_WEEK ->
                                R.string.home_period_week

                            TimeWindow.LAST_MONTH ->
                                R.string.home_period_month
                        }

                        FilterChip(
                            selected = state.timeWindow == window,
                            onClick = {
                                onAction(
                                    HomeAction.SelectTimeWindow(window)
                                )
                            },
                            label = {
                                Text(stringResource(labelRes))
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.home_magnitude_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MagnitudeThreshold.entries.forEach { threshold ->
                        val labelRes = when (threshold) {
                            MagnitudeThreshold.ALL ->
                                R.string.home_magnitude_all

                            MagnitudeThreshold.MAGNITUDE_2_5 ->
                                R.string.home_magnitude_2_5

                            MagnitudeThreshold.MAGNITUDE_4_5 ->
                                R.string.home_magnitude_4_5
                        }

                        FilterChip(
                            selected = state.minimumMagnitude == threshold,
                            onClick = {
                                onAction(
                                    HomeAction.SelectMagnitude(threshold)
                                )
                            },
                            label = {
                                Text(stringResource(labelRes))
                            }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_results_title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedButton(
                        onClick = {
                            onAction(HomeAction.Refresh)
                        },
                        enabled = state.refreshState !=
                                HomeRefreshState.Refreshing
                    ) {
                        Text(stringResource(R.string.home_refresh))
                    }
                }
            }

            if (state.refreshState == HomeRefreshState.Refreshing) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = stringResource(R.string.home_refreshing),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            val refreshFailure =
                state.refreshState as? HomeRefreshState.Failed

            if (refreshFailure != null) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = refreshFailureMessage(
                                refreshFailure.reason
                            ),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (state.localReadFailed) {
                item {
                    Text(
                        text = stringResource(R.string.home_local_error),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (state.isReadingLocal && state.earthquakes.isEmpty()) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()

                        Text(
                            text = stringResource(R.string.home_loading_local)
                        )
                    }
                }
            } else if (
                !state.localReadFailed &&
                state.earthquakes.isEmpty() &&
                state.refreshState != HomeRefreshState.Refreshing
            ) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (refreshFailure != null) {
                                    stringResource(R.string.home_no_cache_title)
                                } else {
                                    stringResource(R.string.home_empty_title)
                                },
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = stringResource(
                                    R.string.home_empty_description
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (state.earthquakes.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(
                            R.string.home_results_count,
                            state.earthquakes.size
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                items(
                    items = state.earthquakes,
                    key = { earthquake -> earthquake.id }
                ) { earthquake ->
                    EarthquakeCard(
                        earthquake = earthquake,
                        onClick = {
                            onEarthquakeClick(earthquake.id)
                        }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.home_source),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text(
                    text = stringResource(R.string.home_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EarthquakeCard(
    earthquake: Earthquake,
    onClick: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]

    val occurredAt = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())
        .format(earthquake.occurredAt)

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("earthquake_${earthquake.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = earthquake.magnitude?.let { magnitude ->
                    stringResource(
                        R.string.home_event_magnitude,
                        magnitude
                    )
                } ?: stringResource(R.string.home_event_magnitude_unknown),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = earthquake.place
                    ?: stringResource(R.string.home_event_place_unknown),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = occurredAt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            earthquake.depthKilometers?.let { depth ->
                Text(
                    text = stringResource(
                        R.string.home_event_depth,
                        depth
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun refreshFailureMessage(
    failure: RefreshFailure?
): String {
    val messageRes = when (failure) {
        RefreshFailure.CONNECTION -> R.string.home_error_connection
        RefreshFailure.SERVICE -> R.string.home_error_service
        RefreshFailure.INVALID_RESPONSE -> R.string.home_error_response
        null -> R.string.home_error_unexpected
    }

    return stringResource(messageRes)
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SeismicExplorerTheme {
        HomeScreen(
            state = HomeUiState(
                isReadingLocal = false,
                refreshState = HomeRefreshState.Completed
            ),
            onAction = {}
        )
    }
}