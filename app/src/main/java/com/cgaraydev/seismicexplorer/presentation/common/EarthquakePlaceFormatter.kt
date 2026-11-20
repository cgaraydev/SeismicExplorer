package com.cgaraydev.seismicexplorer.presentation.common

import java.util.Locale

/**
 * Presentación conservadora de los lugares publicados por USGS.
 * No modifica el valor original, los nombres propios desconocidos ni las coordenadas.
 */
object EarthquakePlaceFormatter {
    private val distanceFromPlace = Regex(
        """^(\d+(?:[.,]\d+)?)\s*(km|mi)\s+(NNE|NNW|ENE|ESE|SSE|SSW|WSW|WNW|NE|NW|SE|SW|N|S|E|W)\s+of\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val directionOfPlace = Regex(
        """^(NNE|NNW|ENE|ESE|SSE|SSW|WSW|WNW|NE|NW|SE|SW|N|S|E|W)\s+of\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val writtenDirectionOfPlace = Regex(
        """^(northeast|northwest|southeast|southwest|north|south|east|west)\s+of\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val directionLabels = mapOf(
        "N" to "N", "NNE" to "NNE", "NE" to "NE", "ENE" to "ENE",
        "E" to "E", "ESE" to "ESE", "SE" to "SE", "SSE" to "SSE",
        "S" to "S", "SSW" to "SSO", "SW" to "SO", "WSW" to "OSO",
        "W" to "O", "WNW" to "ONO", "NW" to "NO", "NNW" to "NNO"
    )
    private val writtenDirections = mapOf(
        "north" to "norte", "south" to "sur", "east" to "este", "west" to "oeste",
        "northeast" to "noreste", "northwest" to "noroeste",
        "southeast" to "sureste", "southwest" to "suroeste"
    )

    private val nearDirectionalCoast = Regex(
        """^near the (north|south|east|west) coast of (.+)$""", RegexOption.IGNORE_CASE
    )
    private val offCoast = Regex("""^off the coast of (.+)$""", RegexOption.IGNORE_CASE)
    private val nearCoast = Regex("""^near the coast of (.+)$""", RegexOption.IGNORE_CASE)
    private val coast = Regex("""^coast of (.+)$""", RegexOption.IGNORE_CASE)
    private val region = Regex("""^(.+?) region$""", RegexOption.IGNORE_CASE)

    fun format(raw: String?, locale: Locale): String? {
        val place = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (locale.language != "es") return place

        distanceFromPlace.matchEntire(place)?.let { match ->
            val (amount, unit, bearing, name) = match.destructured
            val direction = directionLabels[bearing.uppercase(Locale.ROOT)] ?: return place
            return "$amount $unit al $direction de ${localizedName(name)}"
        }
        directionOfPlace.matchEntire(place)?.let { match ->
            val direction = directionLabels[match.groupValues[1].uppercase(Locale.ROOT)]
                ?: return place
            return "al $direction de ${localizedName(match.groupValues[2])}"
        }
        writtenDirectionOfPlace.matchEntire(place)?.let { match ->
            val direction = writtenDirections[match.groupValues[1].lowercase(Locale.ROOT)]
                ?: return place
            val originalName = match.groupValues[2]
            val translatedName = localizedName(originalName)
            // No generar híbridos como «al sur de the Unknown Islands».
            // Las expresiones con artículo inglés se traducen solo si conocemos el topónimo.
            if (originalName.startsWith("the ", ignoreCase = true) &&
                translatedName.equals(originalName, ignoreCase = true)
            ) return place
            return "al $direction de $translatedName"
        }
        nearDirectionalCoast.matchEntire(place)?.let { match ->
            val adjective = when (match.groupValues[1].lowercase(Locale.ROOT)) {
                "north" -> "norte"
                "south" -> "sur"
                "east" -> "este"
                else -> "oeste"
            }
            return "cerca de la costa $adjective de ${localizedName(match.groupValues[2])}"
        }
        offCoast.matchEntire(place)?.let {
            return "frente a la costa de ${localizedName(it.groupValues[1])}"
        }
        nearCoast.matchEntire(place)?.let {
            return "cerca de la costa de ${localizedName(it.groupValues[1])}"
        }
        coast.matchEntire(place)?.let {
            return "Costa de ${localizedName(it.groupValues[1])}"
        }
        region.matchEntire(place)?.let {
            val name = it.groupValues[1]
            return if (name.equals("South Sandwich Islands", ignoreCase = true)) {
                "región de las islas Sandwich del Sur"
            } else {
                "región de ${localizedName(name)}"
            }
        }
        if (place.equals("South Sandwich Islands", ignoreCase = true)) {
            return "islas Sandwich del Sur"
        }
        return localizedName(place)
    }

    /** Solo traducciones geográficas inequívocas, no un diccionario libre de topónimos. */
    private fun localizedName(raw: String): String {
        val name = raw.trim()
        if (name.endsWith(", Japan", ignoreCase = true)) {
            return name.dropLast(", Japan".length) + ", Japón"
        }
        if (name.endsWith(", U.S. Virgin Islands", ignoreCase = true)) {
            return name.dropLast(", U.S. Virgin Islands".length) + ", Islas Vírgenes de EE. UU."
        }
        return when (name.lowercase(Locale.ROOT)) {
            "the loyalty islands" -> "las islas de la Lealtad"
            "loyalty islands" -> "islas de la Lealtad"
            "the south sandwich islands" -> "las islas Sandwich del Sur"
            "south georgia island" -> "la isla Georgia del Sur"
            "south sandwich islands" -> "islas Sandwich del Sur"
            "south sandwich islands region" -> "región de las islas Sandwich del Sur"
            "south atlantic ocean" -> "océano Atlántico Sur"
            "north atlantic ocean" -> "océano Atlántico Norte"
            "pacific ocean" -> "océano Pacífico"
            "pacific-antarctic ridge" -> "Dorsal del Pacífico-Antártico"
            "central east pacific rise" -> "Dorsal del Pacífico Oriental Central"
            "u.s. virgin islands" -> "Islas Vírgenes de EE. UU."
            "japan" -> "Japón"
            "central alaska" -> "Alaska central"
            "southern alaska" -> "sur de Alaska"
            "northern alaska" -> "norte de Alaska"
            else -> name
        }
    }
}
