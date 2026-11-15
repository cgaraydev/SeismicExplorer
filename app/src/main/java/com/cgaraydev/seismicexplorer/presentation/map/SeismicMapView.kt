
package com.cgaraydev.seismicexplorer.presentation.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
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
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

private const val CLUSTER_ZOOM_INCREMENT = 2.0
private const val CLUSTER_MAX_TARGET_ZOOM = 12.0
private const val CLUSTER_ANIMATION_DURATION_MS = 450
private const val EVENT_TOUCH_RADIUS_DP = 16f

internal fun defaultSeismicCameraPosition(): CameraPosition {
    return CameraPosition.Builder()
        .target(LatLng(-15.0, -125.0))
        .zoom(2.0)
        .build()
}

@Composable
fun SeismicMapView(
    geoJson: String,
    modifier: Modifier = Modifier,
    styleBuilder: Style.Builder? = null,
    initialCamera: CameraPosition = defaultSeismicCameraPosition(),
    onEarthquakeClick: (String) -> Unit = {},
    onCameraChanged: (CameraPosition) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    key(lifecycleOwner, styleBuilder) {
        AndroidView(
            factory = { context ->
                SeismicMapHostView(
                    context = context,
                    lifecycle = lifecycleOwner.lifecycle,
                    initialCamera = initialCamera,
                    styleBuilder = styleBuilder
                        ?: Style.Builder().fromUri(
                            MapStyleProvider.OPEN_FREE_MAP.styleUrl
                        )
                )
            },
            modifier = modifier.fillMaxSize(),
            update = { host ->
                host.onEarthquakeClick = onEarthquakeClick
                host.onCameraChanged = onCameraChanged
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
    private val initialCamera: CameraPosition,
    private val styleBuilder: Style.Builder
) : FrameLayout(context) {

    private val mapView = MapView(context)

    private var mapLibreMap: MapLibreMap? = null
    private var loadedStyle: Style? = null
    private var latestGeoJson = MapEarthquakeGeoJson.EMPTY

    private var started = false
    private var resumed = false
    private var released = false

    var onEarthquakeClick: (String) -> Unit = {}
    var onCameraChanged: (CameraPosition) -> Unit = {}

    private val mapClickListener =
        MapLibreMap.OnMapClickListener { position ->
            val currentMap = mapLibreMap

            if (
                released ||
                loadedStyle == null ||
                currentMap == null
            ) {
                false
            } else {
                handleMapClick(currentMap, position)
            }
        }

    private val cameraIdleListener =
        MapLibreMap.OnCameraIdleListener {
            if (!released) {
                mapLibreMap?.cameraPosition?.let {
                    onCameraChanged(it)
                }
            }
        }

    private val lifecycleObserver =
        object : DefaultLifecycleObserver {

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
        mapView.getMapAsync { readyMap ->
            if (released) {
                return@getMapAsync
            }

            mapLibreMap = readyMap
            readyMap.cameraPosition = initialCamera

            readyMap.addOnMapClickListener(mapClickListener)
            readyMap.addOnCameraIdleListener(cameraIdleListener)

            readyMap.setStyle(styleBuilder) { style ->
                if (!released) {
                    MapEarthquakeLayers.install(
                        style = style,
                        geoJson = latestGeoJson
                    )

                    loadedStyle = style
                }
            }
        }
    }

    private fun handleMapClick(
        map: MapLibreMap,
        position: LatLng
    ): Boolean {
        val screenPoint = map.projection
            .toScreenLocation(position)

        // Priorizar clusters cuando un grupo está visible.
        val cluster = map.queryRenderedFeatures(
            screenPoint,
            MapEarthquakeLayers.CLUSTER_LAYER_ID
        ).firstOrNull()

        if (cluster != null) {
            expandCluster(map, cluster, position)
            return true
        }

        // Ampliar ligeramente la superficie táctil de
        // los círculos individuales.
        val radius = EVENT_TOUCH_RADIUS_DP *
                resources.displayMetrics.density

        val touchBounds = RectF(
            screenPoint.x - radius,
            screenPoint.y - radius,
            screenPoint.x + radius,
            screenPoint.y + radius
        )

        val event = map.queryRenderedFeatures(
            touchBounds,
            *MapEarthquakeLayers.EVENT_LAYER_IDS
        )
            .filter { it.hasProperty("eventId") }
            .minByOrNull { feature ->
                distanceSquared(
                    map = map,
                    feature = feature,
                    x = screenPoint.x,
                    y = screenPoint.y
                )
            }

        val eventId = event
            ?.getStringProperty("eventId")
            ?.takeIf { it.isNotBlank() }

        if (eventId != null) {
            // Guardar el encuadre antes de navegar.
            onCameraChanged(map.cameraPosition)
            onEarthquakeClick(eventId)
            return true
        }

        return false
    }

    private fun expandCluster(
        map: MapLibreMap,
        cluster: Feature,
        tappedPosition: LatLng
    ) {
        val point = cluster.geometry() as? Point

        val center = if (point != null) {
            LatLng(point.latitude(), point.longitude())
        } else {
            tappedPosition
        }

        val targetZoom = (
                map.cameraPosition.zoom +
                        CLUSTER_ZOOM_INCREMENT
                ).coerceAtMost(CLUSTER_MAX_TARGET_ZOOM)

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                center,
                targetZoom
            ),
            CLUSTER_ANIMATION_DURATION_MS
        )
    }

    private fun distanceSquared(
        map: MapLibreMap,
        feature: Feature,
        x: Float,
        y: Float
    ): Float {
        val point = feature.geometry() as? Point
            ?: return Float.MAX_VALUE

        val projected = map.projection.toScreenLocation(
            LatLng(point.latitude(), point.longitude())
        )

        val dx = projected.x - x
        val dy = projected.y - y

        return dx * dx + dy * dy
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

        mapLibreMap?.let { map ->
            map.removeOnMapClickListener(mapClickListener)
            map.removeOnCameraIdleListener(cameraIdleListener)
        }

        mapLibreMap = null

        stopMap()

        loadedStyle = null
        onEarthquakeClick = {}
        onCameraChanged = {}

        mapView.onDestroy()
    }
}
