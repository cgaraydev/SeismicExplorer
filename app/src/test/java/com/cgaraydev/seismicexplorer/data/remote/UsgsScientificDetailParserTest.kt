package com.cgaraydev.seismicexplorer.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsgsScientificDetailParserTest {
    private val parser = UsgsScientificDetailParser(Json { ignoreUnknownKeys = true })

    @Test fun identifiesOnlyNonDeletedProducts() {
        val parsed = parser.parse(sample("us123"), "us123")!!
        assertEquals("us123", parsed.eventId)
        assertEquals(1700000003000L, parsed.sourceUpdatedAtEpochMillis)
        assertEquals(listOf("dyfi", "shakemap"), parsed.availableProductTypes)
        assertEquals(3, parsed.availableProductCount)
    }

    @Test fun rejectsOtherEventIdAndNonEarthquakes() {
        assertNull(parser.parse(sample("us456"), "us123"))
        assertNull(parser.parse(sample("us123").replace("\"earthquake\"", "\"quarry blast\""), "us123"))
    }

    @Test fun emptyProductMapIsValid() {
        val raw = """{
            "type": "Feature", "id": "us123",
            "properties": {"type": "earthquake", "updated": 1700000003000, "products": {}}
        }""".trimIndent()
        val parsed = parser.parse(raw, "us123")!!
        assertEquals(emptyList<String>(), parsed.availableProductTypes)
        assertEquals(0, parsed.availableProductCount)
    }

    private fun sample(id: String) = """{
        "type": "Feature", "id": "$id", "properties": {
            "type": "earthquake", "updated": 1700000003000,
            "products": {
                "shakemap": [{"status":"UPDATE"}, {"status":"DELETE"}],
                "dyfi": [{"status":"UPDATE"}, {}]
            }
        }
    }""".trimIndent()
}
