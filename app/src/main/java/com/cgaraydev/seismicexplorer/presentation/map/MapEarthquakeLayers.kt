
package com.cgaraydev.seismicexplorer.presentation.map

import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

internal object MapEarthquakeLayers {

    const val SOURCE_ID = "seismic-earthquakes"

    private const val CLUSTER_LAYER_ID = "seismic-clusters"
    private const val CLUSTER_COUNT_LAYER_ID = "seismic-cluster-count"

    private const val UNKNOWN_LAYER_ID = "seismic-unknown"
    private const val LOW_LAYER_ID = "seismic-low"
    private const val MEDIUM_LAYER_ID = "seismic-medium"
    private const val HIGH_LAYER_ID = "seismic-high"

    fun install(style: Style, geoJson: String) {
        style.addSource(
            GeoJsonSource(
                SOURCE_ID,
                geoJson,
                GeoJsonOptions()
                    .withCluster(true)
                    .withClusterRadius(50)
                    .withClusterMaxZoom(11)
            )
        )

        val magnitude = Expression.get("magnitude")
        val notCluster = Expression.not(
            Expression.has("point_count")
        )

        addEventLayer(
            style = style,
            id = UNKNOWN_LAYER_ID,
            color = "#8492A6",
            radius = 5f,
            filter = Expression.all(
                notCluster,
                Expression.not(Expression.has("magnitude"))
            )
        )

        addEventLayer(
            style = style,
            id = LOW_LAYER_ID,
            color = "#35B4B9",
            radius = 5f,
            filter = Expression.all(
                notCluster,
                Expression.has("magnitude"),
                Expression.lt(magnitude, Expression.literal(2.5))
            )
        )

        addEventLayer(
            style = style,
            id = MEDIUM_LAYER_ID,
            color = "#F2B34A",
            radius = 7f,
            filter = Expression.all(
                notCluster,
                Expression.has("magnitude"),
                Expression.gte(magnitude, Expression.literal(2.5)),
                Expression.lt(magnitude, Expression.literal(4.5))
            )
        )

        addEventLayer(
            style = style,
            id = HIGH_LAYER_ID,
            color = "#D45845",
            radius = 9f,
            filter = Expression.all(
                notCluster,
                Expression.has("magnitude"),
                Expression.gte(magnitude, Expression.literal(4.5))
            )
        )

        style.addLayer(
            CircleLayer(CLUSTER_LAYER_ID, SOURCE_ID)
                .withFilter(Expression.has("point_count"))
                .withProperties(
                    PropertyFactory.circleColor("#24354B"),
                    PropertyFactory.circleRadius(21f),
                    PropertyFactory.circleStrokeColor("#FFFFFF"),
                    PropertyFactory.circleStrokeWidth(2f),
                    PropertyFactory.circleOpacity(0.95f)
                )
        )


        style.addLayer(
            SymbolLayer(CLUSTER_COUNT_LAYER_ID, SOURCE_ID)
                .withFilter(
                    Expression.has("point_count")
                )
                .withProperties(
                    PropertyFactory.textField(
                        Expression.toString(
                            Expression.get("point_count_abbreviated")
                        )
                    ),
                    PropertyFactory.textFont(
                        arrayOf("Noto Sans Regular")
                    ),
                    PropertyFactory.textSize(14f),
                    PropertyFactory.textColor("#FFFFFF"),
                    PropertyFactory.textAllowOverlap(true),
                    PropertyFactory.textIgnorePlacement(true)
                )
        )

    }

    private fun addEventLayer(
        style: Style,
        id: String,
        color: String,
        radius: Float,
        filter: Expression
    ) {
        style.addLayer(
            CircleLayer(id, SOURCE_ID)
                .withFilter(filter)
                .withProperties(
                    PropertyFactory.circleColor(color),
                    PropertyFactory.circleRadius(radius),
                    PropertyFactory.circleStrokeColor("#FFFFFF"),
                    PropertyFactory.circleStrokeWidth(1f),
                    PropertyFactory.circleOpacity(0.9f)
                )
        )
    }
}
