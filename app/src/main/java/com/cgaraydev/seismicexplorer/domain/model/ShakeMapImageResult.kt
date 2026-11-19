package com.cgaraydev.seismicexplorer.domain.model

/** El resultado transporta bytes JPEG, no datos de intensidad interpretados. */
sealed interface ShakeMapImageResult {
    class Available(val bytes: ByteArray, val fromCache: Boolean, val isStale: Boolean) : ShakeMapImageResult
    data class Failed(val reason: ShakeMapImageFailure) : ShakeMapImageResult
}

enum class ShakeMapImageFailure {
    INVALID_RESOURCE, NOT_FOUND, CONNECTION, SERVICE, TOO_LARGE, INVALID_IMAGE
}
