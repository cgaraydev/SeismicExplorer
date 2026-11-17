package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.presentation.common.AtlasWaveformIcon
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun EditorialMasthead() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        .testTag("editorial_header"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasWaveformIcon(Modifier.size(30.dp, 22.dp))
            Text(stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleSmall)
        }
        Text(stringResource(R.string.editorial_title),
            style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.editorial_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
    }
}

@Composable
internal fun EditorialFeaturedEvent(earthquake: Earthquake, onOpen: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale).withZone(ZoneId.systemDefault()).format(earthquake.occurredAt)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        .testTag("editorial_featured"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.editorial_featured_label),
            style = MaterialTheme.typography.labelLarge, color = colors.secondary)
        // Arte decorativo: no representa la localización del evento.
        // Con texto grande se apila para que la imagen no comprima los datos.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 300.dp || LocalConfiguration.current.fontScale > 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeaturedText(earthquake)
                    FeaturedArtwork(Modifier.fillMaxWidth().height(132.dp))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1.15f)) { FeaturedText(earthquake) }
                    FeaturedArtwork(Modifier.weight(1f).height(172.dp))
                }
            }
        }
        Text(date, style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
        TextButton(onClick = { onOpen(earthquake.id) },
            modifier = Modifier.testTag("editorial_open_featured")) {
            Text(stringResource(R.string.editorial_open_event))
        }
        HorizontalDivider(color = colors.outlineVariant)
    }
}

@Composable
private fun FeaturedText(earthquake: Earthquake) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(earthquake.magnitude?.let {
            stringResource(R.string.home_event_magnitude, it)
        } ?: stringResource(R.string.home_event_magnitude_unknown),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.secondary)
        Text(earthquake.place ?: stringResource(R.string.home_event_place_unknown),
            style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun FeaturedArtwork(modifier: Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier.clip(RoundedCornerShape(12.dp))) {
        Image(painterResource(R.drawable.atlas_mineral_explore),
            contentDescription = null, contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(background.copy(alpha = 0.30f), background.copy(alpha = 0.04f)))))
        Surface(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
            color = background.copy(alpha = 0.94f), shape = RoundedCornerShape(6.dp)) {
            Text(stringResource(R.string.editorial_decorative_art),
                Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun EditorialActions(onMapClick: () -> Unit, onLearnClick: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        if (maxWidth < 320.dp || LocalConfiguration.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorialActionTile(true, onMapClick, Modifier.fillMaxWidth())
                EditorialActionTile(false, onLearnClick, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorialActionTile(true, onMapClick, Modifier.weight(1f))
                EditorialActionTile(false, onLearnClick, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EditorialActionTile(isMap: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick,
        modifier = modifier.testTag(if (isMap) "editorial_open_map" else "editorial_learn"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor =
            if (isMap) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtlasLines(isMap, Modifier.fillMaxWidth().height(45.dp))
            Text(stringResource(if (isMap) R.string.editorial_open_atlas
                else R.string.editorial_understand), style = MaterialTheme.typography.titleMedium,
                color = if (isMap) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer)
            Text(stringResource(if (isMap) R.string.editorial_map_hint
                else R.string.editorial_learn_hint), style = MaterialTheme.typography.bodySmall,
                color = if (isMap) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun AtlasLines(isMap: Boolean, modifier: Modifier) {
    val color = if (isMap) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onTertiaryContainer
    // Motivo editorial abstracto; no representa relieve ni ondas medidas.
    Canvas(modifier) {
        for (line in 0..4) {
            val y = size.height * (0.12f + line * 0.15f)
            val path = Path().apply {
                moveTo(0f, y)
                cubicTo(size.width * 0.25f, y + size.height * 0.30f,
                    size.width * 0.65f, y - size.height * (if (isMap) 0.22f else 0.10f),
                    size.width, y + size.height * 0.12f)
            }
            drawPath(path, color.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
internal fun EditorialHourlyPreview() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.editorial_demo_accessibility)
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .testTag("editorial_chart"), shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.editorial_hourly_title),
                style = MaterialTheme.typography.titleLarge)
            Surface(color = colors.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                Text(stringResource(R.string.editorial_demo_badge),
                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    color = colors.onSecondaryContainer,
                    style = MaterialTheme.typography.labelMedium)
            }
            Text(stringResource(R.string.editorial_demo_description),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
            if (expanded) {
                // Datos fijos de diseño, aislados del estado de eventos y de los filtros.
                Canvas(Modifier.fillMaxWidth().height(112.dp).clearAndSetSemantics {
                    contentDescription = description
                }) {
                    val bars = listOf(3f, 5f, 4f, 7f, 6f, 9f, 8f, 4f, 5f, 11f, 13f, 9f,
                        7f, 10f, 8f, 5f, 6f, 12f, 15f, 11f, 8f, 6f, 4f, 5f)
                    val bottom = size.height - 2.dp.toPx()
                    val step = size.width / bars.size
                    for (i in 1..3) {
                        val y = bottom * i / 4f
                        drawLine(colors.outlineVariant, Offset(0f, y), Offset(size.width, y),
                            strokeWidth = 1.dp.toPx())
                    }
                    bars.forEachIndexed { index, value ->
                        val h = bottom * value / 18f
                        drawRect(if (index == 18) colors.secondary else colors.primary,
                            Offset(index * step + step * 0.15f, bottom - h),
                            Size(step * 0.7f, h))
                    }
                    drawLine(colors.outlineVariant, Offset(0f, bottom),
                        Offset(size.width, bottom), 1.dp.toPx())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("00 h", "06 h", "12 h", "18 h", "24 h").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant)
                    }
                }
            }
            TextButton(onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.End).testTag("editorial_chart_toggle")) {
                Text(stringResource(if (expanded) R.string.editorial_collapse_demo
                    else R.string.editorial_expand_demo))
            }
        }
    }
}

@Composable
internal fun EditorialLearningDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        modifier = Modifier.testTag("editorial_learning_dialog"),
        title = { Text(stringResource(R.string.editorial_understand)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.editorial_learn_magnitude))
                Text(stringResource(R.string.editorial_learn_depth))
                Text(stringResource(R.string.editorial_learn_intensity))
                Text(stringResource(R.string.editorial_learn_source),
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.editorial_close))
        } })
}
