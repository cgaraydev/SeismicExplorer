package com.cgaraydev.seismicexplorer.presentation.map

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.RectF
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cgaraydev.seismicexplorer.R
import com.cgaraydev.seismicexplorer.data.map.MapStyleProvider
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
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
private const val CAMERA_RESET_ANIMATION_DURATION_MS = 600
private const val FIT_CAMERA_ANIMATION_DURATION_MS = 600
private const val FIT_CAMERA_PADDING_DP = 48f
private const val SINGLE_LOCATION_ZOOM = 9.0
private const val USER_LOCATION_ZOOM = 8.0
private const val USER_LOCATION_ANIMATION_MS = 600
private const val TAG = "SeismicMapView"

private enum class MapStyleLoadState {
    LOADING,
    READY,
    FAILED
}

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
    onCameraChanged: (CameraPosition) -> Unit = {},
    resetCameraRequestId: Int = 0,
    eventBounds: MapEventBounds? = null,
    fitEventsRequestId: Int = 0,
    userPosition: LatLng? = null,
    centerUserRequestId: Int = 0
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    key(lifecycleOwner, styleBuilder) {
        var styleLoadState by remember {
            mutableStateOf(MapStyleLoadState.LOADING)
        }
        var retryStyleRequestId by remember {
            mutableIntStateOf(0)
        }

        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    SeismicMapHostView(
                        context = context,
                        lifecycle = lifecycleOwner.lifecycle,
                        initialCamera = initialCamera,
                        styleBuilder = styleBuilder
                            ?: Style.Builder().fromUri(
                                MapStyleProvider.OPEN_FREE_MAP.styleUrl
                            ),
                        initialResetCameraRequestId = resetCameraRequestId,
                        initialFitEventsRequestId = fitEventsRequestId,
                        onStyleStateChanged = { styleLoadState = it }
                    )
                },
                modifier = Modifier.fillMaxSize(),
                update = { host ->
                    host.onEarthquakeClick = onEarthquakeClick
                    host.onCameraChanged = onCameraChanged
                    host.updateGeoJson(geoJson)
                    host.resetCameraIfRequested(resetCameraRequestId)
                    host.fitEventsIfRequested(
                        requestId = fitEventsRequestId,
                        bounds = eventBounds
                    )
                    host.centerOnUserIfRequested(
                        requestId = centerUserRequestId,
                        position = userPosition
                    )
                    host.retryStyleIfRequested(retryStyleRequestId)
                },
                onRelease = { host ->
                    host.release()
                }
            )

            if (styleLoadState == MapStyleLoadState.FAILED) {
                MapStyleFailureMessage(
                    modifier = Modifier.align(Alignment.Center),
                    onRetry = { retryStyleRequestId++ }
                )
            }
        }
    }
}

@Composable
internal fun MapStyleFailureMessage(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(20.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.map_style_load_failed),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.map_style_retry))
            }
        }
    }
}

@SuppressLint("ViewConstructor")
private class SeismicMapHostView(
    context: Context,
    private val lifecycle: Lifecycle,
    private val initialCamera: CameraPosition,
    private val styleBuilder: Style.Builder,
    private val initialResetCameraRequestId: Int,
    private val initialFitEventsRequestId: Int,
    private val onStyleStateChanged: (MapStyleLoadState) -> Unit
) : FrameLayout(context) {

    private val mapView = MapView(context)
    private val callbacksContext = context.applicationContext ?: context

    private var mapLibreMap: MapLibreMap? = null
    private var loadedStyle: Style? = null
    private var latestGeoJson = MapEarthquakeGeoJson.EMPTY
    private var isStyleLoading = false
    private var styleRequestGeneration = 0
    private var lastRetryStyleRequestId = 0

    private var started = false
    private var resumed = false
    private var released = false

    var onEarthquakeClick: (String) -> Unit = {}
    var onCameraChanged: (CameraPosition) -> Unit = {}
    private var lastResetCameraRequestId = initialResetCameraRequestId
    private val pendingCameraCommands = MapCameraCommandCoordinator()

    private var lastFitEventsRequestId = initialFitEventsRequestId
    private var pendingFitBounds: MapEventBounds? = null

    private var lastUserCenterRequestId = 0
    private var pendingUserCenter: LatLng? = null

    private val mapFailureListener =
        MapView.OnDidFailLoadingMapListener { message ->
            // Solo tratamos como fallo de estilo un error ocurrido
            // mientras se está cargando el estilo. No confundimos
            // fallos de teselas posteriores con una carga fallida.
            if (!released && isStyleLoading && loadedStyle == null) {
                Log.w(TAG, "MapLibre style load failed: $message")
                isStyleLoading = false
                onStyleStateChanged(MapStyleLoadState.FAILED)
            }
        }

    private val mapClickListener =
        MapLibreMap.OnMapClickListener { position ->
            val currentMap = mapLibreMap

            if (released || loadedStyle == null || currentMap == null) {
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

    // El MapView recibe presion de memoria incluso si vive en AndroidView.
    // Se registra en Application y se desregistra al liberar el host.
    private val memoryCallbacks = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) = Unit

        override fun onLowMemory() {
            forwardLowMemory()
        }

        @Suppress("DEPRECATION")
        override fun onTrimMemory(level: Int) {
            // Los niveles RUNNING_* de Android antiguos estan deprecados,
            // pero siguen siendo utiles en minSdk 29; incluye UI_HIDDEN.
            if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                forwardLowMemory()
            }
        }
    }

    private fun forwardLowMemory() {
        if (!released) mapView.onLowMemory()
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
        mapView.addOnDidFailLoadingMapListener(mapFailureListener)
        callbacksContext.registerComponentCallbacks(memoryCallbacks)
        lifecycle.addObserver(lifecycleObserver)

        if (!released) {
            initializeMap()
        }
    }

    private fun initializeMap() {
        mapView.getMapAsync { readyMap ->
            if (released) return@getMapAsync

            mapLibreMap = readyMap
            readyMap.cameraPosition = initialCamera
            applyPendingResetCamera()
            applyPendingFitIfReady()
            applyPendingUserCenter()

            readyMap.addOnMapClickListener(mapClickListener)
            readyMap.addOnCameraIdleListener(cameraIdleListener)
            loadStyle()
        }
    }

    private fun loadStyle() {
        if (released) return

        val map = mapLibreMap ?: return
        val generation = ++styleRequestGeneration
        loadedStyle = null
        isStyleLoading = true
        onStyleStateChanged(MapStyleLoadState.LOADING)

        try {
            map.setStyle(styleBuilder) { style ->
                if (released || generation != styleRequestGeneration) {
                    return@setStyle
                }

                try {
                    // El estilo nuevo requiere su propia fuente y capas.
                    // Usamos el último GeoJSON guardado mientras cargaba.
                    MapEarthquakeLayers.install(
                        style = style,
                        geoJson = latestGeoJson
                    )
                    loadedStyle = style
                    isStyleLoading = false
                    onStyleStateChanged(MapStyleLoadState.READY)
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to install seismic layers", exception)
                    loadedStyle = null
                    isStyleLoading = false
                    onStyleStateChanged(MapStyleLoadState.FAILED)
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to start MapLibre style load", exception)
            loadedStyle = null
            isStyleLoading = false
            onStyleStateChanged(MapStyleLoadState.FAILED)
        }
    }

    fun retryStyleIfRequested(requestId: Int) {
        if (released || requestId <= lastRetryStyleRequestId) return

        lastRetryStyleRequestId = requestId
        loadStyle()
    }

    private fun handleMapClick(
        map: MapLibreMap,
        position: LatLng
    ): Boolean {
        val screenPoint = map.projection.toScreenLocation(position)

        val cluster = map.queryRenderedFeatures(
            screenPoint,
            MapEarthquakeLayers.CLUSTER_LAYER_ID
        ).firstOrNull()

        if (cluster != null) {
            expandCluster(map, cluster, position)
            return true
        }

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
            map.cameraPosition.zoom + CLUSTER_ZOOM_INCREMENT
        ).coerceAtMost(CLUSTER_MAX_TARGET_ZOOM)

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(center, targetZoom),
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
        if (released || geoJson == latestGeoJson) return

        latestGeoJson = geoJson

        loadedStyle
            ?.getSourceAs<GeoJsonSource>(
                MapEarthquakeLayers.SOURCE_ID
            )
            ?.setGeoJson(geoJson)
    }

    private fun startMap() {
        if (released || started) return
        mapView.onStart()
        started = true
    }

    private fun resumeMap() {
        if (released || resumed) return

        if (!started) startMap()

        mapView.onResume()
        resumed = true
    }

    // Guarda una instantánea incluso si la cámara estaba en movimiento
    // y todavía no se había recibido OnCameraIdleListener.
    private fun saveCurrentCamera() {
        if (released) return
        mapLibreMap?.cameraPosition?.let(onCameraChanged)
    }

    private fun pauseMap() {
        if (!resumed) return
        saveCurrentCamera()
        mapView.onPause()
        resumed = false
    }

    private fun stopMap() {
        pauseMap()
        if (!started) return
        mapView.onStop()
        started = false
    }

    fun release() {
        if (released) return
        saveCurrentCamera()
        released = true

        lifecycle.removeObserver(lifecycleObserver)
        callbacksContext.unregisterComponentCallbacks(memoryCallbacks)
        mapView.removeOnDidFailLoadingMapListener(mapFailureListener)

        mapLibreMap?.let { map ->
            map.removeOnMapClickListener(mapClickListener)
            map.removeOnCameraIdleListener(cameraIdleListener)
        }

        mapLibreMap = null
        loadedStyle = null
        isStyleLoading = false
        pendingFitBounds = null
        pendingUserCenter = null
        pendingCameraCommands.clear()

        stopMap()

        onEarthquakeClick = {}
        onCameraChanged = {}

        mapView.onDestroy()
    }

    fun resetCameraIfRequested(requestId: Int) {
        if (released || requestId == lastResetCameraRequestId) return

        lastResetCameraRequestId = requestId
        pendingFitBounds = null
        pendingUserCenter = null
        pendingCameraCommands.request(MapCameraCommandKind.RESET)
        applyPendingResetCamera()
    }

    private fun applyPendingResetCamera() {
        val map = mapLibreMap ?: return
        if (!pendingCameraCommands.consume(MapCameraCommandKind.RESET)) return
        animateResetCamera(map)
    }

    private fun animateResetCamera(map: MapLibreMap) {
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                defaultSeismicCameraPosition()
            ),
            CAMERA_RESET_ANIMATION_DURATION_MS
        )
    }

    fun fitEventsIfRequested(
        requestId: Int,
        bounds: MapEventBounds?
    ) {
        if (released || requestId == lastFitEventsRequestId) return

        lastFitEventsRequestId = requestId
        if (bounds == null) return

        pendingUserCenter = null
        pendingFitBounds = bounds
        pendingCameraCommands.request(MapCameraCommandKind.FIT_EVENTS)
        applyPendingFitIfReady()
    }

    private fun applyPendingFitIfReady() {
        val map = mapLibreMap ?: return
        if (!pendingCameraCommands.isPending(MapCameraCommandKind.FIT_EVENTS)) return

        mapView.doOnLayout { view ->
            if (released || mapLibreMap !== map) return@doOnLayout

            val bounds = pendingFitBounds ?: return@doOnLayout
            if (view.width <= 0 || view.height <= 0) {
                return@doOnLayout
            }
            if (!pendingCameraCommands.consume(MapCameraCommandKind.FIT_EVENTS)) {
                return@doOnLayout
            }

            pendingFitBounds = null

            val cameraUpdate = if (bounds.isSingleLocation) {
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(bounds.south, bounds.west),
                    SINGLE_LOCATION_ZOOM
                )
            } else {
                val geographicalBounds = LatLngBounds.from(
                    bounds.north,
                    bounds.east,
                    bounds.south,
                    bounds.west
                )

                val paddingPx = (
                    FIT_CAMERA_PADDING_DP *
                        resources.displayMetrics.density
                ).toInt()

                CameraUpdateFactory.newLatLngBounds(
                    geographicalBounds,
                    0.0,
                    0.0,
                    paddingPx
                )
            }

            map.animateCamera(
                cameraUpdate,
                FIT_CAMERA_ANIMATION_DURATION_MS
            )
        }
    }

    fun centerOnUserIfRequested(
        requestId: Int,
        position: LatLng?
    ) {
        if (
            released ||
            requestId <= 0 ||
            requestId == lastUserCenterRequestId
        ) {
            return
        }

        if (position == null) return

        lastUserCenterRequestId = requestId
        pendingFitBounds = null
        pendingUserCenter = position
        pendingCameraCommands.request(MapCameraCommandKind.CENTER_USER)

        applyPendingUserCenter()
    }

    private fun applyPendingUserCenter() {
        if (released) return

        val map = mapLibreMap ?: return
        val position = pendingUserCenter ?: return
        if (!pendingCameraCommands.consume(MapCameraCommandKind.CENTER_USER)) return
        pendingUserCenter = null

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                position,
                USER_LOCATION_ZOOM
            ),
            USER_LOCATION_ANIMATION_MS
        )
    }
}
