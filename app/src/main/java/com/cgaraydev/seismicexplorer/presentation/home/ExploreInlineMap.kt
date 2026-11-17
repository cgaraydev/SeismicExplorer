package com.cgaraydev.seismicexplorer.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.presentation.map.MapEarthquakeGeoJson
import com.cgaraydev.seismicexplorer.presentation.map.MapEventBounds
import com.cgaraydev.seismicexplorer.presentation.map.SeismicMapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraPosition

private data class PreviewMapData(
    val geoJson: String = MapEarthquakeGeoJson.EMPTY,
    val bounds: MapEventBounds? = null
)

/**
 * Vista MapLibre dentro de Explorar. Reutiliza exclusivamente los eventos
 * ya filtrados en Home: Lista y Mapa no tienen consultas independientes.
 *
 * La serializacion y el calculo de limites son trabajo de CPU y se ejecutan
 * fuera del hilo principal. Al salir de esta pestaña se libera el MapView;
 * la camara se conserva en HomeScreen para reconstruirlo al regresar.
 */
@Composable
internal fun ExploreInlineMap(
    earthquakes: List<Earthquake>,
    initialCamera: CameraPosition,
    onCameraChanged: (CameraPosition) -> Unit,
    onEarthquakeClick: (String) -> Unit,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mapData by produceState(
        initialValue = PreviewMapData(),
        key1 = earthquakes
    ) {
        value = withContext(Dispatchers.Default) {
            PreviewMapData(
                geoJson = MapEarthquakeGeoJson.encode(earthquakes),
                bounds = MapEventBounds.fromEarthquakes(earthquakes)
            )
        }
    }
    var fitRequestId by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.explore_results_count, earthquakes.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(
                onClick = onExpandClick,
                modifier = Modifier.testTag("explore_expand_map")
            ) {
                Text(stringResource(R.string.explore_expand_map))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag("explore_map_preview")
        ) {
            SeismicMapView(
                geoJson = mapData.geoJson,
                initialCamera = initialCamera,
                onCameraChanged = onCameraChanged,
                onEarthquakeClick = onEarthquakeClick,
                eventBounds = mapData.bounds,
                fitEventsRequestId = fitRequestId,
                gesturesEnabled = false
            )

            // La vista previa no captura arrastres verticales del LazyColumn.
            // Tocar el área abre la cartografía interactiva completa.
            val expandDescription = stringResource(R.string.explore_expand_map)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    // Dejar libre la atribución cartográfica en el borde inferior.
                    .padding(bottom = 34.dp)
                    .clickable(onClick = onExpandClick)
                    .testTag("explore_preview_open")
                    .semantics { contentDescription = expandDescription }
            )

            val fitDescription = stringResource(R.string.map_fit_events)
            FilledTonalIconButton(
                onClick = { fitRequestId++ },
                enabled = mapData.bounds != null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .semantics { contentDescription = fitDescription }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_map_fit_events),
                    contentDescription = null
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                text = stringResource(R.string.explore_map_hint),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
