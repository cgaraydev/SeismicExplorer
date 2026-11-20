package com.cgaraydev.seismicexplorer.presentation.detail

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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

/** Cabecera geológica editorial: trazos decorativos y magnitud dinámica de texto real. */
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
    val dramatic = colors.background.luminance() < 0.45f
    val headerColors = if (dramatic) {
        listOf(Color(0xFF0C3444), Color(0xFF0C1E2C), Color(0xFF211B23))
    } else {
        listOf(colors.surfaceContainer, colors.surfaceContainerLow, colors.surface)
    }
    val headerText = if (dramatic) Color(0xFFF4F4F0) else colors.onSurface
    val headerSecondaryText = if (dramatic) Color(0xFFD0DDE2) else colors.onSurfaceVariant
    val largeText = LocalConfiguration.current.fontScale > 1.25f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(headerColors))
            .testTag("detail_essential_hero")
    ) {
        Image(painterResource(R.drawable.detail_ocean_relief), contentDescription = null,
            modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            if (dramatic) listOf(Color(0xCC04131C), Color(0x9904131C))
            else listOf(colors.surface.copy(alpha = 0.92f), colors.surface.copy(alpha = 0.86f))
        )))
        // Fundido inferior: la geología se integra con la pantalla, sin borde de card.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.68f to Color.Transparent,
                    1.0f to colors.background
                )
            )
        )
        val stacked = maxWidth < 335.dp || largeText
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MagnitudeDial(
                    valueText = magnitude,
                    hasMagnitude = earthquake.magnitude != null,
                    onOpenMagnitudeEducation = onMagnitudeInfo,
                    educationDescription = stringResource(R.string.learn_magnitude_info_button),
                    diameter = 168.dp
                )
                Text(
                    text = place,
                    modifier = Modifier.fillMaxWidth().testTag("detail_place"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = headerText
                )
                Text(
                    text = utcTime,
                    modifier = Modifier.fillMaxWidth().testTag("detail_time_utc"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = headerSecondaryText
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MagnitudeDial(
                    valueText = magnitude,
                    hasMagnitude = earthquake.magnitude != null,
                    onOpenMagnitudeEducation = onMagnitudeInfo,
                    educationDescription = stringResource(R.string.learn_magnitude_info_button),
                    diameter = 150.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = place,
                        modifier = Modifier.testTag("detail_place"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = headerText
                    )
                    Text(
                        text = utcTime,
                        modifier = Modifier.testTag("detail_time_utc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = headerSecondaryText
                    )
                }
            }
        }
    }
}

/** Ficha alineada: tres cabeceras y tres valores a la misma altura. */
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

    Surface(
        modifier = modifier.fillMaxWidth().testTag("detail_essential_metrics"),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainerLow
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 325.dp || LocalConfiguration.current.fontScale > 1.25f
            if (stacked) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SeismicMetricColumn(
                        title = stringResource(R.string.detail_depth), value = depth,
                        onClick = onDepthInfo, tag = "detail_depth_info",
                        a11y = stringResource(R.string.learn_depth_info_button)
                    )
                    SeismicMetricColumn(
                        title = stringResource(R.string.detail_pilot_scale), value = magnitudeType,
                        onClick = onScaleInfo, tag = "detail_scale_info",
                        a11y = stringResource(R.string.detail_essential_scale_help_button)
                    )
                    SeismicMetricColumn(
                        title = stringResource(R.string.detail_pilot_status), value = stringResource(review),
                        onClick = null, tag = "detail_metric_status", a11y = null
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SeismicMetricColumn(
                        title = stringResource(R.string.detail_depth), value = depth,
                        onClick = onDepthInfo, tag = "detail_depth_info",
                        a11y = stringResource(R.string.learn_depth_info_button),
                        modifier = Modifier.weight(1f)
                    )
                    SeismicMetricColumn(
                        title = stringResource(R.string.detail_pilot_scale), value = magnitudeType,
                        onClick = onScaleInfo, tag = "detail_scale_info",
                        a11y = stringResource(R.string.detail_essential_scale_help_button),
                        modifier = Modifier.weight(1f)
                    )
                    SeismicMetricColumn(
                        title = stringResource(R.string.detail_pilot_status), value = stringResource(review),
                        onClick = null, tag = "detail_metric_status", a11y = null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeismicMetricColumn(
    title: String,
    value: String,
    onClick: (() -> Unit)?,
    tag: String,
    a11y: String?,
    modifier: Modifier = Modifier
) {
    val base = modifier.testTag(tag)
    val clickable = if (onClick != null) {
        base.clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .then(if (a11y != null) Modifier.semantics { contentDescription = a11y } else Modifier)
    } else base
    Column(
        modifier = clickable.heightIn(min = 54.dp).padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(painterResource(when (tag) {
            "detail_depth_info" -> R.drawable.ic_detail_depth
            "detail_scale_info" -> R.drawable.ic_detail_method
            else -> R.drawable.ic_detail_review
        }), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3
        )
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
