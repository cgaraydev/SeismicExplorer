package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.FeedCacheStatus
import com.cgaraydev.seismicexplorer.presentation.common.FeedCacheStatusText
import com.cgaraydev.seismicexplorer.presentation.common.SeismicMagnitudeScale
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Catálogo con filtros propios, sobre la misma fuente local Room que el Observatorio. */
@Composable
fun SeismicCatalogScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onEarthquakeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberLazyListState()
    val period = when (state.timeWindow) {
        TimeWindow.LAST_DAY -> R.string.home_period_day
        TimeWindow.LAST_WEEK -> R.string.home_period_week
        TimeWindow.LAST_MONTH -> R.string.home_period_month
    }
    val mag = when (state.minimumMagnitude) {
        MagnitudeThreshold.ALL -> R.string.explore_all_magnitudes
        MagnitudeThreshold.MAGNITUDE_2_5 -> R.string.home_magnitude_2_5
        MagnitudeThreshold.MAGNITUDE_4_5 -> R.string.home_magnitude_4_5
    }

    LazyColumn(
        state = scroll,
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("catalog_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "catalog_header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.observatory_catalog),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.observatory_catalog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "filters") {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                TextButton(
                    onClick = { filtersExpanded = !filtersExpanded },
                    modifier = Modifier.testTag("catalog_filter_toggle")
                ) {
                    Icon(painterResource(R.drawable.ic_map_filters), contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.explore_filters_label), Modifier.padding(start = 8.dp))
                    Icon(painterResource(R.drawable.ic_atlas_chevron_down), contentDescription = null,
                        modifier = Modifier.size(20.dp))
                }
                Text(
                    text = stringResource(R.string.explore_filter_summary,
                        stringResource(period), stringResource(mag)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("catalog_filter_summary")
                )
                if (filtersExpanded) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("catalog_filter_panel"),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.home_period_title),
                                style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TimeWindow.entries.forEach { window ->
                                    val res = when (window) {
                                        TimeWindow.LAST_DAY -> R.string.explore_period_short_day
                                        TimeWindow.LAST_WEEK -> R.string.home_period_week
                                        TimeWindow.LAST_MONTH -> R.string.home_period_month
                                    }
                                    FilterChip(
                                        selected = window == state.timeWindow,
                                        onClick = { onAction(HomeAction.SelectTimeWindow(window)) },
                                        label = { Text(stringResource(res)) },
                                        modifier = Modifier.testTag("catalog_period_${window.name}")
                                    )
                                }
                            }
                            Text(stringResource(R.string.home_magnitude_title),
                                style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MagnitudeThreshold.entries.forEach { threshold ->
                                    val res = when (threshold) {
                                        MagnitudeThreshold.ALL -> R.string.home_magnitude_all
                                        MagnitudeThreshold.MAGNITUDE_2_5 -> R.string.home_magnitude_2_5
                                        MagnitudeThreshold.MAGNITUDE_4_5 -> R.string.home_magnitude_4_5
                                    }
                                    FilterChip(
                                        selected = threshold == state.minimumMagnitude,
                                        onClick = { onAction(HomeAction.SelectMagnitude(threshold)) },
                                        label = { Text(stringResource(res)) },
                                        modifier = Modifier.testTag("catalog_magnitude_${threshold.name}")
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    onAction(HomeAction.SelectTimeWindow(TimeWindow.LAST_DAY))
                                    onAction(HomeAction.SelectMagnitude(MagnitudeThreshold.ALL))
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text(stringResource(R.string.explore_reset_filters)) }
                        }
                    }
                }
            }
        }
        item(key = "result_count") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.explore_results_count, state.earthquakes.size),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground)
                TextButton(
                    onClick = { onAction(HomeAction.Refresh) },
                    enabled = state.refreshState != HomeRefreshState.Refreshing
                ) { Text(stringResource(R.string.home_refresh)) }
            }
        }
        if (state.refreshState == HomeRefreshState.Refreshing) {
            item(key = "refresh") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        if (state.feedCacheStatus != FeedCacheStatus.Unknown) {
            item(key = "cache") { FeedCacheStatusText(state.feedCacheStatus) }
        }
        if (state.localReadFailed) {
            item(key = "error") {
                Text(stringResource(R.string.home_local_error), color = MaterialTheme.colorScheme.error)
            }
        } else if (state.isReadingLocal && state.earthquakes.isEmpty()) {
            item(key = "loading") { Text(stringResource(R.string.home_loading_local)) }
        } else if (state.earthquakes.isEmpty()) {
            item(key = "empty") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.home_empty_title),
                        style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.home_empty_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(state.earthquakes, key = { it.id }) { earthquake ->
            CatalogEarthquakeCard(
                earthquake = earthquake,
                onClick = { onEarthquakeClick(earthquake.id) }
            )
        }
        item(key = "source") {
            Text(stringResource(R.string.home_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CatalogEarthquakeCard(earthquake: Earthquake, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale).withZone(ZoneId.systemDefault())
        .format(earthquake.occurredAt)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("catalog_event_${earthquake.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = SeismicMagnitudeScale.accent(earthquake.magnitude).copy(alpha = 0.20f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = earthquake.magnitude?.let {
                        stringResource(R.string.home_event_magnitude, it)
                    } ?: stringResource(R.string.home_event_magnitude_short_unknown),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    earthquake.place ?: stringResource(R.string.home_event_place_unknown),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                earthquake.depthKilometers?.let { depth ->
                    Text(
                        stringResource(R.string.home_event_depth, depth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
