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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.presentation.common.localizedEarthquakePlace
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.domain.model.RefreshFailure
import com.cgaraydev.seismicexplorer.presentation.common.FeedCacheStatusText
import com.cgaraydev.seismicexplorer.presentation.common.SeismicMagnitudeScale
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
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var showLearning by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Destacado real del conjunto mostrado; nunca se inventa una magnitud.
    val featured = remember(state.earthquakes) {
        state.earthquakes.filter { it.magnitude != null }
            .maxWithOrNull(compareBy<Earthquake> { it.magnitude!! }
                .thenBy { it.occurredAt }.thenBy { it.id })
    }
    val periodLabel = when (state.timeWindow) {
        TimeWindow.LAST_DAY -> R.string.home_period_day
        TimeWindow.LAST_WEEK -> R.string.home_period_week
        TimeWindow.LAST_MONTH -> R.string.home_period_month
    }
    val magnitudeLabel = when (state.minimumMagnitude) {
        MagnitudeThreshold.ALL -> R.string.explore_all_magnitudes
        MagnitudeThreshold.MAGNITUDE_2_5 -> R.string.home_magnitude_2_5
        MagnitudeThreshold.MAGNITUDE_4_5 -> R.string.home_magnitude_4_5
    }
    val filterState = stringResource(if (filtersExpanded) R.string.explore_filters_expanded
        else R.string.explore_filters_collapsed)
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().testTag("home_list"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "editorial_title") { EditorialMasthead() }
        item(key = "editorial_controls") {
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.editorial_scope),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onAction(HomeAction.Refresh) },
                        enabled = state.refreshState != HomeRefreshState.Refreshing,
                        modifier = Modifier.testTag("explore_refresh")
                    ) {
                        Icon(painterResource(R.drawable.ic_atlas_refresh),
                            contentDescription = stringResource(R.string.home_refresh))
                    }
                    TextButton(
                        onClick = { filtersExpanded = !filtersExpanded },
                        modifier = Modifier.testTag("explore_filters_toggle").semantics {
                            stateDescription = filterState
                        }
                    ) {
                        Icon(painterResource(R.drawable.ic_map_filters), null,
                            Modifier.size(18.dp))
                        Text(stringResource(R.string.explore_filters_label),
                            Modifier.padding(start = 6.dp))
                    }
                }
                Text(
                    stringResource(R.string.explore_filter_summary,
                        stringResource(periodLabel), stringResource(magnitudeLabel)),
                    Modifier.testTag("explore_filter_summary"),
                    style = MaterialTheme.typography.bodyMedium
                )
                AnimatedVisibility(visible = filtersExpanded,
                    enter = expandVertically(tween(180)),
                    exit = shrinkVertically(tween(180))) {
                    ExploreFilterRow(state, onAction)
                }
                if (state.feedCacheStatus != FeedCacheStatus.Unknown) {
                    FeedCacheStatusText(state.feedCacheStatus)
                }
                if (state.refreshState == HomeRefreshState.Refreshing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.home_refreshing),
                        style = MaterialTheme.typography.bodySmall)
                }
                (state.refreshState as? HomeRefreshState.Failed)?.let { failure ->
                    Text(refreshFailureMessage(failure.reason),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (state.localReadFailed) {
                    Text(stringResource(R.string.home_local_error),
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
        featured?.let { earthquake ->
            item(key = "editorial_feature") {
                EditorialFeaturedEvent(earthquake, onEarthquakeClick)
            }
        }
        item(key = "editorial_actions") {
            EditorialActions(onMapClick, onLearnClick = { showLearning = true })
        }
        item(key = "editorial_activity") {
            Column(Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.explore_recent_title),
                    style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.explore_results_count, state.earthquakes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.isReadingLocal && state.earthquakes.isEmpty()) {
            item(key = "loading") {
                Row(Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(stringResource(R.string.home_loading_local))
                }
            }
        } else if (!state.localReadFailed && state.earthquakes.isEmpty() &&
            state.refreshState != HomeRefreshState.Refreshing) {
            item(key = "empty") {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(stringResource(R.string.home_empty_title),
                        style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.home_empty_description),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        // Primeros tres eventos antes del bloque visual; los demás siguen abajo.
        items(state.earthquakes.take(3), key = { it.id }) { earthquake ->
            EarthquakeCard(earthquake, { onEarthquakeClick(earthquake.id) },
                Modifier.padding(horizontal = 16.dp))
        }
        item(key = "editorial_chart") { EditorialHourlyPreview() }
        items(state.earthquakes.drop(3), key = { it.id }) { earthquake ->
            EarthquakeCard(earthquake, { onEarthquakeClick(earthquake.id) },
                Modifier.padding(horizontal = 16.dp))
        }
        item(key = "source") {
            Column(Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_source),
                    style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.home_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showLearning) EditorialLearningDialog(onDismiss = { showLearning = false })
}

@Composable
private fun ExploreFilterRow(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("explore_filter_panel"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.home_period_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                TimeWindow.entries.forEach { window ->
                    val label = when (window) {
                        TimeWindow.LAST_DAY -> R.string.explore_period_short_day
                        TimeWindow.LAST_WEEK -> R.string.home_period_week
                        TimeWindow.LAST_MONTH -> R.string.home_period_month
                    }
                    FilterChip(
                        selected = state.timeWindow == window,
                        onClick = { onAction(HomeAction.SelectTimeWindow(window)) },
                        label = { Text(stringResource(label)) },
                        modifier = Modifier.testTag("explore_period_${window.name}")
                    )
                }
            }

            Text(
                text = stringResource(R.string.home_magnitude_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                MagnitudeThreshold.entries.forEach { threshold ->
                    val label = when (threshold) {
                        MagnitudeThreshold.ALL -> R.string.home_magnitude_all
                        MagnitudeThreshold.MAGNITUDE_2_5 -> R.string.home_magnitude_2_5
                        MagnitudeThreshold.MAGNITUDE_4_5 -> R.string.home_magnitude_4_5
                    }
                    FilterChip(
                        selected = state.minimumMagnitude == threshold,
                        onClick = { onAction(HomeAction.SelectMagnitude(threshold)) },
                        label = { Text(stringResource(label)) },
                        modifier = Modifier.testTag("explore_magnitude_${threshold.name}")
                    )
                }
            }
            TextButton(
                onClick = {
                    if (state.timeWindow != TimeWindow.LAST_DAY) {
                        onAction(HomeAction.SelectTimeWindow(TimeWindow.LAST_DAY))
                    }
                    if (state.minimumMagnitude != MagnitudeThreshold.ALL) {
                        onAction(HomeAction.SelectMagnitude(MagnitudeThreshold.ALL))
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = state.timeWindow != TimeWindow.LAST_DAY ||
                    state.minimumMagnitude != MagnitudeThreshold.ALL
            ) {
                Text(stringResource(R.string.explore_reset_filters))
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
            containerColor = MaterialTheme.colorScheme.background
        ),
        shape = RoundedCornerShape(0.dp)
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
                    text = localizedEarthquakePlace(earthquake.place),
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

