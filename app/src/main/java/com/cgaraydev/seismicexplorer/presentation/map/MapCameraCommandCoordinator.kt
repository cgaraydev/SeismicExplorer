package com.cgaraydev.seismicexplorer.presentation.map

/**
 * Coordinación de órdenes de cámara que todavía no se han ejecutado.
 *
 * La última orden sustituye a la anterior. Una vez ejecutada, se consume
 * para evitar que un cambio de layout o una nueva carga la reproduzcan.
 */
internal enum class MapCameraCommandKind {
    RESET,
    FIT_EVENTS,
    CENTER_USER
}

internal class MapCameraCommandCoordinator {

    private var pending: MapCameraCommandKind? = null

    fun request(command: MapCameraCommandKind) {
        pending = command
    }

    fun isPending(command: MapCameraCommandKind): Boolean =
        pending == command

    fun consume(command: MapCameraCommandKind): Boolean {
        if (pending != command) return false
        pending = null
        return true
    }

    fun clear() {
        pending = null
    }
}
