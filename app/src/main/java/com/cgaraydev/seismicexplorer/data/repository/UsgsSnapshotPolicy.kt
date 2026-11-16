package com.cgaraydev.seismicexplorer.data.repository

import com.cgaraydev.seismicexplorer.data.local.dao.FeedReconciliationScope
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeCollectionDto
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.RecentWindow
import java.time.Duration

/** Determina si una respuesta permite reconciliar ausencias, no solo incorporar revisiones. */
internal sealed interface UsgsSnapshotDecision {
    data class Trusted(val scope: FeedReconciliationScope) : UsgsSnapshotDecision
    data object Unverifiable : UsgsSnapshotDecision
    data object Invalid : UsgsSnapshotDecision
}

internal object UsgsSnapshotPolicy {
    private const val SUCCESS_STATUS = 200
    private val MAX_RESPONSE_AGE_MS = Duration.ofHours(1).toMillis()
    private val FUTURE_SKEW_MS = Duration.ofMinutes(5).toMillis()

    fun assess(
        response: UsgsEarthquakeCollectionDto,
        mappedEarthquakes: List<Earthquake>,
        window: RecentWindow,
        nowEpochMillis: Long
    ): UsgsSnapshotDecision {
        // Compatibilidad defensiva con clientes/fixtures sin metadata:
        // se incorporan revisiones, pero nunca se eliminan ausencias.
        val metadata = response.metadata ?: return UsgsSnapshotDecision.Unverifiable
        val generated = metadata.generated ?: return UsgsSnapshotDecision.Invalid
        val count = metadata.count ?: return UsgsSnapshotDecision.Invalid
        val status = metadata.status ?: return UsgsSnapshotDecision.Invalid

        if (status != SUCCESS_STATUS || count != response.features.size) {
            return UsgsSnapshotDecision.Invalid
        }
        if (generated <= 0L || generated > nowEpochMillis + FUTURE_SKEW_MS ||
            generated < nowEpochMillis - MAX_RESPONSE_AGE_MS
        ) {
            return UsgsSnapshotDecision.Invalid
        }

        // No considerar como retirada una fila que el mapper no pudo procesar.
        // Otros tipos de evento (por ejemplo, explosiones) sí quedan excluidos.
        if (response.features.any {
                it.id.isNullOrBlank() || it.properties?.type.isNullOrBlank()
            }
        ) {
            return UsgsSnapshotDecision.Invalid
        }
        val expected = response.features
            .filter { it.properties?.type.equals("earthquake", ignoreCase = true) }
            .map { it.id!! }
        val mapped = mappedEarthquakes.map { it.id }
        if (expected.size != mapped.size || expected.toSet() != mapped.toSet()) {
            return UsgsSnapshotDecision.Invalid
        }

        return UsgsSnapshotDecision.Trusted(
            FeedReconciliationScope(
                startEpochMillis = generated - window.duration.toMillis(),
                endEpochMillis = generated
            )
        )
    }
}
