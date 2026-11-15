package com.cgaraydev.seismicexplorer.presentation.map

import android.graphics.RectF
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.junit.Assert.assertSame

class SeismicMapNativeClusteringTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tenNearbyEarthquakes_formClusterWithTenUniqueLeaves() {
        val earthquakes = createSyntheticEarthquakes()
        val expectedIds = earthquakes.map { it.id }.toSet()
        val geoJson = MapEarthquakeGeoJson.encode(earthquakes)

        var source: GeoJsonSource? = null
        var nativeMap: MapLibreMap? = null
        var nativeView: MapView? = null

        var released = false
        var styleReady = false

        var sourceFeatureCount = 0
        var sourceClusterSizes = emptyList<Int>()
        var renderedClusterSizes = emptyList<Int>()
        var viewSize = "sin vista"

        var foundCluster: Feature? = null

        var renderedFrames = 0
        var sourceChanges = 0

        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        // MapLibre debe inicializarse antes de crear MapView.
        composeRule.runOnUiThread {
            MapLibre.getInstance(context)
        }

        composeRule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    MapView(viewContext).apply {
                        nativeView = this


                        addOnDidFinishRenderingFrameListener { _, _, _ ->
                            renderedFrames++
                        }

                        addOnSourceChangedListener { sourceId ->
                            if (sourceId == MapEarthquakeLayers.SOURCE_ID) {
                                sourceChanges++
                            }
                        }


                        onCreate(null)
                        onStart()
                        onResume()

                        getMapAsync { map ->
                            if (!released) {
                                nativeMap = map

                                map.cameraPosition =
                                    CameraPosition.Builder()
                                        .target(
                                            LatLng(
                                                -33.45,
                                                -71.62
                                            )
                                        )
                                        .zoom(3.0)
                                        .build()

                                map.setStyle(
                                    Style.Builder().fromJson(
                                        LOCAL_STYLE_JSON
                                    )
                                ) { style ->
                                    if (!released) {

                                        MapEarthquakeLayers.install(
                                            style = style,
                                            geoJson = geoJson
                                        )

                                        style.removeLayer("seismic-cluster-count")

                                        source = style.getSourceAs(
                                            MapEarthquakeLayers.SOURCE_ID
                                        )

                                        styleReady = true

                                    }
                                }
                            }
                        }
                    }
                },
                onRelease = { mapView ->
                    released = true

                    source = null
                    nativeMap = null
                    nativeView = null

                    mapView.onPause()
                    mapView.onStop()
                    mapView.onDestroy()
                }
            )
        }

        // La fuente y las teselas se procesan de forma
        // asíncrona. Esperamos hasta detectar un cluster.
        try {
            composeRule.waitUntil(
                timeoutMillis = CLUSTER_TIMEOUT_MS
            ) {
                composeRule.runOnUiThread {
                    val currentSource = source
                    val currentMap = nativeMap
                    val currentView = nativeView

                    if (currentView != null) {
                        viewSize =
                            "${currentView.width}x${currentView.height}"
                    }

                    if (
                        currentSource == null ||
                        currentMap == null ||
                        currentView == null ||
                        !styleReady ||
                        currentView.width <= 0 ||
                        currentView.height <= 0
                    ) {
                        false
                    } else {
                        // Consultar las features de las
                        // teselas cargadas por la fuente.
                        val sourceFeatures =
                            currentSource.querySourceFeatures(null)

                        sourceFeatureCount = sourceFeatures.size

                        val sourceClusters =
                            sourceFeatures.filter { feature ->
                                feature.hasProperty("point_count")
                            }

                        sourceClusterSizes =
                            sourceClusters.map { feature ->
                                feature.getNumberProperty(
                                    "point_count"
                                ).toInt()
                            }

                        // Consultar los clusters que el
                        // renderer está dibujando.
                        val renderedClusters =
                            currentMap.queryRenderedFeatures(
                                RectF(
                                    0f,
                                    0f,
                                    currentView.width.toFloat(),
                                    currentView.height.toFloat()
                                ),
                                MapEarthquakeLayers.CLUSTER_LAYER_ID
                            )

                        renderedClusterSizes =
                            renderedClusters.mapNotNull { feature ->
                                if (
                                    feature.hasProperty(
                                        "point_count"
                                    )
                                ) {
                                    feature.getNumberProperty(
                                        "point_count"
                                    ).toInt()
                                } else {
                                    null
                                }
                            }

                        // Priorizar un cluster renderizado.
                        // Si no aparece, comprobar los de
                        // la fuente cartográfica.
                        foundCluster =
                            renderedClusters.firstOrNull { feature ->
                                feature.hasProperty("point_count") &&
                                        feature.getNumberProperty(
                                            "point_count"
                                        ).toInt() == 10
                            } ?: sourceClusters.firstOrNull { feature ->
                                feature.getNumberProperty(
                                    "point_count"
                                ).toInt() == 10
                            }

                        foundCluster != null
                    }
                }
            }
        } catch (exception: ComposeTimeoutException) {
            val diagnostics = composeRule.runOnUiThread {
                """
                No apareció un cluster de 10 eventos.

                styleReady=$styleReady
                sourceAvailable=${source != null}
                mapAvailable=${nativeMap != null}
                viewSize=$viewSize
                sourceFeatureCount=$sourceFeatureCount
                sourceClusterSizes=$sourceClusterSizes
                renderedClusterSizes=$renderedClusterSizes
                renderedFrames=${'$'}renderedFrames
                sourceChanges=${'$'}sourceChanges
                renderSurfaceAttached=${'$'}{nativeView?.renderView?.isAttachedToWindow}
                renderSurfaceSize=${'$'}{nativeView?.renderView?.width}x${'$'}{nativeView?.renderView?.height}
                cameraPosition=${'$'}{nativeMap?.cameraPosition}
                """.trimIndent()
            }

            throw AssertionError(
                diagnostics,
                exception
            )
        }

        // Consultar inmediatamente las hojas del cluster
        // encontrado. No actualizar GeoJSON entre ambas
        // operaciones, para no invalidar su cluster_id.
        composeRule.runOnUiThread {
            val currentSource = checkNotNull(source) {
                "GeoJsonSource no está disponible"
            }

            val cluster = checkNotNull(foundCluster) {
                "No se conservó el cluster observado"
            }

            assertEquals(
                10,
                cluster.getNumberProperty(
                    "point_count"
                ).toInt()
            )

            val leaves = currentSource
                .getClusterLeaves(
                    cluster,
                    10L,
                    0L
                )
                .features()
                .orEmpty()

            val actualIds = leaves.map { feature ->
                feature.getStringProperty("eventId")
            }

            assertEquals(
                "El cluster debe contener 10 hojas",
                10,
                leaves.size
            )

            assertEquals(
                "Los IDs deben coincidir con los originales",
                expectedIds,
                actualIds.toSet()
            )

            assertEquals(
                "No debe haber IDs duplicados",
                10,
                actualIds.toSet().size
            )
        }
    }


    @Test
    fun geoJsonUpdates_replaceClustersWithoutRecreatingMap() {
        val tenEarthquakes = createSyntheticEarthquakes()
        val fourEarthquakes = tenEarthquakes.take(4)

        val expectedTenIds = tenEarthquakes.map { it.id }.toSet()
        val expectedFourIds = fourEarthquakes.map { it.id }.toSet()

        val tenGeoJson = MapEarthquakeGeoJson.encode(tenEarthquakes)
        val fourGeoJson = MapEarthquakeGeoJson.encode(fourEarthquakes)

        var source: GeoJsonSource? = null
        var nativeMap: MapLibreMap? = null
        var nativeView: MapView? = null

        var released = false
        var styleInstallCount = 0

        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        composeRule.runOnUiThread {
            MapLibre.getInstance(context)
        }

        composeRule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    MapView(viewContext).apply {
                        nativeView = this

                        onCreate(null)
                        onStart()
                        onResume()

                        getMapAsync { map ->
                            if (!released) {
                                nativeMap = map

                                map.cameraPosition =
                                    CameraPosition.Builder()
                                        .target(LatLng(-33.45, -71.62))
                                        .zoom(3.0)
                                        .build()

                                map.setStyle(
                                    Style.Builder().fromJson(
                                        LOCAL_STYLE_JSON
                                    )
                                ) { style ->
                                    if (!released) {
                                        MapEarthquakeLayers.install(
                                            style = style,
                                            geoJson = MapEarthquakeGeoJson.EMPTY
                                        )

                                        // El estilo local no declara glyphs.
                                        // Esta prueba no necesita etiquetas.
                                        style.removeLayer(
                                            "seismic-cluster-count"
                                        )

                                        source = style.getSourceAs(
                                            MapEarthquakeLayers.SOURCE_ID
                                        )

                                        styleInstallCount++
                                    }
                                }
                            }
                        }
                    }
                },
                onRelease = { mapView ->
                    released = true
                    source = null
                    nativeMap = null
                    nativeView = null

                    mapView.onPause()
                    mapView.onStop()
                    mapView.onDestroy()
                }
            )
        }

        // Esperar a que el mapa y la fuente estén disponibles.
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.runOnUiThread {
                source != null &&
                        nativeMap != null &&
                        (nativeView?.width ?: 0) > 0 &&
                        (nativeView?.height ?: 0) > 0
            }
        }

        val initialSource = composeRule.runOnUiThread {
            checkNotNull(source)
        }

        // Estado inicial: cero terremotos.
        composeRule.runOnUiThread {
            assertEquals(
                0,
                initialSource.querySourceFeatures(null).size
            )

            initialSource.setGeoJson(tenGeoJson)
        }

        // Transición 0 -> 10.
        awaitClusterWithIds(
            sourceProvider = { source },
            mapProvider = { nativeMap },
            viewProvider = { nativeView },
            expectedIds = expectedTenIds
        )

        // Transición 10 -> 4 usando la misma fuente.
        composeRule.runOnUiThread {
            assertEquals(1, styleInstallCount)
            assertSame(initialSource, source)

            initialSource.setGeoJson(fourGeoJson)
        }

        awaitClusterWithIds(
            sourceProvider = { source },
            mapProvider = { nativeMap },
            viewProvider = { nativeView },
            expectedIds = expectedFourIds
        )

        // La fuente y el estilo no deben haberse reinstalado.
        composeRule.runOnUiThread {
            assertEquals(1, styleInstallCount)
            assertSame(initialSource, source)
        }
    }

    private fun awaitClusterWithIds(
        sourceProvider: () -> GeoJsonSource?,
        mapProvider: () -> MapLibreMap?,
        viewProvider: () -> MapView?,
        expectedIds: Set<String>
    ) {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.runOnUiThread {
                val currentSource = sourceProvider()
                    ?: return@runOnUiThread false

                val currentMap = mapProvider()
                    ?: return@runOnUiThread false

                val currentView = viewProvider()
                    ?: return@runOnUiThread false

                if (
                    currentView.width <= 0 ||
                    currentView.height <= 0
                ) {
                    return@runOnUiThread false
                }

                val bounds = RectF(
                    0f,
                    0f,
                    currentView.width.toFloat(),
                    currentView.height.toFloat()
                )

                val features =
                    currentSource.querySourceFeatures(null) +
                            currentMap.queryRenderedFeatures(
                                bounds,
                                MapEarthquakeLayers.CLUSTER_LAYER_ID
                            )

                features.any { feature ->
                    if (
                        !feature.hasProperty("point_count") ||
                        feature.getNumberProperty(
                            "point_count"
                        ).toInt() != expectedIds.size
                    ) {
                        false
                    } else {
                        val leaves = currentSource
                            .getClusterLeaves(
                                feature,
                                expectedIds.size.toLong(),
                                0L
                            )
                            .features()
                            .orEmpty()

                        val actualIds = leaves.map {
                            it.getStringProperty("eventId")
                        }

                        leaves.size == expectedIds.size &&
                                actualIds.toSet() == expectedIds
                    }
                }
            }
        }
    }


    private fun createSyntheticEarthquakes(): List<Earthquake> {
        val instant = Instant.parse(
            "2026-11-14T12:00:00Z"
        )

        return (0 until 10).map { index ->
            Earthquake(
                id = "synthetic-$index",
                magnitude = 3.5,
                place = "Synthetic test event",
                coordinates = GeoCoordinates(
                    latitude = -33.45 + index * 0.00001,
                    longitude = -71.62 + index * 0.00001
                ),
                depthKilometers = 10.0,
                occurredAt = instant,
                updatedAt = instant,
                detailUrl = null
            )
        }
    }

    private companion object {

        const val CLUSTER_TIMEOUT_MS = 20_000L

        const val LOCAL_STYLE_JSON = """
            {
              "version": 8,
              "name": "Native clustering test",
              "sources": {},
              "layers": []
            }
        """
    }
}
