package com.cgaraydev.seismicexplorer.domain.model

sealed interface RefreshResult {

    data object Success : RefreshResult

    data class Failure(
        val reason: RefreshFailure
    ) : RefreshResult
}

enum class RefreshFailure {
    CONNECTION,
    SERVICE,
    INVALID_RESPONSE
}