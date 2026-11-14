
package com.cgaraydev.seismicexplorer.presentation.map

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cgaraydev.seismicexplorer.data.map.MapStyleProvider
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import android.util.Log
import android.annotation.SuppressLint

@Composable
fun SeismicMapView(
    geoJson: String,
    modifier: Modifier = Modifier,
    styleBuilder: Style.Builder? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    key(lifecycleOwner, styleBuilder) {
        AndroidView(
            factory = { context ->
                SeismicMapHostView(
                    context = context,
                    lifecycle = lifecycleOwner.lifecycle,
                    styleBuilder = styleBuilder
                        ?: Style.Builder().fromUri(
                            MapStyleProvider.OPEN_FREE_MAP.styleUrl
                        )
                )
            },
            modifier = modifier.fillMaxSize(),
            update = { host ->
                host.updateGeoJson(geoJson)
            },
            onRelease = { host ->
                host.release()
            }
        )
    }
}

@SuppressLint("ViewConstructor")
private class SeismicMapHostView(
    context: Context,
    private val lifecycle: Lifecycle,
    private val styleBuilder: Style.Builder
) : FrameLayout(context) {

    private val mapView = MapView(context)

    private var loadedStyle: Style? = null
    private var latestGeoJson = MapEarthquakeGeoJson.EMPTY

    private var started = false
    private var resumed = false
    private var released = false

    private val lifecycleObserver = object : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            startMap()
        }

        override fun onResume(owner: LifecycleOwner) {
            resumeMap()
        }

        override fun onPause(owner: LifecycleOwner) {
            pauseMap()
        }

        override fun onStop(owner: LifecycleOwner) {
            stopMap()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            release()
        }
    }

    init {
        addView(
            mapView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        mapView.onCreate(null)

        lifecycle.addObserver(lifecycleObserver)

        if (!released) {
            initializeMap()
        }
    }

    private fun initializeMap() {
        mapView.getMapAsync { map ->
            if (released) {
                return@getMapAsync
            }

            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(-15.0, -125.0))
                .zoom(2.0)
                .build()

            map.setStyle(styleBuilder) { style ->
                if (!released) {
                    MapEarthquakeLayers.install(
                        style = style,
                        geoJson = latestGeoJson
                    )

                    loadedStyle = style
                }
            }

            map.addOnMapClickListener {
                val features = loadedStyle
                    ?.getSourceAs<GeoJsonSource>(
                        MapEarthquakeLayers.SOURCE_ID
                    )
                    ?.querySourceFeatures(null)
                    .orEmpty()

                val clusters = features.count { feature ->
                    feature.hasProperty("point_count")
                }

                val individual = features.size - clusters

                Log.d(
                    "SeismicMapDiag",
                    "features=${features.size}, " +
                            "clusters=$clusters, " +
                            "individual=$individual"
                )

                false
            }
        }
    }

    fun updateGeoJson(geoJson: String) {
        if (released || geoJson == latestGeoJson) {
            return
        }

        latestGeoJson = geoJson

        loadedStyle
            ?.getSourceAs<GeoJsonSource>(
                MapEarthquakeLayers.SOURCE_ID
            )
            ?.setGeoJson(geoJson)
    }

    private fun startMap() {
        if (released || started) {
            return
        }

        mapView.onStart()
        started = true
    }

    private fun resumeMap() {
        if (released || resumed) {
            return
        }

        if (!started) {
            startMap()
        }

        mapView.onResume()
        resumed = true
    }

    private fun pauseMap() {
        if (!resumed) {
            return
        }

        mapView.onPause()
        resumed = false
    }

    private fun stopMap() {
        pauseMap()

        if (!started) {
            return
        }

        mapView.onStop()
        started = false
    }

    fun release() {
        if (released) {
            return
        }

        released = true

        lifecycle.removeObserver(lifecycleObserver)

        stopMap()

        loadedStyle = null

        mapView.onDestroy()
    }
}
