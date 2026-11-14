package com.cgaraydev.seismicexplorer.presentation.map

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject

object MapEarthquakeGeoJson {

    const val EMPTY =
        """{"type":"FeatureCollection","features":[]}"""

    fun encode(earthquakes: List<Earthquake>): String {
        if (earthquakes.isEmpty()) {
            return EMPTY
        }

        return buildJsonObject {
            put("type", "FeatureCollection")

            putJsonArray("features") {
                earthquakes.forEach { earthquake ->
                    addJsonObject {
                        put("type", "Feature")
                        put("id", earthquake.id)

                        putJsonObject("geometry") {
                            put("type", "Point")

                            putJsonArray("coordinates") {
                                add(earthquake.coordinates.longitude)
                                add(earthquake.coordinates.latitude)
                            }
                        }

                        putJsonObject("properties") {
                            put("eventId", earthquake.id)

                            earthquake.magnitude?.let { magnitude ->
                                put("magnitude", magnitude)
                            }

                            earthquake.place?.let { place ->
                                put("place", place)
                            }
                        }
                    }
                }
            }
        }.toString()
    }
}