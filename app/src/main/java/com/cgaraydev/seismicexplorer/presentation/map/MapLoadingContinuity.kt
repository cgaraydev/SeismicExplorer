package com.cgaraydev.seismicexplorer.presentation.map

/**
 * Conserva el ultimo conjunto de marcadores mientras una nueva consulta local
 * esta pendiente. No enmascara resultados vacios reales ni errores de lectura.
 *
 * La instancia pertenece al ViewModel para sobrevivir a las pausas de la
 * coleccion de StateFlow (SharingStarted.WhileSubscribed).
 */
internal class MapLoadingContinuity {

    private var lastSuccessfulState: SeismicMapUiState? = null

    fun accept(incoming: SeismicMapUiState): SeismicMapUiState {
        if (incoming.isLoading) {
            val previous = lastSuccessfulState ?: return incoming

            return incoming.copy(
                geoJson = previous.geoJson,
                eventCount = previous.eventCount,
                eventBounds = previous.eventBounds
            )
        }

        lastSuccessfulState = incoming.takeUnless { it.hasError }
        return incoming
    }
}