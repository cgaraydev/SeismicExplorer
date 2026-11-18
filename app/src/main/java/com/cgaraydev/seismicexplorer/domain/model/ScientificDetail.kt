package com.cgaraydev.seismicexplorer.domain.model

/** Inventario publicado por USGS: no implica descarga de un producto ni medicion local. */
data class ScientificDetail(
    val eventId: String,
    val sourceUpdatedAtEpochMillis: Long,
    val fetchedAtEpochMillis: Long,
    val availableProductTypes: List<String>,
    val availableProductCount: Int,
    // Descriptor opcional: solo referencia archivos publicados, no los descarga.
    val shakeMap: ShakeMapProduct? = null
)

enum class ScientificDetailSource { NETWORK, CACHE }

enum class ScientificDetailFailure {
    INVALID_EVENT_ID,
    EVENT_NOT_FOUND,
    CONNECTION,
    SERVICE,
    INVALID_RESPONSE,
    RESPONSE_TOO_LARGE
}

sealed interface ScientificDetailResult {
    data class Available(
        val detail: ScientificDetail,
        val source: ScientificDetailSource,
        val isStale: Boolean
    ) : ScientificDetailResult

    data class Failed(val reason: ScientificDetailFailure) : ScientificDetailResult
}
