package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cgaraydev.seismicexplorer.data.map.MapStyleProvider
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val EARTHQUAKE_SOURCE_ID = "seismic-earthquakes"
private const val EARTHQUAKE_LAYER_ID = "seismic-earthquake-circles"

@Composable
fun SeismicMapView(
    geoJson: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember(context, lifecycleOwner) {
        MapView(context).apply {
            onCreate(null)
        }
    }

    var loadedStyle by remember(mapView) {
        mutableStateOf<Style?>(null)
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(mapView) {
        var active = true

        mapView.getMapAsync { map ->
            if (active) {
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(0.0, 0.0))
                    .zoom(1.5)
                    .build()

                map.setStyle(
                    MapStyleProvider.OPEN_FREE_MAP.styleUrl
                ) { style ->
                    if (active) {
                        style.addSource(
                            GeoJsonSource(
                                EARTHQUAKE_SOURCE_ID,
                                MapEarthquakeGeoJson.EMPTY
                            )
                        )

                        style.addLayer(
                            CircleLayer(
                                EARTHQUAKE_LAYER_ID,
                                EARTHQUAKE_SOURCE_ID
                            ).withProperties(
                                PropertyFactory.circleColor("#C75A45"),
                                PropertyFactory.circleRadius(6f),
                                PropertyFactory.circleStrokeColor("#FFFFFF"),
                                PropertyFactory.circleStrokeWidth(1f),
                                PropertyFactory.circleOpacity(0.9f)
                            )
                        )

                        loadedStyle = style
                    }
                }
            }
        }

        onDispose {
            active = false
            loadedStyle = null
        }
    }

    LaunchedEffect(mapView, loadedStyle, geoJson) {
        loadedStyle
            ?.getSourceAs<GeoJsonSource>(EARTHQUAKE_SOURCE_ID)
            ?.setGeoJson(geoJson)
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)

            if (
                lifecycle.currentState.isAtLeast(
                    Lifecycle.State.RESUMED
                )
            ) {
                mapView.onPause()
            }

            if (
                lifecycle.currentState.isAtLeast(
                    Lifecycle.State.STARTED
                )
            ) {
                mapView.onStop()
            }

            mapView.onDestroy()
        }
    }
}