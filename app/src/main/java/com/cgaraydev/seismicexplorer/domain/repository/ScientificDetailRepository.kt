package com.cgaraydev.seismicexplorer.domain.repository

import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailResult

/** Consulta explicita: ningun feed ni pantalla existente inicia descargas de GeoJSON Detail. */
interface ScientificDetailRepository {
    suspend fun getDetail(
        eventId: String,
        knownUpdatedAtEpochMillis: Long? = null,
        forceRefresh: Boolean = false
    ): ScientificDetailResult
}
