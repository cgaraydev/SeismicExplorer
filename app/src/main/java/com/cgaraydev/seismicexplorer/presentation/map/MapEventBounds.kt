
package com.cgaraydev.seismicexplorer.presentation.map

import com.cgaraydev.seismicexplorer.domain.model.Earthquake

data class MapEventBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double
) {
    val isSingleLocation: Boolean
        get() = south == north && west == east

    companion object {

        fun fromEarthquakes(
            earthquakes: List<Earthquake>
        ): MapEventBounds? {
            if (earthquakes.isEmpty()) return null

            val south = earthquakes.minOf {
                it.coordinates.latitude
            }

            val north = earthquakes.maxOf {
                it.coordinates.latitude
            }

            // Normalizar longitudes a [0, 360).
            val longitudes = earthquakes
                .map {
                    (it.coordinates.longitude + 360.0) % 360.0
                }
                .sorted()

            // El mayor espacio sin eventos permite
            // identificar el arco mínimo que contiene
            // todas las longitudes.
            var largestGap = -1.0
            var arcStart = longitudes.first()

            for (index in longitudes.indices) {
                val current = longitudes[index]

                val next = if (index == longitudes.lastIndex) {
                    longitudes.first() + 360.0
                } else {
                    longitudes[index + 1]
                }

                val gap = next - current

                if (gap > largestGap) {
                    largestGap = gap
                    arcStart = if (index == longitudes.lastIndex) {
                        longitudes.first()
                    } else {
                        longitudes[index + 1]
                    }
                }
            }

            val west = if (arcStart > 180.0) {
                arcStart - 360.0
            } else {
                arcStart
            }

            val east = west + (360.0 - largestGap)

            return MapEventBounds(
                south = south,
                west = west,
                north = north,
                east = east
            )
        }
    }
}
