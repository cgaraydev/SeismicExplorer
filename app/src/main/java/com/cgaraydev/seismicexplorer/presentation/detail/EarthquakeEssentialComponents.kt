package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.presentation.common.EarthquakeTimeFormatter
import com.cgaraydev.seismicexplorer.presentation.map.MapEarthquakeGeoJson
import com.cgaraydev.seismicexplorer.presentation.map.SeismicMapView
import java.util.Locale
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

/** Una ficha legible: magnitud y ubicación no compiten por una columna estrecha. */
@Composable
internal fun EarthquakeEssentialHero(
    earthquake: Earthquake,
    place: String,
    magnitude: String,
    onMagnitudeInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val utcTime = remember(earthquake.occurredAt, locale) {
        EarthquakeTimeFormatter.utc(earthquake.occurredAt, locale)
    }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth().testTag("detail_essential_hero"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.detail_essential_magnitude),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )
            DetailQuestionButton(
                onClick = onMagnitudeInfo,
                description = stringResource(R.string.learn_magnitude_info_button),
                modifier = Modifier.testTag("detail_magnitude_info")
            )
        }
        Text(
            text = magnitude,
            modifier = Modifier.fillMaxWidth().testTag("detail_magnitude_primary"),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = colors.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = place,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            modifier = Modifier.fillMaxWidth().testTag("detail_place"),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Column(
            modifier = Modifier.padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_essential_utc_time),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                text = utcTime,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                modifier = Modifier.testTag("detail_time_utc")
            )
        }
    }
}

/** Los tres encabezados y sus valores comparten una sola línea por nivel. */
@Composable
internal fun EarthquakeEssentialMetrics(
    earthquake: Earthquake,
    onDepthInfo: () -> Unit,
    onScaleInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val depth = earthquake.depthKilometers?.let {
        stringResource(R.string.detail_depth_value, it)
    } ?: stringResource(R.string.detail_unknown)
    val magnitudeType = earthquake.magType ?: stringResource(R.string.detail_unknown)
    val review = when (earthquake.reviewStatus?.lowercase(Locale.ROOT)) {
        "reviewed" -> R.string.detail_reviewed
        "automatic" -> R.string.detail_automatic
        else -> R.string.detail_unknown
    }

    val depthHelpDescription = stringResource(R.string.learn_depth_info_button)

    Surface(
        modifier = modifier.fillMaxWidth().testTag("detail_essential_metrics"),
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Se conserva el acceso a la explicación, pero sin un icono extra.
            Column(
                modifier = Modifier.weight(1.15f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDepthInfo)
                    .testTag("detail_depth_info")
                    .semantics {
                        contentDescription = depthHelpDescription
                    },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = stringResource(R.string.detail_depth),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = depth,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(modifier = Modifier.weight(0.92f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.detail_essential_scale),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                    DetailQuestionButton(
                        onClick = onScaleInfo,
                        description = stringResource(R.string.detail_essential_scale_help_button),
                        modifier = Modifier.testTag("detail_scale_info")
                    )
                }
                Text(
                    text = magnitudeType,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(modifier = Modifier.weight(0.96f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = stringResource(R.string.detail_essential_status),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = stringResource(review),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Interrogación editorial pequeña con área de interacción accesible. */
@Composable
private fun DetailQuestionButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = modifier.size(36.dp).semantics { contentDescription = description }
    ) {
        Box(
            modifier = Modifier.size(19.dp)
                .border(1.dp, colors.secondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Cartografía real MapLibre con un único epicentro, sin círculos de alcance inventados. */
@Composable
internal fun EarthquakeEssentialMap(
    earthquake: Earthquake,
    modifier: Modifier = Modifier
) {
    val lat = earthquake.coordinates.latitude
    val lon = earthquake.coordinates.longitude
    val camera = remember(lat, lon) {
        CameraPosition.Builder().target(LatLng(lat, lon)).zoom(5.1).build()
    }
    val geoJson = remember(earthquake) { MapEarthquakeGeoJson.encode(listOf(earthquake)) }
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier.fillMaxWidth().height(195.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainerLow)
            .testTag("detail_essential_map")
    ) {
        SeismicMapView(
            geoJson = geoJson,
            initialCamera = camera,
            gesturesEnabled = false,
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            color = colors.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(9.dp)
        ) {
            Text(
                stringResource(R.string.detail_essential_epicenter),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
internal fun EarthquakeEssentialJump(
    number: String,
    title: String,
    description: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainerLow,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(number, style = MaterialTheme.typography.titleMedium, color = colors.secondary)
            Column(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = colors.onSurfaceVariant)
        }
    }
}
