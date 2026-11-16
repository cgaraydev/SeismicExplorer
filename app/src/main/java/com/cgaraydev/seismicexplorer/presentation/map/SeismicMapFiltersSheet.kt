package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeDepthFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SeismicMapFiltersSheet(
    filters: MapFilterSelection,
    onTimeWindowSelect: (RecentWindow) -> Unit,
    onMagnitudeSelect: (MapMagnitudeFilter) -> Unit,
    onDepthFilterSelect: (EarthquakeDepthFilter) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.map_filters),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(R.string.home_period_title),
                style = MaterialTheme.typography.titleMedium
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecentWindow.entries.forEach { window ->
                    val label = when (window) {
                        RecentWindow.LAST_DAY ->
                            R.string.home_period_day

                        RecentWindow.LAST_WEEK ->
                            R.string.home_period_week

                        RecentWindow.LAST_30_DAYS ->
                            R.string.home_period_month
                    }

                    FilterChip(
                        selected = filters.timeWindow == window,
                        onClick = {
                            onTimeWindowSelect(window)
                        },
                        label = {
                            Text(stringResource(label))
                        }
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.home_magnitude_title
                ),
                style = MaterialTheme.typography.titleMedium
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapMagnitudeFilter.entries.forEach { magnitude ->
                    val label = when (magnitude) {
                        MapMagnitudeFilter.ALL ->
                            R.string.home_magnitude_all

                        MapMagnitudeFilter.MAGNITUDE_2_5 ->
                            R.string.home_magnitude_2_5

                        MapMagnitudeFilter.MAGNITUDE_4_5 ->
                            R.string.home_magnitude_4_5
                    }

                    FilterChip(
                        selected = filters.magnitude == magnitude,
                        onClick = {
                            onMagnitudeSelect(magnitude)
                        },
                        label = {
                            Text(stringResource(label))
                        }
                    )
                }
            }

            Text(
                text = stringResource(R.string.map_depth_title),
                style = MaterialTheme.typography.titleMedium
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EarthquakeDepthFilter.entries.forEach { depthFilter ->
                    val labelRes = when (depthFilter) {
                        EarthquakeDepthFilter.ALL ->
                            R.string.map_depth_all

                        EarthquakeDepthFilter.SHALLOW ->
                            R.string.map_depth_shallow

                        EarthquakeDepthFilter.INTERMEDIATE ->
                            R.string.map_depth_intermediate

                        EarthquakeDepthFilter.DEEP ->
                            R.string.map_depth_deep
                    }

                    FilterChip(
                        selected = filters.depthFilter == depthFilter,
                        onClick = {
                            onDepthFilterSelect(depthFilter)
                        },
                        label = {
                            Text(stringResource(labelRes))
                        }
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.map_depth_unknown_notice
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(
                    R.string.map_filters_local_notice
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun mapFiltersSummary(
    filters: MapFilterSelection
): String {
    val periodLabel = when (filters.timeWindow) {
        RecentWindow.LAST_DAY -> R.string.home_period_day
        RecentWindow.LAST_WEEK -> R.string.home_period_week
        RecentWindow.LAST_30_DAYS -> R.string.home_period_month
    }

    val magnitudeLabel = when (filters.magnitude) {
        MapMagnitudeFilter.ALL -> R.string.home_magnitude_all
        MapMagnitudeFilter.MAGNITUDE_2_5 ->
            R.string.home_magnitude_2_5
        MapMagnitudeFilter.MAGNITUDE_4_5 ->
            R.string.home_magnitude_4_5
    }

    val depthLabel = when (filters.depthFilter) {
        EarthquakeDepthFilter.ALL ->
            R.string.map_depth_all

        EarthquakeDepthFilter.SHALLOW ->
            R.string.map_depth_shallow

        EarthquakeDepthFilter.INTERMEDIATE ->
            R.string.map_depth_intermediate

        EarthquakeDepthFilter.DEEP ->
            R.string.map_depth_deep
    }

    return stringResource(
        R.string.map_filters_summary,
        stringResource(periodLabel),
        stringResource(magnitudeLabel),
        stringResource(depthLabel)
    )
}