package com.cgaraydev.seismicexplorer.presentation.detail

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun EarthquakeDetailRoute(
    viewModel: EarthquakeDetailViewModel,
    onBack: () -> Unit,
    onOpenLearn: () -> Unit,
    onOpenDepthLearn: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    EarthquakeDetailScreen(
        state = state,
        onBack = onBack,
        onOpenSource = uriHandler::openUri,
        onOpenLearn = onOpenLearn,
        onOpenDepthLearn = onOpenDepthLearn
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthquakeDetailScreen(
    state: EarthquakeDetailUiState,
    onBack: () -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenLearn: () -> Unit = {},
    onOpenDepthLearn: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.detail_title))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.detail_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        when (state) {
            EarthquakeDetailUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }

            EarthquakeDetailUiState.NotFound -> {
                DetailMessage(
                    title = stringResource(R.string.detail_not_found),
                    description = stringResource(
                        R.string.detail_not_found_description
                    ),
                    modifier = Modifier.padding(innerPadding)
                )
            }

            EarthquakeDetailUiState.Error -> {
                DetailMessage(
                    title = stringResource(R.string.detail_error),
                    description = stringResource(
                        R.string.detail_error_description
                    ),
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is EarthquakeDetailUiState.Content -> {
                EarthquakeDetailContent(
                    earthquake = state.earthquake,
                    onOpenSource = onOpenSource,
                    onOpenLearn = onOpenLearn,
                    onOpenDepthLearn = onOpenDepthLearn,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EarthquakeDetailContent(
    earthquake: Earthquake,
    onOpenSource: (String) -> Unit,
    onOpenLearn: () -> Unit,
    onOpenDepthLearn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    var showMagnitudeInfo by rememberSaveable { mutableStateOf(false) }
    var showDepthInfo by rememberSaveable { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())

    val magnitude = earthquake.magnitude?.let {
        stringResource(R.string.home_event_magnitude, it)
    } ?: stringResource(R.string.home_event_magnitude_unknown)

    val place = earthquake.place
        ?: stringResource(R.string.home_event_place_unknown)

    val officialPage = earthquake.detailUrl?.takeIf { url ->
        val uri = Uri.parse(url)

        uri.scheme.equals("https", ignoreCase = true) &&
                uri.host.equals("earthquake.usgs.gov", ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = magnitude,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLarge
            )
            IconButton(onClick = { showMagnitudeInfo = true },
                modifier = Modifier.testTag("detail_magnitude_info")) {
                Icon(
                    painter = painterResource(R.drawable.ic_learn_info),
                    contentDescription = stringResource(R.string.learn_magnitude_info_button),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Text(
            text = place,
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_event_information),
                    style = MaterialTheme.typography.titleMedium
                )

                DetailField(
                    label = stringResource(R.string.detail_occurred_at),
                    value = dateFormatter.format(earthquake.occurredAt)
                )

                DetailField(
                    label = stringResource(R.string.detail_updated_at),
                    value = dateFormatter.format(earthquake.updatedAt)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        DetailField(
                            label = stringResource(R.string.detail_depth),
                            value = earthquake.depthKilometers?.let { depth ->
                                stringResource(R.string.detail_depth_value, depth)
                            } ?: stringResource(R.string.detail_unknown)
                        )
                    }
                    IconButton(
                        onClick = { showDepthInfo = true },
                        modifier = Modifier.testTag("detail_depth_info")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_learn_info),
                            contentDescription = stringResource(R.string.learn_depth_info_button),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_location),
                    style = MaterialTheme.typography.titleMedium
                )

                DetailField(
                    label = stringResource(R.string.detail_latitude),
                    value = formatCoordinate(
                        earthquake.coordinates.latitude,
                        locale
                    )
                )

                DetailField(
                    label = stringResource(R.string.detail_longitude),
                    value = formatCoordinate(
                        earthquake.coordinates.longitude,
                        locale
                    )
                )
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_source),
                    style = MaterialTheme.typography.titleMedium
                )

                DetailField(
                    label = stringResource(R.string.detail_event_id),
                    value = earthquake.id
                )

                Text(
                    text = stringResource(R.string.detail_provider),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (officialPage != null) {
                    OutlinedButton(
                        onClick = {
                            onOpenSource(officialPage)
                        }
                    ) {
                        Text(stringResource(R.string.detail_open_official))
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.home_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showDepthInfo) {
        ModalBottomSheet(
            onDismissRequest = { showDepthInfo = false },
            modifier = Modifier.testTag("detail_depth_sheet")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(stringResource(R.string.learn_depth_sheet_heading),
                    style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.learn_depth_sheet_body),
                    style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = {
                        showDepthInfo = false
                        onOpenDepthLearn()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("detail_depth_learn_button")
                ) {
                    Text(stringResource(R.string.learn_depth_sheet_read_article))
                }
            }
        }
    }

    if (showMagnitudeInfo) {
        ModalBottomSheet(
            onDismissRequest = { showMagnitudeInfo = false },
            modifier = Modifier.testTag("detail_magnitude_sheet")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(stringResource(R.string.learn_sheet_heading),
                    style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.learn_sheet_body),
                    style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = {
                        showMagnitudeInfo = false
                        onOpenLearn()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("detail_magnitude_learn_button")
                ) {
                    Text(stringResource(R.string.learn_sheet_read_article))
                }
            }
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun DetailMessage(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatCoordinate(
    coordinate: Double,
    locale: Locale
): String {
    return String.format(locale, "%.4f°", coordinate)
}

@Preview(showBackground = true)
@Composable
private fun EarthquakeDetailPreview() {
    SeismicExplorerTheme {
        EarthquakeDetailScreen(
            state = EarthquakeDetailUiState.Content(
                Earthquake(
                    id = "us-example",
                    magnitude = 5.2,
                    place = "Coast of Chile",
                    coordinates = GeoCoordinates(
                        latitude = -33.4,
                        longitude = -71.6
                    ),
                    depthKilometers = 12.0,
                    occurredAt = Instant.parse(
                        "2026-11-14T10:00:00Z"
                    ),
                    updatedAt = Instant.parse(
                        "2026-11-14T10:30:00Z"
                    ),
                    detailUrl = null
                )
            ),
            onBack = {},
            onOpenSource = {}
        )
    }
}