package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.presentation.common.EarthquakeTimeFormatter
import com.cgaraydev.seismicexplorer.presentation.map.MapEarthquakeGeoJson
import com.cgaraydev.seismicexplorer.presentation.map.SeismicMapView
import java.util.Locale
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

/** Cabecera editorial adaptable; conserva el lugar completo y cambia a columna con texto grande. */
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

    val largeText = LocalConfiguration.current.fontScale > 1.25f
    Column(
        modifier = modifier.fillMaxWidth().testTag("detail_essential_hero"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 330.dp || largeText
            val magnitudeContent: @Composable () -> Unit = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.detail_essential_magnitude),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant)
                        DetailQuestionButton(onMagnitudeInfo,
                            stringResource(R.string.learn_magnitude_info_button),
                            Modifier.testTag("detail_magnitude_info"))
                    }
                    Text(magnitude,
                        modifier = Modifier.testTag("detail_magnitude_primary"),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 40.sp, lineHeight = 48.sp),
                        fontWeight = FontWeight.Bold, color = colors.secondary)
                }
            }
            val placeContent: @Composable () -> Unit = {
                Text(place, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold, color = colors.onSurface,
                    modifier = Modifier.testTag("detail_place"))
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    magnitudeContent()
                    placeContent()
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(0.95f)) { magnitudeContent() }
                    Box(Modifier.width(1.dp).height(72.dp).background(colors.outlineVariant))
                    Column(Modifier.weight(1.4f)) { placeContent() }
                }
            }
        }
        Text(utcTime, style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant, modifier = Modifier.testTag("detail_time_utc"))
        HorizontalDivider(color = colors.outlineVariant)
    }
}

/** Métricas compactas; disposición vertical con texto grande o poco ancho. */
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

    BoxWithConstraints(modifier.fillMaxWidth().testTag("detail_essential_metrics")) {
        val stacked = maxWidth < 320.dp || LocalConfiguration.current.fontScale > 1.25f
        val depthContent: @Composable () -> Unit = {
            Column(Modifier.clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDepthInfo).heightIn(min = 48.dp)
                .testTag("detail_depth_info")
                .semantics { contentDescription = depthHelpDescription },
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.detail_depth),
                    style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(depth, style = MaterialTheme.typography.titleMedium)
            }
        }
        val typeContent: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.detail_visual_type),
                    style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(magnitudeType, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    DetailQuestionButton(onScaleInfo,
                        stringResource(R.string.detail_essential_scale_help_button),
                        Modifier.testTag("detail_scale_info"))
                }
            }
        }
        val statusContent: @Composable () -> Unit = {
            Surface(color = colors.secondaryContainer, shape = RoundedCornerShape(50)) {
                Text(stringResource(review), style = MaterialTheme.typography.labelMedium,
                    color = colors.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            }
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                depthContent()
                typeContent()
                statusContent()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { depthContent() }
                Column(Modifier.weight(1.45f)) { typeContent() }
                statusContent()
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
        modifier = modifier.size(48.dp).semantics { contentDescription = description }
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
        modifier = modifier.fillMaxWidth().height(168.dp)
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
            Icon(painterResource(when (number) {
                "01" -> R.drawable.ic_detail_wave
                "02" -> R.drawable.ic_detail_layers
                else -> R.drawable.ic_detail_region
            }), contentDescription = null, tint = colors.secondary,
                modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.ic_detail_chevron), contentDescription = null,
                tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
