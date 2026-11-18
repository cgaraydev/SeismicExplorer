package com.cgaraydev.seismicexplorer.presentation.home

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import java.time.Instant

/** Resumen calculado sobre la selección observable de Room; nunca extrapola a todo el planeta. */
data class ObservatoryStatistics(
    val eventCount: Int,
    val largestEvent: Earthquake?,
    val magnitudeAtLeast45: Int,
    val magnitudeAtLeast25: Int,
    val maxDepthKilometers: Double?,
    val averageDepthKilometers: Double?,
    val hourly: Boolean,
    val bucketCounts: List<Int>
)

/**
 * El período es una ventana móvil que termina en [endAt].
 * Los eventos en el límite inferior se incluyen; los del límite superior se excluyen.
 * Las barras se calculan desde los eventos disponibles, incluso sin conectividad.
 */
fun calculateObservatoryStatistics(
    earthquakes: List<Earthquake>,
    window: TimeWindow,
    endAt: Instant
): ObservatoryStatistics {
    val hourly = window == TimeWindow.LAST_DAY
    val bucketCount = when (window) {
        TimeWindow.LAST_DAY -> 24
        TimeWindow.LAST_WEEK -> 7
        TimeWindow.LAST_MONTH -> 30
    }
    val bucketSeconds = if (hourly) 3_600L else 86_400L
    val start = endAt.minusSeconds(bucketCount * bucketSeconds)
    val counts = IntArray(bucketCount)

    earthquakes.forEach { earthquake ->
        val occurredAt = earthquake.occurredAt
        if (!occurredAt.isBefore(start) && occurredAt.isBefore(endAt)) {
            val seconds = occurredAt.epochSecond - start.epochSecond
            val index = (seconds / bucketSeconds).toInt()
            if (index in counts.indices) counts[index]++
        }
    }

    val largestEvent = earthquakes
        .asSequence()
        .filter { it.magnitude != null }
        .maxWithOrNull(
            compareBy<Earthquake> { it.magnitude!! }
                .thenBy { it.occurredAt }
                .thenBy { it.id }
        )
    val knownDepths = earthquakes.mapNotNull { it.depthKilometers?.takeIf { depth -> depth >= 0.0 } }
    return ObservatoryStatistics(
        eventCount = earthquakes.size,
        largestEvent = largestEvent,
        magnitudeAtLeast45 = earthquakes.count { (it.magnitude ?: Double.NEGATIVE_INFINITY) >= 4.5 },
        magnitudeAtLeast25 = earthquakes.count { (it.magnitude ?: Double.NEGATIVE_INFINITY) >= 2.5 },
        maxDepthKilometers = knownDepths.maxOrNull(),
        averageDepthKilometers = if (knownDepths.isEmpty()) null else knownDepths.average(),
        hourly = hourly,
        bucketCounts = counts.toList()
    )
}
