package com.cgaraydev.seismicexplorer.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapProduct
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Extrae un inventario de productos sin confundir sus metadatos con observaciones. */
class UsgsScientificDetailParser(private val json: Json) {
    data class Parsed(
        val eventId: String,
        val sourceUpdatedAtEpochMillis: Long,
        val availableProductTypes: List<String>,
        val availableProductCount: Int,
        val shakeMap: ShakeMapProduct?
    )

    fun parse(raw: String, expectedEventId: String): Parsed? {
        val root = json.parseToJsonElement(raw) as? JsonObject ?: return null
        if (root["type"]?.jsonPrimitive?.contentOrNull != "Feature") return null
        val id = root["id"]?.jsonPrimitive?.contentOrNull ?: return null
        if (id != expectedEventId) return null
        val properties = root["properties"] as? JsonObject ?: return null
        if (properties["type"]?.jsonPrimitive?.contentOrNull != "earthquake") return null
        val updated = properties["updated"]?.jsonPrimitive?.longOrNull ?: return null
        if (updated <= 0L) return null

        val products = properties["products"] as? JsonObject ?: return null
        var total = 0
        val types = mutableListOf<String>()
        products.forEach { (type, element) ->
            val entries = element as? JsonArray ?: return null
            if (type.isBlank()) return@forEach
            val active = entries.count { item ->
                val product = item as? JsonObject ?: return@count false
                !product["status"].let { status ->
                    status?.jsonPrimitive?.contentOrNull.equals("DELETE", ignoreCase = true)
                }
            }
            if (active > 0) {
                types += type
                total += active
            }
        }
        // Los enlaces se leen del GeoJSON ya descargado y validado, sin ninguna petición extra.
        // Se selecciona la versión activa preferida de ComCat cuando está disponible.
        val shakeMap = UsgsShakeMapResourceParser.extract(products["shakemap"])
        return Parsed(id, updated, types.sorted(), total, shakeMap)
    }
}
