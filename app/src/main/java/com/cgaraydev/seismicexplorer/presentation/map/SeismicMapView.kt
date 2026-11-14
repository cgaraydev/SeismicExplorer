package com.cgaraydev.seismicexplorer.presentation.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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

@Composable
fun SeismicMapView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember(context, lifecycleOwner) {
        MapView(context).apply {
            onCreate(null)
        }
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
                )
            }
        }

        onDispose {
            active = false
        }
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

            if (lifecycle.currentState.isAtLeast(
                    Lifecycle.State.RESUMED
                )
            ) {
                mapView.onPause()
            }

            if (lifecycle.currentState.isAtLeast(
                    Lifecycle.State.STARTED
                )
            ) {
                mapView.onStop()
            }

            mapView.onDestroy()
        }
    }
}