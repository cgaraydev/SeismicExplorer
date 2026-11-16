package com.cgaraydev.seismicexplorer.presentation.map

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serializa directamente a GeoJSON sin materializar un arbol de JsonObject,
 * JsonArray y JsonPrimitive por cada terremoto.
 *
 * StringBuilder es local a cada llamada: no comparte estado entre consultas
 * concurrentes y conserva el orden que entrega Room.
 */
object MapEarthquakeGeoJson {

    const val EMPTY =
        """{"type":"FeatureCollection","features":[]}"""

    fun encode(earthquakes: List<Earthquake>): String {
        if (earthquakes.isEmpty()) return EMPTY

        // Capacidad aproximada: reduce realojamientos para feeds extensos
        // sin reservar una cantidad desproporcionada de memoria.
        val capacity = (64L + earthquakes.size.toLong() * 160L)
            .coerceAtMost(4_000_000L)
            .toInt()

        return buildString(capacity) {
            append("{\"type\":\"FeatureCollection\",\"features\":[")

            earthquakes.forEachIndexed { index, earthquake ->
                if (index > 0) append(',')

                // El serializador de Kotlinx aplica el escape JSON correcto
                // a comillas, backslashes, controles y caracteres Unicode.
                val encodedId = Json.encodeToString(earthquake.id)

                append("{\"type\":\"Feature\",\"id\":")
                append(encodedId)
                append(",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
                append(earthquake.coordinates.longitude)
                append(',')
                append(earthquake.coordinates.latitude)
                append("]},\"properties\":{\"eventId\":")
                append(encodedId)

                earthquake.magnitude?.let { magnitude ->
                    append(",\"magnitude\":")
                    append(magnitude)
                }

                earthquake.place?.let { place ->
                    append(",\"place\":")
                    append(Json.encodeToString(place))
                }

                append("}}")
            }

            append("]}")
        }
    }
}
