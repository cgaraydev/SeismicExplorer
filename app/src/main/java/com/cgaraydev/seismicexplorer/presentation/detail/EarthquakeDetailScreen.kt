package com.cgaraydev.seismicexplorer.presentation.detail

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.size
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
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailFailure
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailSource
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResourceKind
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResource
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
    onOpenDepthLearn: () -> Unit,
    onOpenRevisionLearn: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scientificState by viewModel.scientificState.collectAsStateWithLifecycle()
    val shakeMapImageState by viewModel.shakeMapImageState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    EarthquakeDetailScreen(
        state = state,
        onBack = onBack,
        onOpenSource = uriHandler::openUri,
        onOpenLearn = onOpenLearn,
        onOpenDepthLearn = onOpenDepthLearn,
        scientificState = scientificState,
        onRequestScientificDetail = viewModel::requestScientificDetail,
        shakeMapImageState = shakeMapImageState,
        onRequestShakeMapImage = viewModel::requestShakeMapImage,
        onOpenRevisionLearn = onOpenRevisionLearn
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
    onOpenDepthLearn: () -> Unit = {},
    scientificState: ScientificDetailUiState = ScientificDetailUiState.Idle,
    onRequestScientificDetail: () -> Unit = {},
    shakeMapImageState: ShakeMapImageUiState = ShakeMapImageUiState.Idle,
    onRequestShakeMapImage: () -> Unit = {},
    onOpenRevisionLearn: () -> Unit = {}
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
                    scientificState = scientificState,
                    onRequestScientificDetail = onRequestScientificDetail,
                    shakeMapImageState = shakeMapImageState,
                    onRequestShakeMapImage = onRequestShakeMapImage,
                    onOpenRevisionLearn = onOpenRevisionLearn,
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
    scientificState: ScientificDetailUiState,
    onRequestScientificDetail: () -> Unit,
    shakeMapImageState: ShakeMapImageUiState,
    onRequestShakeMapImage: () -> Unit,
    onOpenRevisionLearn: () -> Unit,
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

                DetailField(
                    label = stringResource(R.string.detail_magnitude_type),
                    value = earthquake.magType ?: stringResource(R.string.detail_unknown)
                )
                Text(
                    text = stringResource(R.string.detail_magnitude_type_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val reviewLabel = when (earthquake.reviewStatus?.lowercase(Locale.ROOT)) {
                    "reviewed" -> R.string.detail_reviewed
                    "automatic" -> R.string.detail_automatic
                    else -> R.string.detail_unknown
                }
                DetailField(
                    label = stringResource(R.string.detail_review_status),
                    value = stringResource(reviewLabel)
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

        ScientificDetailCard(
            earthquake = earthquake,
            state = scientificState,
            onRequest = onRequestScientificDetail,
            shakeMapImageState = shakeMapImageState,
            onRequestShakeMapImage = onRequestShakeMapImage,
            onOpenRevisions = onOpenRevisionLearn
        )

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

/** Inventario, no un visor ni un indicador de intensidad o daño medido. */
@Composable
private fun ScientificDetailCard(
    earthquake: Earthquake,
    state: ScientificDetailUiState,
    shakeMapImageState: ShakeMapImageUiState,
    onRequestShakeMapImage: () -> Unit,
    onRequest: () -> Unit,
    onOpenRevisions: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(modifier = Modifier.fillMaxWidth().testTag("detail_science_card")) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.detail_science_heading),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            Text(
                stringResource(R.string.detail_science_description),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            when (state) {
                ScientificDetailUiState.Idle -> Text(
                    stringResource(R.string.detail_science_idle),
                    modifier = Modifier.testTag("detail_science_idle"),
                    color = colors.onSurfaceVariant
                )
                ScientificDetailUiState.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.detail_science_loading),
                        color = colors.onSurface,
                        modifier = Modifier.testTag("detail_science_loading"))
                }
                is ScientificDetailUiState.Error -> Text(
                    stringResource(scientificDetailErrorRes(state.failure)),
                    color = colors.onSurface,
                    modifier = Modifier.testTag("detail_science_error")
                )
                is ScientificDetailUiState.Available -> {
                    val detail = state.detail
                    val stale = state.isStale ||
                        detail.sourceUpdatedAtEpochMillis < earthquake.updatedAt.toEpochMilli()
                    val provenanceRes = if (state.source == ScientificDetailSource.CACHE) {
                        R.string.detail_science_from_cache
                    } else {
                        R.string.detail_science_from_network
                    }
                    Text(stringResource(provenanceRes),
                        color = colors.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.testTag("detail_science_status"))
                    if (stale) {
                        Text(stringResource(R.string.detail_science_stale),
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.testTag("detail_science_stale"))
                    }
                    if (detail.availableProductTypes.isEmpty()) {
                        Text(stringResource(R.string.detail_science_no_products),
                            color = colors.onSurface,
                            modifier = Modifier.testTag("detail_science_empty"))
                    } else {
                        Text(stringResource(
                            R.string.detail_science_product_count,
                            detail.availableProductCount,
                            detail.availableProductTypes.size
                        ), color = colors.onSurfaceVariant)
                        detail.availableProductTypes.forEach { productType ->
                            val nameRes = scientificProductNameRes(productType)
                            val label = if (nameRes == null) productType else {
                                "${stringResource(nameRes)} · $productType"
                            }
                            Text(label, color = colors.onSurface,
                                modifier = Modifier.testTag("detail_science_product_$productType"))
                        }
                    }
                    val shakeMap = detail.shakeMap
                    if (shakeMap != null) {
                        val jpg = shakeMap.resources.firstOrNull {
                            it.kind == ShakeMapResourceKind.INTENSITY_MAP
                        }
                        if (jpg != null) {
                            ShakeMapIntensityPreview(
                                resource = jpg,
                                state = shakeMapImageState,
                                sourceName = shakeMap.source,
                                metadataStale = stale,
                                onRequest = onRequestShakeMapImage
                            )
                        } else {
                            Text(stringResource(R.string.detail_shakemap_unavailable),
                                color = colors.onSurfaceVariant)
                        }
                    }
                    Text(stringResource(R.string.detail_science_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
            }
            OutlinedButton(
                onClick = onRequest,
                enabled = state != ScientificDetailUiState.Loading,
                modifier = Modifier.fillMaxWidth().testTag("detail_science_request")
            ) {
                Text(stringResource(when (state) {
                    ScientificDetailUiState.Idle -> R.string.detail_science_consult
                    is ScientificDetailUiState.Available -> R.string.detail_science_refresh
                    else -> R.string.detail_science_retry
                }))
            }
            TextButton(onClick = onOpenRevisions,
                modifier = Modifier.testTag("detail_science_learn")) {
                Text(stringResource(R.string.detail_science_learn))
            }
        }
    }
}

/** El JPEG pertenece al producto de USGS: nunca equivale a observaciones directas de daños. */
@Composable
private fun ShakeMapIntensityPreview(
    resource: ShakeMapResource,
    state: ShakeMapImageUiState,
    sourceName: String?,
    metadataStale: Boolean,
    onRequest: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().testTag("detail_shakemap_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.detail_shakemap_title),
            style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(stringResource(R.string.detail_shakemap_description),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Text(stringResource(R.string.detail_shakemap_source, sourceName ?: "USGS"),
            style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        if (metadataStale) {
            Text(stringResource(R.string.detail_shakemap_metadata_stale),
                color = colors.onSurfaceVariant)
        }
        when (state) {
            ShakeMapImageUiState.Idle -> Unit
            ShakeMapImageUiState.Loading -> Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.detail_shakemap_loading), color = colors.onSurface)
            }
            is ShakeMapImageUiState.Error -> Text(
                stringResource(when (state.reason) {
                    com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure.INVALID_RESOURCE -> R.string.detail_shakemap_invalid
                    com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure.TOO_LARGE -> R.string.detail_shakemap_too_large
                    com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure.NOT_FOUND -> R.string.detail_shakemap_not_found
                    com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure.CONNECTION -> R.string.detail_shakemap_connection
                    com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure.SERVICE -> R.string.detail_shakemap_service
                    com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure.INVALID_IMAGE -> R.string.detail_shakemap_invalid
                }), modifier = Modifier.testTag("detail_shakemap_error"), color = colors.onSurface
            )
            is ShakeMapImageUiState.Available -> {
                Image(bitmap = state.image,
                    contentDescription = stringResource(R.string.detail_shakemap_accessibility),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
                        .testTag("detail_shakemap_image"))
                Text(stringResource(if (state.fromCache) R.string.detail_shakemap_cache
                    else R.string.detail_shakemap_network),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium)
                if (state.isStale) {
                    Text(stringResource(R.string.detail_shakemap_cached_stale),
                        color = colors.onSurfaceVariant)
                }
            }
        }
        OutlinedButton(
            onClick = onRequest,
            enabled = state != ShakeMapImageUiState.Loading,
            modifier = Modifier.fillMaxWidth().testTag("detail_shakemap_request")
        ) {
            Text(stringResource(if (state is ShakeMapImageUiState.Available)
                R.string.detail_shakemap_retry else R.string.detail_shakemap_view))
        }
        Text(stringResource(R.string.detail_shakemap_caveat),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
    }
}

private fun scientificProductNameRes(type: String): Int? = when (type.lowercase(Locale.ROOT)) {
    "shakemap" -> R.string.detail_science_shakemap
    "dyfi" -> R.string.detail_science_dyfi
    "pager" -> R.string.detail_science_pager
    "moment-tensor" -> R.string.detail_science_moment_tensor
    "finite-fault" -> R.string.detail_science_finite_fault
    "phase-data" -> R.string.detail_science_phase_data
    "origin" -> R.string.detail_science_origin
    "magnitude" -> R.string.detail_science_magnitude
    else -> null
}

private fun scientificDetailErrorRes(error: ScientificDetailFailure): Int = when (error) {
    ScientificDetailFailure.INVALID_EVENT_ID -> R.string.detail_science_invalid_event
    ScientificDetailFailure.EVENT_NOT_FOUND -> R.string.detail_science_not_found
    ScientificDetailFailure.CONNECTION -> R.string.detail_science_connection_error
    ScientificDetailFailure.SERVICE -> R.string.detail_science_service_error
    ScientificDetailFailure.INVALID_RESPONSE -> R.string.detail_science_invalid_response
    ScientificDetailFailure.RESPONSE_TOO_LARGE -> R.string.detail_science_too_large
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