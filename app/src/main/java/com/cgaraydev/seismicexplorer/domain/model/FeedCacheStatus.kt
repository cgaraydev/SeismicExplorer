package com.cgaraydev.seismicexplorer.domain.model

sealed interface FeedCacheStatus {

    // Todavía no se ha consultado el estado.
    data object Unknown : FeedCacheStatus

    // No existe una sincronización confirmada
    // para este feed. Puede haber eventos en Room.
    data object Unverified : FeedCacheStatus

    data class Synced(
        val lastSuccessEpochMillis: Long,
        val isFresh: Boolean
    ) : FeedCacheStatus
}