package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.presentation.common.AtlasExploreHeader
import com.cgaraydev.seismicexplorer.presentation.common.FeedCacheStatusText
import com.cgaraydev.seismicexplorer.presentation.common.SeismicMagnitudeScale
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeRoute(
    onEarthquakeClick: (String) -> Unit,
    onMapClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        onPauseOrDispose { }
    }

    HomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onEarthquakeClick = onEarthquakeClick,
        onMapClick = onMapClick
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    onEarthquakeClick: (String) -> Unit = {},
    onMapClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .testTag("home_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "atlas_header") {
            AtlasExploreHeader()
        }

        item(key = "explore_switch") {
            ExploreViewSelector(
                onMapClick = onMapClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "explore_filters") {
            ExploreFilterRow(
                state = state,
                onAction = onAction,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "explore_results_header") {
            Column(
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.explore_recent_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onAction(HomeAction.Refresh) },
                        enabled = state.refreshState != HomeRefreshState.Refreshing
                    ) {
                        Text(stringResource(R.string.home_refresh))
                    }
                }

                if (!state.isReadingLocal || state.earthquakes.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.explore_results_count,
                            state.earthquakes.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.feedCacheStatus != FeedCacheStatus.Unknown) {
            item(key = "cache_status") {
                FeedCacheStatusText(
                    status = state.feedCacheStatus,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        if (state.refreshState == HomeRefreshState.Refreshing) {
            item(key = "refresh_progress") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.home_refreshing),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        val refreshFailure = state.refreshState as? HomeRefreshState.Failed
        if (refreshFailure != null) {
            item(key = "refresh_failure") {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = refreshFailureMessage(refreshFailure.reason),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (state.localReadFailed) {
            item(key = "local_error") {
                Text(
                    text = stringResource(R.string.home_local_error),
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (state.isReadingLocal && state.earthquakes.isEmpty()) {
            item(key = "local_loading") {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.home_loading_local))
                }
            }
        } else if (
            !state.localReadFailed &&
            state.earthquakes.isEmpty() &&
            state.refreshState != HomeRefreshState.Refreshing
        ) {
            item(key = "empty_state") {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_empty_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.home_empty_description),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        items(
            items = state.earthquakes,
            key = { earthquake -> earthquake.id }
        ) { earthquake ->
            EarthquakeCard(
                earthquake = earthquake,
                onClick = { onEarthquakeClick(earthquake.id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "source") {
            Text(
                text = stringResource(R.string.home_source),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item(key = "disclaimer") {
            Text(
                text = stringResource(R.string.home_disclaimer),
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExploreViewSelector(
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .semantics { selected = true },
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.explore_list),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        OutlinedButton(
            onClick = onMapClick,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .testTag("explore_map"),
            shape = RoundedCornerShape(12.dp),
            border = null,
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(stringResource(R.string.explore_map))
        }
    }
}

@Composable
private fun ExploreFilterRow(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            val currentLabel = when (state.timeWindow) {
                TimeWindow.LAST_DAY -> R.string.home_period_day
                TimeWindow.LAST_WEEK -> R.string.home_period_week
                TimeWindow.LAST_MONTH -> R.string.home_period_month
            }
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("explore_period"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(currentLabel),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(" ▾")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TimeWindow.entries.forEach { window ->
                    val label = when (window) {
                        TimeWindow.LAST_DAY -> R.string.home_period_day
                        TimeWindow.LAST_WEEK -> R.string.home_period_week
                        TimeWindow.LAST_MONTH -> R.string.home_period_month
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            expanded = false
                            onAction(HomeAction.SelectTimeWindow(window))
                        }
                    )
                }
            }
        }
        Box(Modifier.weight(1f)) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            val currentLabel = when (state.minimumMagnitude) {
                MagnitudeThreshold.ALL -> R.string.home_magnitude_all
                MagnitudeThreshold.MAGNITUDE_2_5 -> R.string.home_magnitude_2_5
                MagnitudeThreshold.MAGNITUDE_4_5 -> R.string.home_magnitude_4_5
            }
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("explore_magnitude"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(currentLabel),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(" ▾")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                MagnitudeThreshold.entries.forEach { threshold ->
                    val label = when (threshold) {
                        MagnitudeThreshold.ALL -> R.string.home_magnitude_all
                        MagnitudeThreshold.MAGNITUDE_2_5 -> R.string.home_magnitude_2_5
                        MagnitudeThreshold.MAGNITUDE_4_5 -> R.string.home_magnitude_4_5
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            expanded = false
                            onAction(HomeAction.SelectMagnitude(threshold))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EarthquakeCard(
    earthquake: Earthquake,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val occurredAt = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())
        .format(earthquake.occurredAt)

    val magnitudeDescription = earthquake.magnitude?.let { magnitude ->
        stringResource(R.string.home_event_magnitude, magnitude)
    } ?: stringResource(R.string.home_event_magnitude_unknown)
    val magnitudeLabel = earthquake.magnitude?.let { magnitude ->
        stringResource(R.string.home_event_magnitude, magnitude)
    } ?: stringResource(R.string.home_event_magnitude_short_unknown)
    val accentColor = SeismicMagnitudeScale.accent(earthquake.magnitude)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("earthquake_${earthquake.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 66.dp)
                    .defaultMinSize(minHeight = 54.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = magnitudeDescription
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = magnitudeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = earthquake.place
                        ?: stringResource(R.string.home_event_place_unknown),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = occurredAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    earthquake.depthKilometers?.let { depth ->
                        Text(
                            text = stringResource(R.string.home_event_depth, depth),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun refreshFailureMessage(failure: RefreshFailure?): String {
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
