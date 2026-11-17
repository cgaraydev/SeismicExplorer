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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.cgaraydev.seismicexplorer.presentation.map.defaultSeismicCameraPosition
import org.maplibre.android.camera.CameraPosition
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
    onMapClick: () -> Unit = {},
    mapPreview: @Composable (
        List<Earthquake>,
        CameraPosition,
        (CameraPosition) -> Unit,
        (String) -> Unit,
        () -> Unit
    ) -> Unit = { events, camera, onCameraChanged, onEventClick, onExpand ->
        ExploreInlineMap(
            earthquakes = events,
            initialCamera = camera,
            onCameraChanged = onCameraChanged,
            onEarthquakeClick = onEventClick,
            onExpandClick = onExpand
        )
    }
) {
    var showMap by rememberSaveable { mutableStateOf(false) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val listScroll = rememberLazyListState()
    // Mantener por separado el scroll de las dos representaciones.
    val mapScroll = rememberLazyListState(initialFirstVisibleItemIndex = 1)
    var mapCamera by rememberSaveable {
        mutableStateOf(defaultSeismicCameraPosition())
    }

    AnimatedContent(
        targetState = showMap,
        transitionSpec = {
            // Mapa avanza desde la derecha; Eventos regresa desde la izquierda.
            if (targetState) {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(260)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(260)
                )
            } else {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(260)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(260)
                )
            }
        },
        label = "explore_mode"
    ) { selectedMap ->
    LazyColumn(
        state = if (selectedMap) mapScroll else listScroll,
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
                selectedMap = selectedMap,
                onListClick = { showMap = false },
                onMapClick = { showMap = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "explore_results_header") {
            ExploreResultsHeader(
                state = state,
                selectedMap = selectedMap,
                filtersExpanded = filtersExpanded,
                onToggleFilters = { filtersExpanded = !filtersExpanded },
                onAction = onAction,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (selectedMap) {
            item(key = "inline_map") {
                mapPreview(
                    state.earthquakes,
                    mapCamera,
                    { camera -> mapCamera = camera },
                    onEarthquakeClick,
                    onMapClick
                )
            }
        } else {
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
    }
}

@Composable
private fun ExploreViewSelector(
    selectedMap: Boolean,
    onListClick: () -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(16.dp)
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ExploreSelectorTab(
            label = stringResource(R.string.explore_list),
            iconRes = R.drawable.ic_atlas_events,
            active = !selectedMap,
            onClick = onListClick,
            modifier = Modifier
                .weight(1f)
                .testTag("explore_list")
        )
        ExploreSelectorTab(
            label = stringResource(R.string.explore_map),
            iconRes = R.drawable.ic_atlas_map,
            active = selectedMap,
            onClick = onMapClick,
            modifier = Modifier
                .weight(1f)
                .testTag("explore_map")
        )
    }
}

@Composable
private fun ExploreSelectorTab(
    label: String,
    iconRes: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics { selected = active },
        shape = RoundedCornerShape(12.dp),
        border = null,
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ExploreResultsHeader(
    state: HomeUiState,
    selectedMap: Boolean,
    filtersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val periodLabel = when (state.timeWindow) {
        TimeWindow.LAST_DAY -> R.string.explore_period_short_day
        TimeWindow.LAST_WEEK -> R.string.home_period_week
        TimeWindow.LAST_MONTH -> R.string.home_period_month
    }
    val filterState = stringResource(
        if (filtersExpanded) R.string.explore_filters_expanded
        else R.string.explore_filters_collapsed
    )
    val magnitudeLabel = when (state.minimumMagnitude) {
        MagnitudeThreshold.ALL -> R.string.explore_all_magnitudes
        MagnitudeThreshold.MAGNITUDE_2_5 -> R.string.home_magnitude_2_5
        MagnitudeThreshold.MAGNITUDE_4_5 -> R.string.home_magnitude_4_5
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (selectedMap) R.string.explore_map_heading
                        else R.string.explore_recent_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
            IconButton(
                onClick = { onAction(HomeAction.Refresh) },
                enabled = state.refreshState != HomeRefreshState.Refreshing,
                modifier = Modifier.testTag("explore_refresh")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_atlas_refresh),
                    contentDescription = stringResource(R.string.home_refresh)
                )
            }
            Surface(
                onClick = onToggleFilters,
                modifier = Modifier
                    .semantics { stateDescription = filterState }
                    .testTag("explore_filters_toggle"),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_map_filters),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.explore_filters_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_atlas_chevron_down),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (filtersExpanded) 180f else 0f)
                    )
                }
            }
        }

        Text(
            text = stringResource(
                R.string.explore_filter_summary,
                stringResource(periodLabel),
                stringResource(magnitudeLabel)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("explore_filter_summary")
        )

        AnimatedVisibility(
            visible = filtersExpanded,
            enter = expandVertically(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(180))
        ) {
            ExploreFilterRow(state = state, onAction = onAction)
        }
    }
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
