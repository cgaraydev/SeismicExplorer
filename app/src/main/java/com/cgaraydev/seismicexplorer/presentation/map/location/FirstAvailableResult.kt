package com.cgaraydev.seismicexplorer.presentation.map.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ejecuta consultas independientes en paralelo y devuelve la primera respuesta no nula.
 * Una respuesta nula no impide que otra consulta todavía pueda resolver la búsqueda.
 * Al terminar, cancela las solicitudes pendientes y respeta la cancelación del llamador.
 */
internal suspend fun <T : Any> firstAvailableResult(
    candidates: List<suspend () -> T?>,
    timeoutMillis: Long
): T? = coroutineScope {
    require(timeoutMillis > 0L) {
        "The location timeout must be positive"
    }

    if (candidates.isEmpty()) return@coroutineScope null

    val responses = Channel<T?>(Channel.UNLIMITED)
    val jobs = candidates.map { candidate ->
        launch {
            // Las excepciones no se silencian aquí. Cada proveedor maneja
            // sus fallos recuperables; las cancelaciones y SecurityException
            // deben propagarse hasta la capa responsable.
            val value = candidate()
            // Otra fuente pudo completar la búsqueda y cerrar el canal.
            // No convertimos esa carrera normal en una excepción del scope.
            responses.trySend(value)
        }
    }

    try {
        withTimeoutOrNull(timeoutMillis) {
            repeat(candidates.size) {
                val value = responses.receive()
                if (value != null) return@withTimeoutOrNull value
            }
            null
        }
    } finally {
        jobs.forEach { it.cancel() }
        responses.close()
    }
}
