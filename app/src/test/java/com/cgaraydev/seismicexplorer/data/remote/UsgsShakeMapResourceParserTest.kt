package com.cgaraydev.seismicexplorer.data.remote

import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResourceKind
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsgsShakeMapResourceParserTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefix =
        "https://earthquake.usgs.gov/realtime/product/shakemap/us123/us/1700000004000/"

    @Test fun selectsPreferredActiveProductAndReadsProvenance() {
        val data = products("""
            {"status":"UPDATE","preferredWeight":5,"updateTime":100,"code":"old"},
            {"status":"DELETE","preferredWeight":999,"updateTime":999,"code":"removed"},
            {"status":"UPDATE","preferredWeight":100,"updateTime":200,
             "source":"us","code":"us123","properties":{"map-status":"REVIEWED"},
             "contents":{
               "download/intensity.jpg":{"url":"${prefix}download/intensity.jpg","contentType":"image/jpeg","length":4500},
               "download/grid.xml":{"url":"${prefix}download/grid.xml","length":99000},
               "download/stationlist.json":{"url":"${prefix}download/stationlist.json"}
             }}
        """)
        val result = UsgsShakeMapResourceParser.extract(data)!!
        assertEquals("us", result.source)
        assertEquals("us123", result.code)
        assertEquals(200L, result.updatedAtEpochMillis)
        assertEquals("REVIEWED", result.mapStatus)
        assertEquals(listOf(
            ShakeMapResourceKind.INTENSITY_MAP,
            ShakeMapResourceKind.GRID_XML,
            ShakeMapResourceKind.STATION_OBSERVATIONS
        ), result.resources.map { it.kind })
        assertEquals(4500L, result.resources.first().lengthBytes)
        assertEquals("image/jpeg", result.resources.first().contentType)
    }

    @Test fun officialProductUrlProducesCompatibleJpeg() {
        val imageUrl = "https://earthquake.usgs.gov/product/shakemap/us123/us/1700000004000/download/intensity.jpg"
        val raw = """{
            "type":"Feature","id":"us123","properties":{
                "type":"earthquake","updated":1700000003000,
                "products":{"shakemap":[{"status":"UPDATE","preferredWeight":100,
                    "contents":{"download/intensity.jpg":{"url":"$imageUrl","contentType":"image/jpeg"}}
                }]}
            }
        }"""
        val detail = UsgsScientificDetailParser(json).parse(raw, "us123")!!
        val resource = detail.shakeMap!!.resources.single()
        assertEquals(ShakeMapResourceKind.INTENSITY_MAP, resource.kind)
        assertEquals(imageUrl, resource.url)
        assertTrue(ShakeMapImageUrlPolicy.accepts(resource.url))
    }

    @Test fun pdlExcerptFromUs7000tz37PreservesExactPublishedUrl() {
        // Recorte de GeoJSON con URL y metadatos reportados por USGS para este evento.
        val payload = javaClass.classLoader!!.getResourceAsStream(
            "usgs/shakemap_pdl_excerpt_us7000tz37.json"
        )!!.bufferedReader().use { it.readText() }
        val parsed = UsgsScientificDetailParser(json).parse(payload, "us7000tz37")!!
        val expected = "https://earthquake.usgs.gov/pdl/products/" +
            "urn:usgs-product:us:shakemap:us7000tz37:1795036656107/" +
            "contents/download/intensity.jpg"
        val product = parsed.shakeMap!!
        val resource = product.resources.single()
        assertEquals("us", product.source)
        assertEquals("us7000tz37", product.code)
        assertEquals(ShakeMapResourceKind.INTENSITY_MAP, resource.kind)
        assertEquals(expected, resource.url)
        assertEquals("image/jpeg", resource.contentType)
        assertEquals(78728L, resource.lengthBytes)
        assertTrue(ShakeMapImageUrlPolicy.accepts(resource.url))
    }

    @Test fun pdlParsesDistinctKindsAndRejectsInvalidUrlsWithoutLosingProduct() {
        val prefix = "https://earthquake.usgs.gov/pdl/products/" +
            "urn:usgs-product:ci:shakemap:ci41363167:1795036656107/contents/"
        val data = products("""{
            "status":"UPDATE", "source":"ci", "code":"ci41363167",
            "contents":{
                "download/intensity.jpg":{"url":"${prefix}download/intensity.jpg"},
                "download/grid.xml":{"url":"${prefix}download/grid.xml"},
                "download/stationlist.json":{"url":"${prefix}download/stationlist.json"},
                "download/intensity_overlay.png":{"url":"${prefix}download/intensity_overlay.png?tracking=1"}
            }
        }""")
        val product = UsgsShakeMapResourceParser.extract(data)!!
        assertEquals("ci", product.source)
        assertEquals(listOf(ShakeMapResourceKind.INTENSITY_MAP,
            ShakeMapResourceKind.GRID_XML, ShakeMapResourceKind.STATION_OBSERVATIONS),
            product.resources.map { it.kind })
    }

    @Test fun newestVersionWinsWhenWeightsAreTied() {
        val data = products("""
            {"status":"UPDATE","preferredWeight":100,"updateTime":100,"code":"older"},
            {"status":"UPDATE","preferredWeight":100,"updateTime":300,"code":"newer"}
        """)
        assertEquals("newer", UsgsShakeMapResourceParser.extract(data)?.code)
    }

    @Test fun rejectsUnsafeUrlsAndUnknownPaths() {
        val data = products("""{"status":"UPDATE","contents":{
            "download/intensity.jpg":{"url":"https://earthquake.usgs.gov.evil.test/realtime/product/shakemap/a/download/intensity.jpg"},
            "download/grid.xml":{"url":"http://earthquake.usgs.gov/realtime/product/shakemap/a/download/grid.xml"},
            "download/intensity_overlay.png":{"url":"${prefix}download/intensity_overlay.png?track=x"},
            "download/stationlist.json":{"url":"${prefix}download/wrong.json"},
            "download/unknown.zip":{"url":"${prefix}download/unknown.zip"}
        }}""")
        val result = UsgsShakeMapResourceParser.extract(data)
        assertNotNull(result)
        assertTrue(result!!.resources.isEmpty())
    }

    @Test fun invalidProductsCannotBeExposedAsAvailableShakeMap() {
        assertNull(UsgsShakeMapResourceParser.extract(products("""{"status":"DELETE"}""")))
        assertNull(UsgsShakeMapResourceParser.extract(products("""{
            "status":"UPDATE","properties":{"map-status":"CANCELLED"}
        }""")))
        assertNull(UsgsShakeMapResourceParser.extract(products("""{
            "status":"UPDATE","properties":{"event-type":"SCENARIO"}
        }""")))
        assertNull(UsgsShakeMapResourceParser.extract(null))
    }

    @Test fun activeProductWithoutSupportedFilesStillKeepsItsProvenance() {
        val result = UsgsShakeMapResourceParser.extract(
            products("""{"status":"UPDATE","source":"nc","code":"quakedata"}""")
        )
        assertNotNull(result)
        assertEquals("nc", result?.source)
        assertTrue(result!!.resources.isEmpty())
    }

    @Test fun domainParserMapsRealShakeMapResourcesWithoutFetchingFiles() {
        val raw = """{
            "type":"Feature","id":"us123","properties":{
                "type":"earthquake","updated":1700000003000,
                "products":{"shakemap":[{"status":"UPDATE","preferredWeight":20,
                    "contents":{"download/intensity.jpg":{"url":"${prefix}download/intensity.jpg"}}
                }]}
            }
        }"""
        val parsed = UsgsScientificDetailParser(json).parse(raw, "us123")!!
        assertEquals(listOf("shakemap"), parsed.availableProductTypes)
        assertEquals(ShakeMapResourceKind.INTENSITY_MAP, parsed.shakeMap!!.resources.single().kind)
    }

    private fun products(inner: String) =
        json.parseToJsonElement("[$inner]")
}
